package com.yss.valset.transfer.domain.gateway;

import com.yss.valset.transfer.domain.model.ProductMatchRule;

import java.util.List;

/**
 * 产品识别规则网关。
 */
public interface ProductMatchRuleGateway {

    List<ProductMatchRule> listEnabledRules();
}
