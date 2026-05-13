package com.yss.valset.transfer.scheduler.task;

import com.yss.valset.transfer.application.command.IngestTransferSourceCommand;
import com.yss.valset.transfer.domain.model.SourceType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 文件收取任务入参。
 */
public class TransferIngestTaskData {

    private final String sourceId;
    private final String sourceType;
    private final String sourceCode;
    private final String triggerType;
    private final Map<String, Object> parameters;
    private final String ingestLockToken;

    public TransferIngestTaskData(String sourceId, String sourceType, String sourceCode, String triggerType, Map<String, Object> parameters, String ingestLockToken) {
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

    public String sourceType() {
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

    public String getSourceType() {
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
        TransferIngestTaskData other = (TransferIngestTaskData) o;
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
        StringBuilder sb = new StringBuilder("TransferIngestTaskData[");
        sb.append("sourceId=").append(sourceId);
        sb.append(", sourceType=").append(sourceType);
        sb.append(", sourceCode=").append(sourceCode);
        sb.append(", triggerType=").append(triggerType);
        sb.append(", parameters=").append(parameters);
        sb.append(", ingestLockToken=").append(ingestLockToken);
        sb.append(']');
        return sb.toString();
    }



public IngestTransferSourceCommand toCommand() {
        return new IngestTransferSourceCommand(
                sourceId,
                resolveSourceType(sourceType),
                sourceCode,
                triggerType,
                parameters == null ? java.util.Collections.emptyMap() : new LinkedHashMap<>(parameters),
                ingestLockToken
        );
    }

    private SourceType resolveSourceType(String rawType) {
        if (rawType == null || rawType.trim().isEmpty()) {
            return null;
        }
        return SourceType.valueOf(rawType.trim().toUpperCase());
    }

}
