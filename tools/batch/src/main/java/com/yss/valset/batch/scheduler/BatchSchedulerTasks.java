package com.yss.valset.batch.scheduler;

import com.github.kagkarlsson.scheduler.task.TaskDescriptor;

/**
 * 批处理调度任务定义。
 */
public final class BatchSchedulerTasks {

    public static final TaskDescriptor<BatchDispatchTaskData> DISPATCH_ONCE_TASK =
            TaskDescriptor.of("batch-dispatch-once", BatchDispatchTaskData.class);

    private BatchSchedulerTasks() {
    }
}
