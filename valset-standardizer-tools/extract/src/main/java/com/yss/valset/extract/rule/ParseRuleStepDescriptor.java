package com.yss.valset.extract.rule;

import com.yss.valset.domain.rule.ParseRuleType;
import lombok.Builder;
import lombok.Value;

/**
 * 运行时解析规则步骤。
 */
@Value
@Builder
public class ParseRuleStepDescriptor {

    Long profileId;

    String profileCode;

    String version;

    ParseRuleType ruleType;

    String stepName;

    String expression;

    String errorPolicy;

    Long timeoutMs;
}
