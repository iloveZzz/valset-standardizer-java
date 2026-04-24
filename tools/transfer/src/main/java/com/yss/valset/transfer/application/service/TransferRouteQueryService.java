package com.yss.valset.transfer.application.service;

import com.yss.valset.transfer.application.dto.TransferRouteViewDTO;

import java.util.List;

/**
 * 文件路由查询服务。
 */
public interface TransferRouteQueryService {

    /**
     * 根据来源和规则查询路由配置列表。
     *
     * @param sourceId 来源主键
     * @param sourceType 来源类型
     * @param sourceCode 来源编码
     * @param ruleId 规则主键
     * @param targetType 目标类型
     * @param targetCode 目标编码
     * @param limit 查询上限
     * @return 路由配置列表
     */
    List<TransferRouteViewDTO> listRoutes(String sourceId,
                                          String sourceType,
                                          String sourceCode,
                                          String ruleId,
                                          String targetType,
                                          String targetCode,
                                          Integer limit);

    /**
     * 根据路由主键查询路由详情。
     *
     * @param routeId 路由主键
     * @return 路由详情
     */
    TransferRouteViewDTO getRoute(String routeId);
}
