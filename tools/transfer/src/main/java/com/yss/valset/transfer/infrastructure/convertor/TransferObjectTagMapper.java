package com.yss.valset.transfer.infrastructure.convertor;

import com.yss.valset.transfer.domain.model.TransferObjectTag;
import com.yss.valset.transfer.infrastructure.entity.TransferObjectTagPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 文件对象标签结果映射器。
 */
@Mapper(componentModel = "spring", uses = TransferJsonMapper.class)
public interface TransferObjectTagMapper extends TransferMapstructSupport {

    @Mapping(target = "id", expression = "java(tag.id())")
    @Mapping(target = "matchSnapshotJson", source = "matchSnapshot", qualifiedByName = "transferToJson")
    @Mapping(target = "createdAt", expression = "java(toLocalDateTime(tag.createdAt()))")
    TransferObjectTagPO toPO(TransferObjectTag tag);

    @Mapping(target = "id", expression = "java(stringValue(po.getId()))")
    @Mapping(target = "matchSnapshot", source = "matchSnapshotJson", qualifiedByName = "transferToJson")
    @Mapping(target = "createdAt", expression = "java(toInstant(po.getCreatedAt()))")
    TransferObjectTag toDomain(TransferObjectTagPO po);
}
