package com.yss.valset.batch.scheduler;

import lombok.Value;

/**
 * 批处理立即触发任务数据。
 */
@Value
public class BatchDispatchTaskData {

    Long taskId;

    public Long taskId() {
        return taskId;
    }
}
