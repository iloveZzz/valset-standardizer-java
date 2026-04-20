package com.yss.valset.transfer.domain.rule;

import com.yss.valset.transfer.domain.model.RuleContext;
import com.yss.valset.transfer.domain.model.RuleDefinition;
import com.yss.valset.transfer.domain.model.RuleEvaluationResult;

/**
 * 规则引擎。
 */
public interface RuleEngine {

    RuleEvaluationResult evaluate(RuleDefinition ruleDefinition, RuleContext context);
}
