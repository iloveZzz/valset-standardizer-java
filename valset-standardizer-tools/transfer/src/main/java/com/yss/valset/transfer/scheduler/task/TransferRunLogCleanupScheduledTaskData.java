package com.yss.valset.transfer.scheduler.task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.kagkarlsson.scheduler.task.helper.ScheduleAndData;
import com.github.kagkarlsson.scheduler.task.schedule.Schedule;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文件收发运行日志清理定时任务入参。
 */
public record TransferRunLogCleanupScheduledTaskData(
        Schedule schedule,
        TransferRunLogCleanupTaskData payload
) implements ScheduleAndData, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public Schedule getSchedule() {
        return schedule;
    }

    @Override
    @JsonIgnore
    public Object getData() {
        return payload;
    }
}
