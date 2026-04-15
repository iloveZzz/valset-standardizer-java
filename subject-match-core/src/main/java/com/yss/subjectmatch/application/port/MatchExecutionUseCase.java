package com.yss.subjectmatch.application.port;

/**
 * 匹配工作流程用例。
 */
public interface MatchExecutionUseCase {
    /**
     * 执行任务 ID 的匹配工作流程。
     */
    void execute(Long taskId);
}
