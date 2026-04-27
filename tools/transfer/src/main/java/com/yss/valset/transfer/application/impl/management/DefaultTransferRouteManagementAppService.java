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
    private final TransferSourceScheduleCoordinator transferSourceScheduleCoordinator;
    private final TransferSecretCodec transferSecretCodec;

    @Override
    public List<TransferRouteViewDTO> listRoutes(String sourceId,
                                                 String sourceType,
                                                 String sourceCode,
                                                 String ruleId,
                                                 String targetType,
                                                 String targetCode,
                                                 Boolean enabled,
                                                 String routeStatus,
                                                 Integer limit) {
        return transferRouteGateway.listRoutes(sourceId, sourceType, sourceCode, ruleId, targetType, targetCode, enabled, routeStatus, limit)
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
        String previousSourceId = existing == null ? null : existing.sourceId();
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
                normalizePollCron(command.getPollCron(), existing),
                command.getTargetPath(),
                command.getRenamePattern(),
                parseEnabled(command.getEnabled(), existing),
                parseStatus(command.getRouteStatus(), existing),
                routeMeta
        );
        TransferRoute saved = transferRouteGateway.save(transferRoute);
        syncRouteSchedule(previousSourceId, saved.sourceId());
        return TransferRouteMutationResponse.builder()
                .operation(createMode ? "create" : "update")
                .message("分拣路由保存成功")
                .formTemplateName(TransferFormTemplateNames.TRANSFER_ROUTE)
                .route(toView(saved))
                .build();
    }

    @Override
    public TransferRouteMutationResponse enableRoute(String routeId) {
        return setRouteEnabled(routeId, true);
    }

    @Override
    public TransferRouteMutationResponse disableRoute(String routeId) {
        return setRouteEnabled(routeId, false);
    }

    @Override
    public TransferRouteMutationResponse deleteRoute(String routeId) {
        TransferRoute existing = transferRouteGateway.findById(routeId)
                .orElseThrow(() -> new IllegalStateException("未找到分拣路由，routeId=" + routeId));
        transferRouteGateway.deleteById(routeId);
        transferSourceScheduleCoordinator.syncSourceScheduleBySourceId(existing.sourceId());
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
                .enabled(route.enabled())
                .pollCron(route.pollCron())
                .targetPath(route.targetPath())
                .renamePattern(route.renamePattern())
                .routeStatus(route.routeStatus() == null ? null : route.routeStatus().name())
                .routeMeta(transferSecretCodec.maskMap(route.routeMeta()))
                .build();
    }

    private TransferRouteMutationResponse setRouteEnabled(String routeId, boolean enabled) {
        TransferRoute existing = transferRouteGateway.findById(routeId)
                .orElseThrow(() -> new IllegalStateException("未找到分拣路由，routeId=" + routeId));
        if (existing.enabled() == enabled) {
            return TransferRouteMutationResponse.builder()
                    .operation(enabled ? "enable" : "disable")
                    .message(enabled ? "分拣路由已启用" : "分拣路由已停用")
                    .formTemplateName(TransferFormTemplateNames.TRANSFER_ROUTE)
                    .route(toView(existing))
                    .build();
        }
        TransferRoute saved = transferRouteGateway.save(new TransferRoute(
                existing.routeId(),
                existing.sourceId(),
                existing.sourceType(),
                existing.sourceCode(),
                existing.ruleId(),
                existing.targetType(),
                existing.targetCode(),
                existing.pollCron(),
                existing.targetPath(),
                existing.renamePattern(),
                enabled,
                existing.routeStatus(),
                existing.routeMeta()
        ));
        transferSourceScheduleCoordinator.syncSourceScheduleBySourceId(saved.sourceId());
        return TransferRouteMutationResponse.builder()
                .operation(enabled ? "enable" : "disable")
                .message(enabled ? "分拣路由启用成功" : "分拣路由停用成功")
                .formTemplateName(TransferFormTemplateNames.TRANSFER_ROUTE)
                .route(toView(saved))
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

    private boolean parseEnabled(Boolean value, TransferRoute existing) {
        if (value == null) {
            return existing == null || existing.enabled();
        }
        return Boolean.TRUE.equals(value);
    }

    private String normalizePollCron(String value, TransferRoute existing) {
        if (value == null || value.isBlank()) {
            return existing == null ? null : existing.pollCron();
        }
        return value.trim();
    }

    private void syncRouteSchedule(String previousSourceId, String currentSourceId) {
        if (previousSourceId != null && !previousSourceId.equals(currentSourceId)) {
            transferSourceScheduleCoordinator.syncSourceScheduleBySourceId(previousSourceId);
        }
        transferSourceScheduleCoordinator.syncSourceScheduleBySourceId(currentSourceId);
    }
}
