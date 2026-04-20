package com.yss.valset.transfer.domain.gateway;

import com.yss.valset.transfer.domain.model.RuleDefinition;

import java.util.List;
import java.util.Optional;

/**
 * 规则网关。
 */
public interface TransferRuleGateway {

    List<RuleDefinition> listEnabledRules();

    Optional<RuleDefinition> findByRuleCode(String ruleCode);
}
