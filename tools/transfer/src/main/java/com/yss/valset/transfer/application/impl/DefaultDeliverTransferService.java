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
import com.yss.valset.transfer.application.port.TransferJobScheduler;
import com.yss.valset.transfer.infrastructure.plugin.TransferActionPluginRegistry;
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
    private final TransferActionPluginRegistry transferActionPluginRegistry;
    private final TransferJobScheduler transferJobScheduler;

    public DefaultDeliverTransferService(
            TransferRouteGateway transferRouteGateway,
            TransferObjectGateway transferObjectGateway,
            TransferTargetGateway transferTargetGateway,
            TransferDeliveryGateway transferDeliveryGateway,
            TransferActionPluginRegistry transferActionPluginRegistry,
            TransferJobScheduler transferJobScheduler
    ) {
        this.transferRouteGateway = transferRouteGateway;
        this.transferObjectGateway = transferObjectGateway;
        this.transferTargetGateway = transferTargetGateway;
        this.transferDeliveryGateway = transferDeliveryGateway;
        this.transferActionPluginRegistry = transferActionPluginRegistry;
        this.transferJobScheduler = transferJobScheduler;
    }

    @Override
    public void execute(Long routeId) {
        TransferRoute route = transferRouteGateway.findById(routeId)
                .orElseThrow(() -> new IllegalStateException("未找到路由记录，routeId=" + routeId));
        TransferObject transferObject = transferObjectGateway.findById(route.transferId())
                .orElseThrow(() -> new IllegalStateException("未找到文件记录，transferId=" + route.transferId()));
        TransferTarget target = transferTargetGateway.findByTargetCode(route.targetCode())
                .orElseThrow(() -> new IllegalStateException("未找到投递目标，targetCode=" + route.targetCode()));
        TransferContext context = new TransferContext(transferObject, route, target, buildAttributes(route, target));
        int attemptIndex = (int) transferDeliveryGateway.countByRouteId(routeId);
        TransferResult result = transferActionPluginRegistry.getRequired(route).execute(context);
        transferDeliveryGateway.recordResult(routeId, result, attemptIndex);
        if (!result.success()) {
            scheduleRetryIfNeeded(routeId, route, attemptIndex + 1);
            throw new IllegalStateException("文件投递失败，routeId=" + routeId + ", messages=" + result.messages());
        }
    }

    private void scheduleRetryIfNeeded(Long routeId, TransferRoute route, int nextAttempt) {
        int maxRetryCount = resolveInt(route, "maxRetryCount", 3);
        int retryDelaySeconds = resolveInt(route, "retryDelaySeconds", 60);
        if (nextAttempt < maxRetryCount) {
            transferJobScheduler.scheduleDeliverRetry(routeId, nextAttempt, retryDelaySeconds);
        }
    }

    private int resolveInt(TransferRoute route, String key, int defaultValue) {
        if (route == null || route.routeMeta() == null) {
            return defaultValue;
        }
        Object raw = route.routeMeta().get(key);
        if (raw == null || String.valueOf(raw).isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(raw));
        } catch (NumberFormatException ex) {
            return defaultValue;
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
