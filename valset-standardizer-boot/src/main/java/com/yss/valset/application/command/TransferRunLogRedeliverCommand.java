package com.yss.valset.application.command;

import lombok.Data;

import java.util.List;

/**
 * 文件收发运行日志批量重投递命令。
 */
@Data
public class TransferRunLogRedeliverCommand {

    /**
     * 运行日志主键集合。
     */
    private List<String> runLogIds;
}
