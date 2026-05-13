package com.yss.valset.transfer.scheduler.task;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.kagkarlsson.scheduler.task.helper.ScheduleAndData;
import com.github.kagkarlsson.scheduler.task.schedule.Schedule;

import java.util.Objects;

/**
 * 文件来源轮询任务入参。
 */
public class TransferIngestScheduledTaskData implements ScheduleAndData {

    private final Schedule schedule;
    private final TransferIngestTaskData payload;

    @JsonCreator
    public TransferIngestScheduledTaskData(
            @JsonProperty("schedule") Schedule schedule,
            @JsonProperty("payload") TransferIngestTaskData payload) {
        this.schedule = schedule;
        this.payload = payload;
    }

    public Schedule schedule() {
        return schedule;
    }

    public TransferIngestTaskData payload() {
        return payload;
    }

    public TransferIngestTaskData getPayload() {
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
        TransferIngestScheduledTaskData that = (TransferIngestScheduledTaskData) o;
        return Objects.equals(schedule, that.schedule) && Objects.equals(payload, that.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schedule, payload);
    }

    @Override
    public String toString() {
        return "TransferIngestScheduledTaskData{" +
                "schedule=" + schedule +
                ", payload=" + payload +
                '}';
    }
}
