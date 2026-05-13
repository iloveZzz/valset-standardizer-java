package com.yss.valset.transfer.domain.model;

import java.util.List;

/**
 * 文件大小统计结果。
 */
public record TransferObjectSizeAnalysis(
        Long totalCount,
        Long totalSizeBytes,
        List<TransferObjectExtensionCount> extensionCounts
) {
}
