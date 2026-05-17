package com.yss.valset.extract.repository.gateway.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.common.exception.TaskNotFoundException;
import com.yss.valset.domain.gateway.WorkflowTaskGateway;
import com.yss.valset.domain.model.TaskStage;
import com.yss.valset.domain.model.TaskStatus;
import com.yss.valset.domain.model.TaskType;
import com.yss.valset.domain.model.WorkflowTask;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * 批量任务 支持的工作流任务网关。
 */
@Repository
@RequiredArgsConstructor
public class WorkflowTaskGatewayImpl implements WorkflowTaskGateway {

    private static final String TASK_ID_PARAM = "taskId";

    private static final String TASK_TYPE_PARAM = "taskType";

    private static final String TASK_STAGE_PARAM = "taskStage";

    private static final String BUSINESS_KEY_PARAM = "businessKey";

    private static final String FILE_ID_PARAM = "fileId";

    private static final String INPUT_PAYLOAD_PARAM = "inputPayload";

    private static final String RESULT_PAYLOAD_CONTEXT_KEY = "resultPayload";

    private static final String RESULT_PAYLOAD_CONTEXT_KEY_ALT = "taskResultPayload";

    private final @Qualifier("springBatchJobExplorer") JobExplorer jobExplorer;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<Long, WorkflowTask> taskCache = new ConcurrentHashMap<>();

    @Override
    public Long save(WorkflowTask workflowTask) {
        Long taskId = IdWorker.getId();
        if (workflowTask == null) {
            return taskId;
        }
        workflowTask.setTaskId(taskId);
        taskCache.put(taskId, copyOf(workflowTask));
        return taskId;
    }

    @Override
    public WorkflowTask findById(Long taskId) {
        if (taskId == null) {
            throw new TaskNotFoundException(null);
        }
        WorkflowTask batchTask = findByBatchExecution(taskId).orElse(null);
        if (batchTask != null) {
            taskCache.put(taskId, copyOf(batchTask));
            return batchTask;
        }
        WorkflowTask cached = taskCache.get(taskId);
        if (cached != null) {
            return copyOf(cached);
        }
        throw new TaskNotFoundException(taskId);
    }

    @Override
    public WorkflowTask findLatestSuccessfulTask(TaskType taskType, String businessKey) {
        if (taskType == null || !StringUtils.hasText(businessKey)) {
            return null;
        }
        WorkflowTask batchTask = findAllBatchTasks().stream()
                .filter(task -> taskType.equals(task.getTaskType())
                        && businessKey.equals(task.getBusinessKey())
                        && TaskStatus.SUCCESS.equals(task.getTaskStatus()))
                .max(Comparator.comparingLong(task -> task.getTaskId() == null ? Long.MIN_VALUE : task.getTaskId()))
                .map(this::copyOf)
                .orElse(null);
        if (batchTask != null) {
            return batchTask;
        }
        return taskCache.values().stream()
                .filter(task -> task != null
                        && taskType.equals(task.getTaskType())
                        && businessKey.equals(task.getBusinessKey())
                        && TaskStatus.SUCCESS.equals(task.getTaskStatus()))
                .max(Comparator.comparingLong(task -> task.getTaskId() == null ? Long.MIN_VALUE : task.getTaskId()))
                .map(this::copyOf)
                .orElse(null);
    }

    @Override
    public boolean markRunning(Long taskId, String taskStage, LocalDateTime taskStartTime) {
        return updateTask(taskId, task -> {
            task.setTaskStatus(TaskStatus.RUNNING);
            if (StringUtils.hasText(taskStage)) {
                try {
                    task.setTaskStage(TaskStage.valueOf(taskStage.trim().toUpperCase()));
                } catch (Exception ignored) {
                    task.setTaskStage(TaskStage.OTHER);
                }
            }
            if (taskStartTime != null) {
                task.setTaskStartTime(taskStartTime);
            }
        });
    }

    @Override
    public boolean markRetrying(Long taskId) {
        return updateTask(taskId, task -> {
            if (task.getTaskStatus() == TaskStatus.FAILED || task.getTaskStatus() == TaskStatus.CANCELED) {
                task.setTaskStatus(TaskStatus.RETRYING);
            }
        });
    }

    @Override
    public void updateTaskTimings(Long taskId, Long parseTaskTimeMs, Long standardizeTimeMs, Long matchStandardSubjectTimeMs) {
        updateTask(taskId, task -> {
            if (parseTaskTimeMs != null) {
                task.setParseTaskTimeMs(parseTaskTimeMs);
            }
            if (standardizeTimeMs != null) {
                task.setStandardizeTimeMs(standardizeTimeMs);
            }
            if (matchStandardSubjectTimeMs != null) {
                task.setMatchStandardSubjectTimeMs(matchStandardSubjectTimeMs);
            }
        });
    }

    @Override
    public void markSuccess(Long taskId, String resultPayload) {
        updateTask(taskId, task -> {
            task.setTaskStatus(TaskStatus.SUCCESS);
            task.setResultPayload(resultPayload);
        });
    }

    @Override
    public void markFailed(Long taskId, String errorMessage) {
        updateTask(taskId, task -> {
            task.setTaskStatus(TaskStatus.FAILED);
            task.setResultPayload(errorMessage);
        });
    }

    private boolean updateTask(Long taskId, java.util.function.Consumer<WorkflowTask> updater) {
        if (taskId == null || updater == null) {
            return false;
        }
        WorkflowTask task = taskCache.computeIfAbsent(taskId, id -> new WorkflowTask());
        task.setTaskId(taskId);
        updater.accept(task);
        taskCache.put(taskId, copyOf(task));
        return true;
    }

    private Optional<WorkflowTask> findByBatchExecution(Long taskId) {
        if (taskId == null || jobExplorer == null) {
            return Optional.empty();
        }
        return findAllExecutions().stream()
                .filter(execution -> execution != null && execution.getJobParameters() != null)
                .filter(execution -> taskId.equals(readTaskId(execution.getJobParameters())))
                .reduce((left, right) -> right)
                .map(this::toTask);
    }

    private List<JobExecution> findAllExecutions() {
        if (jobExplorer == null) {
            return java.util.Collections.emptyList();
        }
        return jobExplorer.getJobNames().stream()
                .flatMap(jobName -> {
                    List<JobInstance> instances = jobExplorer.getJobInstances(jobName, 0, Integer.MAX_VALUE);
                    if (CollectionUtils.isEmpty(instances)) {
                        return java.util.stream.Stream.empty();
                    }
                    return instances.stream().flatMap(instance -> {
                        List<JobExecution> executions = jobExplorer.getJobExecutions(instance);
                        return CollectionUtils.isEmpty(executions) ? java.util.stream.Stream.empty() : executions.stream();
                    });
                })
                .collect(Collectors.toList());
    }

    private List<WorkflowTask> findAllBatchTasks() {
        return findAllExecutions().stream()
                .map(this::toTask)
                .filter(task -> task != null && task.getTaskId() != null)
                .collect(Collectors.toList());
    }

    private WorkflowTask toTask(JobExecution execution) {
        if (execution == null) {
            return null;
        }
        JobParameters jobParameters = execution.getJobParameters();
        Long taskId = readTaskId(jobParameters);
        WorkflowTask cached = taskId == null ? null : taskCache.get(taskId);
        WorkflowTask builder = cached == null ? WorkflowTask.builder().build() : copyOf(cached);
        if (taskId != null) {
            builder.setTaskId(taskId);
        }
        builder.setTaskType(resolveTaskType(jobParameters, builder.getTaskType()));
        builder.setTaskStage(resolveTaskStage(jobParameters, builder.getTaskStage()));
        builder.setBusinessKey(firstText(readString(jobParameters, BUSINESS_KEY_PARAM), builder.getBusinessKey()));
        builder.setFileId(firstLong(readLong(jobParameters, FILE_ID_PARAM), builder.getFileId()));
        builder.setInputPayload(firstText(readString(jobParameters, INPUT_PAYLOAD_PARAM), builder.getInputPayload()));
        builder.setTaskStatus(resolveTaskStatus(execution, builder.getTaskStatus()));
        builder.setTaskStartTime(resolveTaskStartTime(execution, builder.getTaskStartTime()));
        builder.setParseTaskTimeMs(firstLong(readContextLong(execution, "parse.fileParseMs"), builder.getParseTaskTimeMs()));
        builder.setStandardizeTimeMs(firstLong(readContextLong(execution, "parse.standardizeMs"), builder.getStandardizeTimeMs()));
        builder.setMatchStandardSubjectTimeMs(firstLong(readContextLong(execution, "match.standardizeMs"), builder.getMatchStandardSubjectTimeMs()));
        String resultPayload = firstText(readContextString(execution, RESULT_PAYLOAD_CONTEXT_KEY),
                readContextString(execution, RESULT_PAYLOAD_CONTEXT_KEY_ALT),
                builder.getResultPayload());
        if (StringUtils.hasText(resultPayload)) {
            builder.setResultPayload(resultPayload);
        }
        return copyOf(builder);
    }

    private TaskStatus resolveTaskStatus(JobExecution execution, TaskStatus cachedStatus) {
        if (execution == null || execution.getStatus() == null) {
            return cachedStatus;
        }
        BatchStatus batchStatus = execution.getStatus();
        if (BatchStatus.COMPLETED.equals(batchStatus)) {
            return TaskStatus.SUCCESS;
        }
        if (BatchStatus.FAILED.equals(batchStatus)) {
            return TaskStatus.FAILED;
        }
        if (BatchStatus.STOPPED.equals(batchStatus) || BatchStatus.STOPPING.equals(batchStatus)) {
            return TaskStatus.CANCELED;
        }
        if (BatchStatus.STARTED.equals(batchStatus) || BatchStatus.STARTING.equals(batchStatus)) {
            return TaskStatus.RUNNING;
        }
        return cachedStatus == null ? TaskStatus.PENDING : cachedStatus;
    }

    private LocalDateTime resolveTaskStartTime(JobExecution execution, LocalDateTime fallback) {
        if (execution == null || execution.getStartTime() == null) {
            return fallback;
        }
        return execution.getStartTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private TaskType resolveTaskType(JobParameters jobParameters, TaskType fallback) {
        String value = readString(jobParameters, TASK_TYPE_PARAM);
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        try {
            return TaskType.valueOf(value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private TaskStage resolveTaskStage(JobParameters jobParameters, TaskStage fallback) {
        String value = readString(jobParameters, TASK_STAGE_PARAM);
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        try {
            return TaskStage.valueOf(value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private Long readTaskId(JobParameters jobParameters) {
        return readLong(jobParameters, TASK_ID_PARAM);
    }

    private String readString(JobParameters jobParameters, String key) {
        if (jobParameters == null || !StringUtils.hasText(key) || !jobParameters.getParameters().containsKey(key)) {
            return null;
        }
        Object value = jobParameters.getParameters().get(key);
        return value == null ? null : String.valueOf(value);
    }

    private Long readLong(JobParameters jobParameters, String key) {
        if (jobParameters == null || !StringUtils.hasText(key) || !jobParameters.getParameters().containsKey(key)) {
            return null;
        }
        Object value = jobParameters.getParameters().get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private String readContextString(JobExecution execution, String key) {
        if (execution == null || execution.getExecutionContext() == null || !execution.getExecutionContext().containsKey(key)) {
            return null;
        }
        Object value = execution.getExecutionContext().get(key);
        return value == null ? null : String.valueOf(value);
    }

    private Long readContextLong(JobExecution execution, String key) {
        if (execution == null || execution.getExecutionContext() == null || !execution.getExecutionContext().containsKey(key)) {
            return null;
        }
        Object value = execution.getExecutionContext().get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private WorkflowTask copyOf(WorkflowTask workflowTask) {
        if (workflowTask == null) {
            return null;
        }
        return WorkflowTask.builder()
                .taskId(workflowTask.getTaskId())
                .taskType(workflowTask.getTaskType())
                .taskStatus(workflowTask.getTaskStatus())
                .taskStage(workflowTask.getTaskStage())
                .businessKey(workflowTask.getBusinessKey())
                .fileId(workflowTask.getFileId())
                .inputPayload(workflowTask.getInputPayload())
                .resultPayload(workflowTask.getResultPayload())
                .taskStartTime(workflowTask.getTaskStartTime())
                .parseTaskTimeMs(workflowTask.getParseTaskTimeMs())
                .standardizeTimeMs(workflowTask.getStandardizeTimeMs())
                .matchStandardSubjectTimeMs(workflowTask.getMatchStandardSubjectTimeMs())
                .build();
    }

    private String firstText(String... values) {
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
}
