package com.yss.valset.transfer.application.impl.query;

import com.yss.valset.transfer.application.dto.TransferRouteViewDTO;
import com.yss.valset.transfer.application.service.TransferRouteQueryService;
import com.yss.valset.transfer.domain.gateway.TransferRouteGateway;
import com.yss.valset.transfer.domain.model.TransferRoute;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * 默认文件路由查询服务。
 */
@Service
@RequiredArgsConstructor
public class DefaultTransferRouteQueryService implements TransferRouteQueryService {

    private final TransferRouteGateway transferRouteGateway;

    @Override
    public List<TransferRouteViewDTO> listRoutes(String sourceId,
                                                 String sourceType,
                                                 String sourceCode,
                                                 String ruleId,
                                                 String targetType,
                                                 String targetCode,
                                                 Boolean enabled,
                                                 Integer limit) {
        return transferRouteGateway.listRoutes(sourceId, sourceType, sourceCode, ruleId, targetType, targetCode, enabled, null, limit)
                .stream()
                .map(this::toView)
                .toList();
    }

    @Override
    public TransferRouteViewDTO getRoute(String routeId) {
        TransferRoute route = transferRouteGateway.findById(routeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到分拣路由，routeId=" + routeId));
        return toView(route);
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
                .routeMeta(route.routeMeta())
                .build();
    }
}
