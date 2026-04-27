package com.yss.valset.transfer.scheduler;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.exceptions.TaskInstanceNotFoundException;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import com.yss.valset.transfer.scheduler.task.TransferRunLogCleanupScheduledTaskData;
import com.yss.valset.transfer.scheduler.task.TransferRunLogCleanupTaskData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 应用启动后的文件收发运行日志清理调度初始化任务。
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 1)
@ConditionalOnProperty(prefix = "db-scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TransferRunLogCleanupBootstrapRunner implements ApplicationRunner {

    private static final String INSTANCE_ID = "default";

    private final SchedulerClient schedulerClient;
    private final String cleanupCronExpression;

    public TransferRunLogCleanupBootstrapRunner(SchedulerClient schedulerClient,
                                                @Value("${subject.match.transfer.run-log-cleanup.cron:0 0 23 * * ?}") String cleanupCronExpression) {
        this.schedulerClient = schedulerClient;
        this.cleanupCronExpression = cleanupCronExpression;
    }

    @Override
    public void run(ApplicationArguments args) {
        SchedulableInstance<TransferRunLogCleanupScheduledTaskData> schedulableInstance = TransferSchedulerTasks.RUN_LOG_CLEANUP_TASK
                .instance(INSTANCE_ID)
                .data(new TransferRunLogCleanupScheduledTaskData(
                        Schedules.cron(DbTransferJobScheduler.normalizeCronExpression(cleanupCronExpression)),
                        new TransferRunLogCleanupTaskData()
                ))
                .scheduledAccordingToData();
        boolean rescheduled;
        try {
            rescheduled = schedulerClient.reschedule(schedulableInstance);
        } catch (TaskInstanceNotFoundException exception) {
            rescheduled = false;
        }
        if (!rescheduled) {
            schedulerClient.scheduleIfNotExists(schedulableInstance);
        }
    }
}
