package com.yss.valset.transfer.domain.model;

import java.util.List;

/**
 * 文件主对象按来源类型统计结果。
 */
public record TransferObjectSourceAnalysis(
        String sourceType,
        Long totalCount,
        List<TransferObjectStatusCount> statusCounts,
        List<TransferObjectMailFolderCount> mailFolderCounts
) {
}
