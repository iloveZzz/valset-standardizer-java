package com.yss.valset.transfer.domain.gateway;

import com.yss.valset.transfer.domain.model.TransferResult;

/**
 * 投递结果网关。
 */
public interface TransferDeliveryGateway {

    void recordResult(Long routeId, TransferResult transferResult);

    void recordResult(Long routeId, TransferResult transferResult, Integer retryCount);

    long countByRouteId(Long routeId);
}
