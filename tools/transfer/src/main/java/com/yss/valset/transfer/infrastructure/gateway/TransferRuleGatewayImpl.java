package com.yss.valset.transfer.infrastructure.gateway;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.valset.transfer.domain.gateway.TransferRuleGateway;
import com.yss.valset.transfer.domain.model.RuleDefinition;
import com.yss.valset.transfer.infrastructure.convertor.TransferJsonMapper;
import com.yss.valset.transfer.infrastructure.entity.TransferRulePO;
import com.yss.valset.transfer.infrastructure.mapper.TransferRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MyBatis 支持的文件规则网关。
 */
@Primary
@Repository
@RequiredArgsConstructor
public class TransferRuleGatewayImpl implements TransferRuleGateway {

    private final TransferRuleRepository transferRuleRepository;
    private final TransferJsonMapper transferJsonMapper;

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
    public Optional<RuleDefinition> findByRuleCode(String ruleCode) {
        TransferRulePO po = transferRuleRepository.selectOne(
                Wrappers.lambdaQuery(TransferRulePO.class)
                        .eq(TransferRulePO::getRuleCode, ruleCode)
                        .last("limit 1")
        );
        return Optional.ofNullable(po).map(this::toDomain);
    }

    private RuleDefinition toDomain(TransferRulePO po) {
        return new RuleDefinition(
                po.getRuleId(),
                po.getRuleCode(),
                po.getRuleName(),
                po.getRuleVersion(),
                Boolean.TRUE.equals(po.getEnabled()),
                po.getPriority() == null ? 0 : po.getPriority(),
                po.getMatchStrategy(),
                po.getScriptLanguage(),
                po.getScriptBody(),
                toInstant(po.getEffectiveFrom()),
                toInstant(po.getEffectiveTo()),
                transferJsonMapper.toMap(po.getRuleMetaJson())
        );
    }

    private Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }
}
