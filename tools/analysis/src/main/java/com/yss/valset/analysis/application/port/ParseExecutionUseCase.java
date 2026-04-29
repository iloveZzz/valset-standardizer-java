package com.yss.valset.analysis.application.port;

/**
 * 解析工作流用例。
 */
public interface ParseExecutionUseCase {
    /**
     * 执行任务 ID 的解析工作流程。
     */
    void execute(Long taskId);
}
