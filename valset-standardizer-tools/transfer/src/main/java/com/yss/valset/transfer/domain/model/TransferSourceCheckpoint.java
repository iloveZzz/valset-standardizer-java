package com.yss.valset.transfer.domain.model;

import java.time.Instant;
import java.util.Map;

/**
 * 来源扫描游标。
 */
public class TransferSourceCheckpoint {

    private final String checkpointId;
    private final String sourceId;
    private final String sourceType;
    private final String checkpointKey;
    private final String checkpointValue;
    private final Map<String, Object> checkpointMeta;
    private final Instant createdAt;
    private final Instant updatedAt;

    public TransferSourceCheckpoint(String checkpointId, String sourceId, String sourceType, String checkpointKey, String checkpointValue, Map<String, Object> checkpointMeta, Instant createdAt, Instant updatedAt) {
        this.checkpointId = checkpointId;
        this.sourceId = sourceId;
        this.sourceType = sourceType;
        this.checkpointKey = checkpointKey;
        this.checkpointValue = checkpointValue;
        this.checkpointMeta = checkpointMeta;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }



    public String checkpointId() {
        return checkpointId;
    }

    public String sourceId() {
        return sourceId;
    }

    public String sourceType() {
        return sourceType;
    }

    public String checkpointKey() {
        return checkpointKey;
    }

    public String checkpointValue() {
        return checkpointValue;
    }

    public Map<String, Object> checkpointMeta() {
        return checkpointMeta;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }



    public String getCheckpointId() {
        return checkpointId;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getCheckpointKey() {
        return checkpointKey;
    }

    public String getCheckpointValue() {
        return checkpointValue;
    }

    public Map<String, Object> getCheckpointMeta() {
        return checkpointMeta;
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
        TransferSourceCheckpoint other = (TransferSourceCheckpoint) o;
        if (!java.util.Objects.equals(checkpointId, other.checkpointId)) {
            return false;
        }
        if (!java.util.Objects.equals(sourceId, other.sourceId)) {
            return false;
        }
        if (!java.util.Objects.equals(sourceType, other.sourceType)) {
            return false;
        }
        if (!java.util.Objects.equals(checkpointKey, other.checkpointKey)) {
            return false;
        }
        if (!java.util.Objects.equals(checkpointValue, other.checkpointValue)) {
            return false;
        }
        if (!java.util.Objects.equals(checkpointMeta, other.checkpointMeta)) {
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
        return java.util.Objects.hash(checkpointId, sourceId, sourceType, checkpointKey, checkpointValue, checkpointMeta, createdAt, updatedAt);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TransferSourceCheckpoint[");
        sb.append("checkpointId=").append(checkpointId);
        sb.append(", sourceId=").append(sourceId);
        sb.append(", sourceType=").append(sourceType);
        sb.append(", checkpointKey=").append(checkpointKey);
        sb.append(", checkpointValue=").append(checkpointValue);
        sb.append(", checkpointMeta=").append(checkpointMeta);
        sb.append(", createdAt=").append(createdAt);
        sb.append(", updatedAt=").append(updatedAt);
        sb.append(']');
        return sb.toString();
    }



}
