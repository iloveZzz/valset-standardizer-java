package com.yss.valset.transfer.application.command;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件收发运行日志清理命令。
 */
@Data
public class TransferRunLogCleanupCommand {

    /**
     * 清理起始时间，包含边界。
     */
    private LocalDateTime startInclusive;

    /**
     * 清理结束时间，不包含边界。
     */
    private LocalDateTime endExclusive;
}
