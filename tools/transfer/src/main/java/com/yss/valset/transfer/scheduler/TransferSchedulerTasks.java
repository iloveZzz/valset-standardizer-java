package com.yss.valset.transfer.scheduler;

import com.github.kagkarlsson.scheduler.task.TaskDescriptor;
import com.yss.valset.transfer.scheduler.task.TransferDeliverTaskData;
import com.yss.valset.transfer.scheduler.task.TransferIngestScheduledTaskData;
import com.yss.valset.transfer.scheduler.task.TransferIngestTaskData;
import com.yss.valset.transfer.scheduler.task.TransferRouteTaskData;
import com.yss.valset.transfer.scheduler.task.TransferRunLogCleanupScheduledTaskData;

/**
 * 文件分拣调度任务标识。
 */
public final class TransferSchedulerTasks {

    public static final TaskDescriptor<TransferIngestTaskData> INGEST_ONCE_TASK =
            TaskDescriptor.of("transfer-ingest-once", TransferIngestTaskData.class);

    public static final TaskDescriptor<TransferIngestScheduledTaskData> INGEST_CRON_TASK =
            TaskDescriptor.of("transfer-ingest-cron", TransferIngestScheduledTaskData.class);

    public static final TaskDescriptor<TransferRouteTaskData> ROUTE_TASK =
            TaskDescriptor.of("transfer-route", TransferRouteTaskData.class);

    public static final TaskDescriptor<TransferDeliverTaskData> DELIVER_TASK =
            TaskDescriptor.of("transfer-deliver", TransferDeliverTaskData.class);

    public static final TaskDescriptor<TransferRunLogCleanupScheduledTaskData> RUN_LOG_CLEANUP_TASK =
            TaskDescriptor.of("transfer-run-log-cleanup", TransferRunLogCleanupScheduledTaskData.class);

    private TransferSchedulerTasks() {
    }
}
