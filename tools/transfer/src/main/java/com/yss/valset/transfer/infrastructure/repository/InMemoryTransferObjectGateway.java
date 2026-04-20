package com.yss.valset.transfer.infrastructure.repository;

import com.yss.valset.transfer.domain.gateway.TransferObjectGateway;
import com.yss.valset.transfer.domain.model.TransferObject;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 文件主对象内存网关，便于先打通流程。
 */
@Profile("memory")
@Repository
public class InMemoryTransferObjectGateway implements TransferObjectGateway {

    private final AtomicLong idGenerator = new AtomicLong(1L);
    private final Map<String, TransferObject> storage = new LinkedHashMap<>();

    @Override
    public Optional<TransferObject> findById(Long transferId) {
        return storage.values().stream()
                .filter(item -> item.transferId() != null && item.transferId().equals(transferId))
                .findFirst();
    }

    @Override
    public Optional<TransferObject> findByFingerprint(String fingerprint) {
        return Optional.ofNullable(storage.get(fingerprint));
    }

    @Override
    public TransferObject save(TransferObject transferObject) {
        TransferObject saved = transferObject.transferId() == null
                ? new TransferObject(
                        idGenerator.getAndIncrement(),
                        transferObject.sourceId(),
                        transferObject.originalName(),
                        transferObject.normalizedName(),
                        transferObject.extension(),
                        transferObject.mimeType(),
                        transferObject.sizeBytes(),
                        transferObject.fingerprint(),
                        transferObject.sourceRef(),
                        transferObject.localTempPath(),
                        transferObject.status(),
                        transferObject.receivedAt(),
                        transferObject.storedAt(),
                        transferObject.routeId(),
                        transferObject.errorMessage(),
                        transferObject.fileMeta()
                )
                : transferObject;
        storage.put(saved.fingerprint(), saved);
        return saved;
    }
}
