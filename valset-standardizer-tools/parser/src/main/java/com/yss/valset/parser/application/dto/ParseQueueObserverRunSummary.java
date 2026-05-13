package com.yss.valset.parser.application.dto;

/**
 * 待解析观察者运行摘要。
 */
public record ParseQueueObserverRunSummary(
        long batchSize,
        long successCount,
        long failedCount,
        long skippedCount,
        long totalSuccessCount,
        long totalFailedCount,
        long totalSkippedCount
) {
}
