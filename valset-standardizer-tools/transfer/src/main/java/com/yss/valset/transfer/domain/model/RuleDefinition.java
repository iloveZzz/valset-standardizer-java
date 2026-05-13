package com.yss.valset.transfer.domain.model;

import java.time.Instant;
import java.util.Map;

/**
 * 规则定义。
 */
public class RuleDefinition {

    private final String ruleId;
    private final String ruleCode;
    private final String ruleName;
    private final String ruleVersion;
    private final boolean enabled;
    private final int priority;
    private final String matchStrategy;
    private final String scriptLanguage;
    private final String scriptBody;
    private final Instant effectiveFrom;
    private final Instant effectiveTo;
    private final Map<String, Object> ruleMeta;

    public RuleDefinition(String ruleId, String ruleCode, String ruleName, String ruleVersion, boolean enabled, int priority, String matchStrategy, String scriptLanguage, String scriptBody, Instant effectiveFrom, Instant effectiveTo, Map<String, Object> ruleMeta) {
        this.ruleId = ruleId;
        this.ruleCode = ruleCode;
        this.ruleName = ruleName;
        this.ruleVersion = ruleVersion;
        this.enabled = enabled;
        this.priority = priority;
        this.matchStrategy = matchStrategy;
        this.scriptLanguage = scriptLanguage;
        this.scriptBody = scriptBody;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.ruleMeta = ruleMeta;
    }



    public String ruleId() {
        return ruleId;
    }

    public String ruleCode() {
        return ruleCode;
    }

    public String ruleName() {
        return ruleName;
    }

    public String ruleVersion() {
        return ruleVersion;
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

    public Instant effectiveFrom() {
        return effectiveFrom;
    }

    public Instant effectiveTo() {
        return effectiveTo;
    }

    public Map<String, Object> ruleMeta() {
        return ruleMeta;
    }



    public String getRuleId() {
        return ruleId;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public String getRuleName() {
        return ruleName;
    }

    public String getRuleVersion() {
        return ruleVersion;
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

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public Instant getEffectiveTo() {
        return effectiveTo;
    }

    public Map<String, Object> getRuleMeta() {
        return ruleMeta;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RuleDefinition other = (RuleDefinition) o;
        if (!java.util.Objects.equals(ruleId, other.ruleId)) {
            return false;
        }
        if (!java.util.Objects.equals(ruleCode, other.ruleCode)) {
            return false;
        }
        if (!java.util.Objects.equals(ruleName, other.ruleName)) {
            return false;
        }
        if (!java.util.Objects.equals(ruleVersion, other.ruleVersion)) {
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
        if (!java.util.Objects.equals(effectiveFrom, other.effectiveFrom)) {
            return false;
        }
        if (!java.util.Objects.equals(effectiveTo, other.effectiveTo)) {
            return false;
        }
        if (!java.util.Objects.equals(ruleMeta, other.ruleMeta)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(ruleId, ruleCode, ruleName, ruleVersion, enabled, priority, matchStrategy, scriptLanguage, scriptBody, effectiveFrom, effectiveTo, ruleMeta);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("RuleDefinition[");
        sb.append("ruleId=").append(ruleId);
        sb.append(", ruleCode=").append(ruleCode);
        sb.append(", ruleName=").append(ruleName);
        sb.append(", ruleVersion=").append(ruleVersion);
        sb.append(", enabled=").append(enabled);
        sb.append(", priority=").append(priority);
        sb.append(", matchStrategy=").append(matchStrategy);
        sb.append(", scriptLanguage=").append(scriptLanguage);
        sb.append(", scriptBody=").append(scriptBody);
        sb.append(", effectiveFrom=").append(effectiveFrom);
        sb.append(", effectiveTo=").append(effectiveTo);
        sb.append(", ruleMeta=").append(ruleMeta);
        sb.append(']');
        return sb.toString();
    }



}
