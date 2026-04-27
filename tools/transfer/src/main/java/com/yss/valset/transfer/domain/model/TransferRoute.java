package com.yss.valset.transfer.domain.model;

import java.util.Map;

/**
 * 文件路由结果。
 */
public record TransferRoute(
        String routeId,
        String sourceId,
        SourceType sourceType,
        String sourceCode,
        String ruleId,
        TargetType targetType,
        String targetCode,
        String pollCron,
        String targetPath,
        String renamePattern,
        boolean enabled,
        TransferStatus routeStatus,
        Map<String, Object> routeMeta
) {
}
