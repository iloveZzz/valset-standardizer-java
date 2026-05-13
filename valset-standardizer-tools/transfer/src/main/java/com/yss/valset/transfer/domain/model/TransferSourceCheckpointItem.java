package com.yss.valset.transfer.domain.model;

import java.time.Instant;
import java.util.Map;

/**
 * 来源处理去重记录。
 */
public class TransferSourceCheckpointItem {

    private final String checkpointItemId;
    private final String sourceId;
    private final String sourceType;
    private final String itemKey;
    private final String itemRef;
    private final String itemName;
    private final Long itemSize;
    private final String itemMimeType;
    private final String itemFingerprint;
    private final Map<String, Object> itemMeta;
    private final String triggerType;
    private final Instant processedAt;
    private final Instant createdAt;
    private final Instant updatedAt;

    public TransferSourceCheckpointItem(String checkpointItemId, String sourceId, String sourceType, String itemKey, String itemRef, String itemName, Long itemSize, String itemMimeType, String itemFingerprint, Map<String, Object> itemMeta, String triggerType, Instant processedAt, Instant createdAt, Instant updatedAt) {
        this.checkpointItemId = checkpointItemId;
        this.sourceId = sourceId;
        this.sourceType = sourceType;
        this.itemKey = itemKey;
        this.itemRef = itemRef;
        this.itemName = itemName;
        this.itemSize = itemSize;
        this.itemMimeType = itemMimeType;
        this.itemFingerprint = itemFingerprint;
        this.itemMeta = itemMeta;
        this.triggerType = triggerType;
        this.processedAt = processedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }



    public String checkpointItemId() {
        return checkpointItemId;
    }

    public String sourceId() {
        return sourceId;
    }

    public String sourceType() {
        return sourceType;
    }

    public String itemKey() {
        return itemKey;
    }

    public String itemRef() {
        return itemRef;
    }

    public String itemName() {
        return itemName;
    }

    public Long itemSize() {
        return itemSize;
    }

    public String itemMimeType() {
        return itemMimeType;
    }

    public String itemFingerprint() {
        return itemFingerprint;
    }

    public Map<String, Object> itemMeta() {
        return itemMeta;
    }

    public String triggerType() {
        return triggerType;
    }

    public Instant processedAt() {
        return processedAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }



    public String getCheckpointItemId() {
        return checkpointItemId;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getItemKey() {
        return itemKey;
    }

    public String getItemRef() {
        return itemRef;
    }

    public String getItemName() {
        return itemName;
    }

    public Long getItemSize() {
        return itemSize;
    }

    public String getItemMimeType() {
        return itemMimeType;
    }

    public String getItemFingerprint() {
        return itemFingerprint;
    }

    public Map<String, Object> getItemMeta() {
        return itemMeta;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public Instant getProcessedAt() {
        return processedAt;
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
        TransferSourceCheckpointItem other = (TransferSourceCheckpointItem) o;
        if (!java.util.Objects.equals(checkpointItemId, other.checkpointItemId)) {
            return false;
        }
        if (!java.util.Objects.equals(sourceId, other.sourceId)) {
            return false;
        }
        if (!java.util.Objects.equals(sourceType, other.sourceType)) {
            return false;
        }
        if (!java.util.Objects.equals(itemKey, other.itemKey)) {
            return false;
        }
        if (!java.util.Objects.equals(itemRef, other.itemRef)) {
            return false;
        }
        if (!java.util.Objects.equals(itemName, other.itemName)) {
            return false;
        }
        if (!java.util.Objects.equals(itemSize, other.itemSize)) {
            return false;
        }
        if (!java.util.Objects.equals(itemMimeType, other.itemMimeType)) {
            return false;
        }
        if (!java.util.Objects.equals(itemFingerprint, other.itemFingerprint)) {
            return false;
        }
        if (!java.util.Objects.equals(itemMeta, other.itemMeta)) {
            return false;
        }
        if (!java.util.Objects.equals(triggerType, other.triggerType)) {
            return false;
        }
        if (!java.util.Objects.equals(processedAt, other.processedAt)) {
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
        return java.util.Objects.hash(checkpointItemId, sourceId, sourceType, itemKey, itemRef, itemName, itemSize, itemMimeType, itemFingerprint, itemMeta, triggerType, processedAt, createdAt, updatedAt);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TransferSourceCheckpointItem[");
        sb.append("checkpointItemId=").append(checkpointItemId);
        sb.append(", sourceId=").append(sourceId);
        sb.append(", sourceType=").append(sourceType);
        sb.append(", itemKey=").append(itemKey);
        sb.append(", itemRef=").append(itemRef);
        sb.append(", itemName=").append(itemName);
        sb.append(", itemSize=").append(itemSize);
        sb.append(", itemMimeType=").append(itemMimeType);
        sb.append(", itemFingerprint=").append(itemFingerprint);
        sb.append(", itemMeta=").append(itemMeta);
        sb.append(", triggerType=").append(triggerType);
        sb.append(", processedAt=").append(processedAt);
        sb.append(", createdAt=").append(createdAt);
        sb.append(", updatedAt=").append(updatedAt);
        sb.append(']');
        return sb.toString();
    }



}
