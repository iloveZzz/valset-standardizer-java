package com.yss.valset.transfer.infrastructure.convertor;

import com.yss.valset.transfer.domain.model.RuleDefinition;
import com.yss.valset.transfer.infrastructure.entity.TransferRulePO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 文件规则映射器。
 */
@Mapper(componentModel = "spring", uses = TransferJsonMapper.class)
public interface TransferRuleMapper extends TransferMapstructSupport {

    @Mapping(target = "enabled", expression = "java(Boolean.TRUE.equals(ruleDefinition.enabled()))")
    @Mapping(target = "ruleId", expression = "java(ruleDefinition.ruleId())")
    @Mapping(target = "effectiveFrom", expression = "java(toLocalDateTime(ruleDefinition.effectiveFrom()))")
    @Mapping(target = "effectiveTo", expression = "java(toLocalDateTime(ruleDefinition.effectiveTo()))")
    @Mapping(target = "ruleMetaJson", source = "ruleMeta", qualifiedByName = "transferToJson")
    TransferRulePO toPO(RuleDefinition ruleDefinition);

    @Mapping(target = "enabled", expression = "java(Boolean.TRUE.equals(po.getEnabled()))")
    @Mapping(target = "ruleId", expression = "java(stringValue(po.getRuleId()))")
    @Mapping(target = "ruleMeta", source = "ruleMetaJson", qualifiedByName = "transferToJson")
    RuleDefinition toDomain(TransferRulePO po);
}
