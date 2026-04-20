package com.yss.valset.transfer.infrastructure.repository;

import com.yss.valset.transfer.domain.gateway.TransferDeliveryGateway;
import com.yss.valset.transfer.domain.model.TransferResult;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 文件投递结果内存网关。
 */
@Profile("memory")
@Repository
public class InMemoryTransferDeliveryGateway implements TransferDeliveryGateway {

    private final Map<Long, TransferResult> storage = new LinkedHashMap<>();

    @Override
    public void recordResult(Long routeId, TransferResult transferResult) {
        storage.put(routeId, transferResult);
    }
}
