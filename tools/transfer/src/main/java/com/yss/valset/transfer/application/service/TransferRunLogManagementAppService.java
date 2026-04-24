package com.yss.valset.transfer.application.service;

import com.yss.valset.transfer.application.command.TransferRunLogRedeliverCommand;
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
}
