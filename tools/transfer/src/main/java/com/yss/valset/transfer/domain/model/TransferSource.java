package com.yss.valset.transfer.domain.model;

import java.time.Instant;
import java.util.Map;

/**
 * 文件来源定义。
 */
public record TransferSource(
        String sourceId,
        String sourceCode,
        String sourceName,
        SourceType sourceType,
        boolean enabled,
        String pollCron,
        Map<String, Object> connectionConfig,
        Map<String, Object> sourceMeta,
        String ingestStatus,
        Instant ingestStartedAt,
        Instant ingestFinishedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
