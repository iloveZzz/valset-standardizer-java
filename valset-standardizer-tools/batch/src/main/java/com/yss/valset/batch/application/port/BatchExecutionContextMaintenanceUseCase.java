package com.yss.valset.batch.application.port;

import com.yss.valset.batch.application.dto.BatchExecutionContextCleanupResponse;

/**
 * 批处理元数据维护用例。
 */
public interface BatchExecutionContextMaintenanceUseCase {

    /**
     * 清理历史执行上下文。
     *
     * @return 清理结果
     */
    BatchExecutionContextCleanupResponse cleanupHistoricalExecutionContexts();
}
