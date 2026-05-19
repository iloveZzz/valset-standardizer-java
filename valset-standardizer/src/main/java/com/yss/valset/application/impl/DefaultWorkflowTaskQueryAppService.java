package com.yss.valset.application.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.application.dto.TaskViewDTO;
import com.yss.valset.application.service.WorkflowTaskQueryAppService;
import com.yss.valset.common.support.TaskFailureClassifier;
import com.yss.valset.domain.gateway.WorkflowTaskGateway;
import com.yss.valset.domain.model.TaskStatus;
import com.yss.valset.domain.model.WorkflowTask;
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

    private final JobExplorer jobExplorer;
    private final ObjectMapper objectMapper;
    private final WorkflowTaskGateway workflowTaskGateway;

    public DefaultWorkflowTaskQueryAppService(@Qualifier("springBatchJobExplorer") JobExplorer jobExplorer,
                                              ObjectMapper objectMapper,
                                              WorkflowTaskGateway workflowTaskGateway) {
        this.jobExplorer = jobExplorer;
        this.objectMapper = objectMapper;
        this.workflowTaskGateway = workflowTaskGateway;
    }

    /**
     * 查询任务并将有效负载扩展为 JSON 友好的映射。
     */
    @Override
    public TaskViewDTO queryTask(Long taskId) {
        Optional<JobExecution> batchExecution = findBatchExecution(taskId);
        if (batchExecution.isPresent()) {
            return buildBatchTaskView(batchExecution.get(), taskId);
        }
        throw new IllegalStateException("未找到 Batch 任务记录，taskId=" + taskId);
    }

    private TaskViewDTO buildBatchTaskView(JobExecution jobExecution, Long taskId) {
        JobParameters jobParameters = jobExecution == null ? null : jobExecution.getJobParameters();
        WorkflowTask workflowTask = taskId == null ? null : workflowTaskGateway.findById(taskId);
        String taskStatus = jobExecution == null || jobExecution.getStatus() == null ? null : jobExecution.getStatus().name();
        boolean failedTask = isFailedStatus(taskStatus);
        String inputPayload = firstText(workflowTask == null ? null : workflowTask.getInputPayload(),
                readString(jobParameters, "inputPayload"),
                readContextString(jobExecution, "inputPayload"));
        String resultPayload = firstText(workflowTask == null ? null : workflowTask.getResultPayload(),
                readContextString(jobExecution, "resultPayload"),
                readContextString(jobExecution, "taskResultPayload"));
        Map<String, Object> resultData = parsePayload(resultPayload, failedTask);
        return TaskViewDTO.builder()
                .taskId(taskId == null ? null : String.valueOf(taskId))
                .taskType(readString(jobParameters, "taskType"))
                .taskStage(readString(jobParameters, "taskStage"))
                .taskStatus(taskStatus)
                .businessKey(readString(jobParameters, "businessKey"))
                .inputPayload(inputPayload)
                .inputData(parsePayload(inputPayload, false))
                .resultPayload(resultPayload)
                .resultData(resultData)
                .errorMessage(failedTask ? resolveErrorMessage(jobExecution, resultData, resultPayload) : null)
                .errorCode(failedTask ? resolveErrorCode(jobExecution, resultData) : null)
                .rowCount(resolveRowCount(resultData))
                .fileSizeBytes(null)
                .durationMs(resolveDurationMs(jobExecution))
                .taskStartTime(resolveTaskStartTime(jobExecution))
                .parseTaskTimeMs(stringValue(readContextLong(jobExecution, "parse.fileParseMs")))
                .standardizeTimeMs(stringValue(readContextLong(jobExecution, "parse.standardizeMs")))
                .matchStandardSubjectTimeMs(stringValue(readContextLong(jobExecution, "match.standardizeMs")))
                .build();
    }

    private Optional<JobExecution> findBatchExecution(Long taskId) {
        if (taskId == null) {
            return Optional.empty();
        }
        List<String> jobNames = jobExplorer.getJobNames();
        if (CollectionUtils.isEmpty(jobNames)) {
            return Optional.empty();
        }
        for (String jobName : jobNames) {
            List<JobInstance> jobInstances = jobExplorer.getJobInstances(jobName, 0, Integer.MAX_VALUE);
            if (CollectionUtils.isEmpty(jobInstances)) {
                continue;
            }
            for (JobInstance jobInstance : jobInstances) {
                List<JobExecution> executions = jobExplorer.getJobExecutions(jobInstance);
                if (CollectionUtils.isEmpty(executions)) {
                    continue;
                }
                JobExecution matched = executions.stream()
                        .filter(execution -> execution != null && execution.getJobParameters() != null)
                        .filter(execution -> taskId.equals(readTaskId(execution.getJobParameters())))
                        .reduce((left, right) -> right)
                        .orElse(null);
                if (matched != null) {
                    return Optional.of(matched);
                }
            }
        }
        return Optional.empty();
    }

    private Long readTaskId(JobParameters jobParameters) {
        if (jobParameters == null || jobParameters.getParameters() == null || !jobParameters.getParameters().containsKey("taskId")) {
            return null;
        }
        Object value = jobParameters.getParameters().get("taskId");
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (Exception ignored) {
            return null;
        }
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
            if (jobExecution.getAllFailureExceptions() != null && !jobExecution.getAllFailureExceptions().isEmpty()) {
                Throwable throwable = jobExecution.getAllFailureExceptions().get(0);
                String message = TaskFailureClassifier.resolveReadableMessage(throwable);
                if (StringUtils.hasText(message)) {
                    return message;
                }
            }
            if (jobExecution.getExitStatus() != null && StringUtils.hasText(jobExecution.getExitStatus().getExitDescription())) {
                return jobExecution.getExitStatus().getExitDescription();
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

    private String readContextString(JobExecution jobExecution, String key) {
        if (jobExecution == null || jobExecution.getExecutionContext() == null || !StringUtils.hasText(key)) {
            return null;
        }
        if (!jobExecution.getExecutionContext().containsKey(key)) {
            return null;
        }
        Object value = jobExecution.getExecutionContext().get(key);
        return value == null ? null : String.valueOf(value);
    }

    private Long readContextLong(JobExecution jobExecution, String key) {
        if (jobExecution == null || jobExecution.getExecutionContext() == null || !StringUtils.hasText(key)) {
            return null;
        }
        if (!jobExecution.getExecutionContext().containsKey(key)) {
            return null;
        }
        Object value = jobExecution.getExecutionContext().get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (Exception ignored) {
            return null;
        }
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

    private LocalDateTime resolveTaskStartTime(JobExecution jobExecution) {
        if (jobExecution == null || jobExecution.getStartTime() == null) {
            return null;
        }
        return jobExecution.getStartTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private boolean isFailedStatus(String status) {
        return TaskStatus.FAILED.name().equalsIgnoreCase(status);
    }

    private String firstText(String... candidates) {
        if (candidates == null) {
            return null;
        }
        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate)) {
                return candidate.trim();
            }
        }
        return null;
    }

    private String stringValue(Long value) {
        return value == null ? null : String.valueOf(value);
    }
}
