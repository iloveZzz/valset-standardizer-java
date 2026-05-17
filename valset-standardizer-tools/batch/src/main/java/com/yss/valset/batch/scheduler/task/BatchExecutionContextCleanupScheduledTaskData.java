package com.yss.valset.batch.scheduler.task;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.kagkarlsson.scheduler.task.helper.ScheduleAndData;
import com.github.kagkarlsson.scheduler.task.schedule.Schedule;

import java.util.Objects;

/**
 * 批处理执行上下文清理定时任务入参。
 */
public class BatchExecutionContextCleanupScheduledTaskData implements ScheduleAndData {

    private final Schedule schedule;
    private final BatchExecutionContextCleanupTaskData payload;

    @JsonCreator
    public BatchExecutionContextCleanupScheduledTaskData(
            @JsonProperty("schedule") Schedule schedule,
            @JsonProperty("payload") BatchExecutionContextCleanupTaskData payload) {
        this.schedule = schedule;
        this.payload = payload;
    }

    public Schedule schedule() {
        return schedule;
    }

    public BatchExecutionContextCleanupTaskData payload() {
        return payload;
    }

    public BatchExecutionContextCleanupTaskData getPayload() {
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
        BatchExecutionContextCleanupScheduledTaskData that = (BatchExecutionContextCleanupScheduledTaskData) o;
        return Objects.equals(schedule, that.schedule) && Objects.equals(payload, that.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schedule, payload);
    }

    @Override
    public String toString() {
        return "BatchExecutionContextCleanupScheduledTaskData{" +
                "schedule=" + schedule +
                ", payload=" + payload +
                '}';
    }
}
