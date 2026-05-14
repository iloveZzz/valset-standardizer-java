package com.yss.valset.task.infrastructure.gateway;

import com.yss.cloud.dto.result.PageResult;
import com.yss.valset.domain.gateway.ValsetFileInfoGateway;
import com.yss.valset.domain.model.ValsetFileInfo;
import com.yss.valset.task.application.command.OutsourcedDataTaskQueryCommand;
import com.yss.valset.task.application.dto.OutsourcedDataTaskBatchDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskStageSummaryDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskSummaryDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskStepDTO;
import com.yss.valset.task.application.port.OutsourcedDataTaskGateway;
import com.yss.valset.task.application.service.workflow.WorkflowRuntimeCatalog;
import com.yss.valset.task.domain.model.OutsourcedDataTaskStage;
import com.yss.valset.task.domain.model.OutsourcedDataTaskStatus;
import com.yss.valset.task.infrastructure.mapper.OutsourcedDataTaskSummaryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Spring Batch 版估值表解析任务读模型网关。
 *
 * <p>
 * 这里只负责把 Batch 元数据回放成页面所需的批次列表、详情和步骤明细。
 * 不再读取或写入任何旧的 {@code t_outsourced_*} 表。
 * </p>
 */
@Primary
@Repository
@RequiredArgsConstructor
public class OutsourcedDataTaskGatewayImpl implements OutsourcedDataTaskGateway {

    private static final String JOB_NAME = "valuationParseJob";

    private static final int DEFAULT_PAGE_SIZE = 10;

    private static final int MAX_PAGE_SIZE = 200;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String PAGE_WORKFLOW_CODE = "VALUATION_PARSE";

    private static final long SNAPSHOT_CACHE_TTL_MILLIS = 3000L;

    private static final long EXECUTION_INDEX_CACHE_TTL_MILLIS = 3000L;

    private static final List<OutsourcedDataTaskStage> PAGE_STAGE_SEQUENCE = Arrays.asList(
            OutsourcedDataTaskStage.FILE_PARSE,
            OutsourcedDataTaskStage.STRUCTURE_STANDARDIZE,
            OutsourcedDataTaskStage.STANDARD_LANDING);

    private static final Map<String, OutsourcedDataTaskStage> PAGE_STAGE_ALIAS_MAP = new LinkedHashMap<>();

    static {
        PAGE_STAGE_ALIAS_MAP.put("RAW_DATA_EXTRACT", OutsourcedDataTaskStage.FILE_PARSE);
        PAGE_STAGE_ALIAS_MAP.put("PARSE", OutsourcedDataTaskStage.FILE_PARSE);
        PAGE_STAGE_ALIAS_MAP.put("SUBJECT_RECOGNIZE", OutsourcedDataTaskStage.STANDARD_LANDING);
        PAGE_STAGE_ALIAS_MAP.put("DATA_PROCESSING", OutsourcedDataTaskStage.STANDARD_LANDING);
        PAGE_STAGE_ALIAS_MAP.put("VERIFY_ARCHIVE", OutsourcedDataTaskStage.STANDARD_LANDING);
    }

    private final ValsetFileInfoGateway valsetFileInfoGateway;
    private final JobExplorer springBatchJobExplorer;
    private final OutsourcedDataTaskSummaryMapper outsourcedDataTaskSummaryMapper;
    private final Map<String, CachedTaskSnapshotIndex> snapshotIndexCache = new ConcurrentHashMap<>();
    private final Map<String, CachedExecutionIndex> executionIndexCache = new ConcurrentHashMap<>();

    private WorkflowRuntimeCatalog stageCatalog;

    @org.springframework.beans.factory.annotation.Autowired
    public void setStageCatalog(WorkflowRuntimeCatalog stageCatalog) {
        if (stageCatalog != null) {
            this.stageCatalog = stageCatalog;
        }
    }

    /**
     * 分页查询任务列表。
     */
    @Override
    public PageResult<OutsourcedDataTaskBatchDTO> pageTasks(OutsourcedDataTaskQueryCommand query) {
        int pageIndex = normalizePageIndex(query == null ? null : query.getPageIndex());
        int pageSize = normalizePageSize(query == null ? null : query.getPageSize());
        if (outsourcedDataTaskSummaryMapper == null || shouldUseSpringBatchSnapshot(query)) {
            TaskSnapshotIndex snapshotIndex = loadTaskSnapshotIndex(query);
            List<OutsourcedDataTaskBatchDTO> records = snapshotIndex.records;
            long total = records.size();
            int fromIndex = Math.min((pageIndex - 1) * pageSize, records.size());
            int toIndex = Math.min(fromIndex + pageSize, records.size());
            List<OutsourcedDataTaskBatchDTO> pageRecords = fromIndex >= toIndex
                    ? java.util.Collections.emptyList()
                    : new ArrayList<>(records.subList(fromIndex, toIndex));
            return PageResult.of(pageRecords, total, pageSize, pageIndex);
        }
        long offset = (long) (pageIndex - 1) * pageSize;
        Long total = outsourcedDataTaskSummaryMapper.countPageTasks(query);
        List<OutsourcedDataTaskBatchDTO> pageRecords = total == null || total <= 0
                ? java.util.Collections.emptyList()
                : outsourcedDataTaskSummaryMapper.selectPageTasks(query, offset, pageSize);
        if (pageRecords != null && !pageRecords.isEmpty()) {
            Map<String, JobExecution> executionIndex = loadSpringBatchExecutionIndex();
            pageRecords = pageRecords.stream()
                    .map(row -> enrichExecutionTime(row, executionIndex.get(row == null ? null : row.getBatchId())))
                    .collect(Collectors.toList());
        }
        return PageResult.of(pageRecords == null ? java.util.Collections.emptyList() : pageRecords,
                total == null ? 0L : total,
                pageSize,
                pageIndex);
    }

    /**
     * 汇总当前查询条件下的任务状态。
     */
    @Override
    public OutsourcedDataTaskSummaryDTO summary(OutsourcedDataTaskQueryCommand query) {
        if (outsourcedDataTaskSummaryMapper == null || shouldUseSpringBatchSnapshot(query)) {
            TaskSnapshotIndex snapshotIndex = loadTaskSnapshotIndex(query);
            OutsourcedDataTaskSummaryDTO summary = new OutsourcedDataTaskSummaryDTO();
            summary.setTotalCount(snapshotIndex.records.size());
            summary.setRunningCount(countByStatus(snapshotIndex.records, OutsourcedDataTaskStatus.RUNNING));
            summary.setSuccessCount(countByStatus(snapshotIndex.records, OutsourcedDataTaskStatus.SUCCESS));
            summary.setFailedCount(snapshotIndex.records.stream()
                    .filter(batch -> isAnyStatus(batch, OutsourcedDataTaskStatus.FAILED, OutsourcedDataTaskStatus.BLOCKED))
                    .count());
            summary.setStepSummaries(buildStepSummaries(snapshotIndex.records));
            summary.setStageCatalog(summary.getStepSummaries());
            fillWorkflowMetadata(summary);
            return summary;
        }
        OutsourcedDataTaskSummaryDTO summary = outsourcedDataTaskSummaryMapper == null
                ? new OutsourcedDataTaskSummaryDTO()
                : outsourcedDataTaskSummaryMapper.selectSummary(query);
        if (summary == null) {
            summary = new OutsourcedDataTaskSummaryDTO();
        }
        List<OutsourcedDataTaskStageSummaryDTO> stageSummaries = outsourcedDataTaskSummaryMapper == null
                ? java.util.Collections.emptyList()
                : outsourcedDataTaskSummaryMapper.selectStageSummaries(query);
        summary.setStepSummaries(stageSummaries);
        summary.setStageCatalog(stageSummaries);
        fillWorkflowMetadata(summary);
        return summary;
    }

    /**
     * 返回当前查询条件下的任务快照。
     */
    @Override
    public List<OutsourcedDataTaskBatchDTO> listTasks(OutsourcedDataTaskQueryCommand query) {
        return loadTaskSnapshotIndex(query).records;
    }

    /**
     * 按批次号读取单个任务详情。
     */
    @Override
    public Optional<OutsourcedDataTaskBatchDTO> findTask(String batchId) {
        if (!StringUtils.hasText(batchId)) {
            return Optional.empty();
        }
        return findSpringBatchTask(batchId);
    }

    /**
     * 按批次号读取步骤列表。
     */
    @Override
    public List<OutsourcedDataTaskStepDTO> listSteps(String batchId) {
        if (!StringUtils.hasText(batchId) || springBatchJobExplorer == null) {
            return java.util.Collections.emptyList();
        }
        JobExecution execution = loadSpringBatchExecutionIndex().get(batchId);
        return execution == null ? java.util.Collections.emptyList() : toSpringBatchSteps(execution);
    }

    /**
     * 统一加载任务快照。
     */
    private TaskSnapshotIndex loadTaskSnapshotIndex(OutsourcedDataTaskQueryCommand query) {
        String cacheKey = buildSnapshotCacheKey(query);
        long now = System.currentTimeMillis();
        CachedTaskSnapshotIndex cached = snapshotIndexCache.get(cacheKey);
        if (cached != null && !cached.isExpired(now)) {
            return cached.snapshot;
        }
        TaskSnapshotIndex snapshot = buildTaskSnapshotIndex(query);
        snapshotIndexCache.put(cacheKey, new CachedTaskSnapshotIndex(snapshot, now));
        return snapshot;
    }

    private TaskSnapshotIndex buildTaskSnapshotIndex(OutsourcedDataTaskQueryCommand query) {
        if (springBatchJobExplorer == null) {
            return TaskSnapshotIndex.empty();
        }
        Map<String, JobExecution> executionIndex = loadSpringBatchExecutionIndex();
        if (executionIndex.isEmpty()) {
            return TaskSnapshotIndex.empty();
        }
        Map<Long, ValsetFileInfo> fileInfoCache = new HashMap<>();
        List<OutsourcedDataTaskBatchDTO> records = executionIndex.values().stream()
                .map(execution -> {
                    return toSpringBatchBatchDTO(execution, fileInfoCache);
                })
                .filter(Objects::nonNull)
                .filter(row -> matchesSnapshot(row, query))
                .sorted((left, right) -> compareDateTime(parseDateTime(right.getStartedAt()), parseDateTime(left.getStartedAt())))
                .collect(Collectors.toList());
        return new TaskSnapshotIndex(records, executionIndex);
    }

    /**
     * 读取单个 Spring Batch 执行对应的任务快照。
     */
    private Optional<OutsourcedDataTaskBatchDTO> findSpringBatchTask(String batchId) {
        JobExecution execution = loadSpringBatchExecutionIndex().get(batchId);
        return execution == null ? Optional.empty() : Optional.ofNullable(toSpringBatchBatchDTO(execution));
    }

    /**
     * 按批次号从 Spring Batch 元数据中定位执行记录。
     */
    private Optional<JobExecution> findSpringBatchExecution(String batchId) {
        return Optional.ofNullable(loadSpringBatchExecutionIndex().get(batchId));
    }

    private static String buildSnapshotCacheKey(OutsourcedDataTaskQueryCommand query) {
        if (query == null) {
            return "default";
        }
        return String.join("|",
                safeCacheValue(query.getBatchId()),
                safeCacheValue(query.getTaskDate()),
                safeCacheValue(query.getBusinessDate()),
                safeCacheValue(query.getManagerName()),
                safeCacheValue(query.getProductKeyword()),
                safeCacheValue(query.getStage()),
                safeCacheValue(query.getStatus()),
                safeCacheValue(query.getSourceType()),
                safeCacheValue(query.getErrorType()));
    }

    private static String safeCacheValue(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private Map<String, JobExecution> loadSpringBatchExecutionIndex() {
        return loadSpringBatchExecutionIndex("default");
    }

    private Map<String, JobExecution> loadSpringBatchExecutionIndex(String cacheKey) {
        long now = System.currentTimeMillis();
        CachedExecutionIndex cached = executionIndexCache.get(cacheKey);
        if (cached != null && !cached.isExpired(now)) {
            return cached.executionByBatchId;
        }
        Map<String, JobExecution> executionByBatchId = buildSpringBatchExecutionIndex();
        executionIndexCache.put(cacheKey, new CachedExecutionIndex(executionByBatchId, now));
        return executionByBatchId;
    }

    private Map<String, JobExecution> buildSpringBatchExecutionIndex() {
        List<JobExecution> executions = loadSpringBatchExecutions();
        Map<String, JobExecution> executionByBatchId = new LinkedHashMap<>();
        for (JobExecution execution : executions) {
            String batchId = resolveSpringBatchBatchId(execution);
            if (StringUtils.hasText(batchId)) {
                executionByBatchId.put(batchId, execution);
            }
        }
        return executionByBatchId;
    }

    /**
     * 加载估值解析作业的最新执行记录。
     */
    private List<JobExecution> loadSpringBatchExecutions() {
        List<JobExecution> executions = new ArrayList<>();
        List<JobInstance> instances = springBatchJobExplorer.getJobInstances(JOB_NAME, 0, Integer.MAX_VALUE);
        if (instances == null || instances.isEmpty()) {
            return executions;
        }
        for (JobInstance instance : instances) {
            List<JobExecution> jobExecutions = springBatchJobExplorer.getJobExecutions(instance);
            if (jobExecutions == null || jobExecutions.isEmpty()) {
                continue;
            }
            JobExecution latest = jobExecutions.stream()
                    .filter(Objects::nonNull)
                    .reduce((left, right) -> compareExecutionTime(left, right) >= 0 ? left : right)
                    .orElse(null);
            if (latest != null) {
                executions.add(latest);
            }
        }
        return executions;
    }

    private int compareExecutionTime(JobExecution left, JobExecution right) {
        LocalDateTime leftTime = toLocalDateTime(left == null ? null : left.getStartTime());
        LocalDateTime rightTime = toLocalDateTime(right == null ? null : right.getStartTime());
        return compareDateTime(leftTime, rightTime);
    }

    private int compareDateTime(LocalDateTime left, LocalDateTime right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        return left.compareTo(right);
    }

    /**
     * 把单个 Spring Batch 执行记录转换成批次列表 DTO。
     */
    private OutsourcedDataTaskBatchDTO toSpringBatchBatchDTO(JobExecution execution) {
        return toSpringBatchBatchDTO(execution, new HashMap<>());
    }

    private OutsourcedDataTaskBatchDTO toSpringBatchBatchDTO(JobExecution execution,
            Map<Long, ValsetFileInfo> fileInfoCache) {
        if (execution == null || execution.getJobParameters() == null) {
            return null;
        }
        JobParameters parameters = execution.getJobParameters();
        Long taskId = parameters.getLong("taskId", null);
        if (taskId == null) {
            return null;
        }
        Long fileId = parameters.getLong("fileId", null);
        String businessKey = parameters.getString("businessKey", null);
        ValsetFileInfo fileInfo = resolveBatchFileInfo(fileId, fileInfoCache);
        OutsourcedDataTaskBatchDTO dto = new OutsourcedDataTaskBatchDTO();
        dto.setBatchId(resolveSpringBatchBatchId(execution));
        dto.setBatchName(firstText(businessKey, dto.getBatchId()));
        dto.setBusinessDate(resolveSpringBatchBusinessDate(fileInfo, execution));
        dto.setFileId(fileId == null ? null : String.valueOf(fileId));
        dto.setFilesysFileId(resolveFilesysFileId(fileInfo));
        dto.setOriginalFileName(fileInfo == null ? null : fileInfo.getFileNameOriginal());
        dto.setProductCode(null);
        dto.setProductName(null);
        dto.setManagerName(null);
        dto.setSourceType(parameters.getString("taskType", null));
        dto.setCurrentStage(resolveSpringBatchCurrentStage(execution));
        dto.setCurrentStep(dto.getCurrentStage());
        dto.setCurrentStageName(stageLabel(dto.getCurrentStage()));
        dto.setCurrentStepName(dto.getCurrentStageName());
        dto.setStatus(resolveSpringBatchStatus(execution));
        dto.setStatusName(statusLabel(dto.getStatus()));
        dto.setProgress(resolveSpringBatchProgress(execution));
        dto.setStartedAt(formatDateTime(toLocalDateTime(execution.getStartTime())));
        dto.setEndedAt(formatDateTime(toLocalDateTime(execution.getEndTime())));
        dto.setDurationMs(durationMs(toLocalDateTime(execution.getStartTime()), toLocalDateTime(execution.getEndTime())));
        dto.setDurationText(formatDuration(dto.getDurationMs(), dto.getStatus()));
        dto.setLastErrorCode(resolveSpringBatchErrorCode(execution));
        dto.setLastErrorMessage(resolveSpringBatchErrorMessage(execution));
        return dto;
    }

    private OutsourcedDataTaskBatchDTO enrichExecutionTime(OutsourcedDataTaskBatchDTO row, JobExecution execution) {
        if (row == null || execution == null) {
            return row;
        }
        row.setStartedAt(formatDateTime(toLocalDateTime(execution.getStartTime())));
        row.setEndedAt(formatDateTime(toLocalDateTime(execution.getEndTime())));
        row.setDurationMs(durationMs(toLocalDateTime(execution.getStartTime()), toLocalDateTime(execution.getEndTime())));
        row.setDurationText(formatDuration(row.getDurationMs(), row.getStatus()));
        return row;
    }

    /**
     * 把 Spring Batch 的步骤执行映射成步骤明细 DTO。
     */
    private List<OutsourcedDataTaskStepDTO> toSpringBatchSteps(JobExecution execution) {
        if (execution == null || execution.getStepExecutions() == null || execution.getStepExecutions().isEmpty()) {
            return java.util.Collections.emptyList();
        }
        List<OutsourcedDataTaskStepDTO> steps = new ArrayList<>();
        List<org.springframework.batch.core.StepExecution> orderedSteps = execution.getStepExecutions().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing((org.springframework.batch.core.StepExecution step) ->
                        pageStageOrder(resolveSpringBatchStageCode(step.getStepName())))
                        .thenComparing(org.springframework.batch.core.StepExecution::getId,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
        int index = 0;
        for (org.springframework.batch.core.StepExecution stepExecution : orderedSteps) {
            index++;
            OutsourcedDataTaskStepDTO dto = new OutsourcedDataTaskStepDTO();
            dto.setStepId(resolveSpringBatchBatchId(execution) + "-" + stepExecution.getStepName());
            dto.setBatchId(resolveSpringBatchBatchId(execution));
            dto.setStage(resolveSpringBatchStageCode(stepExecution.getStepName()));
            dto.setStep(resolveSpringBatchStageCode(stepExecution.getStepName()));
            dto.setStageName(stageLabel(dto.getStage()));
            dto.setStepName(dto.getStageName());
            dto.setTaskId(String.valueOf(execution.getJobParameters().getLong("taskId", null)));
            dto.setTaskType(execution.getJobParameters().getString("taskType", null));
            dto.setRunNo(index);
            dto.setCurrentFlag(isCurrentSpringBatchStep(execution, stepExecution));
            dto.setTriggerMode("SPRING_BATCH");
            dto.setTriggerModeName("Spring Batch");
            dto.setStatus(stepExecution.getStatus() == null ? null : stepExecution.getStatus().name());
            dto.setStatusName(statusLabel(dto.getStatus()));
            dto.setProgress(resolveSpringBatchStepProgress(stepExecution));
            dto.setStartedAt(formatDateTime(toLocalDateTime(stepExecution.getStartTime())));
            dto.setEndedAt(formatDateTime(toLocalDateTime(stepExecution.getEndTime())));
            dto.setDurationMs(durationMs(toLocalDateTime(stepExecution.getStartTime()), toLocalDateTime(stepExecution.getEndTime())));
            dto.setDurationText(formatDuration(dto.getDurationMs(), dto.getStatus()));
            dto.setInputSummary(stepExecution.getExecutionContext() == null ? null
                    : stepExecution.getExecutionContext().getString("inputSummary", null));
            dto.setOutputSummary(stepExecution.getExecutionContext() == null ? null
                    : stepExecution.getExecutionContext().getString("outputSummary", null));
            dto.setErrorCode(resolveSpringBatchStepErrorCode(stepExecution));
            dto.setErrorMessage(resolveSpringBatchStepErrorMessage(stepExecution));
            dto.setLogRef("spring-batch:" + execution.getId() + ":" + stepExecution.getStepName());
            steps.add(dto);
        }
        return steps;
    }

    private static boolean isCurrentSpringBatchStep(JobExecution execution,
            org.springframework.batch.core.StepExecution stepExecution) {
        if (execution == null || stepExecution == null) {
            return false;
        }
        BatchStatus jobStatus = execution.getStatus();
        if (jobStatus == null || jobStatus.isUnsuccessful() || jobStatus == BatchStatus.COMPLETED) {
            return false;
        }
        BatchStatus stepStatus = stepExecution.getStatus();
        return stepStatus == BatchStatus.STARTING || stepStatus == BatchStatus.STARTED;
    }

    private String resolveSpringBatchBatchId(JobExecution execution) {
        if (execution == null || execution.getJobParameters() == null) {
            return null;
        }
        JobParameters parameters = execution.getJobParameters();
        return resolveBatchId(parameters.getLong("fileId", null), parameters.getString("businessKey", null),
                parameters.getLong("taskId", null));
    }

    private String resolveSpringBatchStatus(JobExecution execution) {
        if (execution == null || execution.getStatus() == null) {
            return OutsourcedDataTaskStatus.PENDING.name();
        }
        BatchStatus status = execution.getStatus();
        if (status == BatchStatus.STARTING || status == BatchStatus.STARTED || status == BatchStatus.STOPPING) {
            return OutsourcedDataTaskStatus.RUNNING.name();
        }
        if (status == BatchStatus.FAILED) {
            return OutsourcedDataTaskStatus.FAILED.name();
        }
        if (status == BatchStatus.STOPPED) {
            return OutsourcedDataTaskStatus.STOPPED.name();
        }
        if (status == BatchStatus.COMPLETED) {
            return OutsourcedDataTaskStatus.SUCCESS.name();
        }
        return OutsourcedDataTaskStatus.PENDING.name();
    }

    private int resolveSpringBatchProgress(JobExecution execution) {
        if (execution == null || execution.getStatus() == null) {
            return 0;
        }
        if (execution.getStatus() == BatchStatus.COMPLETED) {
            return 100;
        }
        if (execution.getStatus() == BatchStatus.FAILED) {
            return 66;
        }
        if (execution.getStatus() == BatchStatus.STARTED || execution.getStatus() == BatchStatus.STARTING) {
            return 50;
        }
        return 0;
    }

    private int resolveSpringBatchStepProgress(org.springframework.batch.core.StepExecution stepExecution) {
        if (stepExecution == null || stepExecution.getStatus() == null) {
            return 0;
        }
        if (stepExecution.getStatus() == BatchStatus.COMPLETED) {
            return 100;
        }
        if (stepExecution.getStatus() == BatchStatus.FAILED) {
            return 66;
        }
        if (stepExecution.getStatus() == BatchStatus.STARTED || stepExecution.getStatus() == BatchStatus.STARTING) {
            return 50;
        }
        return 0;
    }

    /**
     * 解析 Spring Batch 步骤错误编码。
     */
    private String resolveSpringBatchStepErrorCode(org.springframework.batch.core.StepExecution stepExecution) {
        if (stepExecution == null) {
            return null;
        }
        if (stepExecution.getExitStatus() != null && StringUtils.hasText(stepExecution.getExitStatus().getExitCode())) {
            return stepExecution.getExitStatus().getExitCode();
        }
        return stepExecution.getStatus() == null ? null : stepExecution.getStatus().name();
    }

    /**
     * 解析 Spring Batch 步骤错误摘要。
     */
    private String resolveSpringBatchStepErrorMessage(org.springframework.batch.core.StepExecution stepExecution) {
        if (stepExecution == null) {
            return null;
        }
        if (stepExecution.getExitStatus() != null && StringUtils.hasText(stepExecution.getExitStatus().getExitDescription())) {
            return stepExecution.getExitStatus().getExitDescription();
        }
        if (stepExecution.getFailureExceptions() != null && !stepExecution.getFailureExceptions().isEmpty()) {
            Throwable throwable = stepExecution.getFailureExceptions().get(0);
            return throwable == null ? null : throwable.getMessage();
        }
        return null;
    }

    private String resolveSpringBatchCurrentStage(JobExecution execution) {
        if (execution == null || execution.getStepExecutions() == null || execution.getStepExecutions().isEmpty()) {
            return OutsourcedDataTaskStage.FILE_PARSE.name();
        }
        return execution.getStepExecutions().stream()
                .filter(Objects::nonNull)
                .sorted((left, right) -> compareDateTime(
                        toLocalDateTime(left.getStartTime()),
                        toLocalDateTime(right.getStartTime())))
                .map(step -> resolveSpringBatchStageCode(step.getStepName()))
                .reduce((left, right) -> right)
                .orElse(OutsourcedDataTaskStage.FILE_PARSE.name());
    }

    private String resolveSpringBatchStageCode(String stepName) {
        if (!StringUtils.hasText(stepName)) {
            return OutsourcedDataTaskStage.FILE_PARSE.name();
        }
        String normalized = stepName.trim().toUpperCase(Locale.ROOT);
        OutsourcedDataTaskStage alias = PAGE_STAGE_ALIAS_MAP.get(normalized);
        if (alias != null) {
            return alias.name();
        }
        try {
            return OutsourcedDataTaskStage.valueOf(normalized).name();
        } catch (Exception ignored) {
            return OutsourcedDataTaskStage.FILE_PARSE.name();
        }
    }

    private String resolveSpringBatchErrorCode(JobExecution execution) {
        if (execution == null) {
            return null;
        }
        if (execution.getExitStatus() != null && StringUtils.hasText(execution.getExitStatus().getExitCode())) {
            return execution.getExitStatus().getExitCode();
        }
        return execution.getStatus() == null ? null : execution.getStatus().name();
    }

    private String resolveSpringBatchErrorMessage(JobExecution execution) {
        if (execution == null) {
            return null;
        }
        if (execution.getExitStatus() != null && StringUtils.hasText(execution.getExitStatus().getExitDescription())) {
            return execution.getExitStatus().getExitDescription();
        }
        if (execution.getAllFailureExceptions() != null && !execution.getAllFailureExceptions().isEmpty()) {
            Throwable throwable = execution.getAllFailureExceptions().get(0);
            return throwable == null ? null : throwable.getMessage();
        }
        return null;
    }

    /**
     * 生成统计卡片所需的阶段汇总。
     */
    private OutsourcedDataTaskStageSummaryDTO toStageSummary(String stage, Map<String, Long> statusCounts) {
        OutsourcedDataTaskStage visibleStage = normalizePageStage(stage);
        OutsourcedDataTaskStageSummaryDTO summary = new OutsourcedDataTaskStageSummaryDTO();
        summary.setStage(visibleStage.name());
        summary.setStep(visibleStage.name());
        summary.setStageName(stageLabel(visibleStage.name()));
        summary.setStepName(stageLabel(visibleStage.name()));
        summary.setStageDescription(stageDescription(visibleStage.name()));
        summary.setStepDescription(stageDescription(visibleStage.name()));
        summary.setTotalCount(statusCounts.values().stream().mapToLong(Long::longValue).sum());
        summary.setRunningCount(statusCounts.getOrDefault(OutsourcedDataTaskStatus.RUNNING.name(), 0L));
        summary.setFailedCount(statusCounts.getOrDefault(OutsourcedDataTaskStatus.FAILED.name(), 0L));
        summary.setPendingCount(statusCounts.getOrDefault(OutsourcedDataTaskStatus.PENDING.name(), 0L));
        return summary;
    }

    private void fillWorkflowMetadata(OutsourcedDataTaskSummaryDTO summary) {
        if (summary == null) {
            return;
        }
        WorkflowRuntimeCatalog catalog = stageCatalog();
        summary.setWorkflowCode(PAGE_WORKFLOW_CODE);
        summary.setWorkflowId(catalog.activeWorkflowId());
        summary.setVersionNo(catalog.activeWorkflowVersionNo());
    }

    private boolean matchesSnapshot(OutsourcedDataTaskBatchDTO row, OutsourcedDataTaskQueryCommand query) {
        if (row == null || query == null) {
            return true;
        }
        if (StringUtils.hasText(query.getBatchId()) && !Objects.equals(trim(query.getBatchId()), trim(row.getBatchId()))) {
            return false;
        }
        if (StringUtils.hasText(query.getTaskDate()) && !matchesDateWindow(row, query.getTaskDate())) {
            return false;
        }
        if (StringUtils.hasText(query.getBusinessDate()) && !matchesDateWindow(row, query.getBusinessDate())) {
            return false;
        }
        if (StringUtils.hasText(query.getManagerName()) && !containsValue(row.getManagerName(), query.getManagerName())) {
            return false;
        }
        if (StringUtils.hasText(query.getProductKeyword())
                && !containsValue(row.getProductName(), query.getProductKeyword())
                && !containsValue(row.getProductCode(), query.getProductKeyword())
                && !containsValue(row.getBatchName(), query.getProductKeyword())) {
            return false;
        }
        if (StringUtils.hasText(query.getStage()) && !hasStage(row, query.getStage())) {
            return false;
        }
        if (StringUtils.hasText(query.getStatus())
                && !Objects.equals(normalizePageStatus(query.getStatus()), normalizePageStatus(row.getStatus()))) {
            return false;
        }
        if (StringUtils.hasText(query.getSourceType())
                && !containsValue(row.getSourceType(), query.getSourceType())) {
            return false;
        }
        if (StringUtils.hasText(query.getErrorType())
                && !containsValue(row.getLastErrorCode(), query.getErrorType())
                && !containsValue(row.getLastErrorMessage(), query.getErrorType())) {
            return false;
        }
        return true;
    }

    private boolean matchesDateWindow(OutsourcedDataTaskBatchDTO row, String dateText) {
        if (row == null || !StringUtils.hasText(dateText)) {
            return true;
        }
        LocalDate date = parseDate(dateText);
        if (date == null) {
            return false;
        }
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        LocalDateTime startedAt = parseDateTime(row.getStartedAt());
        LocalDateTime endedAt = parseDateTime(row.getEndedAt());
        return isWithinWindow(startedAt, start, end) || isWithinWindow(endedAt, start, end);
    }

    private static boolean isWithinWindow(LocalDateTime value, LocalDateTime start, LocalDateTime end) {
        return value != null && !value.isBefore(start) && value.isBefore(end);
    }

    private static LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), DATE_FORMATTER);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean containsValue(String actual, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        return StringUtils.hasText(actual) && actual.toLowerCase(Locale.ROOT).contains(keyword.trim().toLowerCase(Locale.ROOT));
    }

    private static boolean hasStage(OutsourcedDataTaskBatchDTO row, String stage) {
        if (row == null || !StringUtils.hasText(stage)) {
            return true;
        }
        String normalizedStage = normalizePageStage(stage).name();
        return Objects.equals(normalizedStage, row.getCurrentStage())
                || Objects.equals(normalizedStage, row.getCurrentStep())
                || containsValue(row.getCurrentStageName(), stage)
                || containsValue(row.getCurrentStepName(), stage);
    }

    private List<OutsourcedDataTaskStepDTO> buildSyntheticSteps(OutsourcedDataTaskBatchDTO batch) {
        if (batch == null) {
            return java.util.Collections.emptyList();
        }
        OutsourcedDataTaskStage batchStage = normalizePageStage(batch.getCurrentStage());
        OutsourcedDataTaskStatus batchStatus = enumStatus(batch.getStatus());
        List<OutsourcedDataTaskStepDTO> steps = new ArrayList<>();
        for (OutsourcedDataTaskStage stage : PAGE_STAGE_SEQUENCE) {
            OutsourcedDataTaskStepDTO dto = new OutsourcedDataTaskStepDTO();
            dto.setStepId(firstText(batch.getBatchId(), "") + "-" + stage.name() + "-1");
            dto.setBatchId(batch.getBatchId());
            dto.setStage(stage.name());
            dto.setStep(stage.name());
            dto.setStageName(stageLabel(stage.name()));
            dto.setStepName(stageLabel(stage.name()));
            dto.setRunNo(1);
            dto.setCurrentFlag(Objects.equals(stage, batchStage));
            dto.setTriggerMode("SPRING_BATCH");
            dto.setTriggerModeName("Spring Batch");
            OutsourcedDataTaskStatus status;
            if (batchStatus == OutsourcedDataTaskStatus.SUCCESS) {
                status = OutsourcedDataTaskStatus.SUCCESS;
            } else if (batchStatus == OutsourcedDataTaskStatus.FAILED) {
                status = stageOrder(stage.name()) < stageOrder(batchStage.name())
                        ? OutsourcedDataTaskStatus.SUCCESS
                        : Objects.equals(stage, batchStage) ? OutsourcedDataTaskStatus.FAILED : OutsourcedDataTaskStatus.PENDING;
            } else if (batchStatus == OutsourcedDataTaskStatus.RUNNING) {
                status = stageOrder(stage.name()) < stageOrder(batchStage.name())
                        ? OutsourcedDataTaskStatus.SUCCESS
                        : Objects.equals(stage, batchStage) ? OutsourcedDataTaskStatus.RUNNING : OutsourcedDataTaskStatus.PENDING;
            } else {
                status = stageOrder(stage.name()) < stageOrder(batchStage.name())
                        ? OutsourcedDataTaskStatus.SUCCESS : OutsourcedDataTaskStatus.PENDING;
            }
            dto.setStatus(status.name());
            dto.setStatusName(statusLabel(status.name()));
            dto.setProgress(status == OutsourcedDataTaskStatus.SUCCESS ? 100
                    : status == OutsourcedDataTaskStatus.RUNNING ? Math.min(95, resolveBatchProgress(batchStage, batchStatus))
                    : 0);
            dto.setStartedAt(Objects.equals(stage, batchStage) ? batch.getStartedAt() : null);
            dto.setEndedAt(Objects.equals(stage, batchStage) ? batch.getEndedAt() : null);
            dto.setDurationMs(Objects.equals(stage, batchStage) ? batch.getDurationMs() : null);
            dto.setDurationText(Objects.equals(stage, batchStage) ? formatDuration(batch.getDurationMs(), batch.getStatus()) : "-");
            dto.setInputSummary(stage.getDescription());
            dto.setOutputSummary(status == OutsourcedDataTaskStatus.SUCCESS ? stage.getLabel() + "已完成" : null);
            dto.setErrorCode(Objects.equals(stage, batchStage) ? batch.getLastErrorCode() : null);
            dto.setErrorMessage(Objects.equals(stage, batchStage) ? batch.getLastErrorMessage() : null);
            dto.setLogRef("batch:" + batch.getBatchId() + ":" + stage.name());
            steps.add(dto);
        }
        return steps;
    }

    private int resolveBatchProgress(OutsourcedDataTaskStage stage, OutsourcedDataTaskStatus status) {
        if (status == OutsourcedDataTaskStatus.SUCCESS) {
            return 100;
        }
        if (stage == null) {
            return 0;
        }
        int order = stageOrder(stage.name());
        if (status == OutsourcedDataTaskStatus.FAILED) {
            return Math.max(15, (order + 1) * 33);
        }
        return Math.min(95, (order + 1) * 33);
    }

    private int normalizePageIndex(Integer pageIndex) {
        if (pageIndex == null || pageIndex < 1) {
            return 1;
        }
        return pageIndex;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private int pageStageOrder(String stage) {
        return stageOrder(stage);
    }

    private int stageOrder(String stage) {
        OutsourcedDataTaskStage normalized = normalizePageStage(stage);
        for (int i = 0; i < PAGE_STAGE_SEQUENCE.size(); i++) {
            if (PAGE_STAGE_SEQUENCE.get(i) == normalized) {
                return i;
            }
        }
        return PAGE_STAGE_SEQUENCE.size();
    }

    private static OutsourcedDataTaskStage normalizePageStage(String stage) {
        if (!StringUtils.hasText(stage)) {
            return OutsourcedDataTaskStage.FILE_PARSE;
        }
        String normalized = stage.trim().toUpperCase(Locale.ROOT);
        OutsourcedDataTaskStage alias = PAGE_STAGE_ALIAS_MAP.get(normalized);
        if (alias != null) {
            return alias;
        }
        try {
            return OutsourcedDataTaskStage.valueOf(normalized);
        } catch (Exception ignored) {
            return OutsourcedDataTaskStage.FILE_PARSE;
        }
    }

    private static String normalizePageStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return OutsourcedDataTaskStatus.PENDING.name();
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (OutsourcedDataTaskStatus.SUCCESS.name().equals(normalized)
                || OutsourcedDataTaskStatus.RUNNING.name().equals(normalized)
                || OutsourcedDataTaskStatus.PENDING.name().equals(normalized)) {
            return normalized;
        }
        if (Objects.equals("STOPPED", normalized) || Objects.equals("BLOCKED", normalized)
                || Objects.equals("FAILED", normalized)) {
            return OutsourcedDataTaskStatus.FAILED.name();
        }
        return OutsourcedDataTaskStatus.PENDING.name();
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static OutsourcedDataTaskStatus enumStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return OutsourcedDataTaskStatus.valueOf(status.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private WorkflowRuntimeCatalog stageCatalog() {
        return Objects.requireNonNull(stageCatalog, "WorkflowRuntimeCatalog 未注入");
    }

    private String stageLabel(String stage) {
        return stageCatalog().stageLabel(stage);
    }

    private String stageDescription(String stage) {
        return stageCatalog().stageDescription(stage);
    }

    private String statusLabel(String status) {
        return stageCatalog().statusLabel(normalizePageStatus(status));
    }

    private static String formatDate(LocalDate value) {
        return value == null ? null : DATE_FORMATTER.format(value);
    }

    private static String formatDateTime(LocalDateTime value) {
        return value == null ? null : DATE_TIME_FORMATTER.format(value);
    }

    private static LocalDateTime parseDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim(), DATE_TIME_FORMATTER);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Long durationMs(LocalDateTime startedAt, LocalDateTime endedAt) {
        if (startedAt == null || endedAt == null || endedAt.isBefore(startedAt)) {
            return null;
        }
        return Duration.between(startedAt, endedAt).toMillis();
    }

    private boolean shouldUseSpringBatchSnapshot(OutsourcedDataTaskQueryCommand query) {
        return query != null && (StringUtils.hasText(query.getTaskDate()) || StringUtils.hasText(query.getBusinessDate()));
    }

    private static LocalDateTime toLocalDateTime(Date date) {
        return date == null ? null : LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    private static String formatDuration(Long durationMs, String status) {
        if (durationMs == null) {
            return OutsourcedDataTaskStatus.RUNNING.name().equals(status) ? "运行中" : "-";
        }
        long seconds = Math.max(1, durationMs / 1000);
        return seconds < 60 ? seconds + "s" : seconds / 60 + "m";
    }

    private static String firstText(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private ValsetFileInfo resolveBatchFileInfo(Long fileId) {
        return resolveBatchFileInfo(fileId, new HashMap<>());
    }

    private ValsetFileInfo resolveBatchFileInfo(Long fileId, Map<Long, ValsetFileInfo> fileInfoCache) {
        if (fileId == null || valsetFileInfoGateway == null) {
            return null;
        }
        if (fileInfoCache != null && fileInfoCache.containsKey(fileId)) {
            return fileInfoCache.get(fileId);
        }
        try {
            ValsetFileInfo fileInfo = valsetFileInfoGateway.findById(fileId);
            if (fileInfoCache != null) {
                fileInfoCache.put(fileId, fileInfo);
            }
            return fileInfo;
        } catch (Exception ignored) {
            if (fileInfoCache != null) {
                fileInfoCache.put(fileId, null);
            }
            return null;
        }
    }

    private String resolveSpringBatchBusinessDate(ValsetFileInfo fileInfo, JobExecution execution) {
        if (fileInfo != null && fileInfo.getBusinessDate() != null) {
            return formatDate(fileInfo.getBusinessDate());
        }
        LocalDateTime startedAt = toLocalDateTime(execution == null ? null : execution.getStartTime());
        return startedAt == null ? null : formatDate(startedAt.toLocalDate());
    }

    private String resolveFilesysFileId(ValsetFileInfo fileInfo) {
        if (fileInfo == null) {
            return null;
        }
        return firstText(fileInfo.getSourceUri(), fileInfo.getStorageMetaJson(), fileInfo.getSourceMetaJson());
    }

    private static String resolveBatchId(Long fileId, String businessKey, Long taskId) {
        if (fileId != null) {
            return "FILE-" + fileId;
        }
        if (StringUtils.hasText(businessKey)) {
            return "BIZ-" + businessKey.trim().replaceAll("[^A-Za-z0-9_-]", "_");
        }
        if (taskId != null) {
            return "TASK-" + taskId;
        }
        return null;
    }

    private static final class TaskSnapshotIndex {

        private static final TaskSnapshotIndex EMPTY = new TaskSnapshotIndex(java.util.Collections.emptyList(),
                java.util.Collections.emptyMap());

        private final List<OutsourcedDataTaskBatchDTO> records;

        private final Map<String, JobExecution> executionByBatchId;

        private TaskSnapshotIndex(List<OutsourcedDataTaskBatchDTO> records,
                Map<String, JobExecution> executionByBatchId) {
            this.records = records;
            this.executionByBatchId = executionByBatchId;
        }

        private static TaskSnapshotIndex empty() {
            return EMPTY;
        }
    }

    private static final class CachedTaskSnapshotIndex {

        private final TaskSnapshotIndex snapshot;

        private final long cachedAt;

        private CachedTaskSnapshotIndex(TaskSnapshotIndex snapshot, long cachedAt) {
            this.snapshot = snapshot;
            this.cachedAt = cachedAt;
        }

        private boolean isExpired(long now) {
            return now - cachedAt > SNAPSHOT_CACHE_TTL_MILLIS;
        }
    }

    private static final class CachedExecutionIndex {

        private final Map<String, JobExecution> executionByBatchId;

        private final long cachedAt;

        private CachedExecutionIndex(Map<String, JobExecution> executionByBatchId, long cachedAt) {
            this.executionByBatchId = executionByBatchId;
            this.cachedAt = cachedAt;
        }

        private boolean isExpired(long now) {
            return now - cachedAt > EXECUTION_INDEX_CACHE_TTL_MILLIS;
        }
    }
}
