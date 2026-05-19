package com.yss.valset.transfer.infrastructure.gateway;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.valset.transfer.domain.gateway.ProductMatchRuleGateway;
import com.yss.valset.transfer.domain.model.ProductMatchRule;
import com.yss.valset.transfer.infrastructure.entity.ProductMatchRulePO;
import com.yss.valset.transfer.infrastructure.mapper.ProductMatchRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MyBatis 支持的产品识别规则网关。
 */
@Primary
@Repository
@RequiredArgsConstructor
public class ProductMatchRuleGatewayImpl implements ProductMatchRuleGateway {

    private final ProductMatchRuleRepository productMatchRuleRepository;

    @Override
    public List<ProductMatchRule> listEnabledRules() {
        return productMatchRuleRepository.selectList(
                        Wrappers.lambdaQuery(ProductMatchRulePO.class)
                                .eq(ProductMatchRulePO::getIsValid, 1)
                                .isNotNull(ProductMatchRulePO::getMatchRules)
                                .orderByAsc(ProductMatchRulePO::getId)
                )
                .stream()
                .filter(po -> po != null && StringUtils.hasText(po.getMatchRules()))
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private ProductMatchRule toDomain(ProductMatchRulePO po) {
        return new ProductMatchRule(
                po.getId() == null ? null : String.valueOf(po.getId()),
                po.getFileTypeName(),
                po.getPdCd(),
                po.getPdNm(),
                po.getOrgCd(),
                po.getOrgNm(),
                po.getPdType(),
                po.getFileType(),
                po.getMatchRules(),
                po.getJobName(),
                po.getJobScene()
        );
    }
}
