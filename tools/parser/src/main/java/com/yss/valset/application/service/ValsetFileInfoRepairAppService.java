package com.yss.valset.application.service;

import com.yss.valset.application.command.ValsetFileInfoRepairCommand;
import com.yss.valset.application.dto.ValsetFileInfoRepairResultDTO;
import com.yss.valset.domain.model.ValsetFileInfo;
import com.yss.valset.transfer.domain.model.TransferObject;

/**
 * 文件主数据回填服务。
 */
public interface ValsetFileInfoRepairAppService {

    /**
     * 按 transferObject 修复或创建文件主数据。
     */
    ValsetFileInfo ensureFromTransferObject(TransferObject transferObject);

    /**
     * 按命令批量回填文件主数据。
     */
    ValsetFileInfoRepairResultDTO repair(ValsetFileInfoRepairCommand command);
}
