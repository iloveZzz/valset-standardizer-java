package com.yss.valset.transfer.scheduler.task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.kagkarlsson.scheduler.task.helper.ScheduleAndData;
import com.github.kagkarlsson.scheduler.task.schedule.Schedule;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分拣对象定时重投递任务调度入参。
 */
public record TransferObjectRedeliverScheduledTaskData(
        Schedule schedule,
        TransferObjectRedeliverTaskData payload
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
