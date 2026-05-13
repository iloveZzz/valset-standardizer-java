package com.yss.valset.transfer.scheduler.task;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.kagkarlsson.scheduler.task.helper.ScheduleAndData;
import com.github.kagkarlsson.scheduler.task.schedule.Schedule;

import java.util.Objects;

/**
 * 分拣对象定时重投递任务调度入参。
 */
public class TransferObjectRedeliverScheduledTaskData implements ScheduleAndData {

    private final Schedule schedule;
    private final TransferObjectRedeliverTaskData payload;

    @JsonCreator
    public TransferObjectRedeliverScheduledTaskData(
            @JsonProperty("schedule") Schedule schedule,
            @JsonProperty("payload") TransferObjectRedeliverTaskData payload) {
        this.schedule = schedule;
        this.payload = payload;
    }

    public Schedule schedule() {
        return schedule;
    }

    public TransferObjectRedeliverTaskData payload() {
        return payload;
    }

    public TransferObjectRedeliverTaskData getPayload() {
        return payload;
    }

    @Override
    public Schedule getSchedule() {
        return schedule;
    }

    @Override
    @JsonIgnore
    public Object getData() {
        return payload;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TransferObjectRedeliverScheduledTaskData that = (TransferObjectRedeliverScheduledTaskData) o;
        return Objects.equals(schedule, that.schedule) && Objects.equals(payload, that.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schedule, payload);
    }

    @Override
    public String toString() {
        return "TransferObjectRedeliverScheduledTaskData{" +
                "schedule=" + schedule +
                ", payload=" + payload +
                '}';
    }
}
