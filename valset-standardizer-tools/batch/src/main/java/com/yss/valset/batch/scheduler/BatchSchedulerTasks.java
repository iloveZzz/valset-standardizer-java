package com.yss.valset.batch.scheduler;

import com.github.kagkarlsson.scheduler.task.TaskDescriptor;
import com.yss.valset.batch.scheduler.task.BatchExecutionContextCleanupScheduledTaskData;

/**
 * 批处理调度任务定义。
 */
public final class BatchSchedulerTasks {

    public static final TaskDescriptor<BatchDispatchTaskData> DISPATCH_ONCE_TASK =
            TaskDescriptor.of("batch-dispatch-once", BatchDispatchTaskData.class);

    public static final TaskDescriptor<BatchExecutionContextCleanupScheduledTaskData> EXECUTION_CONTEXT_CLEANUP_TASK =
            TaskDescriptor.of("batch-execution-context-cleanup", BatchExecutionContextCleanupScheduledTaskData.class);

    private BatchSchedulerTasks() {
    }
}
