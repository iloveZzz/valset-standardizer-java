package com.yss.valset.transfer.domain.gateway;

import com.yss.valset.transfer.domain.model.TransferRoute;

import java.util.List;
import java.util.Optional;

/**
 * 路由结果网关。
 */
public interface TransferRouteGateway {

    Optional<TransferRoute> findById(Long routeId);

    TransferRoute save(TransferRoute transferRoute);

    List<TransferRoute> listByTransferId(Long transferId);
}
