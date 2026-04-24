package com.yss.valset.transfer.web.controller;

import com.yss.valset.transfer.application.dto.TransferRouteViewDTO;
import com.yss.valset.transfer.application.service.TransferRouteQueryService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分拣路由查询接口。
 */
@RestController
@RequestMapping("/api/transfer-routes")
public class TransferRouteController {

    private final TransferRouteQueryService transferRouteQueryService;

    public TransferRouteController(TransferRouteQueryService transferRouteQueryService) {
        this.transferRouteQueryService = transferRouteQueryService;
    }

    /**
     * 查询分拣路由配置列表。
     *
     * @param sourceId 来源主键
     * @param sourceType 来源类型
     * @param sourceCode 来源编码
     * @param ruleId 规则主键
     * @param targetType 目标类型
     * @param targetCode 目标编码
     * @param limit 查询上限
     * @return 分拣路由列表
     */
    @GetMapping
    @Operation(summary = "查询分拣路由配置列表", description = "返回路由主键、规则主键、目标信息、路由状态和路由元数据。")
    public List<TransferRouteViewDTO> listRoutes(@RequestParam(value = "sourceId", required = false) String sourceId,
                                                 @RequestParam(value = "sourceType", required = false) String sourceType,
                                                 @RequestParam(value = "sourceCode", required = false) String sourceCode,
                                                 @RequestParam(value = "ruleId", required = false) String ruleId,
                                                 @RequestParam(value = "targetType", required = false) String targetType,
                                                 @RequestParam(value = "targetCode", required = false) String targetCode,
                                                 @RequestParam(value = "limit", required = false) Integer limit) {
        return transferRouteQueryService.listRoutes(sourceId, sourceType, sourceCode, ruleId, targetType, targetCode, limit);
    }

    /**
     * 根据路由主键查询分拣路由详情。
     *
     * @param routeId 路由主键
     * @return 分拣路由详情
     */
    @GetMapping("/{routeId}")
    @Operation(summary = "根据路由主键查询分拣路由详情", description = "返回单条路由的目标信息、规则信息和路由元数据。")
    public TransferRouteViewDTO getRoute(@PathVariable String routeId) {
        return transferRouteQueryService.getRoute(routeId);
    }
}
