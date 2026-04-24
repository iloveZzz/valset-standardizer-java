package com.yss.valset.controller;

import com.yss.cloud.dto.response.MultiResult;
import com.yss.cloud.dto.response.SingleResult;
import com.yss.valset.application.command.TransferRouteUpsertCommand;
import com.yss.valset.application.dto.TransferRouteMutationResponse;
import com.yss.valset.application.dto.TransferRouteViewDTO;
import com.yss.valset.application.service.TransferRouteManagementAppService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分拣路由配置接口。
 */
@RestController
@RequestMapping("/api/transfer-route-configs")
public class TransferRouteConfigController {

    private final TransferRouteManagementAppService transferRouteManagementAppService;

    public TransferRouteConfigController(TransferRouteManagementAppService transferRouteManagementAppService) {
        this.transferRouteManagementAppService = transferRouteManagementAppService;
    }

    /**
     * 查询路由配置列表。
     *
     * @param sourceId 来源主键
     * @param sourceType 来源类型
     * @param sourceCode 来源编码
     * @param ruleId 规则主键
     * @param targetType 目标类型
     * @param targetCode 目标编码
     * @param routeStatus 路由状态
     * @param limit 查询上限
     * @return 路由配置列表
     */
    @GetMapping
    @Operation(summary = "查询路由配置列表", description = "按来源、规则、目标和状态查询分拣路由配置。")
    public MultiResult<TransferRouteViewDTO> listRoutes(@RequestParam(value = "sourceId", required = false) String sourceId,
                                                        @RequestParam(value = "sourceType", required = false) String sourceType,
                                                        @RequestParam(value = "sourceCode", required = false) String sourceCode,
                                                        @RequestParam(value = "ruleId", required = false) String ruleId,
                                                        @RequestParam(value = "targetType", required = false) String targetType,
                                                        @RequestParam(value = "targetCode", required = false) String targetCode,
                                                        @RequestParam(value = "routeStatus", required = false) String routeStatus,
                                                        @RequestParam(value = "limit", required = false) Integer limit) {
        List<TransferRouteViewDTO> routes = transferRouteManagementAppService.listRoutes(sourceId, sourceType, sourceCode, ruleId, targetType, targetCode, routeStatus, limit);
        return MultiResult.of(routes);
    }

    /**
     * 查询路由配置详情。
     *
     * @param routeId 路由主键
     * @return 路由配置详情
     */
    @GetMapping("/{routeId}")
    @Operation(summary = "查询路由配置详情", description = "返回单条路由配置的来源、目标、规则和扩展信息。")
    public SingleResult<TransferRouteViewDTO> getRoute(@PathVariable String routeId) {
        return SingleResult.of(transferRouteManagementAppService.getRoute(routeId));
    }

    /**
     * 创建路由配置。
     *
     * @param command 路由新增命令
     * @return 路由变更结果
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建路由配置", description = "创建来源到目标的分拣路由配置。")
    public SingleResult<TransferRouteMutationResponse> createRoute(@Valid @RequestBody TransferRouteUpsertCommand command) {
        command.setRouteId(null);
        return SingleResult.of(transferRouteManagementAppService.upsertRoute(command));
    }

    /**
     * 更新路由配置。
     *
     * @param routeId 路由主键
     * @param command 路由更新命令
     * @return 路由变更结果
     */
    @PutMapping("/{routeId}")
    @Operation(summary = "更新路由配置", description = "更新来源到目标的分拣路由配置。")
    public SingleResult<TransferRouteMutationResponse> updateRoute(@PathVariable String routeId,
                                                                   @Valid @RequestBody TransferRouteUpsertCommand command) {
        command.setRouteId(routeId);
        return SingleResult.of(transferRouteManagementAppService.upsertRoute(command));
    }

    /**
     * 删除路由配置。
     *
     * @param routeId 路由主键
     * @return 路由变更结果
     */
    @DeleteMapping("/{routeId}")
    @Operation(summary = "删除路由配置", description = "按路由主键删除分拣路由配置。")
    public SingleResult<TransferRouteMutationResponse> deleteRoute(@PathVariable String routeId) {
        return SingleResult.of(transferRouteManagementAppService.deleteRoute(routeId));
    }
}
