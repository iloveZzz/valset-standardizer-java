package com.yss.valset.transfer.infrastructure.gateway;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.valset.transfer.domain.gateway.TransferObjectGateway;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.infrastructure.convertor.TransferJsonMapper;
import com.yss.valset.transfer.infrastructure.entity.TransferObjectPO;
import com.yss.valset.transfer.infrastructure.mapper.TransferObjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * MyBatis 支持的文件主对象网关。
 */
@Primary
@Repository
@RequiredArgsConstructor
public class TransferObjectGatewayImpl implements TransferObjectGateway {

    private final TransferObjectRepository transferObjectRepository;
    private final TransferJsonMapper transferJsonMapper;

    @Override
    public Optional<TransferObject> findById(Long transferId) {
        TransferObjectPO po = transferObjectRepository.selectById(transferId);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<TransferObject> findByFingerprint(String fingerprint) {
        TransferObjectPO po = transferObjectRepository.selectOne(
                Wrappers.lambdaQuery(TransferObjectPO.class)
                        .eq(TransferObjectPO::getFingerprint, fingerprint)
                        .last("limit 1")
        );
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public TransferObject save(TransferObject transferObject) {
        TransferObjectPO po = toPO(transferObject);
        if (po.getTransferId() == null) {
            transferObjectRepository.insert(po);
        } else {
            transferObjectRepository.updateById(po);
        }
        return toDomain(po);
    }

    private TransferObjectPO toPO(TransferObject transferObject) {
        TransferObjectPO po = new TransferObjectPO();
        po.setTransferId(transferObject.transferId());
        po.setSourceId(transferObject.sourceId());
        po.setSourceType(stringOf(transferObject.fileMeta(), "sourceType"));
        po.setSourceCode(stringOf(transferObject.fileMeta(), "sourceCode"));
        po.setOriginalName(transferObject.originalName());
        po.setNormalizedName(transferObject.normalizedName());
        po.setExtension(transferObject.extension());
        po.setMimeType(transferObject.mimeType());
        po.setSizeBytes(transferObject.sizeBytes());
        po.setFingerprint(transferObject.fingerprint());
        po.setSourceRef(transferObject.sourceRef());
        po.setLocalTempPath(transferObject.localTempPath());
        po.setStatus(transferObject.status() == null ? null : transferObject.status().name());
        po.setReceivedAt(toLocalDateTime(transferObject.receivedAt()));
        po.setStoredAt(toLocalDateTime(transferObject.storedAt()));
        po.setRouteId(transferObject.routeId());
        po.setErrorMessage(transferObject.errorMessage());
        po.setFileMetaJson(transferJsonMapper.toJson(transferObject.fileMeta()));
        return po;
    }

    private TransferObject toDomain(TransferObjectPO po) {
        return new TransferObject(
                po.getTransferId(),
                po.getSourceId(),
                po.getOriginalName(),
                po.getNormalizedName(),
                po.getExtension(),
                po.getMimeType(),
                po.getSizeBytes(),
                po.getFingerprint(),
                po.getSourceRef(),
                po.getLocalTempPath(),
                po.getStatus() == null ? null : com.yss.valset.transfer.domain.model.TransferStatus.valueOf(po.getStatus()),
                toInstant(po.getReceivedAt()),
                toInstant(po.getStoredAt()),
                po.getRouteId(),
                po.getErrorMessage(),
                transferJsonMapper.toMap(po.getFileMetaJson())
        );
    }

    private LocalDateTime toLocalDateTime(java.time.Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    private java.time.Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }

    private String stringOf(java.util.Map<String, Object> value, String key) {
        if (value == null) {
            return null;
        }
        Object raw = value.get(key);
        return raw == null ? null : String.valueOf(raw);
    }
}
