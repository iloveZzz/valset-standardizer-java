package com.yss.valset.transfer.domain.model;

/**
 * 文件后缀统计项。
 */
public record TransferObjectExtensionCount(
        String extension,
        Long count
) {
}
