package com.yss.valset.controller;

import com.yss.cloud.dto.response.MultiResult;
import com.yss.cloud.dto.response.PageResult;
import com.yss.cloud.dto.response.SingleResult;
import com.yss.valset.application.dto.TransferDeliveryRecordViewDTO;
import com.yss.valset.application.service.TransferDeliveryRecordQueryService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文件投递结果查询接口。
 */
@RestController
@RequestMapping("/api/transfer-delivery-records")
public class TransferDeliveryRecordController {

    private final TransferDeliveryRecordQueryService transferDeliveryRecordQueryService;

    public TransferDeliveryRecordController(TransferDeliveryRecordQueryService transferDeliveryRecordQueryService) {
        this.transferDeliveryRecordQueryService = transferDeliveryRecordQueryService;
    }

    /**
     * 查询文件投递结果列表。
     *
     * @param routeId 路由主键
     * @param transferId 文件主键
     * @param targetCode 目标编码
     * @param executeStatus 执行状态
     * @param limit 查询上限
     * @return 文件投递结果列表
     */
    @GetMapping
    @Operation(summary = "查询文件投递结果列表", description = "支持按路由、文件、目标编码和执行状态查询投递结果。")
    public MultiResult<TransferDeliveryRecordViewDTO> listRecords(@RequestParam(value = "routeId", required = false) String routeId,
                                                                  @RequestParam(value = "transferId", required = false) String transferId,
                                                                  @RequestParam(value = "targetCode", required = false) String targetCode,
                                                                  @RequestParam(value = "executeStatus", required = false) String executeStatus,
                                                                  @RequestParam(value = "limit", required = false) Integer limit) {
        return MultiResult.of(transferDeliveryRecordQueryService.listRecords(routeId, transferId, targetCode, executeStatus, limit));
    }

    /**
     * 分页查询文件投递结果列表。
     *
     * @param routeId 路由主键
     * @param transferId 文件主键
     * @param targetCode 目标编码
     * @param executeStatus 执行状态
     * @param pageIndex 页码
     * @param pageSize 每页条数
     * @return 文件投递结果分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询文件投递结果列表", description = "支持按路由、文件、目标编码和执行状态分页查询投递结果。")
    public PageResult<TransferDeliveryRecordViewDTO> pageRecords(@RequestParam(value = "routeId", required = false) String routeId,
                                                                 @RequestParam(value = "transferId", required = false) String transferId,
                                                                 @RequestParam(value = "targetCode", required = false) String targetCode,
                                                                 @RequestParam(value = "executeStatus", required = false) String executeStatus,
                                                                 @RequestParam(value = "pageIndex", required = false) Integer pageIndex,
                                                                 @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return transferDeliveryRecordQueryService.pageRecords(routeId, transferId, targetCode, executeStatus, pageIndex, pageSize);
    }

    /**
     * 查询文件投递结果详情。
     *
     * @param deliveryId 投递记录主键
     * @return 文件投递结果详情
     */
    @GetMapping("/{deliveryId}")
    @Operation(summary = "查询文件投递结果详情", description = "根据投递记录主键查询单条投递结果。")
    public SingleResult<TransferDeliveryRecordViewDTO> getRecord(@PathVariable String deliveryId) {
        return SingleResult.of(transferDeliveryRecordQueryService.getRecord(deliveryId));
    }
}
