package com.yss.valset.transfer.domain.model.config;

import com.yss.valset.transfer.domain.model.TransferRoute;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 文件路由配置。
 */
public class TransferRouteConfig {

    private final int maxRetryCount;
    private final int retryDelaySeconds;
    private final String targetPath;
    private final String ruleMessage;
    private final String probeDetectedType;
    private final Map<String, Object> probeAttributes;

    public TransferRouteConfig(int maxRetryCount, int retryDelaySeconds, String targetPath, String ruleMessage, String probeDetectedType, Map<String, Object> probeAttributes) {
        this.maxRetryCount = maxRetryCount;
        this.retryDelaySeconds = retryDelaySeconds;
        this.targetPath = targetPath;
        this.ruleMessage = ruleMessage;
        this.probeDetectedType = probeDetectedType;
        this.probeAttributes = probeAttributes;
    }



    public int maxRetryCount() {
        return maxRetryCount;
    }

    public int retryDelaySeconds() {
        return retryDelaySeconds;
    }

    public String targetPath() {
        return targetPath;
    }

    public String ruleMessage() {
        return ruleMessage;
    }

    public String probeDetectedType() {
        return probeDetectedType;
    }

    public Map<String, Object> probeAttributes() {
        return probeAttributes;
    }



    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public int getRetryDelaySeconds() {
        return retryDelaySeconds;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public String getRuleMessage() {
        return ruleMessage;
    }

    public String getProbeDetectedType() {
        return probeDetectedType;
    }

    public Map<String, Object> getProbeAttributes() {
        return probeAttributes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TransferRouteConfig other = (TransferRouteConfig) o;
        if (maxRetryCount != other.maxRetryCount) {
            return false;
        }
        if (retryDelaySeconds != other.retryDelaySeconds) {
            return false;
        }
        if (!java.util.Objects.equals(targetPath, other.targetPath)) {
            return false;
        }
        if (!java.util.Objects.equals(ruleMessage, other.ruleMessage)) {
            return false;
        }
        if (!java.util.Objects.equals(probeDetectedType, other.probeDetectedType)) {
            return false;
        }
        if (!java.util.Objects.equals(probeAttributes, other.probeAttributes)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(maxRetryCount, retryDelaySeconds, targetPath, ruleMessage, probeDetectedType, probeAttributes);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TransferRouteConfig[");
        sb.append("maxRetryCount=").append(maxRetryCount);
        sb.append(", retryDelaySeconds=").append(retryDelaySeconds);
        sb.append(", targetPath=").append(targetPath);
        sb.append(", ruleMessage=").append(ruleMessage);
        sb.append(", probeDetectedType=").append(probeDetectedType);
        sb.append(", probeAttributes=").append(probeAttributes);
        sb.append(']');
        return sb.toString();
    }



public static TransferRouteConfig from(TransferRoute route) {
        return from(route == null ? null : route.routeMeta());
    }

    public static TransferRouteConfig from(Map<String, Object> routeMeta) {
        Map<String, Object> config = routeMeta == null ? java.util.Collections.emptyMap() : routeMeta;
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
        if (targetPath != null && !targetPath.trim().isEmpty()) {
            meta.put(TransferConfigKeys.TARGET_PATH, targetPath);
        }
        if (ruleMessage != null && !ruleMessage.trim().isEmpty()) {
            meta.put(TransferConfigKeys.RULE_MESSAGE, ruleMessage);
        }
        if (probeDetectedType != null && !probeDetectedType.trim().isEmpty()) {
            meta.put(TransferConfigKeys.PROBE_DETECTED_TYPE, probeDetectedType);
        }
        if (probeAttributes != null && !probeAttributes.isEmpty()) {
            meta.put(TransferConfigKeys.PROBE_ATTRIBUTES, probeAttributes);
        }
        return meta;
    }

    private static Map<String, Object> probeAttributes(Map<String, Object> config) {
        Object raw = config.get(TransferConfigKeys.PROBE_ATTRIBUTES);
        if (raw instanceof Map<?, ?>) {
            Map<?, ?> map = (Map<?, ?>) raw;
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    result.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            return result;
        }
        return java.util.Collections.emptyMap();
    }

    private static String stringValue(Map<String, Object> config, String key, String defaultValue) {
        Object raw = config.get(key);
        return raw == null ? defaultValue : String.valueOf(raw);
    }

    private static int intValue(Map<String, Object> config, String key, int defaultValue) {
        Object raw = config.get(key);
        if (raw == null || String.valueOf(raw).trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(raw));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

}
