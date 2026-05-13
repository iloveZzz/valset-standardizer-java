package com.yss.valset.transfer.domain.model;

import java.time.Instant;
import java.util.Map;

/**
 * 文件投递目标定义。
 */
public record TransferTarget(
        Long targetId,
        String targetCode,
        String targetName,
        TargetType targetType,
        boolean enabled,
        String targetPathTemplate,
        Map<String, Object> connectionConfig,
        Map<String, Object> targetMeta,
        Instant createdAt,
        Instant updatedAt
) {
}
