package com.yss.subjectmatch.batch.scheduler;

import com.yss.subjectmatch.domain.gateway.ScheduleGateway;
import com.yss.subjectmatch.domain.gateway.TaskGateway;
import com.yss.subjectmatch.domain.model.ScheduleDefinition;
import com.yss.subjectmatch.domain.model.TaskInfo;
import com.yss.subjectmatch.domain.model.TaskStatus;
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
