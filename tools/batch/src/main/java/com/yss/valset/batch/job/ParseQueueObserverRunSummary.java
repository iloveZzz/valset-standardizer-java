package com.yss.valset.batch.job;

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
