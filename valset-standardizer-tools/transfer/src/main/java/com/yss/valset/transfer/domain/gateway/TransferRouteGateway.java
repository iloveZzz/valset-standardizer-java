package com.yss.valset.transfer.domain.gateway;

import com.yss.valset.transfer.domain.model.TransferRoute;

import java.util.List;
import java.util.Optional;

/**
 * 路由结果网关。
 */
public interface TransferRouteGateway {

    Optional<TransferRoute> findById(String routeId);

    TransferRoute save(TransferRoute transferRoute);

    long countBySourceId(String sourceId);

    long countEnabledBySourceId(String sourceId);

    long countByTargetCode(String targetCode);

    List<TransferRoute> listRoutes(String sourceId,
                                   String sourceType,
                                   String sourceCode,
                                   String ruleId,
                                   String targetType,
                                   String targetCode,
                                   Boolean enabled,
                                   String routeStatus,
                                   Integer limit);

    void deleteById(String routeId);
}
