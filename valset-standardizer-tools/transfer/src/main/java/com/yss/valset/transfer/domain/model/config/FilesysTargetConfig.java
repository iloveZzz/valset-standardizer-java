package com.yss.valset.transfer.domain.model.config;

import com.yss.valset.transfer.domain.model.TransferContext;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * yss-filesys 目标配置。
 */
public class FilesysTargetConfig {

    private final String parentId;
    private final String storageSettingId;
    private final long chunkSize;

    public FilesysTargetConfig(String parentId, String storageSettingId, long chunkSize) {
        this.parentId = parentId;
        this.storageSettingId = storageSettingId;
        this.chunkSize = chunkSize;
    }



    public String parentId() {
        return parentId;
    }

    public String storageSettingId() {
        return storageSettingId;
    }

    public long chunkSize() {
        return chunkSize;
    }



    public String getParentId() {
        return parentId;
    }

    public String getStorageSettingId() {
        return storageSettingId;
    }

    public long getChunkSize() {
        return chunkSize;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FilesysTargetConfig other = (FilesysTargetConfig) o;
        if (!java.util.Objects.equals(parentId, other.parentId)) {
            return false;
        }
        if (!java.util.Objects.equals(storageSettingId, other.storageSettingId)) {
            return false;
        }
        if (chunkSize != other.chunkSize) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(parentId, storageSettingId, chunkSize);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("FilesysTargetConfig[");
        sb.append("parentId=").append(parentId);
        sb.append(", storageSettingId=").append(storageSettingId);
        sb.append(", chunkSize=").append(chunkSize);
        sb.append(']');
        return sb.toString();
    }



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
        if (raw == null || String.valueOf(raw).trim().isEmpty()) {
            throw new IllegalArgumentException("yss-filesys 目标缺少必要配置: " + key);
        }
        return String.valueOf(raw);
    }

    private static long longValue(Map<String, Object> config, String key, long defaultValue) {
        Object raw = config.get(key);
        if (raw == null || String.valueOf(raw).trim().isEmpty()) {
            return defaultValue;
        }
        if (raw instanceof Number) {
            return ((Number) raw).longValue();
        }
        return Long.parseLong(String.valueOf(raw));
    }

}
