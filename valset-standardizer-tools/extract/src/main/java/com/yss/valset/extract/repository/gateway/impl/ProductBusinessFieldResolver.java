package com.yss.valset.extract.repository.gateway.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.valset.extract.support.ProductBusinessFields;
import com.yss.valset.transfer.infrastructure.entity.ProductMatchRulePO;
import com.yss.valset.transfer.infrastructure.entity.TransferObjectTagPO;
import com.yss.valset.transfer.infrastructure.mapper.ProductMatchRuleRepository;
import com.yss.valset.transfer.infrastructure.mapper.TransferObjectTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 从传输对象产品识别标签解析标准落地表需要的产品业务字段。
 */
@Component
@RequiredArgsConstructor
public class ProductBusinessFieldResolver {

    private static final String PRODUCT_MATCH_RULE_TAG_CODE = "PRODUCT_MATCH_RULE";

    private final TransferObjectTagRepository transferObjectTagRepository;
    private final ProductMatchRuleRepository productMatchRuleRepository;

    public ProductBusinessFields resolve(Long fileId) {
        if (fileId == null) {
            return null;
        }
        List<TransferObjectTagPO> tags = transferObjectTagRepository.selectList(
                Wrappers.lambdaQuery(TransferObjectTagPO.class)
                        .eq(TransferObjectTagPO::getTransferId, String.valueOf(fileId))
                        .eq(TransferObjectTagPO::getTagCode, PRODUCT_MATCH_RULE_TAG_CODE)
                        .isNotNull(TransferObjectTagPO::getTagValue)
                        .orderByDesc(TransferObjectTagPO::getCreatedAt)
                        .orderByDesc(TransferObjectTagPO::getId)
        );
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        for (TransferObjectTagPO tag : tags) {
            Long ruleId = parseRuleId(tag == null ? null : tag.getTagValue());
            if (ruleId == null) {
                continue;
            }
            ProductMatchRulePO rule = productMatchRuleRepository.selectById(ruleId);
            if (rule != null) {
                return new ProductBusinessFields(rule.getPdCd(), rule.getOrgCd());
            }
        }
        return null;
    }

    private Long parseRuleId(String tagValue) {
        if (!StringUtils.hasText(tagValue)) {
            return null;
        }
        try {
            return Long.valueOf(tagValue.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
