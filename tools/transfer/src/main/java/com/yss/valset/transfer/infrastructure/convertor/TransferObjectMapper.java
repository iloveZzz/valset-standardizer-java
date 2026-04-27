package com.yss.valset.transfer.infrastructure.convertor;

import com.yss.valset.transfer.domain.model.ProbeResult;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.config.TransferConfigKeys;
import com.yss.valset.transfer.infrastructure.entity.TransferObjectPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Context;

import java.util.Map;

/**
 * 文件主对象映射器。
 */
@Mapper(componentModel = "spring", uses = TransferJsonMapper.class, imports = TransferConfigKeys.class)
public interface TransferObjectMapper extends TransferMapstructSupport {

    @Mapping(target = "sourceType", source = "sourceType")
    @Mapping(target = "sourceCode", source = "sourceCode")
    @Mapping(target = "transferId", expression = "java(transferObject.transferId())")
    @Mapping(target = "sourceId", expression = "java(transferObject.sourceId())")
    @Mapping(target = "routeId", expression = "java(transferObject.routeId())")
    @Mapping(target = "realStoragePath", expression = "java(transferObject.realStoragePath())")
    @Mapping(target = "probeResultJson", source = "probeResult")
    @Mapping(target = "fileMetaJson", source = "fileMeta")
    @Mapping(target = "status", expression = "java(enumName(transferObject.status()))")
    @Mapping(target = "receivedAt", expression = "java(toLocalDateTime(transferObject.receivedAt()))")
    @Mapping(target = "storedAt", expression = "java(toLocalDateTime(transferObject.storedAt()))")
    TransferObjectPO toPO(TransferObject transferObject, @Context TransferJsonMapper transferJsonMapper);

    @Mapping(target = "transferId", expression = "java(stringValue(po.getTransferId()))")
    @Mapping(target = "sourceId", expression = "java(stringValue(po.getSourceId()))")
    @Mapping(target = "routeId", expression = "java(stringValue(po.getRouteId()))")
    @Mapping(target = "realStoragePath", expression = "java(stringValue(po.getRealStoragePath()))")
    @Mapping(target = "status", expression = "java(statusOf(po.getStatus()))")
    @Mapping(target = "receivedAt", expression = "java(toInstant(po.getReceivedAt()))")
    @Mapping(target = "storedAt", expression = "java(toInstant(po.getStoredAt()))")
    @Mapping(target = "probeResult", expression = "java(parseProbeResult(po, transferJsonMapper))")
    @Mapping(target = "fileMeta", expression = "java(mergeFileMeta(po, transferJsonMapper))")
    TransferObject toDomain(TransferObjectPO po, @Context TransferJsonMapper transferJsonMapper);

    default ProbeResult parseProbeResult(TransferObjectPO po, TransferJsonMapper transferJsonMapper) {
        if (po == null) {
            return null;
        }
        Map<String, Object> stored = transferJsonMapper.toMap(po.getProbeResultJson());
        if (stored == null || stored.isEmpty()) {
            stored = transferJsonMapper.toMap(po.getFileMetaJson());
        }
        if (stored == null || stored.isEmpty()) {
            return null;
        }
        Object detectedRaw = stored.get("detected");
        Object detectedTypeRaw = stored.get("detectedType");
        Object attributesRaw = stored.get("attributes");
        if (detectedRaw == null && detectedTypeRaw == null && attributesRaw == null) {
            detectedRaw = stored.get(TransferConfigKeys.PROBE_DETECTED);
            detectedTypeRaw = stored.get(TransferConfigKeys.PROBE_DETECTED_TYPE);
            attributesRaw = stored.get(TransferConfigKeys.PROBE_ATTRIBUTES);
        }
        return new ProbeResult(
                detectedRaw == null || Boolean.parseBoolean(String.valueOf(detectedRaw)),
                detectedTypeRaw == null ? null : String.valueOf(detectedTypeRaw),
                attributesRaw instanceof Map<?, ?> map ? safeMap(castMap(map)) : Map.of()
        );
    }

    default Map<String, Object> mergeFileMeta(TransferObjectPO po, TransferJsonMapper transferJsonMapper) {
        Map<String, Object> meta = new java.util.LinkedHashMap<>();
        Map<String, Object> storedMeta = transferJsonMapper.toMap(po.getFileMetaJson());
        if (storedMeta != null) {
            meta.putAll(storedMeta);
        }
        meta.putIfAbsent(TransferConfigKeys.SOURCE_TYPE, po.getSourceType());
        meta.putIfAbsent(TransferConfigKeys.SOURCE_CODE, po.getSourceCode());
        meta.putIfAbsent("realStoragePath", po.getRealStoragePath());
        return meta;
    }

    @SuppressWarnings("unchecked")
    default Map<String, Object> castMap(Map<?, ?> source) {
        Map<String, Object> target = new java.util.LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            target.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return target;
    }
}
