package com.yss.valset.task.infrastructure.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.cloud.dto.result.PageResult;
import com.yss.valset.common.support.TaskFailureClassifier;
import com.yss.valset.task.application.command.OutsourcedDataTaskQueryCommand;
import com.yss.valset.task.application.dto.OutsourcedDataTaskBatchDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskStageSummaryDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskSummaryDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskStepDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskTraceDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskTraceLogDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskTraceRecordDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskTraceResultSummaryDTO;
import com.yss.valset.task.application.service.workflow.WorkflowRuntimeCatalog;
import com.yss.valset.task.domain.model.OutsourcedDataTaskStage;
import com.yss.valset.task.domain.model.OutsourcedDataTaskStatus;
import com.yss.valset.task.infrastructure.dto.OutsourcedDataTaskTraceRow;
import com.yss.valset.task.infrastructure.mapper.OutsourcedDataTaskBatchQueryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 基于 批量任务 元数据的批次读模型查询仓库。
 */
@Repository
@RequiredArgsConstructor
public class OutsourcedDataTaskBatchQueryRepository {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 200;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final OutsourcedDataTaskBatchQueryMapper mapper;
    private final JobExplorer jobExplorer;
    private final WorkflowRuntimeCatalog stageCatalog;
    private final ObjectMapper objectMapper;

    public PageResult<OutsourcedDataTaskBatchDTO> pageTasks(OutsourcedDataTaskQueryCommand query, int pageIndex,
            int pageSize) {
        int safePageIndex = normalizePageIndex(pageIndex);
        int safePageSize = normalizePageSize(pageSize);
        int offset = Math.max(0, (safePageIndex - 1) * safePageSize);
        long total = mapper.countTasks(query);
        List<OutsourcedDataTaskBatchDTO> rows = mapper.pageTasks(query, offset, safePageSize).stream()
                .map(this::enrichBatchRow)
                .collect(Collectors.toList());
        return PageResult.of(rows, total, safePageSize, safePageIndex);
    }

    public List<OutsourcedDataTaskBatchDTO> listTasks(OutsourcedDataTaskQueryCommand query) {
        List<OutsourcedDataTaskBatchDTO> results = new ArrayList<>();
        int pageIndex = 1;
        while (true) {
            PageResult<OutsourcedDataTaskBatchDTO> page = pageTasks(query, pageIndex, MAX_PAGE_SIZE);
            List<OutsourcedDataTaskBatchDTO> rows = page.getData();
            if (rows == null || rows.isEmpty()) {
                break;
            }
            results.addAll(rows);
            if (rows.size() < MAX_PAGE_SIZE) {
                break;
            }
            pageIndex++;
        }
        return results;
    }

    public OutsourcedDataTaskSummaryDTO summary(OutsourcedDataTaskQueryCommand query) {
        OutsourcedDataTaskSummaryDTO summary = new OutsourcedDataTaskSummaryDTO();
        summary.setWorkflowCode("VALUATION_PARSE");
        summary.setWorkflowId(stageCatalog.activeWorkflowId());
        summary.setVersionNo(stageCatalog.activeWorkflowVersionNo());
        summary.setTotalCount(mapper.countTasks(query));
        summary.setRunningCount(mapper.countStatusTasks(query, "RUNNING"));
        summary.setSuccessCount(mapper.countStatusTasks(query, "SUCCESS"));
        summary.setFailedCount(mapper.countStatusTasks(query, "FAILED", "STOPPED"));
        List<OutsourcedDataTaskStageSummaryDTO> stageSummaries = mapper.queryStageSummaries(query).stream()
                .map(this::enrichStageSummary)
                .collect(Collectors.toList());
        summary.setStepSummaries(stageSummaries);
        summary.setStageCatalog(stageSummaries);
        return summary;
    }

    public Optional<OutsourcedDataTaskBatchDTO> findTask(String batchId) {
        if (!StringUtils.hasText(batchId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(enrichBatchRow(mapper.findTaskByBatchId(batchId)));
    }

    public Optional<Long> findExecutionId(String batchId) {
        if (!StringUtils.hasText(batchId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.findExecutionIdByBatchId(batchId));
    }

    public List<OutsourcedDataTaskStepDTO> listSteps(String batchId) {
        Optional<Long> executionId = findExecutionId(batchId);
        if (!executionId.isPresent() || jobExplorer == null) {
            return java.util.Collections.emptyList();
        }
        JobExecution execution = jobExplorer.getJobExecution(executionId.get());
        return execution == null ? java.util.Collections.emptyList() : toSpringBatchSteps(execution);
    }

    public OutsourcedDataTaskTraceDTO getTrace(String batchId) {
        OutsourcedDataTaskBatchDTO batch = findTask(batchId).orElse(null);
        OutsourcedDataTaskTraceRow row = mapper.findTraceRowByBatchId(batchId);
        List<OutsourcedDataTaskStepDTO> steps = listSteps(batchId);
        OutsourcedDataTaskTraceDTO trace = new OutsourcedDataTaskTraceDTO();
        trace.setBatch(batch);
        trace.setTaskSteps(steps);
        trace.setTransferObject(buildTransferRecord(row, batch));
        trace.setParseQueue(buildParseQueueRecord(row, batch));
        trace.setJobExecution(buildJobExecutionRecord(row, batch));
        trace.setStepExecutions(steps.stream()
                .map(this::buildStepExecutionRecord)
                .collect(Collectors.toList()));
        trace.setLogs(buildTraceLogs(row, batch, steps));
        trace.setResultSummary(buildResultSummary(row, batch, steps));
        return trace;
    }

    private OutsourcedDataTaskBatchDTO enrichBatchRow(OutsourcedDataTaskBatchDTO row) {
        if (row == null) {
            return null;
        }
        JsonNode payload = parsePayload(row.getInputPayload());
        row.setOriginalFileName(firstText(payloadText(payload, "fileNameOriginal"), row.getOriginalFileName()));
        row.setSourceType(firstText(payloadText(payload, "dataSourceType"), row.getSourceType()));
        row.setBatchName(resolveBatchName(row));
        row.setDurationText(formatDuration(row.getDurationMs(), row.getStatus()));
        row.setLastErrorCode(firstText(row.getLastErrorCode(), row.getStepStatus()));
        row.setLastErrorMessage(resolveBatchErrorMessage(row));
        row.setSourceTypeName(resolveTaskTypeLabel(row.getSourceType()));
        row.setTaskStageName(stageLabel(row.getTaskStage()));
        row.setCurrentStageName(stageLabel(row.getCurrentStage()));
        row.setCurrentStepName(stageLabel(row.getCurrentStep()));
        row.setStatusName(stageCatalog.resolveStatusLabel(row.getStatus()));
        return row;
    }

    private String resolveBatchErrorMessage(OutsourcedDataTaskBatchDTO row) {
        if (row == null || !isFailedStatus(row.getStatus())) {
            return null;
        }
        String message = resolveBatchExecutionErrorMessage(row.getExecutionId());
        return firstText(message, row.getLastErrorMessage(), row.getTransferErrorMessage());
    }

    private String resolveBatchExecutionErrorMessage(Long executionId) {
        if (executionId == null || jobExplorer == null) {
            return null;
        }
        JobExecution execution = jobExplorer.getJobExecution(executionId);
        if (execution == null) {
            return null;
        }
        List<Throwable> failures = execution.getAllFailureExceptions();
        if (failures == null || failures.isEmpty()) {
            return null;
        }
        for (Throwable failure : failures) {
            String message = TaskFailureClassifier.resolveReadableMessage(failure);
            if (StringUtils.hasText(message)) {
                return message;
            }
        }
        return null;
    }

    private OutsourcedDataTaskStageSummaryDTO enrichStageSummary(OutsourcedDataTaskStageSummaryDTO summary) {
        if (summary == null) {
            return null;
        }
        summary.setStageName(stageLabel(summary.getStage()));
        summary.setStepName(stageLabel(summary.getStep()));
        summary.setStageDescription(stageCatalog.stageDescription(summary.getStage()));
        summary.setStepDescription(stageCatalog.stageDescription(summary.getStep()));
        return summary;
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
                .collect(Collectors.toList());
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
        dto.setStatusName(stageCatalog.resolveStatusLabel(dto.getStatus()));
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

    private String resolveBatchName(OutsourcedDataTaskBatchDTO row) {
        String candidate = firstText(row.getOriginalFileName(), row.getBatchName());
        if (StringUtils.hasText(candidate)) {
            return candidate;
        }
        String key = firstText(row.getFileId(), row.getTaskId() == null ? null : String.valueOf(row.getTaskId()),
                row.getExecutionId() == null ? null : String.valueOf(row.getExecutionId()));
        return StringUtils.hasText(key) ? "估值解析批次-" + key : "估值解析批次";
    }

    private JsonNode parsePayload(String payload) {
        if (!StringUtils.hasText(payload)) {
            return null;
        }
        try {
            return objectMapper.readTree(payload);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String payloadText(JsonNode payload, String fieldName) {
        if (payload == null || !StringUtils.hasText(fieldName)) {
            return null;
        }
        JsonNode value = payload.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asText(null);
    }

    private String firstText(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private OutsourcedDataTaskTraceRecordDTO buildTransferRecord(OutsourcedDataTaskTraceRow row,
            OutsourcedDataTaskBatchDTO batch) {
        OutsourcedDataTaskTraceRecordDTO record = new OutsourcedDataTaskTraceRecordDTO();
        record.setType("TRANSFER_OBJECT");
        record.setId(firstText(row == null ? null : row.getTransferId(), batch == null ? null : batch.getFileId()));
        record.setName(firstText(row == null ? null : row.getTransferOriginalName(), batch == null ? null : batch.getOriginalFileName(), "投递对象"));
        record.setStatus(resolveTransferTraceStatus(row, hasText(record.getId())));
        record.setStatusName(resolveTraceStatusLabel(record.getStatus()));
        record.setDownstreamId(row == null ? null : row.getQueueId());
        record.setStartedAt(row == null ? null : row.getTransferReceivedAt());
        record.setEndedAt(row == null ? null : firstText(row.getDeliveryDeliveredAt(), row.getTransferStoredAt()));
        record.setErrorMessage(row == null ? null : firstText(row.getDeliveryErrorMessage(), row.getTransferErrorMessage()));
        record.setInputSummary(attributeSummary("来源", row == null ? null : row.getTransferSourceType(),
                "来源编码", row == null ? null : row.getTransferSourceCode()));
        record.setOutputSummary(attributeSummary("投递状态", row == null ? null : firstText(row.getDeliveryExecuteStatus(), row.getQueueDeliveryStatus()),
                "文件名", row == null ? null : row.getTransferOriginalName()));
        record.setLogRef(hasText(record.getId()) ? "transfer-object:" + record.getId() : null);
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("transferId", record.getId());
        attributes.put("sourceType", row == null ? null : row.getTransferSourceType());
        attributes.put("sourceCode", row == null ? null : row.getTransferSourceCode());
        attributes.put("businessDate", row == null ? null : row.getTransferBusinessDate());
        attributes.put("transferStatus", row == null ? null : row.getTransferStatus());
        attributes.put("deliveryStatus", row == null ? null : row.getQueueDeliveryStatus());
        attributes.put("deliveryExecuteStatus", row == null ? null : row.getDeliveryExecuteStatus());
        record.setAttributes(attributes);
        return record;
    }

    private OutsourcedDataTaskTraceRecordDTO buildParseQueueRecord(OutsourcedDataTaskTraceRow row,
            OutsourcedDataTaskBatchDTO batch) {
        OutsourcedDataTaskTraceRecordDTO record = new OutsourcedDataTaskTraceRecordDTO();
        record.setType("PARSE_QUEUE");
        record.setId(row == null ? null : row.getQueueId());
        record.setName(firstText(row == null ? null : row.getQueueId(), "解析队列"));
        record.setStatus(normalizeTraceStatus(row == null ? null : row.getQueueStatus(), hasText(record.getId())));
        record.setStatusName(resolveTraceStatusLabel(record.getStatus()));
        record.setUpstreamId(row == null ? null : row.getTransferId());
        record.setDownstreamId(batch == null || batch.getTaskId() == null ? null : String.valueOf(batch.getTaskId()));
        record.setStartedAt(row == null ? null : row.getQueueClaimedAt());
        record.setEndedAt(row == null ? null : row.getQueueParsedAt());
        record.setErrorMessage(row == null ? null : row.getQueueErrorMessage());
        record.setInputSummary(row == null ? null : row.getQueueRequestJson());
        record.setOutputSummary(row == null ? null : row.getQueueResultJson());
        record.setLogRef(hasText(record.getId()) ? "parse-queue:" + record.getId() : null);
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("triggerMode", row == null ? null : row.getQueueTriggerMode());
        attributes.put("retryCount", row == null ? null : row.getQueueRetryCount());
        attributes.put("claimedBy", row == null ? null : row.getQueueClaimedBy());
        attributes.put("businessKey", row == null ? null : row.getBusinessKey());
        record.setAttributes(attributes);
        return record;
    }

    private OutsourcedDataTaskTraceRecordDTO buildJobExecutionRecord(OutsourcedDataTaskTraceRow row,
            OutsourcedDataTaskBatchDTO batch) {
        OutsourcedDataTaskTraceRecordDTO record = new OutsourcedDataTaskTraceRecordDTO();
        record.setType("JOB_EXECUTION");
        record.setId(row == null || row.getExecutionId() == null ? null : String.valueOf(row.getExecutionId()));
        record.setName(firstText(row == null ? null : row.getJobName(), "批量任务 Job"));
        record.setStatus(normalizeTraceStatus(row == null ? null : row.getJobStatus(), hasText(record.getId())));
        record.setStatusName(resolveTraceStatusLabel(record.getStatus()));
        record.setUpstreamId(row == null ? null : row.getQueueId());
        record.setDownstreamId(batch == null || batch.getTaskId() == null ? null : String.valueOf(batch.getTaskId()));
        record.setStartedAt(row == null ? null : row.getJobStartTime());
        record.setEndedAt(row == null ? null : row.getJobEndTime());
        record.setDurationMs(row == null ? null : row.getJobDurationMs());
        record.setErrorCode(row == null ? null : row.getJobExitCode());
        record.setErrorMessage(row == null ? null : row.getJobExitMessage());
        record.setInputSummary(firstText(row == null ? null : row.getInputPayload(), batch == null ? null : batch.getInputPayload()));
        record.setOutputSummary(attributeSummary("taskId", row == null || row.getTaskId() == null ? null : String.valueOf(row.getTaskId()),
                "taskStage", row == null ? null : row.getTaskStage()));
        record.setLogRef(hasText(record.getId()) ? "spring-batch:" + record.getId() : null);
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("jobInstanceId", row == null ? null : row.getJobInstanceId());
        attributes.put("taskId", row == null ? null : row.getTaskId());
        attributes.put("fileId", row == null ? null : row.getFileId());
        attributes.put("businessKey", row == null ? null : row.getBusinessKey());
        attributes.put("taskType", row == null ? null : row.getTaskType());
        attributes.put("createTime", row == null ? null : row.getJobCreateTime());
        record.setAttributes(attributes);
        return record;
    }

    private OutsourcedDataTaskTraceRecordDTO buildStepExecutionRecord(OutsourcedDataTaskStepDTO step) {
        OutsourcedDataTaskTraceRecordDTO record = new OutsourcedDataTaskTraceRecordDTO();
        record.setType("STEP_EXECUTION");
        record.setId(step == null ? null : step.getStepId());
        record.setName(firstText(step == null ? null : step.getStepName(), "Batch Step"));
        record.setStatus(normalizeTraceStatus(step == null ? null : step.getStatus(), step != null));
        record.setStatusName(step == null ? null : step.getStatusName());
        record.setStartedAt(step == null ? null : step.getStartedAt());
        record.setEndedAt(step == null ? null : step.getEndedAt());
        record.setDurationMs(step == null ? null : step.getDurationMs());
        record.setErrorCode(step == null ? null : step.getErrorCode());
        record.setErrorMessage(step == null ? null : step.getErrorMessage());
        record.setInputSummary(step == null ? null : step.getInputSummary());
        record.setOutputSummary(step == null ? null : step.getOutputSummary());
        record.setLogRef(step == null ? null : step.getLogRef());
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("stage", step == null ? null : step.getStage());
        attributes.put("taskId", step == null ? null : step.getTaskId());
        attributes.put("runNo", step == null ? null : step.getRunNo());
        record.setAttributes(attributes);
        return record;
    }

    private List<OutsourcedDataTaskTraceLogDTO> buildTraceLogs(OutsourcedDataTaskTraceRow row,
            OutsourcedDataTaskBatchDTO batch,
            List<OutsourcedDataTaskStepDTO> steps) {
        List<OutsourcedDataTaskTraceLogDTO> logs = new ArrayList<>();
        appendLog(logs, "TRANSFER_OBJECT", row == null ? null : row.getTransferId(),
                row == null ? null : row.getTransferErrorMessage(),
                row == null ? null : row.getTransferStoredAt(),
                "transfer-object:" + (row == null ? "" : firstText(row.getTransferId(), "")));
        appendLog(logs, "PARSE_QUEUE", row == null ? null : row.getQueueId(),
                row == null ? null : row.getQueueErrorMessage(),
                row == null ? null : firstText(row.getQueueParsedAt(), row.getQueueClaimedAt()),
                "parse-queue:" + (row == null ? "" : firstText(row.getQueueId(), "")));
        appendLog(logs, "JOB_EXECUTION", row == null || row.getExecutionId() == null ? null : String.valueOf(row.getExecutionId()),
                firstText(row == null ? null : row.getJobExitMessage(), batch == null ? null : batch.getLastErrorMessage()),
                row == null ? null : firstText(row.getJobEndTime(), row.getJobStartTime()),
                "spring-batch:" + (row == null || row.getExecutionId() == null ? "" : row.getExecutionId()));
        for (OutsourcedDataTaskStepDTO step : steps) {
            appendLog(logs, "STEP_EXECUTION", step == null ? null : step.getStepId(),
                    step == null ? null : step.getErrorMessage(),
                    step == null ? null : firstText(step.getEndedAt(), step.getStartedAt()),
                    step == null ? null : step.getLogRef());
        }
        return logs;
    }

    private void appendLog(List<OutsourcedDataTaskTraceLogDTO> logs,
            String nodeType,
            String nodeId,
            String message,
            String loggedAt,
            String logRef) {
        if (!hasText(message)) {
            return;
        }
        OutsourcedDataTaskTraceLogDTO log = new OutsourcedDataTaskTraceLogDTO();
        log.setLogId(nodeType + ":" + firstText(nodeId, String.valueOf(logs.size() + 1)));
        log.setNodeType(nodeType);
        log.setNodeId(nodeId);
        log.setLevel("ERROR");
        log.setMessage(message);
        log.setLoggedAt(loggedAt);
        log.setLogRef(logRef);
        logs.add(log);
    }

    private OutsourcedDataTaskTraceResultSummaryDTO buildResultSummary(OutsourcedDataTaskTraceRow row,
            OutsourcedDataTaskBatchDTO batch,
            List<OutsourcedDataTaskStepDTO> steps) {
        OutsourcedDataTaskTraceResultSummaryDTO summary = new OutsourcedDataTaskTraceResultSummaryDTO();
        String status = firstText(batch == null ? null : batch.getStatus(), row == null ? null : row.getJobStatus());
        summary.setStatus(normalizeTraceStatus(status, batch != null || row != null));
        summary.setStatusName(resolveTraceStatusLabel(summary.getStatus()));
        summary.setStartedAt(firstText(batch == null ? null : batch.getStartedAt(), row == null ? null : row.getJobStartTime()));
        summary.setEndedAt(firstText(batch == null ? null : batch.getEndedAt(), row == null ? null : row.getJobEndTime()));
        summary.setDurationMs(firstLong(batch == null ? null : batch.getDurationMs(), row == null ? null : row.getJobDurationMs()));
        summary.setInputSummary(firstText(row == null ? null : row.getInputPayload(), batch == null ? null : batch.getInputPayload()));
        summary.setOutputSummary(steps == null || steps.isEmpty() ? null : "Batch Step " + steps.size() + " 个");
        summary.setErrorCode(firstText(batch == null ? null : batch.getLastErrorCode(), row == null ? null : row.getJobExitCode()));
        summary.setErrorMessage(firstText(batch == null ? null : batch.getLastErrorMessage(), row == null ? null : row.getJobExitMessage()));
        return summary;
    }

    private String attributeSummary(String label1, String value1, String label2, String value2) {
        List<String> parts = new ArrayList<>();
        if (hasText(value1)) {
            parts.add(label1 + "=" + value1);
        }
        if (hasText(value2)) {
            parts.add(label2 + "=" + value2);
        }
        return parts.isEmpty() ? null : String.join("，", parts);
    }

    private String normalizeTraceStatus(String status, boolean exists) {
        if (!exists) {
            return "MISSING";
        }
        if (!hasText(status)) {
            return "PENDING";
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        switch (normalized) {
            case "COMPLETED":
            case "PARSED":
            case "DELIVERED":
            case "SUCCESS":
                return "SUCCESS";
            case "STARTING":
            case "STARTED":
            case "STOPPING":
            case "PARSING":
            case "RUNNING":
                return "RUNNING";
            case "FAILED":
            case "BLOCKED":
                return "FAILED";
            case "STOPPED":
            case "ABANDONED":
                return "STOPPED";
            default:
                return normalized;
        }
    }

    private String resolveTransferTraceStatus(OutsourcedDataTaskTraceRow row, boolean exists) {
        if (!exists) {
            return "MISSING";
        }
        String deliveryExecuteStatus = row == null ? null : row.getDeliveryExecuteStatus();
        if ("SUCCESS".equalsIgnoreCase(trimToEmpty(deliveryExecuteStatus))) {
            return "SUCCESS";
        }
        if ("FAILED".equalsIgnoreCase(trimToEmpty(deliveryExecuteStatus))) {
            return "FAILED";
        }
        String queueDeliveryStatus = row == null ? null : row.getQueueDeliveryStatus();
        if ("DELIVERED".equalsIgnoreCase(trimToEmpty(queueDeliveryStatus))) {
            return "SUCCESS";
        }
        if ("UNDELIVERED".equalsIgnoreCase(trimToEmpty(queueDeliveryStatus))) {
            return "PENDING";
        }
        return normalizeTraceStatus(row == null ? null : row.getTransferStatus(), exists);
    }

    private String resolveTraceStatusLabel(String status) {
        String normalized = normalizeTraceStatus(status, hasText(status));
        if ("MISSING".equals(normalized)) {
            return "缺失";
        }
        return stageCatalog.resolveStatusLabel(normalized);
    }

    private boolean hasText(String value) {
        return StringUtils.hasText(value);
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private Long firstLong(Long... values) {
        if (values == null) {
            return null;
        }
        for (Long value : values) {
            if (value != null) {
                return value;
            }
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
        return stageCatalog.stageOrder(stage);
    }

    private String stageLabel(String stage) {
        return stageCatalog.stageLabel(stage);
    }

    private String resolveTaskTypeLabel(String taskType) {
        if (!StringUtils.hasText(taskType)) {
            return "-";
        }
        switch (taskType.trim().toUpperCase(Locale.ROOT)) {
            case "EXTRACT_DATA":
                return "原始数据提取";
            case "PARSE_WORKBOOK":
                return "解析工作簿";
            case "MATCH_SUBJECT":
                return "标准科目匹配";
            case "EVALUATE_MAPPING":
                return "映射评估";
            case "EXPORT_RESULT":
                return "结果导出";
            case "REFRESH_STANDARD_SUBJECT":
                return "刷新标准科目";
            case "REFRESH_MAPPING_HINT":
                return "刷新映射提示";
            default:
                return taskType;
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

    private static String resolveSpringBatchStepErrorCode(StepExecution stepExecution) {
        if (stepExecution == null) {
            return null;
        }
        if (stepExecution.getExitStatus() != null && StringUtils.hasText(stepExecution.getExitStatus().getExitCode())) {
            return stepExecution.getExitStatus().getExitCode();
        }
        return stepExecution.getStatus() == null ? null : stepExecution.getStatus().name();
    }

    private static String resolveSpringBatchStepErrorMessage(StepExecution stepExecution) {
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

    private static boolean isFailedStatus(String status) {
        return "FAILED".equalsIgnoreCase(status) || "STOPPED".equalsIgnoreCase(status);
    }

    private static LocalDateTime toLocalDateTime(java.util.Date date) {
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
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
