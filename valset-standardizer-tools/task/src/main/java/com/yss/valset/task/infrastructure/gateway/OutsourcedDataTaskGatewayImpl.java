package com.yss.valset.task.infrastructure.gateway;

import com.alibaba.nacos.shaded.com.google.common.collect.ImmutableList;
import com.alibaba.nacos.shaded.com.google.common.collect.ImmutableMap;
import com.alibaba.nacos.shaded.com.google.common.collect.ImmutableSet;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.cloud.dto.result.PageResult;
import com.yss.valset.application.event.lifecycle.ParseLifecycleEvent;
import com.yss.valset.application.event.lifecycle.ParseLifecycleStage;
import com.yss.valset.application.event.lifecycle.WorkflowTaskLifecycleEvent;
import com.yss.valset.domain.gateway.ValsetFileInfoGateway;
import com.yss.valset.domain.model.TaskStatus;
import com.yss.valset.domain.model.ValsetFileInfo;
import com.yss.valset.task.application.command.OutsourcedDataTaskQueryCommand;
import com.yss.valset.task.application.service.workflow.WorkflowRuntimeCatalog;
import com.yss.valset.common.support.DatabaseDialectSupport;
import com.yss.valset.task.application.dto.OutsourcedDataTaskBatchDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskStageSummaryDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskSummaryDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskStepDTO;
import com.yss.valset.task.application.port.OutsourcedDataTaskGateway;
import com.yss.valset.task.domain.model.OutsourcedDataTaskStage;
import com.yss.valset.task.domain.model.OutsourcedDataTaskStatus;
import com.yss.valset.task.infrastructure.entity.OutsourcedDataTaskBatchPO;
import com.yss.valset.task.infrastructure.entity.OutsourcedDataTaskLogPO;
import com.yss.valset.task.infrastructure.entity.OutsourcedDataTaskStepPO;
import com.yss.valset.task.infrastructure.mapper.OutsourcedDataTaskBatchRepository;
import com.yss.valset.task.infrastructure.mapper.OutsourcedDataTaskLogRepository;
import com.yss.valset.task.infrastructure.mapper.OutsourcedDataTaskStepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MyBatis 支持的估值表解析任务持久化网关。
 */
@Primary
@Repository
@RequiredArgsConstructor
public class OutsourcedDataTaskGatewayImpl implements OutsourcedDataTaskGateway {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String PAGE_WORKFLOW_CODE = "VALUATION_PARSE";

    private static final List<OutsourcedDataTaskStage> PAGE_STAGE_SEQUENCE = ImmutableList.of(
            OutsourcedDataTaskStage.FILE_PARSE,
            OutsourcedDataTaskStage.STRUCTURE_STANDARDIZE,
            OutsourcedDataTaskStage.STANDARD_LANDING);

    private static final Map<String, OutsourcedDataTaskStage> PAGE_STAGE_ALIAS_MAP = ImmutableMap.of(
            "RAW_DATA_EXTRACT", OutsourcedDataTaskStage.FILE_PARSE,
            "SUBJECT_RECOGNIZE", OutsourcedDataTaskStage.STANDARD_LANDING,
            "DATA_PROCESSING", OutsourcedDataTaskStage.STANDARD_LANDING,
            "VERIFY_ARCHIVE", OutsourcedDataTaskStage.STANDARD_LANDING);

    private static final Set<String> PAGE_ABNORMAL_STATUSES = ImmutableSet.of(
            OutsourcedDataTaskStatus.FAILED.name(),
            OutsourcedDataTaskStatus.BLOCKED.name(),
            OutsourcedDataTaskStatus.STOPPED.name());

    private final OutsourcedDataTaskBatchRepository batchRepository;

    private final OutsourcedDataTaskStepRepository stepRepository;

    private final OutsourcedDataTaskLogRepository logRepository;

    private final ValsetFileInfoGateway valsetFileInfoGateway;
    private final DatabaseDialectSupport databaseDialectSupport;

    private WorkflowRuntimeCatalog stageCatalog;

    @org.springframework.beans.factory.annotation.Autowired
    public void setStageCatalog(WorkflowRuntimeCatalog stageCatalog) {
        if (stageCatalog != null) {
            this.stageCatalog = stageCatalog;
        }
    }

    @Override
    public PageResult<OutsourcedDataTaskBatchDTO> pageTasks(OutsourcedDataTaskQueryCommand query) {
        List<OutsourcedDataTaskBatchDTO> records = loadTaskSnapshots(query);
        int current = normalizePageIndex(query == null ? null : query.getPageIndex());
        int size = normalizePageSize(query == null ? null : query.getPageSize());
        long total = records.size();
        int fromIndex = Math.min((current - 1) * size, records.size());
        int toIndex = Math.min(fromIndex + size, records.size());
        List<OutsourcedDataTaskBatchDTO> pageRecords = fromIndex >= toIndex
                ? java.util.Collections.emptyList()
                : new ArrayList<>(records.subList(fromIndex, toIndex));
        return PageResult.of(pageRecords, total, size, current);
    }

    @Override
    public OutsourcedDataTaskSummaryDTO summary(OutsourcedDataTaskQueryCommand query) {
        OutsourcedDataTaskQueryCommand baseQuery = copyQueryWithoutDynamicFilters(query);
        List<OutsourcedDataTaskBatchPO> batches = batchRepository.selectList(
                buildBatchQuery(baseQuery)
                        .orderByDesc(OutsourcedDataTaskBatchPO::getStartedAt)
                        .orderByDesc(OutsourcedDataTaskBatchPO::getBatchId));
        if (batches == null || batches.isEmpty()) {
            OutsourcedDataTaskSummaryDTO summary = new OutsourcedDataTaskSummaryDTO();
            summary.setStepSummaries(PAGE_STAGE_SEQUENCE.stream()
                    .map(stage -> toStageSummary(stage.name(), java.util.Collections.emptyMap()))
                    .collect(java.util.stream.Collectors.toList()));
            summary.setStageCatalog(summary.getStepSummaries());
            fillWorkflowMetadata(summary);
            return summary;
        }
        Map<String, List<OutsourcedDataTaskStepDTO>> stepsByBatchId = loadCurrentStepDtos(
                batches.stream()
                        .map(OutsourcedDataTaskBatchPO::getBatchId)
                        .filter(StringUtils::hasText)
                        .collect(java.util.stream.Collectors.toList()));
        Map<String, OutsourcedDataTaskBatchPO> batchById = batches.stream()
                .collect(Collectors.toMap(
                        OutsourcedDataTaskBatchPO::getBatchId,
                        batch -> batch,
                        (left, right) -> right,
                        LinkedHashMap::new));
        List<OutsourcedDataTaskBatchDTO> records = batches.stream()
                .map(batch -> toBatchDTO(batch,
                        stepsByBatchId.getOrDefault(batch.getBatchId(), java.util.Collections.emptyList())))
                .filter(row -> matchesSnapshot(row, query))
                .collect(java.util.stream.Collectors.toList());
        OutsourcedDataTaskSummaryDTO summary = new OutsourcedDataTaskSummaryDTO();
        summary.setTotalCount(records.size());
        summary.setRunningCount(records.stream().filter(row -> OutsourcedDataTaskStatus.RUNNING.name().equals(row.getStatus())).count());
        summary.setSuccessCount(records.stream().filter(row -> OutsourcedDataTaskStatus.SUCCESS.name().equals(row.getStatus())).count());
        summary.setFailedCount(records.stream().filter(row -> OutsourcedDataTaskStatus.FAILED.name().equals(row.getStatus())).count());
        Map<String, Map<String, Long>> stageStatusCountMap = new LinkedHashMap<>();
        for (OutsourcedDataTaskStage stage : PAGE_STAGE_SEQUENCE) {
            stageStatusCountMap.put(stage.name(), new LinkedHashMap<>());
        }
        for (OutsourcedDataTaskBatchDTO record : records) {
            List<OutsourcedDataTaskStepDTO> steps = stepsByBatchId.get(record.getBatchId());
            if (steps == null || steps.isEmpty()) {
                List<OutsourcedDataTaskStepDTO> syntheticSteps = PAGE_STAGE_SEQUENCE.stream()
                        .map(stage -> buildSyntheticStep(batchById.get(record.getBatchId()), stage,
                                normalizePageStage(record.getCurrentStage()),
                                enumStatus(normalizePageStatus(record.getStatus()))))
                        .collect(java.util.stream.Collectors.toList());
                for (OutsourcedDataTaskStepDTO step : syntheticSteps) {
                    Map<String, Long> statusMap = stageStatusCountMap.computeIfAbsent(step.getStage(),
                            key -> new LinkedHashMap<>());
                    statusMap.merge(normalizePageStatus(step.getStatus()), 1L, Long::sum);
                }
                continue;
            }
            for (OutsourcedDataTaskStepDTO step : steps) {
                Map<String, Long> statusMap = stageStatusCountMap.computeIfAbsent(step.getStage(),
                        key -> new LinkedHashMap<>());
                statusMap.merge(normalizePageStatus(step.getStatus()), 1L, Long::sum);
            }
        }
        summary.setStepSummaries(PAGE_STAGE_SEQUENCE.stream()
                .map(stage -> toStageSummary(stage.name(), stageStatusCountMap.getOrDefault(stage.name(), java.util.Collections.emptyMap())))
                .collect(java.util.stream.Collectors.toList()));
        summary.setStageCatalog(summary.getStepSummaries());
        fillWorkflowMetadata(summary);
        return summary;
    }

    @Override
    public List<OutsourcedDataTaskBatchDTO> listTasks(OutsourcedDataTaskQueryCommand query) {
        return loadTaskSnapshots(query);
    }

    @Override
    public Optional<OutsourcedDataTaskBatchDTO> findTask(String batchId) {
        if (!StringUtils.hasText(batchId)) {
            return Optional.empty();
        }
        OutsourcedDataTaskBatchPO batch = batchRepository.selectById(batchId);
        if (batch == null) {
            return Optional.empty();
        }
        List<OutsourcedDataTaskStepDTO> steps = loadCurrentStepDtos(java.util.Collections.singletonList(batchId))
                .getOrDefault(batchId, java.util.Collections.emptyList());
        return Optional.of(toBatchDTO(batch, steps));
    }

    @Override
    public List<OutsourcedDataTaskStepDTO> listSteps(String batchId) {
        if (!StringUtils.hasText(batchId)) {
            return java.util.Arrays.asList();
        }
        OutsourcedDataTaskBatchPO batch = batchRepository.selectById(batchId);
        List<OutsourcedDataTaskStepDTO> currentSteps = loadCurrentStepDtos(java.util.Collections.singletonList(batchId))
                .getOrDefault(batchId, java.util.Collections.emptyList());
        if (batch == null) {
            return currentSteps;
        }
        return mergeStepsWithBatch(batch, currentSteps);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordParseLifecycleEvent(ParseLifecycleEvent event) {
        if (event == null || event.getStage() == null) {
            return;
        }
        String batchId = resolveBatchId(event);
        if (!StringUtils.hasText(batchId)) {
            return;
        }
        LocalDateTime occurredAt = toLocalDateTime(event.getOccurredAt());
        OutsourcedDataTaskStage stage = mapStage(event.getStage());
        OutsourcedDataTaskStatus status = mapParseStatus(event);
        upsertBatch(event, batchId, stage, status, occurredAt);
        if (stage != null) {
            upsertStep(event, batchId, stage, status, occurredAt);
        }
        refreshBatchAggregation(batchId, occurredAt);
        insertLog(event, batchId, stage, status, occurredAt);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordWorkflowTaskLifecycleEvent(WorkflowTaskLifecycleEvent event) {
        if (event == null || event.getTaskId() == null || stageCatalog().ignoreWorkflowTaskType(event.getTaskType())) {
            return;
        }
        String batchId = resolveBatchId(resolveFileId(event), event.getBusinessKey(), event.getTaskId());
        LocalDateTime occurredAt = toLocalDateTime(event.getOccurredAt());
        OutsourcedDataTaskStage stage = mapWorkflowStage(event);
        OutsourcedDataTaskStatus status = mapWorkflowStatus(event.getTaskStatus());
        upsertWorkflowBatch(event, batchId, stage, status, occurredAt);
        upsertWorkflowStep(event, batchId, stage, status, occurredAt);
        refreshBatchAggregation(batchId, occurredAt);
        insertWorkflowLog(event, batchId, stage, occurredAt);
    }

    private LambdaQueryWrapper<OutsourcedDataTaskBatchPO> buildBatchQuery(OutsourcedDataTaskQueryCommand query) {
        if (query == null) {
            return Wrappers.lambdaQuery(OutsourcedDataTaskBatchPO.class)
                    .and(this::applyCurrentBatchFilter);
        }
        LocalDate taskDate = parseDate(query.getTaskDate());
        LocalDate businessDate = parseDate(query.getBusinessDate());
        LambdaQueryWrapper<OutsourcedDataTaskBatchPO> wrapper = Wrappers.lambdaQuery(OutsourcedDataTaskBatchPO.class)
                .eq(StringUtils.hasText(query.getBatchId()), OutsourcedDataTaskBatchPO::getBatchId,
                        trim(query.getBatchId()))
                .ge(taskDate != null, OutsourcedDataTaskBatchPO::getStartedAt,
                        taskDate != null ? taskDate.atStartOfDay() : null)
                .lt(taskDate != null, OutsourcedDataTaskBatchPO::getStartedAt,
                        taskDate != null ? taskDate.plusDays(1).atStartOfDay() : null)
                .eq(businessDate != null, OutsourcedDataTaskBatchPO::getBusinessDate, businessDate)
                .like(StringUtils.hasText(query.getManagerName()), OutsourcedDataTaskBatchPO::getManagerName,
                        trim(query.getManagerName()))
                .and(StringUtils.hasText(query.getProductKeyword()), criteria -> criteria
                        .like(OutsourcedDataTaskBatchPO::getProductCode, trim(query.getProductKeyword()))
                        .or()
                        .like(OutsourcedDataTaskBatchPO::getProductName, trim(query.getProductKeyword())))
                .eq(StringUtils.hasText(query.getStatus()), OutsourcedDataTaskBatchPO::getStatus,
                        trim(query.getStatus()))
                .eq(StringUtils.hasText(query.getSourceType()), OutsourcedDataTaskBatchPO::getSourceType,
                        trim(query.getSourceType()))
                .and(StringUtils.hasText(query.getErrorType()), criteria -> criteria
                        .like(OutsourcedDataTaskBatchPO::getLastErrorCode, trim(query.getErrorType()))
                        .or()
                        .like(OutsourcedDataTaskBatchPO::getLastErrorMessage, trim(query.getErrorType())));
        if (StringUtils.hasText(query.getStage())) {
            List<String> stageBatchIds = resolveBatchIdsByStage(query.getStage());
            if (stageBatchIds.isEmpty()) {
                return wrapper.eq(OutsourcedDataTaskBatchPO::getBatchId, "__NO_MATCH__");
            }
            wrapper.in(OutsourcedDataTaskBatchPO::getBatchId, stageBatchIds);
        }
        wrapper.and(this::applyCurrentBatchFilter);
        return wrapper;
    }

    private QueryWrapper<OutsourcedDataTaskBatchPO> buildBatchSummaryQuery(OutsourcedDataTaskQueryCommand query) {
        QueryWrapper<OutsourcedDataTaskBatchPO> wrapper = Wrappers.query();
        if (query == null) {
            applyCurrentBatchFilter(wrapper);
            return wrapper;
        }
        LocalDate taskDate = parseDate(query.getTaskDate());
        LocalDate businessDate = parseDate(query.getBusinessDate());
        wrapper.eq(StringUtils.hasText(query.getBatchId()), "batch_id", trim(query.getBatchId()))
                .ge(taskDate != null, "started_at", taskDate != null ? taskDate.atStartOfDay() : null)
                .lt(taskDate != null, "started_at", taskDate != null ? taskDate.plusDays(1).atStartOfDay() : null)
                .eq(businessDate != null, "business_date", businessDate)
                .like(StringUtils.hasText(query.getManagerName()), "manager_name", trim(query.getManagerName()))
                .and(StringUtils.hasText(query.getProductKeyword()), criteria -> criteria
                        .like("product_code", trim(query.getProductKeyword()))
                        .or()
                        .like("product_name", trim(query.getProductKeyword())))
                .eq(StringUtils.hasText(query.getStatus()), "status", trim(query.getStatus()))
                .eq(StringUtils.hasText(query.getSourceType()), "source_type", trim(query.getSourceType()))
                .and(StringUtils.hasText(query.getErrorType()), criteria -> criteria
                        .like("last_error_code", trim(query.getErrorType()))
                        .or()
                        .like("last_error_message", trim(query.getErrorType())));
        if (StringUtils.hasText(query.getStage())) {
            List<String> stageBatchIds = resolveBatchIdsByStage(query.getStage());
            if (stageBatchIds.isEmpty()) {
                return wrapper.eq("batch_id", "__NO_MATCH__");
            }
            wrapper.in("batch_id", stageBatchIds);
        }
        applyCurrentBatchFilter(wrapper);
        return wrapper;
    }

    private List<OutsourcedDataTaskBatchDTO> loadTaskSnapshots(OutsourcedDataTaskQueryCommand query) {
        OutsourcedDataTaskQueryCommand baseQuery = copyQueryWithoutDynamicFilters(query);
        List<OutsourcedDataTaskBatchPO> batches = batchRepository.selectList(
                buildBatchQuery(baseQuery)
                        .orderByDesc(OutsourcedDataTaskBatchPO::getStartedAt)
                        .orderByDesc(OutsourcedDataTaskBatchPO::getBatchId));
        if (batches == null || batches.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        Map<String, List<OutsourcedDataTaskStepDTO>> stepsByBatchId = loadCurrentStepDtos(
                batches.stream()
                        .map(OutsourcedDataTaskBatchPO::getBatchId)
                        .filter(StringUtils::hasText)
                        .collect(java.util.stream.Collectors.toList()));
        return batches.stream()
                .map(batch -> toBatchDTO(batch,
                        stepsByBatchId.getOrDefault(batch.getBatchId(), java.util.Collections.emptyList())))
                .filter(row -> matchesSnapshot(row, query))
                .collect(java.util.stream.Collectors.toList());
    }

    private Map<String, List<OutsourcedDataTaskStepDTO>> loadCurrentStepDtos(List<String> batchIds) {
        if (batchIds == null || batchIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        return stepRepository.selectList(
                Wrappers.lambdaQuery(OutsourcedDataTaskStepPO.class)
                        .in(OutsourcedDataTaskStepPO::getBatchId, batchIds)
                        .eq(OutsourcedDataTaskStepPO::getCurrentFlag, true)
                        .orderByAsc(OutsourcedDataTaskStepPO::getStage)
                        .orderByAsc(OutsourcedDataTaskStepPO::getRunNo)
                        .orderByAsc(OutsourcedDataTaskStepPO::getStepId))
                .stream()
                .map(this::toStepDTO)
                .collect(Collectors.groupingBy(
                        OutsourcedDataTaskStepDTO::getBatchId,
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    private static OutsourcedDataTaskQueryCommand copyQueryWithoutDynamicFilters(OutsourcedDataTaskQueryCommand query) {
        if (query == null) {
            return null;
        }
        OutsourcedDataTaskQueryCommand copy = new OutsourcedDataTaskQueryCommand();
        copy.setBatchId(query.getBatchId());
        copy.setTaskDate(query.getTaskDate());
        copy.setBusinessDate(query.getBusinessDate());
        copy.setManagerName(query.getManagerName());
        copy.setProductKeyword(query.getProductKeyword());
        copy.setSourceType(query.getSourceType());
        copy.setPageIndex(query.getPageIndex());
        copy.setPageSize(query.getPageSize());
        return copy;
    }

    private boolean matchesSnapshot(OutsourcedDataTaskBatchDTO row, OutsourcedDataTaskQueryCommand query) {
        if (row == null || query == null) {
            return true;
        }
        if (StringUtils.hasText(query.getStatus())
                && !Objects.equals(normalizePageStatus(query.getStatus()), normalizePageStatus(row.getStatus()))) {
            return false;
        }
        if (StringUtils.hasText(query.getStage())
                && !hasStage(row, query.getStage())) {
            return false;
        }
        if (StringUtils.hasText(query.getErrorType())) {
            String errorType = trim(query.getErrorType());
            String lastErrorCode = trim(row.getLastErrorCode());
            String lastErrorMessage = trim(row.getLastErrorMessage());
            if (!matches(lastErrorCode, errorType) && !matches(lastErrorMessage, errorType)) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasStage(OutsourcedDataTaskBatchDTO row, String stage) {
        if (row == null || !StringUtils.hasText(stage)) {
            return true;
        }
        String normalizedStage = normalizePageStage(stage).name();
        if (Objects.equals(normalizedStage, row.getCurrentStage())
                || Objects.equals(normalizedStage, row.getCurrentStep())) {
            return true;
        }
        return row.getCurrentStageName() != null && row.getCurrentStageName().contains(stage.trim())
                || row.getCurrentStepName() != null && row.getCurrentStepName().contains(stage.trim())
                || row.getCurrentStageName() != null && row.getCurrentStageName().contains(normalizedStage)
                || row.getCurrentStepName() != null && row.getCurrentStepName().contains(normalizedStage);
    }

    private static boolean matches(String actual, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        return StringUtils.hasText(actual) && actual.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private void applyCurrentBatchFilter(LambdaQueryWrapper<OutsourcedDataTaskBatchPO> wrapper) {
        wrapper.and(criteria -> criteria
                .likeRight(OutsourcedDataTaskBatchPO::getBatchId, "FILE-")
                .or()
                .isNotNull(OutsourcedDataTaskBatchPO::getFileId));
    }

    private void applyCurrentBatchFilter(QueryWrapper<OutsourcedDataTaskBatchPO> wrapper) {
        wrapper.and(criteria -> criteria
                .likeRight("batch_id", "FILE-")
                .or()
                .isNotNull("file_id"));
    }

    private OutsourcedDataTaskStageSummaryDTO toStageSummary(String stage, Map<String, Long> statusCounts) {
        OutsourcedDataTaskStageSummaryDTO summary = new OutsourcedDataTaskStageSummaryDTO();
        summary.setStage(stage);
        summary.setStep(stage);
        OutsourcedDataTaskStage visibleStage = normalizePageStage(stage);
        summary.setStageName(visibleStage.getLabel());
        summary.setStepName(visibleStage.getLabel());
        summary.setStageDescription(visibleStage.getDescription());
        summary.setStepDescription(visibleStage.getDescription());
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
        summary.setWorkflowCode(PAGE_WORKFLOW_CODE);
        summary.setWorkflowId(null);
        summary.setVersionNo(null);
    }

    private OutsourcedDataTaskBatchDTO toBatchDTO(OutsourcedDataTaskBatchPO po) {
        List<OutsourcedDataTaskStepDTO> steps = loadCurrentStepDtos(
                java.util.Collections.singletonList(po.getBatchId()))
                .getOrDefault(po.getBatchId(), java.util.Collections.emptyList());
        return toBatchDTO(po, steps);
    }

    private OutsourcedDataTaskBatchDTO toBatchDTO(OutsourcedDataTaskBatchPO po,
            List<OutsourcedDataTaskStepDTO> currentSteps) {
        OutsourcedDataTaskBatchDTO dto = new OutsourcedDataTaskBatchDTO();
        dto.setBatchId(po.getBatchId());
        dto.setBatchName(po.getBatchName());
        dto.setBusinessDate(formatDate(po.getBusinessDate()));
        dto.setProductCode(po.getProductCode());
        dto.setProductName(po.getProductName());
        dto.setManagerName(po.getManagerName());
        ValsetFileInfo fileInfo = resolveBatchFileInfo(po);
        dto.setBusinessDate(formatDate(resolveBatchBusinessDate(po, fileInfo)));
        dto.setFileId(firstText(po.getFileId(),
                fileInfo == null || fileInfo.getFileId() == null ? null : String.valueOf(fileInfo.getFileId())));
        dto.setFilesysFileId(firstText(po.getFilesysFileId(), resolveFilesysFileId(fileInfo)));
        dto.setOriginalFileName(
                firstText(po.getOriginalFileName(), fileInfo == null ? null : fileInfo.getFileNameOriginal()));
        dto.setSourceType(po.getSourceType());
        List<OutsourcedDataTaskStepDTO> orderedSteps = normalizeVisibleSteps(currentSteps);
        OutsourcedDataTaskStage currentStage = orderedSteps.isEmpty()
                ? normalizePageStage(po.getCurrentStage())
                : resolveBatchStageFromSteps(orderedSteps);
        String currentStatus = orderedSteps.isEmpty()
                ? normalizePageStatus(po.getStatus())
                : resolveBatchStatusFromSteps(orderedSteps).name();
        dto.setCurrentStage(currentStage.name());
        dto.setCurrentStep(currentStage.name());
        dto.setCurrentStageName(currentStage.getLabel());
        dto.setCurrentStepName(currentStage.getLabel());
        dto.setStatus(currentStatus);
        dto.setStatusName(statusLabel(currentStatus));
        dto.setProgress(resolveBatchProgress(currentStage, enumStatus(currentStatus)));
        LocalDateTime startedAt = orderedSteps.isEmpty()
                ? resolveBatchStartedAt(po.getBatchId(), po.getStartedAt())
                : parseDateTime(orderedSteps.get(0).getStartedAt());
        LocalDateTime endedAt = orderedSteps.isEmpty()
                ? resolveBatchEndedAt(po.getBatchId(), po.getEndedAt())
                : parseDateTime(orderedSteps.get(orderedSteps.size() - 1).getEndedAt());
        dto.setStartedAt(formatDateTime(startedAt));
        dto.setEndedAt(formatDateTime(endedAt));
        Long durationMs = durationMs(startedAt, endedAt);
        dto.setDurationMs(durationMs);
        dto.setDurationText(formatDuration(durationMs, currentStatus));
        Optional<OutsourcedDataTaskStepDTO> errorStep = orderedSteps.stream()
                .filter(step -> PAGE_ABNORMAL_STATUSES.contains(step.getStatus()))
                .reduce((left, right) -> right);
        dto.setLastErrorCode(errorStep.map(OutsourcedDataTaskStepDTO::getErrorCode).orElse(po.getLastErrorCode()));
        dto.setLastErrorMessage(errorStep.map(OutsourcedDataTaskStepDTO::getErrorMessage).orElse(po.getLastErrorMessage()));
        return dto;
    }

    private LocalDateTime resolveBatchStartedAt(String batchId, LocalDateTime fallbackStartedAt) {
        if (!StringUtils.hasText(batchId)) {
            return fallbackStartedAt;
        }
        List<OutsourcedDataTaskStepPO> steps = stepRepository.selectList(
                Wrappers.lambdaQuery(OutsourcedDataTaskStepPO.class)
                        .eq(OutsourcedDataTaskStepPO::getBatchId, batchId)
                        .eq(OutsourcedDataTaskStepPO::getCurrentFlag, true));
        return resolveBatchStartedAt(steps, fallbackStartedAt);
    }

    private LocalDateTime resolveBatchEndedAt(String batchId, LocalDateTime fallbackEndedAt) {
        if (!StringUtils.hasText(batchId)) {
            return fallbackEndedAt;
        }
        List<OutsourcedDataTaskStepPO> steps = stepRepository.selectList(
                Wrappers.lambdaQuery(OutsourcedDataTaskStepPO.class)
                        .eq(OutsourcedDataTaskStepPO::getBatchId, batchId)
                        .eq(OutsourcedDataTaskStepPO::getCurrentFlag, true));
        return resolveBatchEndedAt(steps, fallbackEndedAt);
    }

    private Long resolveBatchDurationMs(List<OutsourcedDataTaskStepPO> steps,
            LocalDateTime fallbackStartedAt,
            LocalDateTime fallbackEndedAt) {
        return durationMs(
                resolveBatchStartedAt(steps, fallbackStartedAt),
                resolveBatchEndedAt(steps, fallbackEndedAt));
    }

    private static LocalDateTime resolveBatchStartedAt(List<OutsourcedDataTaskStepPO> steps,
            LocalDateTime fallbackStartedAt) {
        if (steps == null || steps.isEmpty()) {
            return fallbackStartedAt;
        }
        Integer targetRunNo = steps.stream()
                .map(OutsourcedDataTaskStepPO::getRunNo)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
        return steps.stream()
                .filter(step -> step.getStartedAt() != null)
                .filter(step -> targetRunNo == null || Objects.equals(targetRunNo, step.getRunNo()))
                .min(Comparator.comparing(OutsourcedDataTaskStepPO::getStartedAt, Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(OutsourcedDataTaskStepPO::getStartedAt)
                .filter(Objects::nonNull)
                .orElse(fallbackStartedAt);
    }

    private static LocalDateTime resolveBatchEndedAt(List<OutsourcedDataTaskStepPO> steps,
            LocalDateTime fallbackEndedAt) {
        if (steps == null || steps.isEmpty()) {
            return fallbackEndedAt;
        }
        return steps.stream()
                .filter(step -> step.getEndedAt() != null)
                .max(Comparator
                        .comparing(OutsourcedDataTaskStepPO::getRunNo, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(OutsourcedDataTaskStepPO::getEndedAt,
                                Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(OutsourcedDataTaskStepPO::getEndedAt)
                .filter(Objects::nonNull)
                .orElse(fallbackEndedAt);
    }

    private List<String> resolveBatchIdsByStage(String stage) {
        return stepRepository.selectList(
                Wrappers.lambdaQuery(OutsourcedDataTaskStepPO.class)
                        .select(OutsourcedDataTaskStepPO::getBatchId)
                        .eq(StringUtils.hasText(stage), OutsourcedDataTaskStepPO::getStage, trim(stage)))
                .stream()
                .map(OutsourcedDataTaskStepPO::getBatchId)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(java.util.stream.Collectors.toList());
    }

    private void upsertBatch(ParseLifecycleEvent event,
            String batchId,
            OutsourcedDataTaskStage stage,
            OutsourcedDataTaskStatus status,
            LocalDateTime occurredAt) {
        OutsourcedDataTaskBatchPO po = batchRepository.selectById(batchId);
        boolean insert = po == null;
        if (insert) {
            po = new OutsourcedDataTaskBatchPO();
            po.setBatchId(batchId);
            po.setBatchName(resolveBatchName(event, batchId));
            po.setBusinessDate(occurredAt.toLocalDate());
            po.setCurrentStage(stage == null ? OutsourcedDataTaskStage.FILE_PARSE.name() : stage.name());
            po.setStatus(OutsourcedDataTaskStatus.PENDING.name());
            po.setProgress(0);
            po.setCreatedAt(occurredAt);
        }
        ValsetFileInfo fileInfo = loadFileInfo(event);
        po.setUpdatedAt(occurredAt);
        po.setBusinessDate(resolveBatchBusinessDate(fileInfo, po.getBusinessDate(), occurredAt.toLocalDate()));
        po.setSourceType(firstText(event.getDataSourceType(), event.getSource(), po.getSourceType()));
        Long fileId = resolveFileId(event);
        po.setFileId(fileId == null ? po.getFileId() : String.valueOf(fileId));
        po.setFilesysFileId(firstText(
                attributeText(event.getAttributes(), "filesysFileId"),
                resolveFilesysFileId(fileInfo),
                po.getFilesysFileId()));
        po.setProductCode(firstText(attributeText(event.getAttributes(), "productCode"), po.getProductCode()));
        po.setProductName(firstText(attributeText(event.getAttributes(), "productName"), po.getProductName()));
        po.setManagerName(firstText(attributeText(event.getAttributes(), "managerName"),
                attributeText(event.getAttributes(), "managerOrg"), po.getManagerName()));
        po.setValuationDate(firstDate(attributeText(event.getAttributes(), "valuationDate"), po.getValuationDate()));
        po.setOriginalFileName(
                firstText(fileInfo == null ? null : fileInfo.getFileNameOriginal(), po.getOriginalFileName()));
        po.setFileFingerprint(
                firstText(fileInfo == null ? null : fileInfo.getFileFingerprint(), po.getFileFingerprint()));
        po.setStartedAt(po.getStartedAt() == null ? occurredAt : po.getStartedAt());
        if (insert) {
            batchRepository.insert(po);
        } else {
            batchRepository.updateById(po);
        }
    }

    private void upsertStep(ParseLifecycleEvent event,
            String batchId,
            OutsourcedDataTaskStage stage,
            OutsourcedDataTaskStatus status,
            LocalDateTime occurredAt) {
        OutsourcedDataTaskStepPO po = findCurrentStep(batchId, stage);
        if (shouldStartNewRun(po, status, event == null ? null : event.getTaskId())) {
            markHistorical(po, occurredAt);
            po = null;
        }
        boolean insert = po == null;
        if (insert) {
            po = new OutsourcedDataTaskStepPO();
            int runNo = nextRunNo(batchId, stage);
            po.setStepId(stepId(batchId, stage, runNo));
            po.setBatchId(batchId);
            po.setStage(stage.name());
            po.setRunNo(runNo);
            po.setCurrentFlag(true);
            po.setCreatedAt(occurredAt);
            po.setStartedAt(occurredAt);
        }
        po.setTaskId(event.getTaskId() == null ? po.getTaskId() : String.valueOf(event.getTaskId()));
        po.setTaskType(firstText(po.getTaskType(), "PARSE_WORKBOOK"));
        po.setTriggerMode(firstText(event.getTriggerMode(), po.getTriggerMode()));
        po.setStatus(status == null ? po.getStatus() : status.name());
        po.setProgress(resolveStepProgress(status));
        po.setInputSummary(firstText(po.getInputSummary(), event.getBusinessKey(), event.getMessage()));
        po.setOutputSummary(status == OutsourcedDataTaskStatus.SUCCESS ? event.getMessage() : po.getOutputSummary());
        po.setUpdatedAt(occurredAt);
        if (status == OutsourcedDataTaskStatus.SUCCESS || status == OutsourcedDataTaskStatus.FAILED
                || status == OutsourcedDataTaskStatus.STOPPED) {
            po.setEndedAt(occurredAt);
            po.setDurationMs(durationMs(po.getStartedAt(), occurredAt));
        }
        if (status == OutsourcedDataTaskStatus.FAILED || status == OutsourcedDataTaskStatus.BLOCKED) {
            po.setErrorCode(event.getStage().name());
            po.setErrorMessage(firstText(event.getErrorMessage(), attributeText(event.getAttributes(), "errorMessage"),
                    event.getMessage()));
        }
        po.setLogRef("parse-lifecycle:" + event.getEventId());
        if (insert) {
            stepRepository.insert(po);
        } else {
            stepRepository.updateById(po);
        }
    }

    private void insertLog(ParseLifecycleEvent event,
            String batchId,
            OutsourcedDataTaskStage stage,
            OutsourcedDataTaskStatus status,
            LocalDateTime occurredAt) {
        OutsourcedDataTaskLogPO po = new OutsourcedDataTaskLogPO();
        po.setLogId(firstText(event.getEventId(), batchId + "-" + event.getStage().name() + "-" + occurredAt));
        po.setBatchId(batchId);
        po.setStepId(stage == null ? null : currentStepId(batchId, stage));
        po.setStage(stage == null ? null : stage.name());
        po.setLogLevel(status == OutsourcedDataTaskStatus.FAILED ? "ERROR" : "INFO");
        po.setMessage(firstText(event.getMessage(), event.getErrorMessage(), event.getStage().name()));
        po.setOccurredAt(occurredAt);
        po.setCreatedAt(occurredAt);
        if (logRepository.selectById(po.getLogId()) == null) {
            logRepository.insert(po);
        }
    }

    private void upsertWorkflowBatch(WorkflowTaskLifecycleEvent event,
            String batchId,
            OutsourcedDataTaskStage stage,
            OutsourcedDataTaskStatus status,
            LocalDateTime occurredAt) {
        OutsourcedDataTaskBatchPO po = batchRepository.selectById(batchId);
        boolean insert = po == null;
        if (insert) {
            po = new OutsourcedDataTaskBatchPO();
            po.setBatchId(batchId);
            po.setBatchName(firstText(event.getBusinessKey(), batchId));
            po.setBusinessDate(occurredAt.toLocalDate());
            po.setCurrentStage(stage.name());
            po.setStatus(OutsourcedDataTaskStatus.PENDING.name());
            po.setProgress(0);
            po.setCreatedAt(occurredAt);
        }
        ValsetFileInfo fileInfo = loadFileInfo(resolveFileId(event));
        po.setUpdatedAt(occurredAt);
        po.setBusinessDate(resolveBatchBusinessDate(fileInfo, po.getBusinessDate(), occurredAt.toLocalDate()));
        Long fileId = resolveFileId(event);
        po.setFileId(fileId == null ? po.getFileId() : String.valueOf(fileId));
        po.setFilesysFileId(firstText(attributeText(event.getAttributes(), "filesysFileId"),
                resolveFilesysFileId(fileInfo), po.getFilesysFileId()));
        po.setProductCode(firstText(attributeText(event.getAttributes(), "productCode"), po.getProductCode()));
        po.setProductName(firstText(attributeText(event.getAttributes(), "productName"), po.getProductName()));
        po.setManagerName(firstText(attributeText(event.getAttributes(), "managerName"),
                attributeText(event.getAttributes(), "managerOrg"), po.getManagerName()));
        po.setValuationDate(firstDate(attributeText(event.getAttributes(), "valuationDate"), po.getValuationDate()));
        po.setOriginalFileName(
                firstText(fileInfo == null ? null : fileInfo.getFileNameOriginal(), po.getOriginalFileName()));
        po.setFileFingerprint(
                firstText(fileInfo == null ? null : fileInfo.getFileFingerprint(), po.getFileFingerprint()));
        po.setSourceType(firstText(attributeText(event.getAttributes(), "dataSourceType"),
                attributeText(event.getAttributes(), "sourceType"),
                event.getTaskType() == null ? null : event.getTaskType().name(), po.getSourceType()));
        po.setStartedAt(po.getStartedAt() == null ? occurredAt : po.getStartedAt());
        if (insert) {
            batchRepository.insert(po);
        } else {
            batchRepository.updateById(po);
        }
    }

    private void upsertWorkflowStep(WorkflowTaskLifecycleEvent event,
            String batchId,
            OutsourcedDataTaskStage stage,
            OutsourcedDataTaskStatus status,
            LocalDateTime occurredAt) {
        OutsourcedDataTaskStepPO po = findCurrentStep(batchId, stage);
        if (shouldStartNewRun(po, status, event == null ? null : event.getTaskId())) {
            markHistorical(po, occurredAt);
            po = null;
        }
        boolean insert = po == null;
        if (insert) {
            po = new OutsourcedDataTaskStepPO();
            int runNo = nextRunNo(batchId, stage);
            po.setStepId(stepId(batchId, stage, runNo));
            po.setBatchId(batchId);
            po.setStage(stage.name());
            po.setRunNo(runNo);
            po.setCurrentFlag(true);
            po.setStartedAt(occurredAt);
            po.setCreatedAt(occurredAt);
        }
        po.setTaskId(String.valueOf(event.getTaskId()));
        po.setTaskType(event.getTaskType() == null ? po.getTaskType() : event.getTaskType().name());
        po.setTriggerMode("DEPENDENCY");
        po.setStatus(status.name());
        po.setProgress(resolveStepProgress(status));
        po.setInputSummary(
                firstText(po.getInputSummary(), event.getInputSummary(), event.getBusinessKey(), event.getMessage()));
        po.setOutputSummary(
                status == OutsourcedDataTaskStatus.SUCCESS ? firstText(event.getOutputSummary(), event.getMessage())
                        : po.getOutputSummary());
        po.setUpdatedAt(occurredAt);
        if (status == OutsourcedDataTaskStatus.SUCCESS || status == OutsourcedDataTaskStatus.FAILED
                || status == OutsourcedDataTaskStatus.STOPPED) {
            po.setEndedAt(occurredAt);
            po.setDurationMs(durationMs(po.getStartedAt(), occurredAt));
        }
        if (status == OutsourcedDataTaskStatus.FAILED || status == OutsourcedDataTaskStatus.BLOCKED) {
            po.setErrorCode(firstText(event.getErrorCode(),
                    event.getTaskStatus() == null ? null : event.getTaskStatus().name()));
            po.setErrorMessage(firstText(event.getErrorMessage(), event.getMessage()));
        }
        po.setLogRef("workflow-task:" + event.getEventId());
        if (insert) {
            stepRepository.insert(po);
        } else {
            stepRepository.updateById(po);
        }
    }

    private void insertWorkflowLog(WorkflowTaskLifecycleEvent event,
            String batchId,
            OutsourcedDataTaskStage stage,
            LocalDateTime occurredAt) {
        OutsourcedDataTaskLogPO po = new OutsourcedDataTaskLogPO();
        po.setLogId(firstText(event.getEventId(), batchId + "-" + event.getTaskStatus() + "-" + occurredAt));
        po.setBatchId(batchId);
        po.setStepId(currentStepId(batchId, stage));
        po.setStage(stage.name());
        po.setLogLevel(event.getTaskStatus() == TaskStatus.FAILED ? "ERROR" : "INFO");
        po.setMessage(firstText(event.getMessage(), event.getErrorMessage(),
                event.getTaskStatus() == null ? null : event.getTaskStatus().name()));
        po.setOccurredAt(occurredAt);
        po.setCreatedAt(occurredAt);
        if (logRepository.selectById(po.getLogId()) == null) {
            logRepository.insert(po);
        }
    }

    private OutsourcedDataTaskStepPO findCurrentStep(String batchId, OutsourcedDataTaskStage stage) {
        List<OutsourcedDataTaskStepPO> steps = stepRepository.selectList(
                Wrappers.lambdaQuery(OutsourcedDataTaskStepPO.class)
                        .eq(OutsourcedDataTaskStepPO::getBatchId, batchId)
                        .eq(OutsourcedDataTaskStepPO::getStage, stage.name())
                        .eq(OutsourcedDataTaskStepPO::getCurrentFlag, true)
                        .orderByDesc(OutsourcedDataTaskStepPO::getRunNo)
                        .orderByDesc(OutsourcedDataTaskStepPO::getStepId));
        return steps == null || steps.isEmpty() ? null : steps.get(0);
    }

    private int nextRunNo(String batchId, OutsourcedDataTaskStage stage) {
        List<OutsourcedDataTaskStepPO> steps = stepRepository.selectList(
                Wrappers.lambdaQuery(OutsourcedDataTaskStepPO.class)
                        .eq(OutsourcedDataTaskStepPO::getBatchId, batchId)
                        .eq(OutsourcedDataTaskStepPO::getStage, stage.name())
                        .orderByDesc(OutsourcedDataTaskStepPO::getRunNo)
                        .last(databaseDialectSupport.limitClause(1)));
        if (steps == null || steps.isEmpty() || steps.get(0).getRunNo() == null) {
            return 1;
        }
        return steps.get(0).getRunNo() + 1;
    }

    private void markHistorical(OutsourcedDataTaskStepPO step, LocalDateTime occurredAt) {
        if (step == null) {
            return;
        }
        step.setCurrentFlag(false);
        step.setUpdatedAt(occurredAt);
        stepRepository.updateById(step);
    }

    private static boolean shouldStartNewRun(OutsourcedDataTaskStepPO step, OutsourcedDataTaskStatus status,
            Long taskId) {
        if (step == null || status != OutsourcedDataTaskStatus.RUNNING) {
            if (step == null || taskId == null) {
                return false;
            }
            return !Objects.equals(step.getTaskId(), String.valueOf(taskId));
        }
        if (taskId != null && !Objects.equals(step.getTaskId(), String.valueOf(taskId))) {
            return true;
        }
        return isTerminalStatus(step.getStatus());
    }

    private static boolean isTerminalStatus(String status) {
        return OutsourcedDataTaskStatus.SUCCESS.name().equals(status)
                || OutsourcedDataTaskStatus.FAILED.name().equals(status)
                || OutsourcedDataTaskStatus.BLOCKED.name().equals(status)
                || OutsourcedDataTaskStatus.STOPPED.name().equals(status);
    }

    private String currentStepId(String batchId, OutsourcedDataTaskStage stage) {
        OutsourcedDataTaskStepPO step = findCurrentStep(batchId, stage);
        return step == null ? stepId(batchId, stage, 1) : step.getStepId();
    }

    private static String stepId(String batchId, OutsourcedDataTaskStage stage, int runNo) {
        return batchId + "-" + stage.name() + "-" + runNo;
    }

    private void refreshBatchAggregation(String batchId, LocalDateTime occurredAt) {
        OutsourcedDataTaskBatchPO batch = batchRepository.selectById(batchId);
        if (batch == null) {
            return;
        }
        List<OutsourcedDataTaskStepPO> steps = stepRepository.selectList(
                Wrappers.lambdaQuery(OutsourcedDataTaskStepPO.class)
                        .eq(OutsourcedDataTaskStepPO::getBatchId, batchId)
                        .eq(OutsourcedDataTaskStepPO::getCurrentFlag, true));
        if (steps == null || steps.isEmpty()) {
            return;
        }
        reconcileSequentialStepStates(steps, occurredAt);
        List<OutsourcedDataTaskStepDTO> visibleSteps = normalizeVisibleSteps(steps.stream()
                .map(this::toStepDTO)
                .collect(java.util.stream.Collectors.toList()));
        OutsourcedDataTaskStepDTO currentStep = resolveCurrentStageStep(visibleSteps);
        String status = aggregateBatchStatus(visibleSteps);
        batch.setCurrentStage(currentStep == null ? batch.getCurrentStage() : currentStep.getStage());
        batch.setStatus(status);
        batch.setProgress(resolveBatchProgress(
                currentStep == null ? null : normalizePageStage(currentStep.getStage()),
                enumStatus(status)));
        batch.setStartedAt(resolveBatchStartedAt(batchId, batch.getStartedAt()));
        batch.setUpdatedAt(occurredAt);
        if (isTerminalStatus(status)) {
            batch.setEndedAt(occurredAt);
            batch.setDurationMs(resolveBatchDurationMs(steps, batch.getStartedAt(), occurredAt));
        } else {
            batch.setEndedAt(null);
            batch.setDurationMs(null);
        }
        Optional<OutsourcedDataTaskStepDTO> errorStep = visibleSteps.stream()
                .filter(step -> PAGE_ABNORMAL_STATUSES.contains(step.getStatus()))
                .reduce((left, right) -> right);
        batch.setLastErrorCode(errorStep.map(OutsourcedDataTaskStepDTO::getErrorCode).orElse(null));
        batch.setLastErrorMessage(errorStep.map(OutsourcedDataTaskStepDTO::getErrorMessage).orElse(null));
        batchRepository.updateById(batch);
    }

    private void reconcileSequentialStepStates(List<OutsourcedDataTaskStepPO> steps, LocalDateTime occurredAt) {
        if (steps == null || steps.isEmpty()) {
            return;
        }
        List<OutsourcedDataTaskStepPO> orderedSteps = steps.stream()
                .sorted(Comparator.comparing((OutsourcedDataTaskStepPO step) -> pageStageOrder(step.getStage()))
                        .thenComparing(OutsourcedDataTaskStepPO::getRunNo,
                                Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(OutsourcedDataTaskStepPO::getStepId,
                                Comparator.nullsFirst(Comparator.naturalOrder())))
                .collect(java.util.stream.Collectors.toList());
        int latestObservedStageOrder = orderedSteps.stream()
                .map(OutsourcedDataTaskStepPO::getStage)
                .mapToInt(OutsourcedDataTaskGatewayImpl::pageStageOrder)
                .max()
                .orElse(-1);
        for (OutsourcedDataTaskStepPO step : orderedSteps) {
            if (step == null || step.getStage() == null) {
                continue;
            }
            int stageOrder = pageStageOrder(step.getStage());
            if (stageOrder >= latestObservedStageOrder) {
                continue;
            }
            if (isTerminalStatus(step.getStatus())) {
                continue;
            }
            step.setStatus(OutsourcedDataTaskStatus.SUCCESS.name());
            step.setProgress(100);
            if (step.getEndedAt() == null) {
                step.setEndedAt(occurredAt);
            }
            if (step.getDurationMs() == null) {
                step.setDurationMs(durationMs(step.getStartedAt(), occurredAt));
            }
            step.setUpdatedAt(occurredAt);
            stepRepository.updateById(step);
        }
    }

    private OutsourcedDataTaskStepDTO resolveCurrentStageStep(List<OutsourcedDataTaskStepDTO> steps) {
        Optional<OutsourcedDataTaskStepDTO> active = steps.stream()
                .filter(step -> OutsourcedDataTaskStatus.RUNNING.name().equals(step.getStatus())
                        || PAGE_ABNORMAL_STATUSES.contains(step.getStatus()))
                .max(Comparator.comparing((OutsourcedDataTaskStepDTO step) -> pageStageOrder(step.getStage()))
                        .thenComparing(OutsourcedDataTaskStepDTO::getRunNo,
                                Comparator.nullsFirst(Comparator.naturalOrder())));
        if (active.isPresent()) {
            return active.get();
        }
        return steps.stream()
                .max(Comparator.comparing((OutsourcedDataTaskStepDTO step) -> pageStageOrder(step.getStage()))
                        .thenComparing(OutsourcedDataTaskStepDTO::getRunNo,
                                Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(null);
    }

    private static String aggregateBatchStatus(List<OutsourcedDataTaskStepDTO> steps) {
        if (steps.stream().anyMatch(step -> PAGE_ABNORMAL_STATUSES.contains(step.getStatus()))) {
            return OutsourcedDataTaskStatus.FAILED.name();
        }
        if (steps.stream().anyMatch(step -> OutsourcedDataTaskStatus.RUNNING.name().equals(step.getStatus()))) {
            return OutsourcedDataTaskStatus.RUNNING.name();
        }
        long successCount = steps.stream()
                .filter(step -> OutsourcedDataTaskStatus.SUCCESS.name().equals(step.getStatus()))
                .count();
        if (successCount >= PAGE_STAGE_SEQUENCE.size()) {
            return OutsourcedDataTaskStatus.SUCCESS.name();
        }
        if (successCount > 0) {
            return OutsourcedDataTaskStatus.RUNNING.name();
        }
        return OutsourcedDataTaskStatus.PENDING.name();
    }

    private OutsourcedDataTaskStepDTO toStepDTO(OutsourcedDataTaskStepPO po) {
        OutsourcedDataTaskStepDTO dto = new OutsourcedDataTaskStepDTO();
        dto.setStepId(po.getStepId());
        dto.setBatchId(po.getBatchId());
        OutsourcedDataTaskStage visibleStage = normalizePageStage(po.getStage());
        dto.setStage(visibleStage.name());
        dto.setStep(visibleStage.name());
        dto.setStageName(stageLabel(visibleStage.name()));
        dto.setStepName(stageLabel(visibleStage.name()));
        dto.setTaskId(po.getTaskId());
        dto.setTaskType(po.getTaskType());
        dto.setRunNo(po.getRunNo());
        dto.setCurrentFlag(po.getCurrentFlag());
        dto.setTriggerMode(po.getTriggerMode());
        dto.setTriggerModeName(triggerModeLabel(po.getTriggerMode()));
        String status = normalizePageStatus(po.getStatus());
        dto.setStatus(status);
        dto.setStatusName(statusLabel(status));
        dto.setProgress(po.getProgress());
        dto.setStartedAt(formatDateTime(po.getStartedAt()));
        dto.setEndedAt(formatDateTime(po.getEndedAt()));
        dto.setDurationMs(po.getDurationMs());
        dto.setDurationText(formatDuration(po.getDurationMs(), status));
        dto.setInputSummary(po.getInputSummary());
        dto.setOutputSummary(po.getOutputSummary());
        dto.setErrorCode(po.getErrorCode());
        dto.setErrorMessage(po.getErrorMessage());
        dto.setLogRef(po.getLogRef());
        return dto;
    }

    private static int normalizePageIndex(Integer pageIndex) {
        return pageIndex == null || pageIndex < 1 ? 1 : pageIndex;
    }

    private static int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, 200);
    }

    private static String resolveBatchId(ParseLifecycleEvent event) {
        Long fileId = resolveFileId(event);
        if (fileId != null) {
            return "FILE-" + fileId;
        }
        if (StringUtils.hasText(event.getBusinessKey())) {
            return "BIZ-" + sanitizeBusinessKey(event.getBusinessKey());
        }
        if (StringUtils.hasText(event.getQueueId())) {
            return "QUEUE-" + event.getQueueId();
        }
        if (event.getTaskId() != null) {
            return "TASK-" + event.getTaskId();
        }
        return null;
    }

    private static String resolveBatchId(Long fileId, String businessKey, Long taskId) {
        if (fileId != null) {
            return "FILE-" + fileId;
        }
        if (StringUtils.hasText(businessKey)) {
            return "BIZ-" + sanitizeBusinessKey(businessKey);
        }
        if (taskId != null) {
            return "TASK-" + taskId;
        }
        return null;
    }

    private static String sanitizeBusinessKey(String businessKey) {
        return businessKey.trim().replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private static String resolveBatchName(ParseLifecycleEvent event, String batchId) {
        return firstText(event.getBusinessKey(), event.getMessage(), batchId);
    }

    private ValsetFileInfo loadFileInfo(ParseLifecycleEvent event) {
        return loadFileInfo(resolveFileId(event));
    }

    private ValsetFileInfo loadFileInfo(Long fileId) {
        if (fileId == null || valsetFileInfoGateway == null) {
            return null;
        }
        try {
            return valsetFileInfoGateway.findById(fileId);
        } catch (Exception ignored) {
            return null;
        }
    }

    private OutsourcedDataTaskStage mapStage(ParseLifecycleStage stage) {
        if (stage == null) {
            return null;
        }
        switch (stage) {
            case FILE_PARSE:
                return OutsourcedDataTaskStage.FILE_PARSE;
            case STRUCTURE_STANDARDIZE:
                return OutsourcedDataTaskStage.STRUCTURE_STANDARDIZE;
            case STANDARD_LANDING:
                return OutsourcedDataTaskStage.STANDARD_LANDING;
            case FAILED:
            case SKIPPED:
                return null;
            default:
                return null;
        }
    }

    private OutsourcedDataTaskStatus mapParseStatus(ParseLifecycleEvent event) {
        if (event == null) {
            return OutsourcedDataTaskStatus.PENDING;
        }
        if (StringUtils.hasText(event.getErrorMessage())) {
            return OutsourcedDataTaskStatus.FAILED;
        }
        String skipped = attributeText(event.getAttributes(), "skipped");
        if (StringUtils.hasText(skipped) && Boolean.parseBoolean(skipped.trim())) {
            return OutsourcedDataTaskStatus.STOPPED;
        }
        return OutsourcedDataTaskStatus.SUCCESS;
    }

    private OutsourcedDataTaskStage mapWorkflowStage(WorkflowTaskLifecycleEvent event) {
        if (event == null) {
            return stageCatalog().workflowFallbackStage();
        }
        return stageCatalog().resolveWorkflowStage(event.getTaskType(), event.getTaskStage());
    }

    private OutsourcedDataTaskStatus mapWorkflowStatus(TaskStatus status) {
        return stageCatalog().resolveWorkflowStatus(status);
    }

    private static int resolveStepProgress(OutsourcedDataTaskStatus status) {
        if (status == OutsourcedDataTaskStatus.SUCCESS) {
            return 100;
        }
        if (status == OutsourcedDataTaskStatus.FAILED || status == OutsourcedDataTaskStatus.BLOCKED) {
            return 66;
        }
        return 0;
    }

    private static OutsourcedDataTaskStage enumStage(String stage) {
        if (!StringUtils.hasText(stage)) {
            return null;
        }
        try {
            String normalized = stage.trim();
            if (Objects.equals("RAW_DATA_EXTRACT", normalized)) {
                return OutsourcedDataTaskStage.FILE_PARSE;
            }
            if (Objects.equals("SUBJECT_RECOGNIZE", normalized)
                    || Objects.equals("VERIFY_ARCHIVE", normalized)
                    || Objects.equals("DATA_PROCESSING", normalized)) {
                return OutsourcedDataTaskStage.STANDARD_LANDING;
            }
            return OutsourcedDataTaskStage.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static OutsourcedDataTaskStage displayStage(OutsourcedDataTaskStage stage) {
        return stage;
    }

    private static String displayStageName(String stage) {
        OutsourcedDataTaskStage displayStage = displayStage(enumStage(stage));
        return displayStage == null ? stage : displayStage.name();
    }

    private static Object mapValue(Map<String, Object> item, String key) {
        if (item == null || key == null) {
            return null;
        }
        Object value = item.get(key);
        if (value != null) {
            return value;
        }
        value = item.get(key.toUpperCase(Locale.ROOT));
        if (value != null) {
            return value;
        }
        return item.get(key.toLowerCase(Locale.ROOT));
    }

    private static String textValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static long longValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private static OutsourcedDataTaskStatus enumStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return OutsourcedDataTaskStatus.valueOf(status.trim());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        Instant value = instant == null ? Instant.now() : instant;
        return LocalDateTime.ofInstant(value, ZoneId.systemDefault());
    }

    private static Long durationMs(LocalDateTime startedAt, LocalDateTime endedAt) {
        if (startedAt == null || endedAt == null || endedAt.isBefore(startedAt)) {
            return null;
        }
        return java.time.Duration.between(startedAt, endedAt).toMillis();
    }

    private static String attributeText(Map<String, Object> attributes, String key) {
        if (attributes == null || key == null || !attributes.containsKey(key)) {
            return null;
        }
        Object value = attributes.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static Long resolveFileId(ParseLifecycleEvent event) {
        if (event == null) {
            return null;
        }
        if (event.getFileId() != null) {
            return event.getFileId();
        }
        String fileIdText = attributeText(event.getAttributes(), "fileId");
        if (!StringUtils.hasText(fileIdText)) {
            return null;
        }
        try {
            return Long.parseLong(fileIdText.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Long resolveFileId(WorkflowTaskLifecycleEvent event) {
        if (event == null) {
            return null;
        }
        if (event.getFileId() != null) {
            return event.getFileId();
        }
        String fileIdText = attributeText(event.getAttributes(), "fileId");
        if (!StringUtils.hasText(fileIdText)) {
            return null;
        }
        try {
            return Long.parseLong(fileIdText.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
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

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return LocalDate.parse(value.trim(), DATE_FORMATTER);
    }

    private static LocalDate firstDate(String value, LocalDate fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        try {
            return parseDate(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String resolveFilesysFileId(ValsetFileInfo fileInfo) {
        if (fileInfo == null) {
            return null;
        }
        String sourceMetaValue = jsonText(fileInfo.getSourceMetaJson(), "filesysFileId");
        if (StringUtils.hasText(sourceMetaValue)) {
            return sourceMetaValue;
        }
        String storageMetaValue = jsonText(fileInfo.getStorageMetaJson(), "filesysFileId");
        if (StringUtils.hasText(storageMetaValue)) {
            return storageMetaValue;
        }
        String sourceUri = fileInfo.getSourceUri();
        if (StringUtils.hasText(sourceUri) && sourceUri.trim().startsWith("filesys:")) {
            return sourceUri.trim().substring("filesys:".length());
        }
        return null;
    }

    private static String jsonText(String json, String key) {
        if (!StringUtils.hasText(json) || !StringUtils.hasText(key)) {
            return null;
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(json);
            JsonNode value = node.get(key);
            if (value == null || value.isNull()) {
                return null;
            }
            return value.isTextual() ? value.asText() : value.toString();
        } catch (Exception ignored) {
            return null;
        }
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

    private static String stageLabel(String stage) {
        return normalizePageStage(stage).getLabel();
    }

    private static String statusLabel(String status) {
        String normalized = normalizePageStatus(status);
        if (OutsourcedDataTaskStatus.RUNNING.name().equals(normalized)) {
            return OutsourcedDataTaskStatus.RUNNING.getLabel();
        }
        if (OutsourcedDataTaskStatus.SUCCESS.name().equals(normalized)) {
            return OutsourcedDataTaskStatus.SUCCESS.getLabel();
        }
        if (OutsourcedDataTaskStatus.FAILED.name().equals(normalized)) {
            return "处理异常";
        }
        return OutsourcedDataTaskStatus.PENDING.getLabel();
    }

    private static String triggerModeLabel(String triggerMode) {
        if ("SCHEDULE".equals(triggerMode)) {
            return "调度执行";
        }
        if ("MANUAL".equals(triggerMode)) {
            return "手动执行";
        }
        if ("DEPENDENCY".equals(triggerMode)) {
            return "依赖触发";
        }
        return triggerMode;
    }

    private static String formatDuration(Long durationMs, String status) {
        if (durationMs == null) {
            return OutsourcedDataTaskStatus.RUNNING.name().equals(status) ? "运行中" : "-";
        }
        long seconds = Math.max(1, durationMs / 1000);
        if (seconds < 60) {
            return seconds + "s";
        }
        return seconds / 60 + "m";
    }

    private int stageOrder(String stage) {
        return pageStageOrder(stage);
    }

    private WorkflowRuntimeCatalog stageCatalog() {
        return Objects.requireNonNull(stageCatalog, "WorkflowRuntimeCatalog 未注入");
    }

    private List<OutsourcedDataTaskStepDTO> mergeStepsWithBatch(OutsourcedDataTaskBatchPO batch,
            List<OutsourcedDataTaskStepDTO> currentSteps) {
        Map<String, OutsourcedDataTaskStepDTO> stepsByStage = currentSteps == null
                ? java.util.Collections.emptyMap()
                : currentSteps.stream()
                        .filter(step -> StringUtils.hasText(step.getStage()))
                        .collect(Collectors.toMap(
                                OutsourcedDataTaskStepDTO::getStage,
                                step -> step,
                                (left, right) -> right,
                                LinkedHashMap::new));
        OutsourcedDataTaskStage batchStage = normalizePageStage(batch.getCurrentStage());
        OutsourcedDataTaskStatus batchStatus = enumStatus(normalizePageStatus(batch.getStatus()));
        return PAGE_STAGE_SEQUENCE.stream()
                .map(stage -> {
                    OutsourcedDataTaskStepDTO current = stepsByStage.get(stage.name());
                    if (current != null) {
                        return current;
                    }
                    return buildSyntheticStep(batch, stage, batchStage, batchStatus);
                })
                .sorted((left, right) -> Integer.compare(stageOrder(left.getStage()), stageOrder(right.getStage())))
                .collect(java.util.stream.Collectors.toList());
    }

    private OutsourcedDataTaskStepDTO buildSyntheticStep(OutsourcedDataTaskBatchPO batch,
            OutsourcedDataTaskStage stage,
            OutsourcedDataTaskStage batchStage,
            OutsourcedDataTaskStatus batchStatus) {
        OutsourcedDataTaskStepDTO dto = new OutsourcedDataTaskStepDTO();
        dto.setStepId(firstText(batch.getBatchId(), "") + "-" + stage.name() + "-1");
        dto.setBatchId(batch.getBatchId());
        dto.setStage(stage.name());
        dto.setStep(stage.name());
        dto.setStageName(stageLabel(stage.name()));
        dto.setStepName(stageLabel(stage.name()));
        dto.setTaskId(null);
        dto.setTaskType(stage.name());
        dto.setRunNo(1);
        dto.setCurrentFlag(batchStage != null && Objects.equals(stage.name(), batchStage.name()));
        boolean beforeCurrent = batchStage != null && stageOrder(stage.name()) < stageOrder(batchStage.name());
        boolean isCurrent = batchStage != null && Objects.equals(stage.name(), batchStage.name());
        OutsourcedDataTaskStatus status;
        if (batchStatus == OutsourcedDataTaskStatus.SUCCESS) {
            status = OutsourcedDataTaskStatus.SUCCESS;
        } else if (batchStatus == OutsourcedDataTaskStatus.FAILED) {
            status = beforeCurrent ? OutsourcedDataTaskStatus.SUCCESS
                    : (isCurrent ? OutsourcedDataTaskStatus.FAILED : OutsourcedDataTaskStatus.PENDING);
        } else if (batchStatus == OutsourcedDataTaskStatus.RUNNING) {
            status = beforeCurrent ? OutsourcedDataTaskStatus.SUCCESS
                    : (isCurrent ? OutsourcedDataTaskStatus.RUNNING : OutsourcedDataTaskStatus.PENDING);
        } else {
            status = beforeCurrent ? OutsourcedDataTaskStatus.SUCCESS
                    : (isCurrent ? OutsourcedDataTaskStatus.PENDING : OutsourcedDataTaskStatus.PENDING);
        }
        dto.setStatus(status.name());
        dto.setStatusName(statusLabel(status.name()));
        dto.setProgress(status == OutsourcedDataTaskStatus.SUCCESS
                ? 100
                : status == OutsourcedDataTaskStatus.RUNNING
                        ? Math.min(95, resolveBatchProgress(batchStage, batchStatus))
                        : 0);
        String startedAt = stage == batchStage
                ? formatDateTime(resolveBatchStartedAt(batch.getBatchId(), batch.getStartedAt()))
                : null;
        dto.setStartedAt(startedAt);
        dto.setEndedAt(status == OutsourcedDataTaskStatus.SUCCESS || status == OutsourcedDataTaskStatus.FAILED
                ? formatDateTime(batch.getEndedAt())
                : null);
        Long durationMs = stage == batchStage ? batch.getDurationMs() : null;
        dto.setDurationMs(durationMs);
        dto.setDurationText(stage == batchStage ? formatDuration(batch.getDurationMs(), batch.getStatus()) : "-");
        dto.setInputSummary(stage.getDescription());
        dto.setOutputSummary(status == OutsourcedDataTaskStatus.SUCCESS ? stage.getLabel() + "已完成" : null);
        dto.setErrorCode(Objects.equals(stage.name(), batch.getCurrentStage()) ? batch.getLastErrorCode() : null);
        dto.setErrorMessage(Objects.equals(stage.name(), batch.getCurrentStage()) ? batch.getLastErrorMessage() : null);
        dto.setLogRef("batch:" + batch.getBatchId() + ":" + stage.name());
        return dto;
    }

    private int resolveBatchProgress(OutsourcedDataTaskStage stage, OutsourcedDataTaskStatus status) {
        if (status == OutsourcedDataTaskStatus.SUCCESS) {
            return 100;
        }
        if (status == OutsourcedDataTaskStatus.FAILED) {
            return stage == null ? 0 : Math.max(15, (stageOrder(stage.name()) + 1) * 33);
        }
        return stage == null ? 0 : Math.min(95, (stageOrder(stage.name()) + 1) * 33);
    }

    private static List<OutsourcedDataTaskStepDTO> normalizeVisibleSteps(List<OutsourcedDataTaskStepDTO> steps) {
        if (steps == null || steps.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        Map<String, OutsourcedDataTaskStepDTO> merged = new LinkedHashMap<>();
        for (OutsourcedDataTaskStepDTO step : steps) {
            if (step == null || !StringUtils.hasText(step.getStage())) {
                continue;
            }
            String stage = normalizePageStage(step.getStage()).name();
            OutsourcedDataTaskStepDTO copy = copyStepForVisibleStage(step, stage);
            merged.put(stage, copy);
        }
        return merged.values().stream()
                .sorted((left, right) -> Integer.compare(pageStageOrder(left.getStage()), pageStageOrder(right.getStage())))
                .collect(java.util.stream.Collectors.toList());
    }

    private static OutsourcedDataTaskStage resolveBatchStageFromSteps(List<OutsourcedDataTaskStepDTO> steps) {
        if (steps == null || steps.isEmpty()) {
            return OutsourcedDataTaskStage.FILE_PARSE;
        }
        Optional<OutsourcedDataTaskStepDTO> active = steps.stream()
                .filter(step -> OutsourcedDataTaskStatus.RUNNING.name().equals(step.getStatus())
                        || PAGE_ABNORMAL_STATUSES.contains(step.getStatus()))
                .max(Comparator.comparing((OutsourcedDataTaskStepDTO step) -> pageStageOrder(step.getStage()))
                        .thenComparing(OutsourcedDataTaskStepDTO::getRunNo,
                                Comparator.nullsFirst(Comparator.naturalOrder())));
        if (active.isPresent()) {
            return normalizePageStage(active.get().getStage());
        }
        return normalizePageStage(steps.get(steps.size() - 1).getStage());
    }

    private static OutsourcedDataTaskStatus resolveBatchStatusFromSteps(List<OutsourcedDataTaskStepDTO> steps) {
        if (steps == null || steps.isEmpty()) {
            return OutsourcedDataTaskStatus.PENDING;
        }
        if (steps.stream().anyMatch(step -> PAGE_ABNORMAL_STATUSES.contains(step.getStatus()))) {
            return OutsourcedDataTaskStatus.FAILED;
        }
        if (steps.stream().anyMatch(step -> OutsourcedDataTaskStatus.RUNNING.name().equals(step.getStatus()))) {
            return OutsourcedDataTaskStatus.RUNNING;
        }
        long successCount = steps.stream()
                .filter(step -> OutsourcedDataTaskStatus.SUCCESS.name().equals(step.getStatus()))
                .count();
        if (successCount >= PAGE_STAGE_SEQUENCE.size()) {
            return OutsourcedDataTaskStatus.SUCCESS;
        }
        if (successCount > 0) {
            return OutsourcedDataTaskStatus.RUNNING;
        }
        return OutsourcedDataTaskStatus.PENDING;
    }

    private static OutsourcedDataTaskStepDTO copyStepForVisibleStage(OutsourcedDataTaskStepDTO source, String stage) {
        OutsourcedDataTaskStepDTO dto = new OutsourcedDataTaskStepDTO();
        dto.setStepId(source.getStepId());
        dto.setBatchId(source.getBatchId());
        dto.setStage(stage);
        dto.setStep(stage);
        dto.setStageName(normalizePageStage(stage).getLabel());
        dto.setStepName(normalizePageStage(stage).getLabel());
        dto.setTaskId(source.getTaskId());
        dto.setTaskType(source.getTaskType());
        dto.setRunNo(source.getRunNo());
        dto.setCurrentFlag(source.getCurrentFlag());
        dto.setTriggerMode(source.getTriggerMode());
        dto.setTriggerModeName(source.getTriggerModeName());
        dto.setStatus(normalizePageStatus(source.getStatus()));
        dto.setStatusName(statusLabel(dto.getStatus()));
        dto.setProgress(source.getProgress());
        dto.setStartedAt(source.getStartedAt());
        dto.setEndedAt(source.getEndedAt());
        dto.setDurationMs(source.getDurationMs());
        dto.setDurationText(source.getDurationText());
        dto.setInputSummary(source.getInputSummary());
        dto.setOutputSummary(source.getOutputSummary());
        dto.setErrorCode(source.getErrorCode());
        dto.setErrorMessage(source.getErrorMessage());
        dto.setLogRef(source.getLogRef());
        return dto;
    }

    private static OutsourcedDataTaskStage normalizePageStage(String stage) {
        if (!StringUtils.hasText(stage)) {
            return OutsourcedDataTaskStage.FILE_PARSE;
        }
        String normalized = stage.trim();
        OutsourcedDataTaskStage alias = PAGE_STAGE_ALIAS_MAP.get(normalized);
        if (alias != null) {
            return alias;
        }
        try {
            return OutsourcedDataTaskStage.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return OutsourcedDataTaskStage.FILE_PARSE;
        }
    }

    private static String normalizePageStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return OutsourcedDataTaskStatus.PENDING.name();
        }
        String normalized = status.trim();
        if (OutsourcedDataTaskStatus.SUCCESS.name().equals(normalized)
                || OutsourcedDataTaskStatus.RUNNING.name().equals(normalized)
                || OutsourcedDataTaskStatus.PENDING.name().equals(normalized)) {
            return normalized;
        }
        if (PAGE_ABNORMAL_STATUSES.contains(normalized)) {
            return OutsourcedDataTaskStatus.FAILED.name();
        }
        return OutsourcedDataTaskStatus.PENDING.name();
    }

    private static int pageStageOrder(String stage) {
        OutsourcedDataTaskStage visibleStage = normalizePageStage(stage);
        for (int i = 0; i < PAGE_STAGE_SEQUENCE.size(); i++) {
            if (PAGE_STAGE_SEQUENCE.get(i) == visibleStage) {
                return i;
            }
        }
        return PAGE_STAGE_SEQUENCE.size();
    }

    private ValsetFileInfo resolveBatchFileInfo(OutsourcedDataTaskBatchPO batch) {
        if (batch == null || valsetFileInfoGateway == null) {
            return null;
        }
        Long fileId = parseLong(batch.getFileId());
        try {
            if (fileId != null) {
                ValsetFileInfo fileInfo = valsetFileInfoGateway.findById(fileId);
                if (fileInfo != null) {
                    return fileInfo;
                }
            }
            if (StringUtils.hasText(batch.getFileFingerprint())) {
                return valsetFileInfoGateway.findByFingerprint(batch.getFileFingerprint().trim());
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private static LocalDate resolveBatchBusinessDate(OutsourcedDataTaskBatchPO batch, ValsetFileInfo fileInfo) {
        if (fileInfo != null && fileInfo.getBusinessDate() != null) {
            return fileInfo.getBusinessDate();
        }
        return batch == null ? null : batch.getBusinessDate();
    }

    private static LocalDate resolveBatchBusinessDate(ValsetFileInfo fileInfo, LocalDate currentValue,
            LocalDate fallbackValue) {
        if (fileInfo != null && fileInfo.getBusinessDate() != null) {
            return fileInfo.getBusinessDate();
        }
        if (currentValue != null) {
            return currentValue;
        }
        return fallbackValue;
    }

    private static Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
