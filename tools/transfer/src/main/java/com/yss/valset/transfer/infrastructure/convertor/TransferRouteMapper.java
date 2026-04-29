package com.yss.valset.transfer.infrastructure.convertor;

import com.yss.valset.transfer.domain.model.TransferRoute;
import com.yss.valset.transfer.infrastructure.entity.TransferRoutePO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 文件路由映射器。
 */
@Mapper(componentModel = "spring", uses = TransferJsonMapper.class)
public interface TransferRouteMapper extends TransferMapstructSupport {

    @Mapping(target = "sourceType", expression = "java(enumName(transferRoute.sourceType()))")
    @Mapping(target = "targetType", expression = "java(enumName(transferRoute.targetType()))")
    @Mapping(target = "routeStatus", expression = "java(enumName(transferRoute.routeStatus()))")
    @Mapping(target = "routeId", expression = "java(transferRoute.routeId())")
    @Mapping(target = "sourceId", expression = "java(transferRoute.sourceId())")
    @Mapping(target = "ruleId", expression = "java(transferRoute.ruleId())")
    @Mapping(target = "enabled", expression = "java(transferRoute.enabled())")
    @Mapping(target = "pollCron", expression = "java(transferRoute.pollCron())")
    @Mapping(target = "routeMetaJson", source = "routeMeta", qualifiedByName = "transferToJson")
    TransferRoutePO toPO(TransferRoute transferRoute);

    @Mapping(target = "sourceType", expression = "java(sourceTypeOf(po.getSourceType()))")
    @Mapping(target = "targetType", expression = "java(targetTypeOf(po.getTargetType()))")
    @Mapping(target = "routeStatus", expression = "java(statusOf(po.getRouteStatus()))")
    @Mapping(target = "routeId", expression = "java(stringValue(po.getRouteId()))")
    @Mapping(target = "sourceId", expression = "java(stringValue(po.getSourceId()))")
    @Mapping(target = "ruleId", expression = "java(stringValue(po.getRuleId()))")
    @Mapping(target = "enabled", expression = "java(Boolean.TRUE.equals(po.getEnabled()))")
    @Mapping(target = "pollCron", expression = "java(po.getPollCron())")
    @Mapping(target = "routeMeta", source = "routeMetaJson", qualifiedByName = "transferToJson")
    TransferRoute toDomain(TransferRoutePO po);
}
