package com.yss.subjectmatch.batch.task;

import com.yss.subjectmatch.domain.model.TaskType;

/**
 * 特定任务类型的执行者合约。
 */
public interface TaskExecutor {
    /**
     * 返回支持的任务类型。
     */
    TaskType support();

    /**
     * 使用给定的 id 执行任务。
     */
    void execute(Long taskId);
}
