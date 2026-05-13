package com.yss.valset.batch.scheduler;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 批处理立即触发任务数据。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BatchDispatchTaskData {

    private final Long taskId;

    @JsonCreator
    public BatchDispatchTaskData(@JsonProperty("taskId") Long taskId) {
        this.taskId = taskId;
    }

    public Long taskId() {
        return taskId;
    }
}
