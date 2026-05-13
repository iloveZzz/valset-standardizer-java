package com.yss.valset.transfer.domain.model.config;

import com.yss.valset.transfer.domain.model.TransferSource;

import java.util.Map;

/**
 * HTTP 来源配置。
 */
public record HttpSourceConfig(
        boolean allowMultipleFiles,
        int limit,
        String sourceCode
) {

    public static HttpSourceConfig from(TransferSource source) {
        Map<String, Object> config = source.connectionConfig() == null ? Map.of() : source.connectionConfig();
        boolean allowMultipleFiles = booleanValue(config, TransferConfigKeys.ALLOW_MULTIPLE_FILES, true);
        int limit = intValue(config, TransferConfigKeys.LIMIT, 0);
        String sourceCode = source.sourceCode() == null || source.sourceCode().isBlank()
                ? source.sourceId()
                : source.sourceCode();
        return new HttpSourceConfig(allowMultipleFiles, limit, sourceCode);
    }

    private static int intValue(Map<String, Object> config, String key, int defaultValue) {
        Object raw = config.get(key);
        if (raw == null || String.valueOf(raw).isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(String.valueOf(raw));
    }

    private static boolean booleanValue(Map<String, Object> config, String key, boolean defaultValue) {
        Object raw = config.get(key);
        return raw == null ? defaultValue : Boolean.parseBoolean(String.valueOf(raw));
    }
}
