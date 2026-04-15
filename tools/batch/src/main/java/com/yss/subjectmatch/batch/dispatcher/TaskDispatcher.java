package com.yss.subjectmatch.batch.dispatcher;

/**
 * 将任务分派给正确的执行者或调度流程。
 */
public interface TaskDispatcher {
    /**
     * 通过 id 调度任务。
     */
    void dispatchTask(Long taskId);

    /**
     * 按计划 ID 调度计划任务。
     */
    void dispatchSchedule(Long scheduleId);
}
