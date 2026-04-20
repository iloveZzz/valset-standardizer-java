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

    @Mapping(target = "targetType", expression = "java(enumName(transferRoute.targetType()))")
    @Mapping(target = "routeStatus", expression = "java(enumName(transferRoute.routeStatus()))")
    @Mapping(target = "routeMetaJson", source = "routeMeta")
    TransferRoutePO toPO(TransferRoute transferRoute);

    @Mapping(target = "targetType", expression = "java(targetTypeOf(po.getTargetType()))")
    @Mapping(target = "routeStatus", expression = "java(statusOf(po.getRouteStatus()))")
    @Mapping(target = "routeMeta", source = "routeMetaJson")
    TransferRoute toDomain(TransferRoutePO po);
}
