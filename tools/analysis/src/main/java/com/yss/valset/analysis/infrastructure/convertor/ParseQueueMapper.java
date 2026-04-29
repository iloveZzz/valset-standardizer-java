package com.yss.valset.analysis.infrastructure.convertor;

import com.yss.valset.analysis.domain.model.ParseQueue;
import com.yss.valset.analysis.domain.model.ParseStatus;
import com.yss.valset.analysis.domain.model.ParseTriggerMode;
import com.yss.valset.analysis.infrastructure.entity.ParseQueuePO;
import com.yss.valset.transfer.infrastructure.convertor.TransferMapstructSupport;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 待解析任务映射器。
 */
@Mapper
public interface ParseQueueMapper extends TransferMapstructSupport {

    ParseQueueMapper INSTANCE = Mappers.getMapper(ParseQueueMapper.class);

    @Mapping(target = "parseStatus", expression = "java(parseStatusOf(po.getParseStatus()))")
    @Mapping(target = "triggerMode", expression = "java(triggerModeOf(po.getTriggerMode()))")
    @Mapping(target = "subscribedAt", expression = "java(toInstant(po.getSubscribedAt()))")
    @Mapping(target = "parsedAt", expression = "java(toInstant(po.getParsedAt()))")
    @Mapping(target = "createdAt", expression = "java(toInstant(po.getCreatedAt()))")
    @Mapping(target = "updatedAt", expression = "java(toInstant(po.getUpdatedAt()))")
    ParseQueue toDomain(ParseQueuePO po);

    @Mapping(target = "parseStatus", expression = "java(enumName(queue.parseStatus()))")
    @Mapping(target = "triggerMode", expression = "java(enumName(queue.triggerMode()))")
    @Mapping(target = "subscribedAt", expression = "java(toLocalDateTime(queue.subscribedAt()))")
    @Mapping(target = "parsedAt", expression = "java(toLocalDateTime(queue.parsedAt()))")
    @Mapping(target = "createdAt", expression = "java(toLocalDateTime(queue.createdAt()))")
    @Mapping(target = "updatedAt", expression = "java(toLocalDateTime(queue.updatedAt()))")
    ParseQueuePO toPO(ParseQueue queue);

    default ParseStatus parseStatusOf(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return ParseStatus.valueOf(value);
    }

    default ParseTriggerMode triggerModeOf(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return ParseTriggerMode.valueOf(value);
    }
}
