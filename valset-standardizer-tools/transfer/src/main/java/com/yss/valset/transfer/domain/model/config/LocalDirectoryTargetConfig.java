package com.yss.valset.transfer.domain.model.config;

import com.yss.valset.transfer.domain.model.TransferContext;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 本地目录投递配置。
 */
public class LocalDirectoryTargetConfig {

    private final String directory;
    private final boolean createParentDirectories;

    public LocalDirectoryTargetConfig(String directory, boolean createParentDirectories) {
        this.directory = directory;
        this.createParentDirectories = createParentDirectories;
    }



    public String directory() {
        return directory;
    }

    public boolean createParentDirectories() {
        return createParentDirectories;
    }



    public String getDirectory() {
        return directory;
    }

    public boolean getCreateParentDirectories() {
        return createParentDirectories;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LocalDirectoryTargetConfig other = (LocalDirectoryTargetConfig) o;
        if (!java.util.Objects.equals(directory, other.directory)) {
            return false;
        }
        if (createParentDirectories != other.createParentDirectories) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(directory, createParentDirectories);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("LocalDirectoryTargetConfig[");
        sb.append("directory=").append(directory);
        sb.append(", createParentDirectories=").append(createParentDirectories);
        sb.append(']');
        return sb.toString();
    }



public static LocalDirectoryTargetConfig from(TransferContext context) {
        Map<String, Object> config = merge(context);
        String directory = requiredString(config, TransferConfigKeys.DIRECTORY);
        boolean createParentDirectories = booleanValue(config, "createParentDirectories", true);
        return new LocalDirectoryTargetConfig(directory, createParentDirectories);
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
            throw new IllegalArgumentException("本地目录目标缺少必要配置: " + key);
        }
        return String.valueOf(raw);
    }

    private static boolean booleanValue(Map<String, Object> config, String key, boolean defaultValue) {
        Object raw = config.get(key);
        return raw == null ? defaultValue : Boolean.parseBoolean(String.valueOf(raw));
    }

}
