package com.yss.subjectmatch.batch.scheduler;

import com.yss.subjectmatch.batch.job.DispatchJob;
import org.quartz.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;

/**
 * Quartz支持的调度程序服务。
 */
@Service
public class QuartzSchedulerService implements SchedulerService {

    private static final String JOB_GROUP = "subject-match-job";
    private static final String TRIGGER_GROUP = "subject-match-trigger";

    private final Scheduler scheduler;

    public QuartzSchedulerService(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * 通过Quartz立即触发任务。
     */
    @Override
    public void triggerNow(Long taskId) {
        try {
            String jobName = "task-" + taskId + "-" + Instant.now().toEpochMilli();
            JobDetail jobDetail = JobBuilder.newJob(DispatchJob.class)
                    .withIdentity(jobName, JOB_GROUP)
                    .usingJobData(DispatchJob.TASK_ID, taskId)
                    .build();
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger-" + jobName, TRIGGER_GROUP)
                    .startAt(new Date())
                    .build();
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            throw new IllegalStateException("Failed to trigger taskId=" + taskId, e);
        }
    }

    /**
     * 注册或替换 cron 犯罪。
     */
    @Override
    public void scheduleCron(Long scheduleId, String scheduleKey, String cronExpression) {
        try {
            JobKey jobKey = JobKey.jobKey(scheduleKey, JOB_GROUP);
            TriggerKey triggerKey = TriggerKey.triggerKey(scheduleKey, TRIGGER_GROUP);
            JobDetail jobDetail = JobBuilder.newJob(DispatchJob.class)
                    .withIdentity(jobKey)
                    .usingJobData(DispatchJob.SCHEDULE_ID, scheduleId)
                    .storeDurably()
                    .build();
            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey)
                    .forJob(jobDetail)
                    .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression)
                            .withMisfireHandlingInstructionDoNothing())
                    .build();

            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
            }
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            throw new IllegalStateException("Failed to schedule scheduleId=" + scheduleId, e);
        }
    }

    /**
     * 暂停正在运行的 cron 作业。
     */
    @Override
    public void pause(String scheduleKey) {
        try {
            scheduler.pauseJob(JobKey.jobKey(scheduleKey, JOB_GROUP));
        } catch (SchedulerException e) {
            throw new IllegalStateException("Failed to pause scheduleKey=" + scheduleKey, e);
        }
    }

    /**
     * 恢复暂停的 cron 作业。
     */
    @Override
    public void resume(String scheduleKey) {
        try {
            scheduler.resumeJob(JobKey.jobKey(scheduleKey, JOB_GROUP));
        } catch (SchedulerException e) {
            throw new IllegalStateException("Failed to resume scheduleKey=" + scheduleKey, e);
        }
    }

    /**
     * 删除一个 cron 作业。
     */
    @Override
    public void delete(String scheduleKey) {
        try {
            scheduler.deleteJob(JobKey.jobKey(scheduleKey, JOB_GROUP));
        } catch (SchedulerException e) {
            throw new IllegalStateException("Failed to delete scheduleKey=" + scheduleKey, e);
        }
    }
}
