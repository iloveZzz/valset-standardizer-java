package com.yss.subjectmatch.batch.dispatcher;

import com.yss.subjectmatch.batch.scheduler.ScheduleTaskCreator;
import com.yss.subjectmatch.batch.task.TaskExecutor;
import com.yss.subjectmatch.domain.gateway.TaskGateway;
import com.yss.subjectmatch.domain.model.TaskInfo;
import com.yss.subjectmatch.domain.model.TaskType;
import org.springframework.stereotype.Component;

import java.util.List;
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

    public DefaultTaskDispatcher(
            TaskGateway taskGateway,
            ScheduleTaskCreator scheduleTaskCreator,
            List<TaskExecutor> executorList
    ) {
        this.taskGateway = taskGateway;
        this.scheduleTaskCreator = scheduleTaskCreator;
        this.executors = executorList.stream().collect(Collectors.toMap(TaskExecutor::support, Function.identity()));
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
            taskGateway.markFailed(taskId, ex.getMessage());
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
}
