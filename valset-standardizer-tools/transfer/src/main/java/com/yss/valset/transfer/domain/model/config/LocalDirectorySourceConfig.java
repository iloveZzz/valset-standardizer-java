package com.yss.valset.transfer.domain.model.config;

import com.yss.valset.transfer.domain.model.TransferSource;

import java.util.Map;

/**
 * 本地目录来源配置。
 */
public class LocalDirectorySourceConfig {

    private final String directory;
    private final boolean recursive;
    private final int limit;
    private final boolean includeHidden;

    public LocalDirectorySourceConfig(String directory, boolean recursive, int limit, boolean includeHidden) {
        this.directory = directory;
        this.recursive = recursive;
        this.limit = limit;
        this.includeHidden = includeHidden;
    }



    public String directory() {
        return directory;
    }

    public boolean recursive() {
        return recursive;
    }

    public int limit() {
        return limit;
    }

    public boolean includeHidden() {
        return includeHidden;
    }



    public String getDirectory() {
        return directory;
    }

    public boolean getRecursive() {
        return recursive;
    }

    public int getLimit() {
        return limit;
    }

    public boolean getIncludeHidden() {
        return includeHidden;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LocalDirectorySourceConfig other = (LocalDirectorySourceConfig) o;
        if (!java.util.Objects.equals(directory, other.directory)) {
            return false;
        }
        if (recursive != other.recursive) {
            return false;
        }
        if (limit != other.limit) {
            return false;
        }
        if (includeHidden != other.includeHidden) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(directory, recursive, limit, includeHidden);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("LocalDirectorySourceConfig[");
        sb.append("directory=").append(directory);
        sb.append(", recursive=").append(recursive);
        sb.append(", limit=").append(limit);
        sb.append(", includeHidden=").append(includeHidden);
        sb.append(']');
        return sb.toString();
    }



public static LocalDirectorySourceConfig from(TransferSource source) {
        Map<String, Object> config = source.connectionConfig() == null ? java.util.Collections.emptyMap() : source.connectionConfig();
        String directory = stringValue(config, TransferConfigKeys.DIRECTORY, null);
        if (directory == null || directory.trim().isEmpty()) {
            directory = source.sourceCode();
        }
        if (directory != null) {
            directory = directory.trim();
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
        if (raw == null || String.valueOf(raw).trim().isEmpty()) {
            return defaultValue;
        }
        return Integer.parseInt(String.valueOf(raw));
    }

}
