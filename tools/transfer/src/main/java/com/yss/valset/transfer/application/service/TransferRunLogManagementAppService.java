package com.yss.valset.transfer.application.service;

import com.yss.valset.transfer.application.command.TransferRunLogCleanupCommand;
import com.yss.valset.transfer.application.command.TransferRunLogRedeliverCommand;
import com.yss.valset.transfer.application.dto.TransferRunLogCleanupResponse;
import com.yss.valset.transfer.application.dto.TransferRunLogRedeliverResponse;

/**
 * 文件收发运行日志管理服务。
 */
public interface TransferRunLogManagementAppService {

    /**
     * 批量重投递失败的文件收发运行日志。
     *
     * @param command 重投递命令
     * @return 重投递结果
     */
    TransferRunLogRedeliverResponse redeliver(TransferRunLogRedeliverCommand command);

    /**
     * 清理前一天的运行日志。
     *
     * @return 清理结果
     */
    TransferRunLogCleanupResponse cleanupYesterdayLogs();

    /**
     * 按时间区间清理运行日志。
     *
     * @param command 清理命令
     * @return 清理结果
     */
    TransferRunLogCleanupResponse cleanupLogs(TransferRunLogCleanupCommand command);
}
