package com.yss.valset.parser.domain.model;

import java.time.Instant;
import lombok.Value;

/**
 * 待解析任务。
 */
@Value
public class ParseQueue {

    String queueId;
    String businessKey;
    String transferId;
    String originalName;
    String sourceId;
    String sourceType;
    String sourceCode;
    String routeId;
    String deliveryId;
    String tagId;
    String tagCode;
    String tagName;
    String fileStatus;
    String deliveryStatus;
    ParseStatus parseStatus;
    ParseTriggerMode triggerMode;
    Integer retryCount;
    String subscribedBy;
    Instant subscribedAt;
    Instant parsedAt;
    String lastErrorMessage;
    String objectSnapshotJson;
    String deliverySnapshotJson;
    String parseRequestJson;
    String parseResultJson;
    Instant createdAt;
    Instant updatedAt;

    public String queueId() { return queueId; }
    public String businessKey() { return businessKey; }
    public String transferId() { return transferId; }
    public String originalName() { return originalName; }
    public String sourceId() { return sourceId; }
    public String sourceType() { return sourceType; }
    public String sourceCode() { return sourceCode; }
    public String routeId() { return routeId; }
    public String deliveryId() { return deliveryId; }
    public String tagId() { return tagId; }
    public String tagCode() { return tagCode; }
    public String tagName() { return tagName; }
    public String fileStatus() { return fileStatus; }
    public String deliveryStatus() { return deliveryStatus; }
    public ParseStatus parseStatus() { return parseStatus; }
    public ParseTriggerMode triggerMode() { return triggerMode; }
    public Integer retryCount() { return retryCount; }
    public String subscribedBy() { return subscribedBy; }
    public Instant subscribedAt() { return subscribedAt; }
    public Instant parsedAt() { return parsedAt; }
    public String lastErrorMessage() { return lastErrorMessage; }
    public String objectSnapshotJson() { return objectSnapshotJson; }
    public String deliverySnapshotJson() { return deliverySnapshotJson; }
    public String parseRequestJson() { return parseRequestJson; }
    public String parseResultJson() { return parseResultJson; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
