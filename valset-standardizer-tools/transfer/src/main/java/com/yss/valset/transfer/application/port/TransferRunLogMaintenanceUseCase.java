package com.yss.valset.transfer.application.port;

import com.yss.valset.transfer.application.dto.TransferRunLogCleanupResponse;

/**
 * 文件收发运行日志维护用例。
 */
public interface TransferRunLogMaintenanceUseCase {

    /**
     * 清理前一天产生的运行日志。
     *
     * @return 清理结果
     */
    TransferRunLogCleanupResponse cleanupYesterdayLogs();
}
