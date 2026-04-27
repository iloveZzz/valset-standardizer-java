package com.yss.valset.transfer.application.service;

import com.yss.valset.transfer.application.command.TransferObjectRedeliverCommand;
import com.yss.valset.transfer.application.dto.TransferObjectRedeliverResponse;

/**
 * 文件主对象管理服务。
 */
public interface TransferObjectManagementAppService {

    /**
     * 重新投递文件主对象。
     *
     * @param command 重新投递命令
     * @return 重新投递结果
     */
    TransferObjectRedeliverResponse redeliver(TransferObjectRedeliverCommand command);
}
