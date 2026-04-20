package com.yss.valset.batch.dispatcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.batch.scheduler.ScheduleTaskCreator;
import com.yss.valset.batch.task.TaskExecutor;
import com.yss.valset.common.support.TaskFailureClassifier;
import com.yss.valset.domain.gateway.TaskGateway;
import com.yss.valset.domain.model.TaskInfo;
import com.yss.valset.domain.model.TaskType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 默认任务调度程序。
 */
@Component
public class DefaultTaskDispatcher implements TaskDispatcher {

    private final TaskGateway taskGateway;
    private final ScheduleTaskCreator scheduleTaskCreator;
    private final Map<TaskType, TaskExecutor> executors;
    private final ObjectMapper objectMapper;

    public DefaultTaskDispatcher(
            TaskGateway taskGateway,
            ScheduleTaskCreator scheduleTaskCreator,
            List<TaskExecutor> executorList,
            ObjectMapper objectMapper
    ) {
        this.taskGateway = taskGateway;
        this.scheduleTaskCreator = scheduleTaskCreator;
        this.executors = executorList.stream().collect(Collectors.toMap(TaskExecutor::support, Function.identity()));
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    /**
     * 通过其注册的执行器分派任务。
     */
    @Override
    public void dispatchTask(Long taskId) {
        TaskInfo task = taskGateway.findById(taskId);
        TaskExecutor executor = executors.get(task.getTaskType());
        if (executor == null) {
            throw new IllegalStateException("No executor for taskType=" + task.getTaskType());
        }

        boolean locked = taskGateway.markRunning(taskId);
        if (!locked) {
            return;
        }

        try {
            executor.execute(taskId);
            String resultPayload = taskGateway.findById(taskId).getResultPayload();
            taskGateway.markSuccess(taskId, resultPayload);
        } catch (Exception ex) {
            taskGateway.markFailed(taskId, buildFailurePayload(task.getTaskType(), ex));
            throw ex;
        }
    }

    /**
     * 从计划中创建任务并立即分派它。
     */
    @Override
    public void dispatchSchedule(Long scheduleId) {
        Long taskId = scheduleTaskCreator.createTaskFromSchedule(scheduleId);
        dispatchTask(taskId);
    }

    private String buildFailurePayload(TaskType taskType, Exception exception) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskType", taskType == null ? null : taskType.name());
        payload.put("errorCode", TaskFailureClassifier.classify(exception));
        payload.put("errorMessage", TaskFailureClassifier.resolveReadableMessage(exception));
        payload.put("rootCauseMessage", TaskFailureClassifier.rootCauseMessage(exception));
        payload.put("errorType", exception == null ? null : exception.getClass().getName());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception jsonException) {
            return payload.toString();
        }
    }
}
