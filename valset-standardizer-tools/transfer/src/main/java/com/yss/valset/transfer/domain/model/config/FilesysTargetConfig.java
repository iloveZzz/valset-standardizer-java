package com.yss.valset.transfer.domain.model.config;

import com.yss.valset.transfer.domain.model.TransferContext;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * yss-filesys 目标配置。
 */
public record FilesysTargetConfig(
        String parentId,
        String storageSettingId,
        long chunkSize
) {

    public static FilesysTargetConfig from(TransferContext context, long defaultChunkSize) {
        Map<String, Object> config = merge(context);
        return new FilesysTargetConfig(
                requiredString(config, TransferConfigKeys.PARENT_ID),
                requiredString(config, TransferConfigKeys.STORAGE_SETTING_ID),
                longValue(config, TransferConfigKeys.CHUNK_SIZE, defaultChunkSize)
        );
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
            throw new IllegalArgumentException("yss-filesys 目标缺少必要配置: " + key);
        }
        return String.valueOf(raw);
    }

    private static long longValue(Map<String, Object> config, String key, long defaultValue) {
        Object raw = config.get(key);
        if (raw == null || String.valueOf(raw).isBlank()) {
            return defaultValue;
        }
        if (raw instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(raw));
    }
}
