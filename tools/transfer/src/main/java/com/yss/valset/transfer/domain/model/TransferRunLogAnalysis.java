package com.yss.valset.transfer.domain.model;

import java.util.List;

/**
 * 文件收发运行日志统计分析结果。
 */
public record TransferRunLogAnalysis(
        long totalCount,
        List<TransferRunLogStageAnalysis> stageAnalyses
) {
}
