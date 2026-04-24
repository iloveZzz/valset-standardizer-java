package com.yss.valset.transfer.scheduler;

import com.github.kagkarlsson.scheduler.SchedulerClient;
import com.github.kagkarlsson.scheduler.exceptions.TaskInstanceNotFoundException;
import com.github.kagkarlsson.scheduler.task.SchedulableInstance;
import com.github.kagkarlsson.scheduler.task.schedule.Schedules;
import com.yss.valset.transfer.application.port.TransferJobScheduler;
import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.scheduler.task.TransferDeliverTaskData;
import com.yss.valset.transfer.scheduler.task.TransferIngestScheduledTaskData;
import com.yss.valset.transfer.scheduler.task.TransferIngestTaskData;
import com.yss.valset.transfer.scheduler.task.TransferRouteTaskData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 基于 db-scheduler 的文件分拣调度器。
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "db-scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DbTransferJobScheduler implements TransferJobScheduler {

    private static final String DEFAULT_CRON = "0 0/5 * * * ?";

    private final SchedulerClient schedulerClient;

    public DbTransferJobScheduler(SchedulerClient schedulerClient) {
        this.schedulerClient = schedulerClient;
    }

    @Override
    public String triggerIngest(String sourceId, String sourceType, String sourceCode, Map<String, Object> parameters, String ingestLockToken) {
        String instanceId = buildManualIngestInstanceId(sourceId);
        log.info("调度来源收取任务，sourceId={}，sourceCode={}，sourceType={}，instanceId={}，hasLockToken={}，parameterCount={}",
                sourceId,
                sourceCode,
                sourceType,
                instanceId,
                ingestLockToken != null && !ingestLockToken.isBlank(),
                parameters == null ? 0 : parameters.size());
        TransferIngestTaskData taskData = new TransferIngestTaskData(
                sourceId,
                sourceType,
                sourceCode,
                "MANUAL",
                copy(parameters),
                ingestLockToken
        );
        scheduleOnce(TransferSchedulerTasks.INGEST_ONCE_TASK.instance(instanceId)
                .data(taskData)
                .scheduledTo(Instant.now()));
        return instanceId;
    }

    @Override
    public void scheduleIngestCron(String sourceId, String sourceType, String sourceCode, Map<String, Object> parameters, String cronExpression) {
        log.info("调度来源轮询任务，sourceId={}，sourceCode={}，sourceType={}，cronExpression={}",
                sourceId,
                sourceCode,
                sourceType,
                cronExpression);
        TransferIngestTaskData taskData = new TransferIngestTaskData(
                sourceId,
                sourceType,
                sourceCode,
                "CRON",
                copy(parameters),
                null
        );
        TransferIngestScheduledTaskData scheduleData = new TransferIngestScheduledTaskData(
                Schedules.cron(normalizeCronExpression(cronExpression)),
                taskData
        );
        SchedulableInstance<TransferIngestScheduledTaskData> schedulableInstance = TransferSchedulerTasks.INGEST_CRON_TASK
                .instance(sourceId)
                .data(scheduleData)
                .scheduledAccordingToData();
        boolean rescheduled;
        try {
            rescheduled = schedulerClient.reschedule(schedulableInstance);
        } catch (TaskInstanceNotFoundException exception) {
            log.info("来源轮询任务不存在，改为直接创建，sourceId={}，taskName={}，instanceId={}",
                    sourceId,
                    "transfer-ingest-cron",
                    TransferSchedulerTasks.INGEST_CRON_TASK.instanceId(sourceId));
            rescheduled = false;
        }
        if (!rescheduled) {
            schedulerClient.scheduleIfNotExists(schedulableInstance);
        }
    }

    @Override
    public void unscheduleIngest(String sourceId) {
        log.info("取消来源轮询任务，sourceId={}", sourceId);
        try {
            schedulerClient.cancel(TransferSchedulerTasks.INGEST_CRON_TASK.instanceId(sourceId));
        } catch (TaskInstanceNotFoundException exception) {
            log.info("来源轮询任务不存在，视为已取消，sourceId={}，taskName={}，instanceId={}",
                    sourceId,
                    "transfer-ingest-cron",
                    TransferSchedulerTasks.INGEST_CRON_TASK.instanceId(sourceId));
        }
    }

    @Override
    public void triggerRoute(String transferId) {
        log.info("调度文件路由任务，transferId={}", transferId);
        scheduleOnce(TransferSchedulerTasks.ROUTE_TASK.instance("route-" + safeId(transferId))
                .data(new TransferRouteTaskData(transferId))
                .scheduledTo(Instant.now()));
    }

    @Override
    public void triggerDeliver(String routeId, String transferId) {
        log.info("调度文件投递任务，routeId={}，transferId={}", routeId, transferId);
        scheduleOnce(TransferSchedulerTasks.DELIVER_TASK.instance("deliver-" + safeId(routeId) + "-" + safeId(transferId))
                .data(new TransferDeliverTaskData(routeId, transferId, 0))
                .scheduledTo(Instant.now()));
    }

    @Override
    public void scheduleDeliverRetry(String routeId, String transferId, int retryCount, int delaySeconds) {
        Instant startAt = Instant.now().plusSeconds(Math.max(delaySeconds, 1L));
        log.info("调度文件投递重试，routeId={}，transferId={}，retryCount={}，delaySeconds={}，scheduledAt={}",
                routeId,
                transferId,
                retryCount,
                delaySeconds,
                startAt);
        scheduleOnce(TransferSchedulerTasks.DELIVER_TASK.instance("deliver-" + safeId(routeId) + "-" + safeId(transferId) + "-" + retryCount)
                .data(new TransferDeliverTaskData(routeId, transferId, retryCount))
                .scheduledTo(startAt));
    }

    @SuppressWarnings("unchecked")
    private void scheduleOnce(SchedulableInstance<?> schedulableInstance) {
        schedulerClient.scheduleIfNotExists((SchedulableInstance<Object>) schedulableInstance);
    }

    static String normalizeCronExpression(String cronExpression) {
        if (cronExpression == null || cronExpression.isBlank()) {
            return DEFAULT_CRON;
        }
        String normalized = cronExpression.trim().replaceAll("\\s+", " ");
        String[] parts = normalized.split(" ");
        if (parts.length == 6) {
            return normalized;
        }
        if (parts.length == 7) {
            String yearPart = parts[6];
            if ("*".equals(yearPart) || "?".equals(yearPart)) {
                return String.join(" ", parts[0], parts[1], parts[2], parts[3], parts[4], parts[5]);
            }
            throw new IllegalArgumentException("轮询表达式包含年份字段，db-scheduler 不支持 7 段 cron：" + cronExpression);
        }
        throw new IllegalArgumentException("轮询表达式必须为 6 段或兼容的 7 段 Quartz cron，当前为 " + parts.length + " 段：" + cronExpression);
    }

    private Map<String, Object> copy(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        return new LinkedHashMap<>(source);
    }

    private String buildManualIngestInstanceId(String sourceId) {
        return "ingest-" + safeId(sourceId) + "-" + System.currentTimeMillis() + "-" + UUID.randomUUID();
    }

    private String safeId(String id) {
        return id == null ? "null" : id;
    }
}
