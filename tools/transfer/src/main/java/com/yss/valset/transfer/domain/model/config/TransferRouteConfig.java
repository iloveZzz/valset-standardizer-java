package com.yss.valset.transfer.domain.model.config;

import com.yss.valset.transfer.domain.model.TransferRoute;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 文件路由配置。
 */
public record TransferRouteConfig(
        int maxRetryCount,
        int retryDelaySeconds,
        String targetPath,
        String ruleMessage,
        String probeDetectedType,
        Map<String, Object> probeAttributes
) {

    public static TransferRouteConfig from(TransferRoute route) {
        return from(route == null ? null : route.routeMeta());
    }

    public static TransferRouteConfig from(Map<String, Object> routeMeta) {
        Map<String, Object> config = routeMeta == null ? Map.of() : routeMeta;
        return new TransferRouteConfig(
                intValue(config, TransferConfigKeys.MAX_RETRY_COUNT, 3),
                intValue(config, TransferConfigKeys.RETRY_DELAY_SECONDS, 60),
                stringValue(config, TransferConfigKeys.TARGET_PATH, null),
                stringValue(config, TransferConfigKeys.RULE_MESSAGE, null),
                stringValue(config, TransferConfigKeys.PROBE_DETECTED_TYPE, null),
                probeAttributes(config)
        );
    }

    public Map<String, Object> toMetaMap() {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put(TransferConfigKeys.MAX_RETRY_COUNT, maxRetryCount);
        meta.put(TransferConfigKeys.RETRY_DELAY_SECONDS, retryDelaySeconds);
        if (targetPath != null && !targetPath.isBlank()) {
            meta.put(TransferConfigKeys.TARGET_PATH, targetPath);
        }
        if (ruleMessage != null && !ruleMessage.isBlank()) {
            meta.put(TransferConfigKeys.RULE_MESSAGE, ruleMessage);
        }
        if (probeDetectedType != null && !probeDetectedType.isBlank()) {
            meta.put(TransferConfigKeys.PROBE_DETECTED_TYPE, probeDetectedType);
        }
        if (probeAttributes != null && !probeAttributes.isEmpty()) {
            meta.put(TransferConfigKeys.PROBE_ATTRIBUTES, probeAttributes);
        }
        return meta;
    }

    private static Map<String, Object> probeAttributes(Map<String, Object> config) {
        Object raw = config.get(TransferConfigKeys.PROBE_ATTRIBUTES);
        if (raw instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    result.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            return result;
        }
        return Map.of();
    }

    private static String stringValue(Map<String, Object> config, String key, String defaultValue) {
        Object raw = config.get(key);
        return raw == null ? defaultValue : String.valueOf(raw);
    }

    private static int intValue(Map<String, Object> config, String key, int defaultValue) {
        Object raw = config.get(key);
        if (raw == null || String.valueOf(raw).isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(raw));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
