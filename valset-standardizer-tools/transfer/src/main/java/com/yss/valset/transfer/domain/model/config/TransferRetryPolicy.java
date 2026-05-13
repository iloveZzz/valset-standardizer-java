package com.yss.valset.transfer.domain.model.config;

import java.util.Map;

/**
 * 文件投递重试策略。
 */
public record TransferRetryPolicy(
        int maxRetryCount,
        int retryDelaySeconds
) {

    public static TransferRetryPolicy from(Map<String, Object> routeMeta) {
        return new TransferRetryPolicy(
                intValue(routeMeta, TransferConfigKeys.MAX_RETRY_COUNT, 3),
                intValue(routeMeta, TransferConfigKeys.RETRY_DELAY_SECONDS, 60)
        );
    }

    private static int intValue(Map<String, Object> routeMeta, String key, int defaultValue) {
        if (routeMeta == null) {
            return defaultValue;
        }
        Object raw = routeMeta.get(key);
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
