package com.yss.valset.batch.scheduler;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.exceptions.TaskInstanceCurrentlyExecutingException;
import com.github.kagkarlsson.scheduler.exceptions.TaskInstanceNotFoundException;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import com.yss.valset.batch.scheduler.task.BatchExecutionContextCleanupScheduledTaskData;
import com.yss.valset.batch.scheduler.task.BatchExecutionContextCleanupTaskData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 应用启动后的批处理执行上下文清理调度初始化任务。
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 1)
@ConditionalOnProperty(prefix = "db-scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BatchMetadataCleanupBootstrapRunner implements ApplicationRunner {

    private static final String INSTANCE_ID = "default";

    private final SchedulerClient schedulerClient;
    private final String cleanupCronExpression;

    public BatchMetadataCleanupBootstrapRunner(SchedulerClient schedulerClient,
                                               @Value("${subject.match.batch.execution-context-cleanup.cron:0 0 1 * * ?}") String cleanupCronExpression) {
        this.schedulerClient = schedulerClient;
        this.cleanupCronExpression = cleanupCronExpression;
    }

    @Override
    public void run(ApplicationArguments args) {
        SchedulableInstance<BatchExecutionContextCleanupScheduledTaskData> schedulableInstance = BatchSchedulerTasks.EXECUTION_CONTEXT_CLEANUP_TASK
                .instance(INSTANCE_ID)
                .data(new BatchExecutionContextCleanupScheduledTaskData(
                        Schedules.cron(BatchSchedulerCronSupport.normalizeCronExpression(cleanupCronExpression)),
                        new BatchExecutionContextCleanupTaskData()
                ))
                .scheduledAccordingToData();
        boolean rescheduled;
        try {
            rescheduled = schedulerClient.reschedule(schedulableInstance);
        } catch (TaskInstanceNotFoundException exception) {
            rescheduled = false;
        } catch (TaskInstanceCurrentlyExecutingException exception) {
            return;
        }
        if (!rescheduled) {
            schedulerClient.scheduleIfNotExists(schedulableInstance);
        }
    }
}
