package com.yss.valset.application.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.application.dto.TaskViewDTO;
import com.yss.valset.application.service.WorkflowTaskQueryAppService;
import com.yss.valset.domain.gateway.WorkflowTaskGateway;
import com.yss.valset.domain.model.TaskStatus;
import com.yss.valset.domain.model.WorkflowTask;
import com.yss.valset.parser.application.support.ParseBatchStepSupport;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 任务查询的默认实现。
 */
@Service
public class DefaultWorkflowTaskQueryAppService implements WorkflowTaskQueryAppService {

    private static final String VALUATION_PARSE_JOB_NAME = "valuationParseJob";

    private final WorkflowTaskGateway taskGateway;
    private final JobExplorer jobExplorer;
    private final ObjectMapper objectMapper;

    public DefaultWorkflowTaskQueryAppService(WorkflowTaskGateway taskGateway,
                                              @Qualifier("springBatchJobExplorer") JobExplorer jobExplorer,
                                              ObjectMapper objectMapper) {
        this.taskGateway = taskGateway;
        this.jobExplorer = jobExplorer;
        this.objectMapper = objectMapper;
    }

    /**
     * 查询任务并将有效负载扩展为 JSON 友好的映射。
     */
    @Override
    public TaskViewDTO queryTask(Long taskId) {
        Optional<JobExecution> batchExecution = findParseBatchExecution(taskId);
        if (batchExecution.isPresent()) {
            return buildBatchTaskView(findLegacyTask(taskId), batchExecution.get(), taskId);
        }
        return buildLegacyTaskView(findLegacyTask(taskId));
    }

    private WorkflowTask findLegacyTask(Long taskId) {
        try {
            return taskGateway.findById(taskId);
        } catch (Exception ignored) {
            return null;
        }
    }

    private TaskViewDTO buildBatchTaskView(WorkflowTask workflowTask, JobExecution jobExecution, Long taskId) {
        JobParameters jobParameters = jobExecution == null ? null : jobExecution.getJobParameters();
        String taskStatus = jobExecution == null || jobExecution.getStatus() == null
                ? resolveLegacyStatus(workflowTask)
                : jobExecution.getStatus().name();
        boolean failedTask = isFailedStatus(taskStatus);
        String inputPayload = firstText(readString(jobParameters, "inputPayload"), workflowTask == null ? null : workflowTask.getInputPayload());
        String resultPayload = firstText(readString(jobExecution == null ? null : jobExecution.getExecutionContext(), ParseBatchStepSupport.JOB_CONTEXT_RESULT_PAYLOAD),
                workflowTask == null ? null : workflowTask.getResultPayload());
        Map<String, Object> resultData = parsePayload(resultPayload, failedTask);
        return TaskViewDTO.builder()
                .taskId(taskId == null ? null : String.valueOf(taskId))
                .taskType(firstText(readString(jobParameters, "taskType"), workflowTask == null || workflowTask.getTaskType() == null ? null : workflowTask.getTaskType().name()))
                .taskStage(firstText(readString(jobParameters, "taskStage"), workflowTask == null || workflowTask.getTaskStage() == null ? null : workflowTask.getTaskStage().name()))
                .taskStatus(taskStatus)
                .businessKey(firstText(readString(jobParameters, "businessKey"), workflowTask == null ? null : workflowTask.getBusinessKey()))
                .inputPayload(inputPayload)
                .inputData(parsePayload(inputPayload, false))
                .resultPayload(resultPayload)
                .resultData(resultData)
                .errorMessage(failedTask ? resolveErrorMessage(jobExecution, resultData, resultPayload) : null)
                .errorCode(failedTask ? resolveErrorCode(jobExecution, resultData) : null)
                .rowCount(resolveRowCount(resultData))
                .fileSizeBytes(null)
                .durationMs(resolveDurationMs(jobExecution))
                .taskStartTime(resolveTaskStartTime(jobExecution, workflowTask))
                .parseTaskTimeMs(firstText(resolveExecutionContextLong(jobExecution, ParseBatchStepSupport.JOB_CONTEXT_FILE_PARSE_MS), workflowTask == null ? null : stringValue(workflowTask.getParseTaskTimeMs())))
                .standardizeTimeMs(firstText(resolveExecutionContextLong(jobExecution, ParseBatchStepSupport.JOB_CONTEXT_STANDARDIZE_MS), workflowTask == null ? null : stringValue(workflowTask.getStandardizeTimeMs())))
                .matchStandardSubjectTimeMs(workflowTask == null ? null : stringValue(workflowTask.getMatchStandardSubjectTimeMs()))
                .build();
    }

    private TaskViewDTO buildLegacyTaskView(WorkflowTask workflowTask) {
        if (workflowTask == null) {
            throw new IllegalStateException("未找到任务记录");
        }
        String taskStatus = workflowTask.getTaskStatus() == null ? null : workflowTask.getTaskStatus().name();
        boolean failedTask = isFailedStatus(taskStatus);
        Map<String, Object> resultData = parsePayload(workflowTask.getResultPayload(), failedTask);
        return TaskViewDTO.builder()
                .taskId(workflowTask.getTaskId() == null ? null : String.valueOf(workflowTask.getTaskId()))
                .taskType(workflowTask.getTaskType() == null ? null : workflowTask.getTaskType().name())
                .taskStage(workflowTask.getTaskStage() == null ? null : workflowTask.getTaskStage().name())
                .taskStatus(taskStatus)
                .businessKey(workflowTask.getBusinessKey())
                .inputPayload(workflowTask.getInputPayload())
                .inputData(parsePayload(workflowTask.getInputPayload(), false))
                .resultPayload(workflowTask.getResultPayload())
                .resultData(resultData)
                .errorMessage(failedTask ? resolveErrorMessage(null, resultData, workflowTask.getResultPayload()) : null)
                .errorCode(failedTask ? resolveErrorCode(null, resultData) : null)
                .rowCount(resolveRowCount(resultData))
                .fileSizeBytes(null)
                .durationMs(null)
                .taskStartTime(workflowTask.getTaskStartTime())
                .parseTaskTimeMs(stringValue(workflowTask.getParseTaskTimeMs()))
                .standardizeTimeMs(stringValue(workflowTask.getStandardizeTimeMs()))
                .matchStandardSubjectTimeMs(stringValue(workflowTask.getMatchStandardSubjectTimeMs()))
                .build();
    }

    private Optional<JobExecution> findParseBatchExecution(Long taskId) {
        if (taskId == null) {
            return Optional.empty();
        }
        List<JobInstance> jobInstances = jobExplorer.getJobInstances(VALUATION_PARSE_JOB_NAME, 0, Integer.MAX_VALUE);
        if (CollectionUtils.isEmpty(jobInstances)) {
            return Optional.empty();
        }
        for (JobInstance jobInstance : jobInstances) {
            List<JobExecution> executions = jobExplorer.getJobExecutions(jobInstance);
            if (CollectionUtils.isEmpty(executions)) {
                continue;
            }
            JobExecution matched = executions.stream()
                    .filter(execution -> execution != null && execution.getJobParameters() != null)
                    .filter(execution -> taskId.equals(execution.getJobParameters().getLong("taskId", null)))
                    .reduce((left, right) -> right)
                    .orElse(null);
            if (matched != null) {
                return Optional.of(matched);
            }
        }
        return Optional.empty();
    }

    private Map<String, Object> parsePayload(String payload, boolean failedTask) {
        if (!StringUtils.hasText(payload)) {
            return null;
        }
        try {
            return objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception exception) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            if (failedTask) {
                fallback.put("errorMessage", payload);
            } else {
                fallback.put("rawText", payload);
            }
            return fallback;
        }
    }

    private String resolveErrorMessage(JobExecution jobExecution, Map<String, Object> resultData, String rawPayload) {
        if (resultData != null) {
            Object errorMessage = resultData.get("errorMessage");
            if (errorMessage != null && StringUtils.hasText(String.valueOf(errorMessage))) {
                return String.valueOf(errorMessage).trim();
            }
            Object rootCauseMessage = resultData.get("rootCauseMessage");
            if (rootCauseMessage != null && StringUtils.hasText(String.valueOf(rootCauseMessage))) {
                return String.valueOf(rootCauseMessage).trim();
            }
        }
        if (jobExecution != null) {
            if (jobExecution.getExitStatus() != null && StringUtils.hasText(jobExecution.getExitStatus().getExitDescription())) {
                return jobExecution.getExitStatus().getExitDescription();
            }
            if (jobExecution.getAllFailureExceptions() != null && !jobExecution.getAllFailureExceptions().isEmpty()) {
                Throwable throwable = jobExecution.getAllFailureExceptions().get(0);
                if (throwable != null && StringUtils.hasText(throwable.getMessage())) {
                    return throwable.getMessage();
                }
            }
        }
        return StringUtils.hasText(rawPayload) ? rawPayload.trim() : null;
    }

    private String resolveErrorCode(JobExecution jobExecution, Map<String, Object> resultData) {
        if (resultData != null) {
            Object errorCode = resultData.get("errorCode");
            if (errorCode != null && StringUtils.hasText(String.valueOf(errorCode))) {
                return String.valueOf(errorCode).trim();
            }
        }
        return jobExecution == null || jobExecution.getStatus() == null ? null : jobExecution.getStatus().name();
    }

    private String resolveRowCount(Map<String, Object> resultData) {
        if (resultData == null) {
            return null;
        }
        Long rowCount = extractLong(resultData, "rowCount");
        if (rowCount != null) {
            return String.valueOf(rowCount);
        }
        Long subjectCount = extractLong(resultData, "subjectCount");
        if (subjectCount != null) {
            return String.valueOf(subjectCount);
        }
        Long metricCount = extractLong(resultData, "metricCount");
        return metricCount == null ? null : String.valueOf(metricCount);
    }

    private Long extractLong(Map<String, Object> resultData, String fieldName) {
        if (resultData == null || !StringUtils.hasText(fieldName)) {
            return null;
        }
        Object value = resultData.get(fieldName);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            String text = ((String) value).trim();
            if (!text.isEmpty()) {
                try {
                    return Long.parseLong(text);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private String readString(JobParameters jobParameters, String key) {
        if (jobParameters == null || !StringUtils.hasText(key)) {
            return null;
        }
        return jobParameters.getString(key, null);
    }

    private String readString(org.springframework.batch.item.ExecutionContext executionContext, String key) {
        if (executionContext == null || !StringUtils.hasText(key) || !executionContext.containsKey(key)) {
            return null;
        }
        return executionContext.getString(key, null);
    }

    private String resolveExecutionContextLong(JobExecution jobExecution, String key) {
        if (jobExecution == null || jobExecution.getExecutionContext() == null || !StringUtils.hasText(key)) {
            return null;
        }
        if (!jobExecution.getExecutionContext().containsKey(key)) {
            return null;
        }
        return stringValue(jobExecution.getExecutionContext().getLong(key, 0L));
    }

    private String resolveDurationMs(JobExecution jobExecution) {
        if (jobExecution == null || jobExecution.getStartTime() == null || jobExecution.getEndTime() == null) {
            return null;
        }
        long duration = Duration.between(
                jobExecution.getStartTime().toInstant(),
                jobExecution.getEndTime().toInstant()).toMillis();
        return String.valueOf(duration);
    }

    private LocalDateTime resolveTaskStartTime(JobExecution jobExecution, WorkflowTask workflowTask) {
        if (jobExecution != null && jobExecution.getStartTime() != null) {
            return jobExecution.getStartTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
        return workflowTask == null ? null : workflowTask.getTaskStartTime();
    }

    private String resolveLegacyStatus(WorkflowTask workflowTask) {
        return workflowTask == null || workflowTask.getTaskStatus() == null ? null : workflowTask.getTaskStatus().name();
    }

    private boolean isFailedStatus(String status) {
        return TaskStatus.FAILED.name().equalsIgnoreCase(status);
    }

    private String firstText(String primary, String fallback) {
        if (StringUtils.hasText(primary)) {
            return primary.trim();
        }
        if (StringUtils.hasText(fallback)) {
            return fallback.trim();
        }
        return null;
    }

    private String stringValue(Long value) {
        return value == null ? null : String.valueOf(value);
    }
}
