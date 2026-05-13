package com.yss.valset.transfer.application.command;

import com.yss.valset.transfer.domain.model.SourceType;

import java.util.Map;

/**
 * 收取文件命令。
 */
public class IngestTransferSourceCommand {

    private final String sourceId;
    private final SourceType sourceType;
    private final String sourceCode;
    private final String triggerType;
    private final Map<String, Object> parameters;
    private final String ingestLockToken;

    public IngestTransferSourceCommand(String sourceId, SourceType sourceType, String sourceCode, String triggerType, Map<String, Object> parameters, String ingestLockToken) {
        this.sourceId = sourceId;
        this.sourceType = sourceType;
        this.sourceCode = sourceCode;
        this.triggerType = triggerType;
        this.parameters = parameters;
        this.ingestLockToken = ingestLockToken;
    }



    public String sourceId() {
        return sourceId;
    }

    public SourceType sourceType() {
        return sourceType;
    }

    public String sourceCode() {
        return sourceCode;
    }

    public String triggerType() {
        return triggerType;
    }

    public Map<String, Object> parameters() {
        return parameters;
    }

    public String ingestLockToken() {
        return ingestLockToken;
    }



    public String getSourceId() {
        return sourceId;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public String getIngestLockToken() {
        return ingestLockToken;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        IngestTransferSourceCommand other = (IngestTransferSourceCommand) o;
        if (!java.util.Objects.equals(sourceId, other.sourceId)) {
            return false;
        }
        if (!java.util.Objects.equals(sourceType, other.sourceType)) {
            return false;
        }
        if (!java.util.Objects.equals(sourceCode, other.sourceCode)) {
            return false;
        }
        if (!java.util.Objects.equals(triggerType, other.triggerType)) {
            return false;
        }
        if (!java.util.Objects.equals(parameters, other.parameters)) {
            return false;
        }
        if (!java.util.Objects.equals(ingestLockToken, other.ingestLockToken)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(sourceId, sourceType, sourceCode, triggerType, parameters, ingestLockToken);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("IngestTransferSourceCommand[");
        sb.append("sourceId=").append(sourceId);
        sb.append(", sourceType=").append(sourceType);
        sb.append(", sourceCode=").append(sourceCode);
        sb.append(", triggerType=").append(triggerType);
        sb.append(", parameters=").append(parameters);
        sb.append(", ingestLockToken=").append(ingestLockToken);
        sb.append(']');
        return sb.toString();
    }



}
