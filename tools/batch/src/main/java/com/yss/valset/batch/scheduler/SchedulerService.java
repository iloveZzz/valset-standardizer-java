package com.yss.valset.batch.scheduler;

/**
 * 任务系统使用的调度程序抽象。
 */
public interface SchedulerService {
    /**
     * 立即触发任务。
     */
    void triggerNow(Long taskId);

    /**
     * 注册一个 cron 计划。
     */
    void scheduleCron(Long scheduleId, String scheduleKey, String cronExpression);

    /**
     * 暂停 cron 计划。
     */
    void pause(String scheduleKey);

    /**
     * 恢复 cron 计划。
     */
    void resume(String scheduleKey);

    /**
     * 删除 cron 计划。
     */
    void delete(String scheduleKey);
}
