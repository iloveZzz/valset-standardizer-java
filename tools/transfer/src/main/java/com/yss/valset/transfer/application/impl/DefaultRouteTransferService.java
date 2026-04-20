package com.yss.valset.transfer.application.impl;

import com.yss.valset.transfer.application.port.RouteTransferUseCase;
import com.yss.valset.transfer.domain.gateway.TransferObjectGateway;
import com.yss.valset.transfer.domain.gateway.TransferRouteGateway;
import com.yss.valset.transfer.domain.model.MatchResult;
import com.yss.valset.transfer.domain.model.ProbeResult;
import com.yss.valset.transfer.domain.model.RecognitionContext;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferRoute;
import com.yss.valset.transfer.domain.model.TransferStatus;
import com.yss.valset.transfer.infrastructure.plugin.FileProbePluginRegistry;
import com.yss.valset.transfer.infrastructure.plugin.RouteMatchPluginRegistry;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 默认文件路由应用服务。
 */
@Service
public class DefaultRouteTransferService implements RouteTransferUseCase {

    private final TransferObjectGateway transferObjectGateway;
    private final TransferRouteGateway transferRouteGateway;
    private final FileProbePluginRegistry fileProbePluginRegistry;
    private final RouteMatchPluginRegistry routeMatchPluginRegistry;

    public DefaultRouteTransferService(
            TransferObjectGateway transferObjectGateway,
            TransferRouteGateway transferRouteGateway,
            FileProbePluginRegistry fileProbePluginRegistry,
            RouteMatchPluginRegistry routeMatchPluginRegistry
    ) {
        this.transferObjectGateway = transferObjectGateway;
        this.transferRouteGateway = transferRouteGateway;
        this.fileProbePluginRegistry = fileProbePluginRegistry;
        this.routeMatchPluginRegistry = routeMatchPluginRegistry;
    }

    @Override
    public void execute(Long transferId) {
        TransferObject transferObject = transferObjectGateway.findById(transferId)
                .orElseThrow(() -> new IllegalStateException("未找到文件记录，transferId=" + transferId));
        RecognitionContext context = toRecognitionContext(transferObject);
        ProbeResult probeResult = fileProbePluginRegistry.getRequired(context).probe(context);
        MatchResult matchResult = routeMatchPluginRegistry.getRequired(context).match(context, probeResult);
        if (matchResult.routes() == null || matchResult.routes().isEmpty()) {
            throw new IllegalStateException("未匹配到可用的分拣规则，transferId=" + transferId);
        }
        for (TransferRoute route : matchResult.routes()) {
            TransferRoute routed = new TransferRoute(
                    null,
                    transferObject.transferId(),
                    route.ruleId(),
                    route.targetType(),
                    route.targetCode(),
                    route.targetPath(),
                    route.renamePattern(),
                    TransferStatus.ROUTED,
                    mergeRouteMeta(route, matchResult.reason(), probeResult)
            );
            transferRouteGateway.save(routed);
        }
    }

    private RecognitionContext toRecognitionContext(TransferObject transferObject) {
        Map<String, Object> fileMeta = transferObject.fileMeta() == null ? Map.of() : transferObject.fileMeta();
        return new RecognitionContext(
                resolveSourceType(fileMeta),
                String.valueOf(fileMeta.getOrDefault("sourceCode", transferObject.sourceId())),
                transferObject.originalName(),
                transferObject.mimeType(),
                transferObject.sizeBytes(),
                transferObject.mailFrom(),
                transferObject.mailTo(),
                transferObject.mailCc(),
                transferObject.mailBcc(),
                transferObject.mailSubject(),
                transferObject.mailBody(),
                transferObject.mailId(),
                transferObject.mailProtocol(),
                transferObject.mailFolder(),
                transferObject.localTempPath(),
                fileMeta
        );
    }

    private Map<String, Object> mergeRouteMeta(TransferRoute route, String message, ProbeResult probeResult) {
        Map<String, Object> routeMeta = route.routeMeta() == null ? new java.util.LinkedHashMap<>() : new java.util.LinkedHashMap<>(route.routeMeta());
        routeMeta.putIfAbsent("ruleMessage", message);
        routeMeta.putIfAbsent("probeDetectedType", probeResult == null ? null : probeResult.detectedType());
        routeMeta.putIfAbsent("probeAttributes", probeResult == null ? null : probeResult.attributes());
        return routeMeta;
    }

    private com.yss.valset.transfer.domain.model.SourceType resolveSourceType(Map<String, Object> fileMeta) {
        Object raw = fileMeta.get("sourceType");
        if (raw == null) {
            return null;
        }
        try {
            return com.yss.valset.transfer.domain.model.SourceType.valueOf(String.valueOf(raw));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
