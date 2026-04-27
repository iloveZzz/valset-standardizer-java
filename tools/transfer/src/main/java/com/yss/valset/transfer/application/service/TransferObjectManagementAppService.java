package com.yss.valset.transfer.application.service;

import com.yss.valset.transfer.application.command.TransferObjectRetagCommand;
import com.yss.valset.transfer.application.command.TransferObjectRedeliverCommand;
import com.yss.valset.transfer.application.dto.TransferObjectRetagResponse;
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

    /**
     * 重新打标文件主对象。
     *
     * @param command 重新打标命令
     * @return 重新打标结果
     */
    TransferObjectRetagResponse retag(TransferObjectRetagCommand command);
}
