package com.yss.valset.batch.scheduler;

/**
 * 任务系统使用的调度程序抽象。
 */
public interface SchedulerService {
    /**
     * 立即触发任务。
     */
    void triggerNow(Long taskId);
}
