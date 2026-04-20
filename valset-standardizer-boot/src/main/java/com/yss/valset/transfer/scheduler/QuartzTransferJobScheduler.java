package com.yss.valset.transfer.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.transfer.application.port.TransferJobScheduler;
import com.yss.valset.transfer.job.TransferDispatchJob;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 基于 Quartz 的文件分拣调度器。
 */
@Service
public class QuartzTransferJobScheduler implements TransferJobScheduler {

    private static final String JOB_GROUP = "valset-transfer-job";
    private static final String TRIGGER_GROUP = "valset-transfer-trigger";
    private static final String DEFAULT_CRON = "0 0/5 * * * ?";

    private final Scheduler scheduler;
    private final ObjectMapper objectMapper;

    public QuartzTransferJobScheduler(Scheduler scheduler, ObjectMapper objectMapper) {
        this.scheduler = scheduler;
        this.objectMapper = objectMapper;
    }

    @Override
    public void triggerIngest(Long sourceId, String sourceType, String sourceCode, Map<String, Object> parameters) {
        scheduleOnce("ingest-" + safeId(sourceId), buildJobData("INGEST", sourceId, sourceType, sourceCode, parameters, null, null, 0), new Date());
    }

    @Override
    public void triggerRoute(Long transferId) {
        scheduleOnce("route-" + safeId(transferId), buildJobData("ROUTE", null, null, null, null, transferId, null, 0), new Date());
    }

    @Override
    public void triggerDeliver(Long routeId) {
        scheduleOnce("deliver-" + safeId(routeId), buildJobData("DELIVER", null, null, null, null, null, routeId, 0), new Date());
    }

    @Override
    public void scheduleDeliverRetry(Long routeId, int retryCount, int delaySeconds) {
        Date startAt = Date.from(Instant.now().plusSeconds(Math.max(delaySeconds, 1)));
        String jobKey = "deliver-retry-" + safeId(routeId) + "-" + retryCount + "-" + UUID.randomUUID();
        scheduleOnce(jobKey, buildJobData("DELIVER", null, null, null, null, null, routeId, retryCount), startAt);
    }

    /**
     * 预留给后续按 cron 的来源扫描使用。
     */
    public void scheduleIngestCron(Long sourceId, String sourceType, String sourceCode, Map<String, Object> parameters, String cronExpression) {
        scheduleCron(
                "ingest-cron-" + safeId(sourceId),
                buildJobData("INGEST", sourceId, sourceType, sourceCode, parameters, null, null, 0),
                cronExpression
        );
    }

    private void scheduleOnce(String jobName, Map<String, Object> jobData, Date startAt) {
        try {
            JobDetail jobDetail = JobBuilder.newJob(TransferDispatchJob.class)
                    .withIdentity(jobName, JOB_GROUP)
                    .usingJobData(toJobDataMap(jobData))
                    .build();
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger-" + jobName, TRIGGER_GROUP)
                    .startAt(startAt)
                    .build();
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            throw new IllegalStateException("调度文件分拣作业失败，jobName=" + jobName, e);
        }
    }

    private void scheduleCron(String jobName, Map<String, Object> jobData, String cronExpression) {
        try {
            JobKey jobKey = JobKey.jobKey(jobName, JOB_GROUP);
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
            }
            JobDetail jobDetail = JobBuilder.newJob(TransferDispatchJob.class)
                    .withIdentity(jobKey)
                    .usingJobData(toJobDataMap(jobData))
                    .storeDurably()
                    .build();
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger-" + jobName, TRIGGER_GROUP)
                    .forJob(jobDetail)
                    .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression == null || cronExpression.isBlank() ? DEFAULT_CRON : cronExpression)
                            .withMisfireHandlingInstructionDoNothing())
                    .build();
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            throw new IllegalStateException("注册文件分拣 cron 作业失败，jobName=" + jobName, e);
        }
    }

    private Map<String, Object> buildJobData(
            String action,
            Long sourceId,
            String sourceType,
            String sourceCode,
            Map<String, Object> parameters,
            Long transferId,
            Long routeId,
            int retryCount
    ) {
        Map<String, Object> jobData = new LinkedHashMap<>();
        jobData.put(TransferDispatchJob.ACTION, action);
        jobData.put(TransferDispatchJob.SOURCE_ID, sourceId);
        jobData.put(TransferDispatchJob.SOURCE_TYPE, sourceType);
        jobData.put(TransferDispatchJob.SOURCE_CODE, sourceCode);
        jobData.put(TransferDispatchJob.PARAMETERS_JSON, toJson(parameters));
        jobData.put(TransferDispatchJob.TRANSFER_ID, transferId);
        jobData.put(TransferDispatchJob.ROUTE_ID, routeId);
        jobData.put(TransferDispatchJob.RETRY_COUNT, retryCount);
        return jobData;
    }

    private org.quartz.JobDataMap toJobDataMap(Map<String, Object> jobData) {
        org.quartz.JobDataMap jobDataMap = new org.quartz.JobDataMap();
        jobDataMap.putAll(jobData);
        return jobDataMap;
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("序列化文件分拣作业参数失败", e);
        }
    }

    private String safeId(Long id) {
        return id == null ? "null" : String.valueOf(id);
    }
}
