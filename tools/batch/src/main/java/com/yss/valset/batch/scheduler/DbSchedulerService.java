package com.yss.valset.batch.scheduler;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import com.yss.valset.domain.gateway.ScheduleGateway;
import com.yss.valset.domain.model.ScheduleDefinition;
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
    private final ScheduleGateway scheduleGateway;

    public DbSchedulerService(SchedulerClient schedulerClient, ScheduleGateway scheduleGateway) {
        this.schedulerClient = schedulerClient;
        this.scheduleGateway = scheduleGateway;
    }

    @Override
    public void triggerNow(Long taskId) {
        scheduleOnce(BatchSchedulerTasks.DISPATCH_ONCE_TASK.instance(buildTaskInstanceId("task", taskId))
                .data(new BatchDispatchTaskData(taskId))
                .scheduledTo(Instant.now()));
    }

    @Override
    public void scheduleCron(Long scheduleId, String scheduleKey, String cronExpression) {
        String instanceId = resolveInstanceId(scheduleId, scheduleKey);
        SchedulableInstance<BatchScheduleTaskData> schedulableInstance = BatchSchedulerTasks.DISPATCH_CRON_TASK
                .instance(instanceId)
                .data(new BatchScheduleTaskData(Schedules.cron(resolveCronExpression(cronExpression)), scheduleId))
                .scheduledAccordingToData();
        if (!schedulerClient.reschedule(schedulableInstance)) {
            schedulerClient.scheduleIfNotExists(schedulableInstance);
        }
    }

    @Override
    public void pause(String scheduleKey) {
        schedulerClient.cancel(BatchSchedulerTasks.DISPATCH_CRON_TASK.instanceId(resolveInstanceId(scheduleKey)));
    }

    @Override
    public void resume(String scheduleKey) {
        Long scheduleId = parseScheduleId(scheduleKey);
        ScheduleDefinition scheduleDefinition = scheduleGateway.findById(scheduleId);
        if (scheduleDefinition.getEnabled() != null && !scheduleDefinition.getEnabled()) {
            return;
        }
        scheduleCron(scheduleId, String.valueOf(scheduleId), scheduleDefinition.getCronExpression());
    }

    @Override
    public void delete(String scheduleKey) {
        schedulerClient.cancel(BatchSchedulerTasks.DISPATCH_CRON_TASK.instanceId(resolveInstanceId(scheduleKey)));
    }

    @SuppressWarnings("unchecked")
    private void scheduleOnce(SchedulableInstance<?> schedulableInstance) {
        schedulerClient.scheduleIfNotExists((SchedulableInstance<Object>) schedulableInstance);
    }

    private String buildTaskInstanceId(String prefix, Long taskId) {
        return prefix + "-" + (taskId == null ? "null" : taskId) + "-" + System.currentTimeMillis() + "-" + UUID.randomUUID();
    }

    private String resolveCronExpression(String cronExpression) {
        if (cronExpression == null || cronExpression.isBlank()) {
            return "0 0/5 * * * ?";
        }
        return cronExpression.trim();
    }

    private String resolveInstanceId(Long scheduleId, String scheduleKey) {
        if (scheduleKey != null && !scheduleKey.isBlank()) {
            return scheduleKey.trim();
        }
        return String.valueOf(scheduleId);
    }

    private String resolveInstanceId(String scheduleKey) {
        if (scheduleKey == null || scheduleKey.isBlank()) {
            throw new IllegalArgumentException("scheduleKey 不能为空");
        }
        return scheduleKey.trim();
    }

    private Long parseScheduleId(String scheduleKey) {
        try {
            return Long.valueOf(resolveInstanceId(scheduleKey));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("scheduleKey 必须是可转为 scheduleId 的数字字符串: " + scheduleKey, exception);
        }
    }
}
