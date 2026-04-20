package com.yss.valset.transfer.infrastructure.convertor;

import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.infrastructure.entity.TransferObjectPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Context;

import java.util.Map;

/**
 * 文件主对象映射器。
 */
@Mapper(componentModel = "spring", uses = TransferJsonMapper.class)
public interface TransferObjectMapper extends TransferMapstructSupport {

    @Mapping(target = "sourceType", expression = "java(stringValue(transferObject.fileMeta(), \"sourceType\"))")
    @Mapping(target = "sourceCode", expression = "java(stringValue(transferObject.fileMeta(), \"sourceCode\"))")
    @Mapping(target = "fileMetaJson", source = "fileMeta")
    @Mapping(target = "status", expression = "java(enumName(transferObject.status()))")
    @Mapping(target = "receivedAt", expression = "java(toLocalDateTime(transferObject.receivedAt()))")
    @Mapping(target = "storedAt", expression = "java(toLocalDateTime(transferObject.storedAt()))")
    TransferObjectPO toPO(TransferObject transferObject, @Context TransferJsonMapper transferJsonMapper);

    @Mapping(target = "status", expression = "java(statusOf(po.getStatus()))")
    @Mapping(target = "receivedAt", expression = "java(toInstant(po.getReceivedAt()))")
    @Mapping(target = "storedAt", expression = "java(toInstant(po.getStoredAt()))")
    @Mapping(target = "fileMeta", expression = "java(mergeFileMeta(po, transferJsonMapper))")
    TransferObject toDomain(TransferObjectPO po, @Context TransferJsonMapper transferJsonMapper);

    default Map<String, Object> mergeFileMeta(TransferObjectPO po, TransferJsonMapper transferJsonMapper) {
        Map<String, Object> meta = new java.util.LinkedHashMap<>();
        Map<String, Object> storedMeta = transferJsonMapper.toMap(po.getFileMetaJson());
        if (storedMeta != null) {
            meta.putAll(storedMeta);
        }
        meta.putIfAbsent("sourceType", po.getSourceType());
        meta.putIfAbsent("sourceCode", po.getSourceCode());
        meta.putIfAbsent("mailId", po.getMailId());
        meta.putIfAbsent("mailFrom", po.getMailFrom());
        meta.putIfAbsent("mailTo", po.getMailTo());
        meta.putIfAbsent("mailCc", po.getMailCc());
        meta.putIfAbsent("mailBcc", po.getMailBcc());
        meta.putIfAbsent("mailSubject", po.getMailSubject());
        meta.putIfAbsent("mailBody", po.getMailBody());
        meta.putIfAbsent("mailProtocol", po.getMailProtocol());
        meta.putIfAbsent("mailFolder", po.getMailFolder());
        return meta;
    }
}
