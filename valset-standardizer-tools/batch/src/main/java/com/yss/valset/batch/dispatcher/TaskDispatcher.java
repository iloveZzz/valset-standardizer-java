package com.yss.valset.batch.dispatcher;

/**
 * 将任务分派给正确的执行者或调度流程。
 */
public interface TaskDispatcher {
    /**
     * 通过 id 调度任务。
     */
    void dispatchTask(Long taskId);
}
