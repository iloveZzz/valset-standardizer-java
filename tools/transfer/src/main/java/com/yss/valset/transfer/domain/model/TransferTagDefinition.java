package com.yss.valset.transfer.domain.model;

import java.time.Instant;
import java.util.Map;

/**
 * 标签定义。
 */
public record TransferTagDefinition(
        String tagId,
        String tagCode,
        String tagName,
        String tagValue,
        boolean enabled,
        int priority,
        String matchStrategy,
        String scriptLanguage,
        String scriptBody,
        String regexPattern,
        Map<String, Object> tagMeta,
        Instant createdAt,
        Instant updatedAt
) {
}
