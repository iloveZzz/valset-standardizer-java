package com.yss.valset.transfer.infrastructure.convertor;

import com.yss.valset.transfer.domain.model.TransferSource;
import com.yss.valset.transfer.infrastructure.entity.TransferSourcePO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;

/**
 * 文件来源映射器。
 */
@Mapper(componentModel = "spring", uses = TransferJsonMapper.class)
public interface TransferSourceMapper extends TransferMapstructSupport {

    @Mapping(target = "sourceType", expression = "java(enumName(transferSource.sourceType()))")
    @Mapping(target = "enabled", expression = "java(Boolean.valueOf(transferSource.enabled()))")
    @Mapping(target = "sourceId", expression = "java(transferSource.sourceId())")
    @Mapping(target = "connectionConfigJson", source = "connectionConfig", qualifiedByName = "transferToJson")
    @Mapping(target = "sourceMetaJson", source = "sourceMeta", qualifiedByName = "transferToJson")
    @Mapping(target = "ingestTriggerType", expression = "java(transferSource.ingestTriggerType())")
    @Mapping(target = "createdAt", expression = "java(toLocalDateTime(transferSource.createdAt()))")
    @Mapping(target = "updatedAt", expression = "java(toLocalDateTime(transferSource.updatedAt()))")
    TransferSourcePO toPO(TransferSource transferSource);

    @Mapping(target = "sourceType", expression = "java(sourceTypeOf(po.getSourceType()))")
    @Mapping(target = "enabled", expression = "java(Boolean.TRUE.equals(po.getEnabled()))")
    @Mapping(target = "sourceId", expression = "java(stringValue(po.getSourceId()))")
    @Mapping(target = "connectionConfig", source = "connectionConfigJson", qualifiedByName = "transferToJson")
    @Mapping(target = "sourceMeta", source = "sourceMetaJson", qualifiedByName = "transferToJson")
    @Mapping(target = "ingestStatus", expression = "java(po.getIngestStatus())")
    @Mapping(target = "ingestTriggerType", expression = "java(po.getIngestTriggerType())")
    @Mapping(target = "ingestStartedAt", expression = "java(toInstant(po.getIngestStartedAt()))")
    @Mapping(target = "ingestFinishedAt", expression = "java(toInstant(po.getIngestFinishedAt()))")
    @Mapping(target = "createdAt", expression = "java(toInstant(po.getCreatedAt()))")
    @Mapping(target = "updatedAt", expression = "java(toInstant(po.getUpdatedAt()))")
    TransferSource toDomain(TransferSourcePO po);
}
