package com.yss.valset.transfer.application.impl.management;

import com.yss.valset.transfer.application.command.TransferRouteUpsertCommand;
import com.yss.valset.transfer.application.dto.TransferRouteMutationResponse;
import com.yss.valset.transfer.application.dto.TransferRouteViewDTO;
import com.yss.valset.transfer.application.service.TransferRouteManagementAppService;
import com.yss.valset.transfer.domain.form.TransferFormTemplateNames;
import com.yss.valset.transfer.domain.gateway.TransferRouteGateway;
import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.domain.model.TargetType;
import com.yss.valset.transfer.domain.model.TransferRoute;
import com.yss.valset.transfer.domain.model.TransferStatus;
import com.yss.valset.transfer.infrastructure.convertor.TransferSecretCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 默认分拣路由管理服务。
 */
@Service
@RequiredArgsConstructor
public class DefaultTransferRouteManagementAppService implements TransferRouteManagementAppService {

    private final TransferRouteGateway transferRouteGateway;
    private final TransferSecretCodec transferSecretCodec;

    @Override
    public List<TransferRouteViewDTO> listRoutes(String sourceId,
                                                 String sourceType,
                                                 String sourceCode,
                                                 String ruleId,
                                                 String targetType,
                                                 String targetCode,
                                                 String routeStatus,
                                                 Integer limit) {
        return transferRouteGateway.listRoutes(sourceId, sourceType, sourceCode, ruleId, targetType, targetCode, routeStatus, limit)
                .stream()
                .map(this::toView)
                .toList();
    }

    @Override
    public TransferRouteViewDTO getRoute(String routeId) {
        TransferRoute route = transferRouteGateway.findById(routeId)
                .orElseThrow(() -> new IllegalStateException("未找到分拣路由，routeId=" + routeId));
        return toView(route);
    }

    @Override
    public TransferRouteMutationResponse upsertRoute(TransferRouteUpsertCommand command) {
        boolean createMode = command.getRouteId() == null;
        TransferRoute existing = command.getRouteId() == null ? null : transferRouteGateway.findById(command.getRouteId())
                .orElseThrow(() -> new IllegalStateException("未找到分拣路由，routeId=" + command.getRouteId()));
        Map<String, Object> routeMeta = mergeConfig(
                existing == null ? null : existing.routeMeta(),
                command.getRouteMeta()
        );
        TransferRoute transferRoute = new TransferRoute(
                command.getRouteId(),
                command.getSourceId() == null ? (existing == null ? null : existing.sourceId()) : command.getSourceId(),
                parseSourceType(command.getSourceType(), existing),
                command.getSourceCode(),
                command.getRuleId(),
                parseTargetType(command.getTargetType()),
                command.getTargetCode(),
                command.getTargetPath(),
                command.getRenamePattern(),
                parseStatus(command.getRouteStatus(), existing),
                routeMeta
        );
        TransferRoute saved = transferRouteGateway.save(transferRoute);
        return TransferRouteMutationResponse.builder()
                .operation(createMode ? "create" : "update")
                .message("分拣路由保存成功")
                .formTemplateName(TransferFormTemplateNames.TRANSFER_ROUTE)
                .route(toView(saved))
                .build();
    }

    @Override
    public TransferRouteMutationResponse deleteRoute(String routeId) {
        TransferRoute existing = transferRouteGateway.findById(routeId)
                .orElseThrow(() -> new IllegalStateException("未找到分拣路由，routeId=" + routeId));
        transferRouteGateway.deleteById(routeId);
        return TransferRouteMutationResponse.builder()
                .operation("delete")
                .message("分拣路由删除成功")
                .formTemplateName(TransferFormTemplateNames.TRANSFER_ROUTE)
                .route(toView(existing))
                .build();
    }

    private TransferRouteViewDTO toView(TransferRoute route) {
        return TransferRouteViewDTO.builder()
                .routeId(route.routeId() == null ? null : String.valueOf(route.routeId()))
                .sourceId(route.sourceId() == null ? null : String.valueOf(route.sourceId()))
                .sourceType(route.sourceType() == null ? null : route.sourceType().name())
                .sourceCode(route.sourceCode())
                .ruleId(route.ruleId() == null ? null : String.valueOf(route.ruleId()))
                .targetType(route.targetType() == null ? null : route.targetType().name())
                .targetCode(route.targetCode())
                .targetPath(route.targetPath())
                .renamePattern(route.renamePattern())
                .routeStatus(route.routeStatus() == null ? null : route.routeStatus().name())
                .routeMeta(transferSecretCodec.maskMap(route.routeMeta()))
                .build();
    }

    private Map<String, Object> mergeConfig(Map<String, Object> existing, Map<String, Object> incoming) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (existing != null && !existing.isEmpty()) {
            merged.putAll(existing);
        }
        if (incoming != null && !incoming.isEmpty()) {
            merged.putAll(incoming);
        }
        return merged;
    }

    private SourceType parseSourceType(String value, TransferRoute existing) {
        if (value == null || value.isBlank()) {
            return existing == null ? null : existing.sourceType();
        }
        return SourceType.valueOf(value);
    }

    private TargetType parseTargetType(String value) {
        return value == null || value.isBlank() ? null : TargetType.valueOf(value);
    }

    private TransferStatus parseStatus(String value, TransferRoute existing) {
        if (value == null || value.isBlank()) {
            return existing == null ? TransferStatus.PENDING : existing.routeStatus();
        }
        return TransferStatus.valueOf(value);
    }
}
