package com.yss.valset.transfer.infrastructure.convertor;

import com.yss.valset.transfer.domain.model.TransferTarget;
import com.yss.valset.transfer.infrastructure.entity.TransferTargetPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 投递目标映射器。
 */
@Mapper(componentModel = "spring", uses = TransferJsonMapper.class)
public interface TransferTargetMapper extends TransferMapstructSupport {

    @Mapping(target = "targetType", expression = "java(enumName(transferTarget.targetType()))")
    @Mapping(target = "enabled", expression = "java(Boolean.valueOf(transferTarget.enabled()))")
    @Mapping(target = "connectionConfigJson", source = "connectionConfig")
    @Mapping(target = "targetMetaJson", source = "targetMeta")
    TransferTargetPO toPO(TransferTarget transferTarget);

    @Mapping(target = "targetType", expression = "java(targetTypeOf(po.getTargetType()))")
    @Mapping(target = "enabled", expression = "java(Boolean.TRUE.equals(po.getEnabled()))")
    @Mapping(target = "connectionConfig", source = "connectionConfigJson")
    @Mapping(target = "targetMeta", source = "targetMetaJson")
    TransferTarget toDomain(TransferTargetPO po);
}
