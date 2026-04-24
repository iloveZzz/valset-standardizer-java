package com.yss.valset.transfer.domain.gateway;

import com.yss.valset.transfer.domain.model.RuleDefinition;

import java.util.List;
import java.util.Optional;

/**
 * 规则网关。
 */
public interface TransferRuleGateway {

    List<RuleDefinition> listRules(String ruleCode, Boolean enabled, Integer limit);

    List<RuleDefinition> listEnabledRules();

    Optional<RuleDefinition> findById(String ruleId);

    Optional<RuleDefinition> findByRuleCode(String ruleCode);

    RuleDefinition save(RuleDefinition ruleDefinition);

    void deleteById(String ruleId);
}
