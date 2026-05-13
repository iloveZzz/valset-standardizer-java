package com.yss.valset.transfer.domain.model.config;

import com.yss.valset.transfer.domain.model.TransferContext;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * S3 投递配置。
 */
public record S3TargetConfig(
        String bucket,
        String region,
        String endpointUrl,
        String accessKey,
        String secretKey,
        boolean usePathStyle,
        String keyPrefix
) {

    public static S3TargetConfig from(TransferContext context) {
        Map<String, Object> config = merge(context);
        String bucket = requiredString(config, TransferConfigKeys.BUCKET);
        String region = stringValue(config, TransferConfigKeys.REGION, "cn-north-1");
        String endpointUrl = stringValue(config, TransferConfigKeys.ENDPOINT_URL, null);
        String accessKey = stringValue(config, TransferConfigKeys.ACCESS_KEY, null);
        String secretKey = stringValue(config, TransferConfigKeys.SECRET_KEY, null);
        boolean usePathStyle = booleanValue(config, TransferConfigKeys.USE_PATH_STYLE, false);
        String keyPrefix = firstNonBlank(
                stringValue(config, TransferConfigKeys.PREFIX, null),
                stringValue(config, TransferConfigKeys.KEY_PREFIX, null)
        );
        return new S3TargetConfig(bucket, region, endpointUrl, accessKey, secretKey, usePathStyle, keyPrefix);
    }

    private static Map<String, Object> merge(TransferContext context) {
        Map<String, Object> config = new LinkedHashMap<>();
        if (context.transferObject() != null && context.transferObject().fileMeta() != null) {
            config.putAll(context.transferObject().fileMeta());
        }
        if (context.attributes() != null) {
            config.putAll(context.attributes());
        }
        if (context.transferRoute() != null && context.transferRoute().routeMeta() != null) {
            config.putAll(context.transferRoute().routeMeta());
        }
        if (context.transferTarget() != null) {
            if (context.transferTarget().connectionConfig() != null) {
                config.putAll(context.transferTarget().connectionConfig());
            }
            if (context.transferTarget().targetMeta() != null) {
                config.putAll(context.transferTarget().targetMeta());
            }
        }
        return config;
    }

    private static String requiredString(Map<String, Object> config, String key) {
        Object raw = config.get(key);
        if (raw == null || String.valueOf(raw).isBlank()) {
            throw new IllegalArgumentException("S3 目标缺少必要配置: " + key);
        }
        return String.valueOf(raw);
    }

    private static String stringValue(Map<String, Object> config, String key, String defaultValue) {
        Object raw = config.get(key);
        return raw == null ? defaultValue : String.valueOf(raw);
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
