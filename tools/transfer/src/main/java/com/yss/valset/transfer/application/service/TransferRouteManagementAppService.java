package com.yss.valset.transfer.application.service;

import com.yss.valset.transfer.application.command.TransferRouteUpsertCommand;
import com.yss.valset.transfer.application.dto.TransferRouteMutationResponse;
import com.yss.valset.transfer.application.dto.TransferRouteViewDTO;

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
                                         Boolean enabled,
                                         String routeStatus,
                                         Integer limit);

    TransferRouteViewDTO getRoute(String routeId);

    TransferRouteMutationResponse upsertRoute(TransferRouteUpsertCommand command);

    TransferRouteMutationResponse enableRoute(String routeId);

    TransferRouteMutationResponse disableRoute(String routeId);

    TransferRouteMutationResponse deleteRoute(String routeId);
}
