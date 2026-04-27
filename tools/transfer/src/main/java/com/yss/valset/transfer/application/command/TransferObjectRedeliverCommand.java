package com.yss.valset.transfer.application.command;

import lombok.Data;

import java.util.List;

/**
 * 文件主对象重新投递命令。
 */
@Data
public class TransferObjectRedeliverCommand {

    /**
     * 文件主键集合。
     */
    private List<String> transferIds;
}
