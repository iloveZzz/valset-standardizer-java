package com.yss.valset.transfer.domain.model;

import java.util.Map;

/**
 * 探测插件返回的文件特征结果。
 */
public class ProbeResult {

    private final boolean detected;
    private final String detectedType;
    private final Map<String, Object> attributes;

    public ProbeResult(boolean detected, String detectedType, Map<String, Object> attributes) {
        this.detected = detected;
        this.detectedType = detectedType;
        this.attributes = attributes;
    }



    public boolean detected() {
        return detected;
    }

    public String detectedType() {
        return detectedType;
    }

    public Map<String, Object> attributes() {
        return attributes;
    }



    public boolean getDetected() {
        return detected;
    }

    public String getDetectedType() {
        return detectedType;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ProbeResult other = (ProbeResult) o;
        if (detected != other.detected) {
            return false;
        }
        if (!java.util.Objects.equals(detectedType, other.detectedType)) {
            return false;
        }
        if (!java.util.Objects.equals(attributes, other.attributes)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(detected, detectedType, attributes);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ProbeResult[");
        sb.append("detected=").append(detected);
        sb.append(", detectedType=").append(detectedType);
        sb.append(", attributes=").append(attributes);
        sb.append(']');
        return sb.toString();
    }



}
