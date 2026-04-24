package com.yss.valset.application.service;

import com.yss.valset.application.command.TransferRouteUpsertCommand;
import com.yss.valset.application.dto.TransferRouteMutationResponse;
import com.yss.valset.application.dto.TransferRouteViewDTO;

import java.util.List;

/**
 * 分拣路由管理服务。
 */
public interface TransferRouteManagementAppService {

    List<TransferRouteViewDTO> listRoutes(String sourceId,
                                         String sourceType,
                                         String sourceCode,
                                         String ruleId,
                                         String targetType,
                                         String targetCode,
                                         String routeStatus,
                                         Integer limit);

    TransferRouteViewDTO getRoute(String routeId);

    TransferRouteMutationResponse upsertRoute(TransferRouteUpsertCommand command);

    TransferRouteMutationResponse deleteRoute(String routeId);
}
