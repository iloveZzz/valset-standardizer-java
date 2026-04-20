package com.yss.valset.transfer.domain.model;

import java.util.Map;

/**
 * 文件来源定义。
 */
public record TransferSource(
        Long sourceId,
        String sourceCode,
        String sourceName,
        SourceType sourceType,
        boolean enabled,
        String pollCron,
        Map<String, Object> connectionConfig,
        Map<String, Object> checkpointConfig,
        Map<String, Object> sourceMeta
) {
}
