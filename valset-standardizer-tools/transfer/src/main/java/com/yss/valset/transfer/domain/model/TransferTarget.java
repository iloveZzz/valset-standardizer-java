package com.yss.valset.transfer.domain.model;

import java.time.Instant;
import java.util.Map;

/**
 * 文件投递目标定义。
 */
public class TransferTarget {

    private final Long targetId;
    private final String targetCode;
    private final String targetName;
    private final TargetType targetType;
    private final boolean enabled;
    private final String targetPathTemplate;
    private final Map<String, Object> connectionConfig;
    private final Map<String, Object> targetMeta;
    private final Instant createdAt;
    private final Instant updatedAt;

    public TransferTarget(Long targetId, String targetCode, String targetName, TargetType targetType, boolean enabled, String targetPathTemplate, Map<String, Object> connectionConfig, Map<String, Object> targetMeta, Instant createdAt, Instant updatedAt) {
        this.targetId = targetId;
        this.targetCode = targetCode;
        this.targetName = targetName;
        this.targetType = targetType;
        this.enabled = enabled;
        this.targetPathTemplate = targetPathTemplate;
        this.connectionConfig = connectionConfig;
        this.targetMeta = targetMeta;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }



    public Long targetId() {
        return targetId;
    }

    public String targetCode() {
        return targetCode;
    }

    public String targetName() {
        return targetName;
    }

    public TargetType targetType() {
        return targetType;
    }

    public boolean enabled() {
        return enabled;
    }

    public String targetPathTemplate() {
        return targetPathTemplate;
    }

    public Map<String, Object> connectionConfig() {
        return connectionConfig;
    }

    public Map<String, Object> targetMeta() {
        return targetMeta;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }



    public Long getTargetId() {
        return targetId;
    }

    public String getTargetCode() {
        return targetCode;
    }

    public String getTargetName() {
        return targetName;
    }

    public TargetType getTargetType() {
        return targetType;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public String getTargetPathTemplate() {
        return targetPathTemplate;
    }

    public Map<String, Object> getConnectionConfig() {
        return connectionConfig;
    }

    public Map<String, Object> getTargetMeta() {
        return targetMeta;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TransferTarget other = (TransferTarget) o;
        if (!java.util.Objects.equals(targetId, other.targetId)) {
            return false;
        }
        if (!java.util.Objects.equals(targetCode, other.targetCode)) {
            return false;
        }
        if (!java.util.Objects.equals(targetName, other.targetName)) {
            return false;
        }
        if (!java.util.Objects.equals(targetType, other.targetType)) {
            return false;
        }
        if (enabled != other.enabled) {
            return false;
        }
        if (!java.util.Objects.equals(targetPathTemplate, other.targetPathTemplate)) {
            return false;
        }
        if (!java.util.Objects.equals(connectionConfig, other.connectionConfig)) {
            return false;
        }
        if (!java.util.Objects.equals(targetMeta, other.targetMeta)) {
            return false;
        }
        if (!java.util.Objects.equals(createdAt, other.createdAt)) {
            return false;
        }
        if (!java.util.Objects.equals(updatedAt, other.updatedAt)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(targetId, targetCode, targetName, targetType, enabled, targetPathTemplate, connectionConfig, targetMeta, createdAt, updatedAt);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TransferTarget[");
        sb.append("targetId=").append(targetId);
        sb.append(", targetCode=").append(targetCode);
        sb.append(", targetName=").append(targetName);
        sb.append(", targetType=").append(targetType);
        sb.append(", enabled=").append(enabled);
        sb.append(", targetPathTemplate=").append(targetPathTemplate);
        sb.append(", connectionConfig=").append(connectionConfig);
        sb.append(", targetMeta=").append(targetMeta);
        sb.append(", createdAt=").append(createdAt);
        sb.append(", updatedAt=").append(updatedAt);
        sb.append(']');
        return sb.toString();
    }



}
