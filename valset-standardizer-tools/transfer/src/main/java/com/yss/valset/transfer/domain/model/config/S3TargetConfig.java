package com.yss.valset.transfer.domain.model.config;

import com.yss.valset.transfer.domain.model.TransferContext;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * S3 投递配置。
 */
public class S3TargetConfig {

    private final String bucket;
    private final String region;
    private final String endpointUrl;
    private final String accessKey;
    private final String secretKey;
    private final boolean usePathStyle;
    private final String keyPrefix;

    public S3TargetConfig(String bucket, String region, String endpointUrl, String accessKey, String secretKey, boolean usePathStyle, String keyPrefix) {
        this.bucket = bucket;
        this.region = region;
        this.endpointUrl = endpointUrl;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.usePathStyle = usePathStyle;
        this.keyPrefix = keyPrefix;
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

    public String keyPrefix() {
        return keyPrefix;
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

    public String getKeyPrefix() {
        return keyPrefix;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        S3TargetConfig other = (S3TargetConfig) o;
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
        if (!java.util.Objects.equals(keyPrefix, other.keyPrefix)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(bucket, region, endpointUrl, accessKey, secretKey, usePathStyle, keyPrefix);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("S3TargetConfig[");
        sb.append("bucket=").append(bucket);
        sb.append(", region=").append(region);
        sb.append(", endpointUrl=").append(endpointUrl);
        sb.append(", accessKey=").append(accessKey);
        sb.append(", secretKey=").append(secretKey);
        sb.append(", usePathStyle=").append(usePathStyle);
        sb.append(", keyPrefix=").append(keyPrefix);
        sb.append(']');
        return sb.toString();
    }



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
        if (raw == null || String.valueOf(raw).trim().isEmpty()) {
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
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }

}
