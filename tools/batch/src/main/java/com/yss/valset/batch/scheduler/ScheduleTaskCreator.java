package com.yss.valset.batch.scheduler;

/**
 * 从持久的计划定义创建可运行的任务。
 */
public interface ScheduleTaskCreator {
    /**
     * 根据计划 ID 创建任务。
     */
    Long createTaskFromSchedule(Long scheduleId);
}
