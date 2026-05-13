package com.yss.valset.transfer.domain.model.config;

import com.yss.valset.transfer.domain.model.TransferSource;

import java.util.Collections;
import java.util.Map;

/**
 * S3 来源配置。
 */
public class S3SourceConfig {

    private final String bucket;
    private final String region;
    private final String endpointUrl;
    private final String accessKey;
    private final String secretKey;
    private final boolean usePathStyle;
    private final String prefix;
    private final int limit;
    private final String sourceCode;

    public S3SourceConfig(String bucket, String region, String endpointUrl, String accessKey, String secretKey, boolean usePathStyle, String prefix, int limit, String sourceCode) {
        this.bucket = bucket;
        this.region = region;
        this.endpointUrl = endpointUrl;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.usePathStyle = usePathStyle;
        this.prefix = prefix;
        this.limit = limit;
        this.sourceCode = sourceCode;
    }



    public String bucket() {
        return bucket;
    }

    public String region() {
        return region;
    }

    public String endpointUrl() {
        return endpointUrl;
    }

    public String accessKey() {
        return accessKey;
    }

    public String secretKey() {
        return secretKey;
    }

    public boolean usePathStyle() {
        return usePathStyle;
    }

    public String prefix() {
        return prefix;
    }

    public int limit() {
        return limit;
    }

    public String sourceCode() {
        return sourceCode;
    }



    public String getBucket() {
        return bucket;
    }

    public String getRegion() {
        return region;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public boolean getUsePathStyle() {
        return usePathStyle;
    }

    public String getPrefix() {
        return prefix;
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
        S3SourceConfig other = (S3SourceConfig) o;
        if (!java.util.Objects.equals(bucket, other.bucket)) {
            return false;
        }
        if (!java.util.Objects.equals(region, other.region)) {
            return false;
        }
        if (!java.util.Objects.equals(endpointUrl, other.endpointUrl)) {
            return false;
        }
        if (!java.util.Objects.equals(accessKey, other.accessKey)) {
            return false;
        }
        if (!java.util.Objects.equals(secretKey, other.secretKey)) {
            return false;
        }
        if (usePathStyle != other.usePathStyle) {
            return false;
        }
        if (!java.util.Objects.equals(prefix, other.prefix)) {
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
        return java.util.Objects.hash(bucket, region, endpointUrl, accessKey, secretKey, usePathStyle, prefix, limit, sourceCode);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("S3SourceConfig[");
        sb.append("bucket=").append(bucket);
        sb.append(", region=").append(region);
        sb.append(", endpointUrl=").append(endpointUrl);
        sb.append(", accessKey=").append(accessKey);
        sb.append(", secretKey=").append(secretKey);
        sb.append(", usePathStyle=").append(usePathStyle);
        sb.append(", prefix=").append(prefix);
        sb.append(", limit=").append(limit);
        sb.append(", sourceCode=").append(sourceCode);
        sb.append(']');
        return sb.toString();
    }



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
        String sourceCode = source.sourceCode() == null || source.sourceCode().trim().isEmpty() ? bucket : source.sourceCode();
        return new S3SourceConfig(bucket, region, endpointUrl, accessKey, secretKey, usePathStyle, prefix, limit, sourceCode);
    }

    private static String requiredString(Map<String, Object> config, String key) {
        Object raw = config.get(key);
        if (raw == null || String.valueOf(raw).trim().isEmpty()) {
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
        if (raw == null || String.valueOf(raw).trim().isEmpty()) {
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
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }

}
