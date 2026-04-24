package com.yss.valset.transfer.infrastructure.gateway;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.valset.transfer.domain.gateway.TransferRuleGateway;
import com.yss.valset.transfer.domain.model.RuleDefinition;
import com.yss.valset.transfer.infrastructure.convertor.TransferRuleMapper;
import com.yss.valset.transfer.infrastructure.entity.TransferRulePO;
import com.yss.valset.transfer.infrastructure.mapper.TransferRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * MyBatis 支持的文件规则网关。
 */
@Primary
@Repository
@RequiredArgsConstructor
public class TransferRuleGatewayImpl implements TransferRuleGateway {

    private final TransferRuleRepository transferRuleRepository;
    private final TransferRuleMapper transferRuleMapper;

    @Override
    public List<RuleDefinition> listRules(String ruleCode, Boolean enabled, Integer limit) {
        var query = Wrappers.lambdaQuery(TransferRulePO.class)
                .like(ruleCode != null && !ruleCode.isBlank(), TransferRulePO::getRuleCode, ruleCode)
                .eq(enabled != null, TransferRulePO::getEnabled, enabled)
                .orderByAsc(TransferRulePO::getPriority)
                .orderByAsc(TransferRulePO::getRuleId);
        if (limit != null && limit > 0) {
            query.last("limit " + limit);
        }
        return transferRuleRepository.selectList(query)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<RuleDefinition> listEnabledRules() {
        LocalDateTime now = LocalDateTime.now();
        return transferRuleRepository.selectList(
                        Wrappers.lambdaQuery(TransferRulePO.class)
                                .eq(TransferRulePO::getEnabled, Boolean.TRUE)
                                .and(wrapper -> wrapper.isNull(TransferRulePO::getEffectiveFrom)
                                        .or()
                                        .le(TransferRulePO::getEffectiveFrom, now))
                                .and(wrapper -> wrapper.isNull(TransferRulePO::getEffectiveTo)
                                        .or()
                                        .ge(TransferRulePO::getEffectiveTo, now))
                                .orderByAsc(TransferRulePO::getPriority)
                                .orderByAsc(TransferRulePO::getRuleId)
                )
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<RuleDefinition> findById(String ruleId) {
        return Optional.ofNullable(transferRuleRepository.selectById(parseLong(ruleId))).map(this::toDomain);
    }

    @Override
    public Optional<RuleDefinition> findByRuleCode(String ruleCode) {
        TransferRulePO po = transferRuleRepository.selectOne(
                Wrappers.lambdaQuery(TransferRulePO.class)
                        .eq(TransferRulePO::getRuleCode, ruleCode)
                        .last("limit 1")
        );
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public RuleDefinition save(RuleDefinition ruleDefinition) {
        TransferRulePO po = transferRuleMapper.toPO(ruleDefinition);
        if (po.getRuleId() == null) {
            transferRuleRepository.insert(po);
        } else {
            transferRuleRepository.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public void deleteById(String ruleId) {
        Long id = parseLong(ruleId);
        if (id != null) {
            transferRuleRepository.deleteById(id);
        }
    }

    private RuleDefinition toDomain(TransferRulePO po) {
        return transferRuleMapper.toDomain(po);
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.valueOf(value);
    }
}
