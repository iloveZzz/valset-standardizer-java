package com.yss.valset.transfer.domain.model;

/**
 * 文件收发运行日志状态统计。
 */
public record TransferRunLogStatusCount(
        String runStatus,
        long count
) {
}
