package com.yss.valset.transfer.application.impl;

import com.yss.valset.transfer.application.port.DeliverTransferUseCase;
import com.yss.valset.transfer.application.port.TargetConnector;
import com.yss.valset.transfer.domain.gateway.TransferDeliveryGateway;
import com.yss.valset.transfer.domain.gateway.TransferObjectGateway;
import com.yss.valset.transfer.domain.gateway.TransferRouteGateway;
import com.yss.valset.transfer.domain.gateway.TransferTargetGateway;
import com.yss.valset.transfer.domain.model.TransferContext;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferResult;
import com.yss.valset.transfer.domain.model.TransferRoute;
import com.yss.valset.transfer.domain.model.TransferTarget;
import com.yss.valset.transfer.infrastructure.connector.TargetConnectorRegistry;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 默认文件投递应用服务。
 */
@Service
public class DefaultDeliverTransferService implements DeliverTransferUseCase {

    private final TransferRouteGateway transferRouteGateway;
    private final TransferObjectGateway transferObjectGateway;
    private final TransferTargetGateway transferTargetGateway;
    private final TransferDeliveryGateway transferDeliveryGateway;
    private final TargetConnectorRegistry targetConnectorRegistry;

    public DefaultDeliverTransferService(
            TransferRouteGateway transferRouteGateway,
            TransferObjectGateway transferObjectGateway,
            TransferTargetGateway transferTargetGateway,
            TransferDeliveryGateway transferDeliveryGateway,
            TargetConnectorRegistry targetConnectorRegistry
    ) {
        this.transferRouteGateway = transferRouteGateway;
        this.transferObjectGateway = transferObjectGateway;
        this.transferTargetGateway = transferTargetGateway;
        this.transferDeliveryGateway = transferDeliveryGateway;
        this.targetConnectorRegistry = targetConnectorRegistry;
    }

    @Override
    public void execute(Long routeId) {
        TransferRoute route = transferRouteGateway.findById(routeId)
                .orElseThrow(() -> new IllegalStateException("未找到路由记录，routeId=" + routeId));
        TransferObject transferObject = transferObjectGateway.findById(route.transferId())
                .orElseThrow(() -> new IllegalStateException("未找到文件记录，transferId=" + route.transferId()));
        TransferTarget target = transferTargetGateway.findByTargetCode(route.targetCode())
                .orElseThrow(() -> new IllegalStateException("未找到投递目标，targetCode=" + route.targetCode()));
        TargetConnector targetConnector = targetConnectorRegistry.getRequired(target);
        TransferContext context = new TransferContext(transferObject, route, target, buildAttributes(route, target));
        TransferResult result = targetConnector.send(context);
        transferDeliveryGateway.recordResult(routeId, result);
        if (!result.success()) {
            throw new IllegalStateException("文件投递失败，routeId=" + routeId + ", messages=" + result.messages());
        }
    }

    private Map<String, Object> buildAttributes(TransferRoute route, TransferTarget target) {
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();
        if (route.routeMeta() != null) {
            attributes.putAll(route.routeMeta());
        }
        attributes.put("targetId", target.targetId());
        attributes.put("targetCode", target.targetCode());
        attributes.put("targetName", target.targetName());
        attributes.put("targetType", target.targetType() == null ? null : target.targetType().name());
        attributes.put("targetPathTemplate", target.targetPathTemplate());
        if (target.connectionConfig() != null) {
            attributes.putAll(target.connectionConfig());
        }
        if (target.targetMeta() != null) {
            attributes.putAll(target.targetMeta());
        }
        return attributes;
    }
}
