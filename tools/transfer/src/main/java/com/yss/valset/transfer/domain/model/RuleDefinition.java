package com.yss.valset.transfer.domain.model;

import java.time.Instant;
import java.util.Map;

/**
 * 规则定义。
 */
public record RuleDefinition(
        Long ruleId,
        String ruleCode,
        String ruleName,
        String ruleVersion,
        boolean enabled,
        int priority,
        String matchStrategy,
        String scriptLanguage,
        String scriptBody,
        Instant effectiveFrom,
        Instant effectiveTo,
        Map<String, Object> ruleMeta
) {
}
