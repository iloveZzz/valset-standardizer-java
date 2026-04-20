package com.yss.valset.transfer.infrastructure.repository;

import com.yss.valset.transfer.domain.gateway.TransferRouteGateway;
import com.yss.valset.transfer.domain.model.TransferRoute;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 文件路由内存网关。
 */
@Profile("memory")
@Repository
public class InMemoryTransferRouteGateway implements TransferRouteGateway {

    private final AtomicLong idGenerator = new AtomicLong(1L);
    private final Map<Long, TransferRoute> storage = new LinkedHashMap<>();

    @Override
    public Optional<TransferRoute> findById(Long routeId) {
        return Optional.ofNullable(storage.get(routeId));
    }

    @Override
    public TransferRoute save(TransferRoute transferRoute) {
        TransferRoute saved = transferRoute.routeId() == null
                ? new TransferRoute(
                        idGenerator.getAndIncrement(),
                        transferRoute.transferId(),
                        transferRoute.ruleId(),
                        transferRoute.targetType(),
                        transferRoute.targetCode(),
                        transferRoute.targetPath(),
                        transferRoute.renamePattern(),
                        transferRoute.routeStatus(),
                        transferRoute.routeMeta()
                )
                : transferRoute;
        storage.put(saved.routeId(), saved);
        return saved;
    }

    @Override
    public List<TransferRoute> listByTransferId(Long transferId) {
        return storage.values().stream()
                .filter(item -> item.transferId() != null && item.transferId().equals(transferId))
                .toList();
    }
}
