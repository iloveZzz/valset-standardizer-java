package com.yss.valset.batch.scheduler;

import com.yss.valset.domain.gateway.ScheduleGateway;
import com.yss.valset.domain.gateway.TaskGateway;
import com.yss.valset.domain.model.ScheduleDefinition;
import com.yss.valset.domain.model.TaskInfo;
import com.yss.valset.domain.model.TaskStatus;
import org.springframework.stereotype.Component;

/**
 * 默认计划任务创建者。
 */
@Component
public class DefaultScheduleTaskCreator implements ScheduleTaskCreator {

    private final ScheduleGateway scheduleGateway;
    private final TaskGateway taskGateway;

    public DefaultScheduleTaskCreator(ScheduleGateway scheduleGateway, TaskGateway taskGateway) {
        this.scheduleGateway = scheduleGateway;
        this.taskGateway = taskGateway;
    }

    /**
     * 从持久计划定义创建待处理任务。
     */
    @Override
    public Long createTaskFromSchedule(Long scheduleId) {
        ScheduleDefinition schedule = scheduleGateway.findById(scheduleId);
        TaskInfo taskInfo = TaskInfo.builder()
                .taskType(schedule.getTaskType())
                .taskStatus(TaskStatus.PENDING)
                .businessKey(schedule.buildRuntimeBusinessKey())
                .build();
        return taskGateway.save(taskInfo);
    }
}
