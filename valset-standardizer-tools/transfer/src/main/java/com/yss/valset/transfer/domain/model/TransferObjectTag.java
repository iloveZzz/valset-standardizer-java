package com.yss.valset.transfer.domain.model;

import java.time.Instant;
import java.util.Map;

/**
 * 文件对象标签。
 */
public class TransferObjectTag {

    private final String id;
    private final String transferId;
    private final String tagId;
    private final String tagCode;
    private final String tagName;
    private final String tagValue;
    private final String matchStrategy;
    private final String matchReason;
    private final String matchedField;
    private final String matchedValue;
    private final Map<String, Object> matchSnapshot;
    private final Instant createdAt;

    public TransferObjectTag(String id, String transferId, String tagId, String tagCode, String tagName, String tagValue, String matchStrategy, String matchReason, String matchedField, String matchedValue, Map<String, Object> matchSnapshot, Instant createdAt) {
        this.id = id;
        this.transferId = transferId;
        this.tagId = tagId;
        this.tagCode = tagCode;
        this.tagName = tagName;
        this.tagValue = tagValue;
        this.matchStrategy = matchStrategy;
        this.matchReason = matchReason;
        this.matchedField = matchedField;
        this.matchedValue = matchedValue;
        this.matchSnapshot = matchSnapshot;
        this.createdAt = createdAt;
    }



    public String id() {
        return id;
    }

    public String transferId() {
        return transferId;
    }

    public String tagId() {
        return tagId;
    }

    public String tagCode() {
        return tagCode;
    }

    public String tagName() {
        return tagName;
    }

    public String tagValue() {
        return tagValue;
    }

    public String matchStrategy() {
        return matchStrategy;
    }

    public String matchReason() {
        return matchReason;
    }

    public String matchedField() {
        return matchedField;
    }

    public String matchedValue() {
        return matchedValue;
    }

    public Map<String, Object> matchSnapshot() {
        return matchSnapshot;
    }

    public Instant createdAt() {
        return createdAt;
    }



    public String getId() {
        return id;
    }

    public String getTransferId() {
        return transferId;
    }

    public String getTagId() {
        return tagId;
    }

    public String getTagCode() {
        return tagCode;
    }

    public String getTagName() {
        return tagName;
    }

    public String getTagValue() {
        return tagValue;
    }

    public String getMatchStrategy() {
        return matchStrategy;
    }

    public String getMatchReason() {
        return matchReason;
    }

    public String getMatchedField() {
        return matchedField;
    }

    public String getMatchedValue() {
        return matchedValue;
    }

    public Map<String, Object> getMatchSnapshot() {
        return matchSnapshot;
    }

    public Instant getCreatedAt() {
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
        TransferObjectTag other = (TransferObjectTag) o;
        if (!java.util.Objects.equals(id, other.id)) {
            return false;
        }
        if (!java.util.Objects.equals(transferId, other.transferId)) {
            return false;
        }
        if (!java.util.Objects.equals(tagId, other.tagId)) {
            return false;
        }
        if (!java.util.Objects.equals(tagCode, other.tagCode)) {
            return false;
        }
        if (!java.util.Objects.equals(tagName, other.tagName)) {
            return false;
        }
        if (!java.util.Objects.equals(tagValue, other.tagValue)) {
            return false;
        }
        if (!java.util.Objects.equals(matchStrategy, other.matchStrategy)) {
            return false;
        }
        if (!java.util.Objects.equals(matchReason, other.matchReason)) {
            return false;
        }
        if (!java.util.Objects.equals(matchedField, other.matchedField)) {
            return false;
        }
        if (!java.util.Objects.equals(matchedValue, other.matchedValue)) {
            return false;
        }
        if (!java.util.Objects.equals(matchSnapshot, other.matchSnapshot)) {
            return false;
        }
        if (!java.util.Objects.equals(createdAt, other.createdAt)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, transferId, tagId, tagCode, tagName, tagValue, matchStrategy, matchReason, matchedField, matchedValue, matchSnapshot, createdAt);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TransferObjectTag[");
        sb.append("id=").append(id);
        sb.append(", transferId=").append(transferId);
        sb.append(", tagId=").append(tagId);
        sb.append(", tagCode=").append(tagCode);
        sb.append(", tagName=").append(tagName);
        sb.append(", tagValue=").append(tagValue);
        sb.append(", matchStrategy=").append(matchStrategy);
        sb.append(", matchReason=").append(matchReason);
        sb.append(", matchedField=").append(matchedField);
        sb.append(", matchedValue=").append(matchedValue);
        sb.append(", matchSnapshot=").append(matchSnapshot);
        sb.append(", createdAt=").append(createdAt);
        sb.append(']');
        return sb.toString();
    }



}
