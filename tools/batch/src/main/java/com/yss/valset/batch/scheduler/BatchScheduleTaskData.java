package com.yss.valset.batch.scheduler;

import com.github.kagkarlsson.scheduler.task.helper.ScheduleAndData;
import com.github.kagkarlsson.scheduler.task.schedule.Schedule;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;

/**
 * 批处理循环任务数据。
 */
public record BatchScheduleTaskData(
        Schedule schedule,
        Long scheduleId
) implements ScheduleAndData, Serializable {

    @Override
    public Schedule getSchedule() {
        return schedule;
    }

    @Override
    @JsonIgnore
    public Object getData() {
        return scheduleId;
    }
}
