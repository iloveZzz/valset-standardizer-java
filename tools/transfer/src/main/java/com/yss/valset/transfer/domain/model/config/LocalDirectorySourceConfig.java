package com.yss.valset.transfer.domain.model.config;

import com.yss.valset.transfer.domain.model.TransferSource;

import java.util.Map;

/**
 * 本地目录来源配置。
 */
public record LocalDirectorySourceConfig(
        String directory,
        boolean recursive,
        int limit,
        boolean includeHidden
) {

    public static LocalDirectorySourceConfig from(TransferSource source) {
        Map<String, Object> config = source.connectionConfig() == null ? Map.of() : source.connectionConfig();
        String directory = stringValue(config, TransferConfigKeys.DIRECTORY, null);
        if (directory == null || directory.isBlank()) {
            directory = source.sourceCode();
        }
        return new LocalDirectorySourceConfig(
                directory,
                booleanValue(config, TransferConfigKeys.RECURSIVE, false),
                intValue(config, TransferConfigKeys.LIMIT, 0),
                booleanValue(config, TransferConfigKeys.INCLUDE_HIDDEN, false)
        );
    }

    private static String stringValue(Map<String, Object> config, String key, String defaultValue) {
        Object raw = config.get(key);
        return raw == null ? defaultValue : String.valueOf(raw);
    }

    private static boolean booleanValue(Map<String, Object> config, String key, boolean defaultValue) {
        Object raw = config.get(key);
        return raw == null ? defaultValue : Boolean.parseBoolean(String.valueOf(raw));
    }

    private static int intValue(Map<String, Object> config, String key, int defaultValue) {
        Object raw = config.get(key);
        if (raw == null || String.valueOf(raw).isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(String.valueOf(raw));
    }
}
