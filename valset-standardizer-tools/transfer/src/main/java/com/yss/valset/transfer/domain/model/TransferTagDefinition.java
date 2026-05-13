package com.yss.valset.transfer.domain.model;

import java.time.Instant;
import java.util.Map;

/**
 * 标签定义。
 */
public class TransferTagDefinition {

    private final String tagId;
    private final String tagCode;
    private final String tagName;
    private final String tagValue;
    private final boolean enabled;
    private final int priority;
    private final String matchStrategy;
    private final String scriptLanguage;
    private final String scriptBody;
    private final String regexPattern;
    private final Map<String, Object> tagMeta;
    private final Instant createdAt;
    private final Instant updatedAt;

    public TransferTagDefinition(String tagId, String tagCode, String tagName, String tagValue, boolean enabled, int priority, String matchStrategy, String scriptLanguage, String scriptBody, String regexPattern, Map<String, Object> tagMeta, Instant createdAt, Instant updatedAt) {
        this.tagId = tagId;
        this.tagCode = tagCode;
        this.tagName = tagName;
        this.tagValue = tagValue;
        this.enabled = enabled;
        this.priority = priority;
        this.matchStrategy = matchStrategy;
        this.scriptLanguage = scriptLanguage;
        this.scriptBody = scriptBody;
        this.regexPattern = regexPattern;
        this.tagMeta = tagMeta;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public boolean enabled() {
        return enabled;
    }

    public int priority() {
        return priority;
    }

    public String matchStrategy() {
        return matchStrategy;
    }

    public String scriptLanguage() {
        return scriptLanguage;
    }

    public String scriptBody() {
        return scriptBody;
    }

    public String regexPattern() {
        return regexPattern;
    }

    public Map<String, Object> tagMeta() {
        return tagMeta;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
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

    public boolean getEnabled() {
        return enabled;
    }

    public int getPriority() {
        return priority;
    }

    public String getMatchStrategy() {
        return matchStrategy;
    }

    public String getScriptLanguage() {
        return scriptLanguage;
    }

    public String getScriptBody() {
        return scriptBody;
    }

    public String getRegexPattern() {
        return regexPattern;
    }

    public Map<String, Object> getTagMeta() {
        return tagMeta;
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
        TransferTagDefinition other = (TransferTagDefinition) o;
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
        if (enabled != other.enabled) {
            return false;
        }
        if (priority != other.priority) {
            return false;
        }
        if (!java.util.Objects.equals(matchStrategy, other.matchStrategy)) {
            return false;
        }
        if (!java.util.Objects.equals(scriptLanguage, other.scriptLanguage)) {
            return false;
        }
        if (!java.util.Objects.equals(scriptBody, other.scriptBody)) {
            return false;
        }
        if (!java.util.Objects.equals(regexPattern, other.regexPattern)) {
            return false;
        }
        if (!java.util.Objects.equals(tagMeta, other.tagMeta)) {
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
        return java.util.Objects.hash(tagId, tagCode, tagName, tagValue, enabled, priority, matchStrategy, scriptLanguage, scriptBody, regexPattern, tagMeta, createdAt, updatedAt);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TransferTagDefinition[");
        sb.append("tagId=").append(tagId);
        sb.append(", tagCode=").append(tagCode);
        sb.append(", tagName=").append(tagName);
        sb.append(", tagValue=").append(tagValue);
        sb.append(", enabled=").append(enabled);
        sb.append(", priority=").append(priority);
        sb.append(", matchStrategy=").append(matchStrategy);
        sb.append(", scriptLanguage=").append(scriptLanguage);
        sb.append(", scriptBody=").append(scriptBody);
        sb.append(", regexPattern=").append(regexPattern);
        sb.append(", tagMeta=").append(tagMeta);
        sb.append(", createdAt=").append(createdAt);
        sb.append(", updatedAt=").append(updatedAt);
        sb.append(']');
        return sb.toString();
    }



}
