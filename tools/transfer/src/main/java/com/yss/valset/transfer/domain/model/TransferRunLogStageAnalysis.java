package com.yss.valset.transfer.domain.model;

import java.util.List;

/**
 * 文件收发运行日志阶段统计。
 */
public record TransferRunLogStageAnalysis(
        String runStage,
        long totalCount,
        List<TransferRunLogStatusCount> statusCounts
) {
}
