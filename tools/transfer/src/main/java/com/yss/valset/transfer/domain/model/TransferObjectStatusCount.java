package com.yss.valset.transfer.domain.model;

/**
 * 文件状态统计项。
 */
public record TransferObjectStatusCount(
        String status,
        Long count
) {
}
