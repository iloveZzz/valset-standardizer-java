package com.yss.valset.transfer.infrastructure.convertor;

import com.yss.valset.transfer.domain.model.TransferTagDefinition;
import com.yss.valset.transfer.infrastructure.entity.TransferTagPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 标签配置映射器。
 */
@Mapper(componentModel = "spring", uses = TransferJsonMapper.class)
public interface TransferTagMapper extends TransferMapstructSupport {

    @Mapping(target = "tagId", expression = "java(definition.tagId())")
    @Mapping(target = "enabled", expression = "java(Boolean.TRUE.equals(definition.enabled()))")
    @Mapping(target = "tagMetaJson", source = "tagMeta", qualifiedByName = "transferToJson")
    @Mapping(target = "createdAt", expression = "java(toLocalDateTime(definition.createdAt()))")
    @Mapping(target = "updatedAt", expression = "java(toLocalDateTime(definition.updatedAt()))")
    TransferTagPO toPO(TransferTagDefinition definition);

    @Mapping(target = "tagId", expression = "java(stringValue(po.getTagId()))")
    @Mapping(target = "enabled", expression = "java(Boolean.TRUE.equals(po.getEnabled()))")
    @Mapping(target = "tagMeta", source = "tagMetaJson", qualifiedByName = "transferToJson")
    @Mapping(target = "createdAt", expression = "java(toInstant(po.getCreatedAt()))")
    @Mapping(target = "updatedAt", expression = "java(toInstant(po.getUpdatedAt()))")
    TransferTagDefinition toDomain(TransferTagPO po);
}
