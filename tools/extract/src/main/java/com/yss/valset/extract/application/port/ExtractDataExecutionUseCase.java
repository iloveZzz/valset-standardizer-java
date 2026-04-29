package com.yss.valset.extract.application.port;

/**
 * 原始数据提取工作流用例。
 */
public interface ExtractDataExecutionUseCase {
    /**
     * 执行指定任务的原始数据提取流程。
     *
     * @param taskId 任务标识
     */
    void execute(Long taskId);
}
