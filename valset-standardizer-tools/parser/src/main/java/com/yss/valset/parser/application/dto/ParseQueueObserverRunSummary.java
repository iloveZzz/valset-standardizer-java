package com.yss.valset.parser.application.dto;

import lombok.Value;

/**
 * 待解析观察者运行摘要。
 */
@Value
public class ParseQueueObserverRunSummary implements java.io.Serializable {

    long batchSize;
    long successCount;
    long failedCount;
    long skippedCount;
    long totalSuccessCount;
    long totalFailedCount;
    long totalSkippedCount;

    public long batchSize() {
        return batchSize;
    }

    public long successCount() {
        return successCount;
    }

    public long failedCount() {
        return failedCount;
    }

    public long skippedCount() {
        return skippedCount;
    }

    public long totalSuccessCount() {
        return totalSuccessCount;
    }

    public long totalFailedCount() {
        return totalFailedCount;
    }

    public long totalSkippedCount() {
        return totalSkippedCount;
    }
}
