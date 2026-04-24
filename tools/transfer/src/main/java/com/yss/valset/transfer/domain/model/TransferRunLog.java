package com.yss.valset.transfer.domain.model;

import java.time.LocalDateTime;

/**
 * 文件收发运行日志。
 */
public record TransferRunLog(
        String runLogId,
        String sourceId,
        String sourceType,
        String sourceCode,
        String sourceName,
        String transferId,
        String routeId,
        String triggerType,
        String runStage,
        String runStatus,
        String logMessage,
        String errorMessage,
        LocalDateTime createdAt
) {
}
