package com.yss.valset.transfer.domain.model;

import java.time.Instant;
import java.util.Map;

/**
 * 文件来源定义。
 */
public class TransferSource {

    private final String sourceId;
    private final String sourceCode;
    private final String sourceName;
    private final SourceType sourceType;
    private final boolean enabled;
    private final String pollCron;
    private final Map<String, Object> connectionConfig;
    private final Map<String, Object> sourceMeta;
    private final String ingestStatus;
    private final String ingestTriggerType;
    private final Instant ingestStartedAt;
    private final Instant ingestFinishedAt;
    private final Instant createdAt;
    private final Instant updatedAt;

    public TransferSource(String sourceId, String sourceCode, String sourceName, SourceType sourceType, boolean enabled, String pollCron, Map<String, Object> connectionConfig, Map<String, Object> sourceMeta, String ingestStatus, String ingestTriggerType, Instant ingestStartedAt, Instant ingestFinishedAt, Instant createdAt, Instant updatedAt) {
        this.sourceId = sourceId;
        this.sourceCode = sourceCode;
        this.sourceName = sourceName;
        this.sourceType = sourceType;
        this.enabled = enabled;
        this.pollCron = pollCron;
        this.connectionConfig = connectionConfig;
        this.sourceMeta = sourceMeta;
        this.ingestStatus = ingestStatus;
        this.ingestTriggerType = ingestTriggerType;
        this.ingestStartedAt = ingestStartedAt;
        this.ingestFinishedAt = ingestFinishedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }



    public String sourceId() {
        return sourceId;
    }

    public String sourceCode() {
        return sourceCode;
    }

    public String sourceName() {
        return sourceName;
    }

    public SourceType sourceType() {
        return sourceType;
    }

    public boolean enabled() {
        return enabled;
    }

    public String pollCron() {
        return pollCron;
    }

    public Map<String, Object> connectionConfig() {
        return connectionConfig;
    }

    public Map<String, Object> sourceMeta() {
        return sourceMeta;
    }

    public String ingestStatus() {
        return ingestStatus;
    }

    public String ingestTriggerType() {
        return ingestTriggerType;
    }

    public Instant ingestStartedAt() {
        return ingestStartedAt;
    }

    public Instant ingestFinishedAt() {
        return ingestFinishedAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }



    public String getSourceId() {
        return sourceId;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public String getSourceName() {
        return sourceName;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public String getPollCron() {
        return pollCron;
    }

    public Map<String, Object> getConnectionConfig() {
        return connectionConfig;
    }

    public Map<String, Object> getSourceMeta() {
        return sourceMeta;
    }

    public String getIngestStatus() {
        return ingestStatus;
    }

    public String getIngestTriggerType() {
        return ingestTriggerType;
    }

    public Instant getIngestStartedAt() {
        return ingestStartedAt;
    }

    public Instant getIngestFinishedAt() {
        return ingestFinishedAt;
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
        TransferSource other = (TransferSource) o;
        if (!java.util.Objects.equals(sourceId, other.sourceId)) {
            return false;
        }
        if (!java.util.Objects.equals(sourceCode, other.sourceCode)) {
            return false;
        }
        if (!java.util.Objects.equals(sourceName, other.sourceName)) {
            return false;
        }
        if (!java.util.Objects.equals(sourceType, other.sourceType)) {
            return false;
        }
        if (enabled != other.enabled) {
            return false;
        }
        if (!java.util.Objects.equals(pollCron, other.pollCron)) {
            return false;
        }
        if (!java.util.Objects.equals(connectionConfig, other.connectionConfig)) {
            return false;
        }
        if (!java.util.Objects.equals(sourceMeta, other.sourceMeta)) {
            return false;
        }
        if (!java.util.Objects.equals(ingestStatus, other.ingestStatus)) {
            return false;
        }
        if (!java.util.Objects.equals(ingestTriggerType, other.ingestTriggerType)) {
            return false;
        }
        if (!java.util.Objects.equals(ingestStartedAt, other.ingestStartedAt)) {
            return false;
        }
        if (!java.util.Objects.equals(ingestFinishedAt, other.ingestFinishedAt)) {
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
        return java.util.Objects.hash(sourceId, sourceCode, sourceName, sourceType, enabled, pollCron, connectionConfig, sourceMeta, ingestStatus, ingestTriggerType, ingestStartedAt, ingestFinishedAt, createdAt, updatedAt);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TransferSource[");
        sb.append("sourceId=").append(sourceId);
        sb.append(", sourceCode=").append(sourceCode);
        sb.append(", sourceName=").append(sourceName);
        sb.append(", sourceType=").append(sourceType);
        sb.append(", enabled=").append(enabled);
        sb.append(", pollCron=").append(pollCron);
        sb.append(", connectionConfig=").append(connectionConfig);
        sb.append(", sourceMeta=").append(sourceMeta);
        sb.append(", ingestStatus=").append(ingestStatus);
        sb.append(", ingestTriggerType=").append(ingestTriggerType);
        sb.append(", ingestStartedAt=").append(ingestStartedAt);
        sb.append(", ingestFinishedAt=").append(ingestFinishedAt);
        sb.append(", createdAt=").append(createdAt);
        sb.append(", updatedAt=").append(updatedAt);
        sb.append(']');
        return sb.toString();
    }



}
