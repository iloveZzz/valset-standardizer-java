package com.yss.valset.transfer.domain.model.config;

import com.yss.valset.transfer.domain.model.TransferSource;

import java.util.Collections;
import java.util.Map;

/**
 * S3 来源配置。
 */
public record S3SourceConfig(
        String bucket,
        String region,
        String endpointUrl,
        String accessKey,
        String secretKey,
        boolean usePathStyle,
        String prefix,
        int limit,
        String sourceCode
) {

    public static S3SourceConfig from(TransferSource source) {
        Map<String, Object> config = source.connectionConfig() == null ? Collections.emptyMap() : source.connectionConfig();
        String bucket = requiredString(config, TransferConfigKeys.BUCKET);
        String region = stringValue(config, TransferConfigKeys.REGION, "cn-north-1");
        String endpointUrl = stringValue(config, TransferConfigKeys.ENDPOINT_URL, null);
        String accessKey = stringValue(config, TransferConfigKeys.ACCESS_KEY, null);
        String secretKey = stringValue(config, TransferConfigKeys.SECRET_KEY, null);
        boolean usePathStyle = booleanValue(config, TransferConfigKeys.USE_PATH_STYLE, false);
        String prefix = firstNonBlank(stringValue(config, TransferConfigKeys.PREFIX, null), stringValue(config, TransferConfigKeys.KEY_PREFIX, null));
        int limit = intValue(config, TransferConfigKeys.LIMIT, 0);
        String sourceCode = source.sourceCode() == null || source.sourceCode().isBlank() ? bucket : source.sourceCode();
        return new S3SourceConfig(bucket, region, endpointUrl, accessKey, secretKey, usePathStyle, prefix, limit, sourceCode);
    }

    private static String requiredString(Map<String, Object> config, String key) {
        Object raw = config.get(key);
        if (raw == null || String.valueOf(raw).isBlank()) {
            throw new IllegalArgumentException("S3 来源缺少必要配置: " + key);
        }
        return String.valueOf(raw);
    }

    private static String stringValue(Map<String, Object> config, String key, String defaultValue) {
        Object raw = config == null ? null : config.get(key);
        return raw == null ? defaultValue : String.valueOf(raw);
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

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
