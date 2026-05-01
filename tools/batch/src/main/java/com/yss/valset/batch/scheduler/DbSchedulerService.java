package com.yss.valset.batch.scheduler;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * 基于 db-scheduler 的任务调度程序服务。
 */
@Service
@ConditionalOnProperty(prefix = "db-scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DbSchedulerService implements SchedulerService {

    private final SchedulerClient schedulerClient;

    public DbSchedulerService(SchedulerClient schedulerClient) {
        this.schedulerClient = schedulerClient;
    }

    @Override
    public void triggerNow(Long taskId) {
        scheduleOnce(BatchSchedulerTasks.DISPATCH_ONCE_TASK.instance(buildTaskInstanceId("task", taskId))
                .data(new BatchDispatchTaskData(taskId))
                .scheduledTo(Instant.now()));
    }

    @SuppressWarnings("unchecked")
    private void scheduleOnce(SchedulableInstance<?> schedulableInstance) {
        schedulerClient.scheduleIfNotExists((SchedulableInstance<Object>) schedulableInstance);
    }

    private String buildTaskInstanceId(String prefix, Long taskId) {
        return prefix + "-" + (taskId == null ? "null" : taskId) + "-" + System.currentTimeMillis() + "-" + UUID.randomUUID();
    }
}
