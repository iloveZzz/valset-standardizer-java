package com.yss.valset.transfer.domain.model;

import java.time.LocalDateTime;

/**
 * 文件收发运行日志。
 */
public class TransferRunLog {

    private final String runLogId;
    private final String sourceId;
    private final String sourceType;
    private final String sourceCode;
    private final String sourceName;
    private final String transferId;
    private final String routeId;
    private final String triggerType;
    private final String runStage;
    private final String runStatus;
    private final String logMessage;
    private final String errorMessage;
    private final LocalDateTime createdAt;

    public TransferRunLog(String runLogId, String sourceId, String sourceType, String sourceCode, String sourceName, String transferId, String routeId, String triggerType, String runStage, String runStatus, String logMessage, String errorMessage, LocalDateTime createdAt) {
        this.runLogId = runLogId;
        this.sourceId = sourceId;
        this.sourceType = sourceType;
        this.sourceCode = sourceCode;
        this.sourceName = sourceName;
        this.transferId = transferId;
        this.routeId = routeId;
        this.triggerType = triggerType;
        this.runStage = runStage;
        this.runStatus = runStatus;
        this.logMessage = logMessage;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
    }



    public String runLogId() {
        return runLogId;
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

    public String sourceName() {
        return sourceName;
    }

    public String transferId() {
        return transferId;
    }

    public String routeId() {
        return routeId;
    }

    public String triggerType() {
        return triggerType;
    }

    public String runStage() {
        return runStage;
    }

    public String runStatus() {
        return runStatus;
    }

    public String logMessage() {
        return logMessage;
    }

    public String errorMessage() {
        return errorMessage;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }



    public String getRunLogId() {
        return runLogId;
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

    public String getSourceName() {
        return sourceName;
    }

    public String getTransferId() {
        return transferId;
    }

    public String getRouteId() {
        return routeId;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public String getRunStage() {
        return runStage;
    }

    public String getRunStatus() {
        return runStatus;
    }

    public String getLogMessage() {
        return logMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TransferRunLog other = (TransferRunLog) o;
        if (!java.util.Objects.equals(runLogId, other.runLogId)) {
            return false;
        }
        if (!java.util.Objects.equals(sourceId, other.sourceId)) {
            return false;
        }
        if (!java.util.Objects.equals(sourceType, other.sourceType)) {
            return false;
        }
        if (!java.util.Objects.equals(sourceCode, other.sourceCode)) {
            return false;
        }
        if (!java.util.Objects.equals(sourceName, other.sourceName)) {
            return false;
        }
        if (!java.util.Objects.equals(transferId, other.transferId)) {
            return false;
        }
        if (!java.util.Objects.equals(routeId, other.routeId)) {
            return false;
        }
        if (!java.util.Objects.equals(triggerType, other.triggerType)) {
            return false;
        }
        if (!java.util.Objects.equals(runStage, other.runStage)) {
            return false;
        }
        if (!java.util.Objects.equals(runStatus, other.runStatus)) {
            return false;
        }
        if (!java.util.Objects.equals(logMessage, other.logMessage)) {
            return false;
        }
        if (!java.util.Objects.equals(errorMessage, other.errorMessage)) {
            return false;
        }
        if (!java.util.Objects.equals(createdAt, other.createdAt)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(runLogId, sourceId, sourceType, sourceCode, sourceName, transferId, routeId, triggerType, runStage, runStatus, logMessage, errorMessage, createdAt);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TransferRunLog[");
        sb.append("runLogId=").append(runLogId);
        sb.append(", sourceId=").append(sourceId);
        sb.append(", sourceType=").append(sourceType);
        sb.append(", sourceCode=").append(sourceCode);
        sb.append(", sourceName=").append(sourceName);
        sb.append(", transferId=").append(transferId);
        sb.append(", routeId=").append(routeId);
        sb.append(", triggerType=").append(triggerType);
        sb.append(", runStage=").append(runStage);
        sb.append(", runStatus=").append(runStatus);
        sb.append(", logMessage=").append(logMessage);
        sb.append(", errorMessage=").append(errorMessage);
        sb.append(", createdAt=").append(createdAt);
        sb.append(']');
        return sb.toString();
    }



}
