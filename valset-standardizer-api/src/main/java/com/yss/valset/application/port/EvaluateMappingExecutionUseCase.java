package com.yss.valset.application.port;

/**
 * 离线评估用例。
 */
public interface EvaluateMappingExecutionUseCase {
    /**
     * 执行任务 ID 的映射评估工作流程。
     */
    void execute(Long taskId);
}
