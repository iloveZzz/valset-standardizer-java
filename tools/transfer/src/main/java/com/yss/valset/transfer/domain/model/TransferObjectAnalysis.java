package com.yss.valset.transfer.domain.model;

import java.util.List;

/**
 * 文件主对象统计分析结果。
 */
public record TransferObjectAnalysis(
        Long totalCount,
        List<TransferObjectSourceAnalysis> sourceAnalyses,
        TransferObjectSizeAnalysis sizeAnalysis
) {
}
