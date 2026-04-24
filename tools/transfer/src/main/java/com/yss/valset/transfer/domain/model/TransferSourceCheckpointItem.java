package com.yss.valset.transfer.domain.model;

import java.time.Instant;
import java.util.Map;

/**
 * 来源处理去重记录。
 */
public record TransferSourceCheckpointItem(
        String checkpointItemId,
        String sourceId,
        String sourceType,
        String itemKey,
        String itemRef,
        String itemName,
        Long itemSize,
        String itemMimeType,
        String itemFingerprint,
        Map<String, Object> itemMeta,
        String triggerType,
        Instant processedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
