package com.yss.valset.task.infrastructure.gateway;

import com.yss.cloud.dto.result.PageResult;
import com.yss.valset.common.support.TaskFailureClassifier;
import com.yss.valset.task.application.command.OutsourcedDataTaskQueryCommand;
import com.yss.valset.task.application.dto.OutsourcedDataTaskBatchDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskStepDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskSummaryDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskTraceDTO;
import com.yss.valset.task.application.port.OutsourcedDataTaskGateway;
import com.yss.valset.task.application.service.workflow.WorkflowRuntimeCatalog;
import com.yss.valset.task.domain.model.OutsourcedDataTaskStage;
import com.yss.valset.task.domain.model.OutsourcedDataTaskStatus;
import com.yss.valset.task.infrastructure.query.OutsourcedDataTaskBatchQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 批量任务 版估值表解析任务读模型网关。
 *
 * <p>
 * 列表与汇总由数据库分页读模型负责，详情与步骤回放仅在单批次维度使用 批量任务 元数据。
 * </p>
 */
@Primary
@Repository
@RequiredArgsConstructor
public class OutsourcedDataTaskGatewayImpl implements OutsourcedDataTaskGateway {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JobExplorer springBatchJobExplorer;
    private final OutsourcedDataTaskBatchQueryRepository batchQueryRepository;

    private WorkflowRuntimeCatalog stageCatalog;

    @org.springframework.beans.factory.annotation.Autowired
    public void setStageCatalog(WorkflowRuntimeCatalog stageCatalog) {
        if (stageCatalog != null) {
            this.stageCatalog = stageCatalog;
        }
    }

    @Override
    public PageResult<OutsourcedDataTaskBatchDTO> pageTasks(OutsourcedDataTaskQueryCommand query) {
        int pageIndex = normalizePageIndex(query == null ? null : query.getPageIndex());
        int pageSize = normalizePageSize(query == null ? null : query.getPageSize());
        return batchQueryRepository.pageTasks(query, pageIndex, pageSize);
    }

    @Override
    public OutsourcedDataTaskSummaryDTO summary(OutsourcedDataTaskQueryCommand query) {
        return batchQueryRepository.summary(query);
    }

    @Override
    public List<OutsourcedDataTaskBatchDTO> listTasks(OutsourcedDataTaskQueryCommand query) {
        return batchQueryRepository.listTasks(query);
    }

    @Override
    public Optional<OutsourcedDataTaskBatchDTO> findTask(String batchId) {
        if (!StringUtils.hasText(batchId)) {
            return Optional.empty();
        }
        return batchQueryRepository.findTask(batchId);
    }

    @Override
    public List<OutsourcedDataTaskStepDTO> listSteps(String batchId) {
        if (!StringUtils.hasText(batchId) || springBatchJobExplorer == null) {
            return java.util.Collections.emptyList();
        }
        Optional<Long> executionId = batchQueryRepository.findExecutionId(batchId);
        if (!executionId.isPresent()) {
            return java.util.Collections.emptyList();
        }
        JobExecution execution = springBatchJobExplorer.getJobExecution(executionId.get());
        return execution == null ? java.util.Collections.emptyList() : toSpringBatchSteps(execution);
    }

    @Override
    public OutsourcedDataTaskTraceDTO getTrace(String batchId) {
        return batchQueryRepository.getTrace(batchId);
    }

    private List<OutsourcedDataTaskStepDTO> toSpringBatchSteps(JobExecution execution) {
        if (execution == null || execution.getStepExecutions() == null || execution.getStepExecutions().isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return execution.getStepExecutions().stream()
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing((StepExecution step) ->
                        pageStageOrder(resolveSpringBatchStageCode(step.getStepName())))
                        .thenComparing(StepExecution::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(stepExecution -> toStepDTO(execution, stepExecution))
                .collect(java.util.stream.Collectors.toList());
    }

    private OutsourcedDataTaskStepDTO toStepDTO(JobExecution execution, StepExecution stepExecution) {
        OutsourcedDataTaskStepDTO dto = new OutsourcedDataTaskStepDTO();
        String batchId = resolveSpringBatchBatchId(execution);
        String stageCode = resolveSpringBatchStageCode(stepExecution.getStepName());
        dto.setStepId(batchId + "-" + stepExecution.getStepName());
        dto.setBatchId(batchId);
        dto.setStage(stageCode);
        dto.setStep(stageCode);
        dto.setStageName(stageLabel(stageCode));
        dto.setStepName(dto.getStageName());
        dto.setTaskId(String.valueOf(execution.getJobParameters().getLong("taskId", null)));
        dto.setTaskType(execution.getJobParameters().getString("taskType", null));
        dto.setRunNo(stepExecution.getId() == null ? null : stepExecution.getId().intValue());
        dto.setCurrentFlag(isCurrentSpringBatchStep(execution, stepExecution));
        dto.setTriggerMode("SPRING_BATCH");
        dto.setTriggerModeName("批量任务");
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
        return dto;
    }

    private static boolean isCurrentSpringBatchStep(JobExecution execution, StepExecution stepExecution) {
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
        Long fileId = execution.getJobParameters().getLong("fileId", null);
        String businessKey = execution.getJobParameters().getString("businessKey", null);
        Long taskId = execution.getJobParameters().getLong("taskId", null);
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

    private String resolveSpringBatchStageCode(String stepName) {
        if (!StringUtils.hasText(stepName)) {
            return OutsourcedDataTaskStage.FILE_PARSE.name();
        }
        String normalized = stepName.trim().toUpperCase(Locale.ROOT);
        switch (normalized) {
            case "RAW_DATA_EXTRACT":
            case "PARSE":
            case "TASK_RAW_PARSED":
            case "TASK_CREATED":
            case "TASK_DISPATCHED":
            case "TASK_EXECUTION_STARTED":
            case "TASK_REUSED":
            case "QUEUE_DISCOVERED":
            case "QUEUE_GENERATED":
            case "QUEUE_BACKFILLED":
            case "QUEUE_REUSED":
            case "QUEUE_UPDATED":
            case "QUEUE_RETRIED":
            case "QUEUE_SUBSCRIBED":
            case "QUEUE_SUBSCRIBE_ATTEMPTED":
            case "QUEUE_SUBSCRIBE_CONFLICT":
            case "QUEUE_SUBSCRIBE_SKIPPED":
            case "QUEUE_FILE_INFO_REPAIR_STARTED":
            case "QUEUE_FILE_INFO_REPAIR_COMPLETED":
            case "QUEUE_FILE_INFO_REPAIR_FAILED":
                return OutsourcedDataTaskStage.FILE_PARSE.name();
            case "SUBJECT_RECOGNIZE":
            case "VERIFY_ARCHIVE":
            case "DATA_PROCESSING":
                return OutsourcedDataTaskStage.STANDARD_LANDING.name();
            default:
                try {
                    return OutsourcedDataTaskStage.valueOf(normalized).name();
                } catch (Exception ignored) {
                    return OutsourcedDataTaskStage.FILE_PARSE.name();
                }
        }
    }

    private int pageStageOrder(String stage) {
        return stageOrder(stage);
    }

    private int stageOrder(String stage) {
        OutsourcedDataTaskStage normalized = normalizePageStage(stage);
        List<OutsourcedDataTaskStage> sequence = stageCatalog == null
                ? Arrays.asList(OutsourcedDataTaskStage.FILE_PARSE,
                OutsourcedDataTaskStage.STRUCTURE_STANDARDIZE,
                OutsourcedDataTaskStage.STANDARD_LANDING)
                : stageCatalog.stageSequence();
        for (int i = 0; i < sequence.size(); i++) {
            if (sequence.get(i) == normalized) {
                return i;
            }
        }
        return sequence.size();
    }

    private static OutsourcedDataTaskStage normalizePageStage(String stage) {
        if (!StringUtils.hasText(stage)) {
            return OutsourcedDataTaskStage.FILE_PARSE;
        }
        String normalized = stage.trim().toUpperCase(Locale.ROOT);
        if ("RAW_DATA_EXTRACT".equals(normalized) || "PARSE".equals(normalized)) {
            return OutsourcedDataTaskStage.FILE_PARSE;
        }
        if ("SUBJECT_RECOGNIZE".equals(normalized) || "VERIFY_ARCHIVE".equals(normalized)
                || "DATA_PROCESSING".equals(normalized)) {
            return OutsourcedDataTaskStage.STANDARD_LANDING;
        }
        try {
            return OutsourcedDataTaskStage.valueOf(normalized);
        } catch (Exception ignored) {
            return OutsourcedDataTaskStage.FILE_PARSE;
        }
    }

    private String stageLabel(String stage) {
        if (stageCatalog == null) {
            return normalizePageStage(stage).getLabel();
        }
        return stageCatalog.stageLabel(stage);
    }

    private String statusLabel(String status) {
        if (stageCatalog == null) {
            return normalizeStatus(status).getLabel();
        }
        return stageCatalog.resolveStatusLabel(status);
    }

    private static OutsourcedDataTaskStatus normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return OutsourcedDataTaskStatus.PENDING;
        }
        try {
            return OutsourcedDataTaskStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return OutsourcedDataTaskStatus.PENDING;
        }
    }

    private static int resolveSpringBatchStepProgress(StepExecution stepExecution) {
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

    private String resolveSpringBatchStepErrorCode(StepExecution stepExecution) {
        if (stepExecution == null) {
            return null;
        }
        if (stepExecution.getExitStatus() != null && StringUtils.hasText(stepExecution.getExitStatus().getExitCode())) {
            return stepExecution.getExitStatus().getExitCode();
        }
        return stepExecution.getStatus() == null ? null : stepExecution.getStatus().name();
    }

    private String resolveSpringBatchStepErrorMessage(StepExecution stepExecution) {
        if (stepExecution == null) {
            return null;
        }
        if (stepExecution.getFailureExceptions() != null && !stepExecution.getFailureExceptions().isEmpty()) {
            Throwable throwable = stepExecution.getFailureExceptions().get(0);
            String message = TaskFailureClassifier.resolveReadableMessage(throwable);
            if (StringUtils.hasText(message)) {
                return message;
            }
        }
        if (stepExecution.getExitStatus() != null && StringUtils.hasText(stepExecution.getExitStatus().getExitDescription())) {
            return stepExecution.getExitStatus().getExitDescription();
        }
        return null;
    }

    private static LocalDateTime toLocalDateTime(Date date) {
        return date == null ? null : LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    private static String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.format(DATE_TIME_FORMATTER);
    }

    private static long durationMs(LocalDateTime startedAt, LocalDateTime endedAt) {
        if (startedAt == null || endedAt == null) {
            return 0L;
        }
        return Math.max(0L, Duration.between(startedAt, endedAt).toMillis());
    }

    private static String formatDuration(Long durationMs, String status) {
        if (durationMs == null) {
            return OutsourcedDataTaskStatus.RUNNING.name().equals(status) ? "运行中" : "-";
        }
        long seconds = Math.max(1, durationMs / 1000);
        return seconds < 60 ? seconds + "s" : seconds / 60 + "m";
    }

    private int normalizePageIndex(Integer pageIndex) {
        if (pageIndex == null || pageIndex < 1) {
            return 1;
        }
        return pageIndex;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 200);
    }
}
