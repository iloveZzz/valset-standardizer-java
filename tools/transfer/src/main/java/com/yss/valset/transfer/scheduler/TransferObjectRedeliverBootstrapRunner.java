package com.yss.valset.transfer.scheduler;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.exceptions.TaskInstanceNotFoundException;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import com.yss.valset.transfer.scheduler.task.TransferObjectRedeliverScheduledTaskData;
import com.yss.valset.transfer.scheduler.task.TransferObjectRedeliverTaskData;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 应用启动后的分拣对象定时重投递调度初始化任务。
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 2)
@ConditionalOnProperty(prefix = "db-scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TransferObjectRedeliverBootstrapRunner implements ApplicationRunner {

    private static final String INSTANCE_ID = "default";

    private final SchedulerClient schedulerClient;

    public TransferObjectRedeliverBootstrapRunner(SchedulerClient schedulerClient) {
        this.schedulerClient = schedulerClient;
    }

    @Override
    public void run(ApplicationArguments args) {
        SchedulableInstance<TransferObjectRedeliverScheduledTaskData> schedulableInstance = TransferSchedulerTasks.OBJECT_REDELIVER_TASK
                .instance(INSTANCE_ID)
                .data(new TransferObjectRedeliverScheduledTaskData(
                        Schedules.fixedDelay(Duration.ofMinutes(2)),
                        new TransferObjectRedeliverTaskData()
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
