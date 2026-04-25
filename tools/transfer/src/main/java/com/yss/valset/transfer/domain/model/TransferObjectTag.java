package com.yss.valset.transfer.domain.model;

import java.time.Instant;
import java.util.Map;

/**
 * 文件对象标签。
 */
public record TransferObjectTag(
        String id,
        String transferId,
        String tagId,
        String tagCode,
        String tagName,
        String tagValue,
        String matchStrategy,
        String matchReason,
        String matchedField,
        String matchedValue,
        Map<String, Object> matchSnapshot,
        Instant createdAt
) {
}
