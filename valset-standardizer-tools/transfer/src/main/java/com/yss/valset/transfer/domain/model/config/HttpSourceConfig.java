package com.yss.valset.transfer.domain.model.config;

import com.yss.valset.transfer.domain.model.TransferSource;

import java.util.Map;

/**
 * HTTP 来源配置。
 */
public class HttpSourceConfig {

    private final boolean allowMultipleFiles;
    private final int limit;
    private final String sourceCode;

    public HttpSourceConfig(boolean allowMultipleFiles, int limit, String sourceCode) {
        this.allowMultipleFiles = allowMultipleFiles;
        this.limit = limit;
        this.sourceCode = sourceCode;
    }



    public boolean allowMultipleFiles() {
        return allowMultipleFiles;
    }

    public int limit() {
        return limit;
    }

    public String sourceCode() {
        return sourceCode;
    }



    public boolean getAllowMultipleFiles() {
        return allowMultipleFiles;
    }

    public int getLimit() {
        return limit;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        HttpSourceConfig other = (HttpSourceConfig) o;
        if (allowMultipleFiles != other.allowMultipleFiles) {
            return false;
        }
        if (limit != other.limit) {
            return false;
        }
        if (!java.util.Objects.equals(sourceCode, other.sourceCode)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(allowMultipleFiles, limit, sourceCode);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("HttpSourceConfig[");
        sb.append("allowMultipleFiles=").append(allowMultipleFiles);
        sb.append(", limit=").append(limit);
        sb.append(", sourceCode=").append(sourceCode);
        sb.append(']');
        return sb.toString();
    }



public static HttpSourceConfig from(TransferSource source) {
        Map<String, Object> config = source.connectionConfig() == null ? java.util.Collections.emptyMap() : source.connectionConfig();
        boolean allowMultipleFiles = booleanValue(config, TransferConfigKeys.ALLOW_MULTIPLE_FILES, true);
        int limit = intValue(config, TransferConfigKeys.LIMIT, 0);
        String sourceCode = source.sourceCode() == null || source.sourceCode().trim().isEmpty()
                ? source.sourceId()
                : source.sourceCode();
        return new HttpSourceConfig(allowMultipleFiles, limit, sourceCode);
    }

    private static int intValue(Map<String, Object> config, String key, int defaultValue) {
        Object raw = config.get(key);
        if (raw == null || String.valueOf(raw).trim().isEmpty()) {
            return defaultValue;
        }
        return Integer.parseInt(String.valueOf(raw));
    }

    private static boolean booleanValue(Map<String, Object> config, String key, boolean defaultValue) {
        Object raw = config.get(key);
        return raw == null ? defaultValue : Boolean.parseBoolean(String.valueOf(raw));
    }

}
