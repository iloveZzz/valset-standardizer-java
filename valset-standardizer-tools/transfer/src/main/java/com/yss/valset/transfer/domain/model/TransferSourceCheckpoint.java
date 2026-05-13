package com.yss.valset.transfer.domain.model;

import java.time.Instant;
import java.util.Map;

/**
 * 来源扫描游标。
 */
public record TransferSourceCheckpoint(
        String checkpointId,
        String sourceId,
        String sourceType,
        String checkpointKey,
        String checkpointValue,
        Map<String, Object> checkpointMeta,
        Instant createdAt,
        Instant updatedAt
) {
}
