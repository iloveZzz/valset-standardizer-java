package com.yss.valset.transfer.web.controller;

import com.yss.cloud.dto.result.SingleResult;
import com.yss.valset.transfer.application.dto.TransferDeliveryRecordSummaryViewDTO;
import com.yss.valset.transfer.application.service.TransferDeliveryRecordQueryService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文件投递结果查询接口。
 */
@RestController
@RequestMapping("/transfer-delivery-records")
public class TransferDeliveryRecordController {

    private final TransferDeliveryRecordQueryService transferDeliveryRecordQueryService;

    public TransferDeliveryRecordController(TransferDeliveryRecordQueryService transferDeliveryRecordQueryService) {
        this.transferDeliveryRecordQueryService = transferDeliveryRecordQueryService;
    }

    /**
     * 统计当天文件投递结果。
     *
     * @return 当天投递统计
     */
    @GetMapping("/summary")
    @Operation(summary = "统计当天文件投递结果", description = "按 Asia/Shanghai 时区统计当天的文件投递总数、成功数、失败数和成功率。")
    public SingleResult<TransferDeliveryRecordSummaryViewDTO> summarizeToday() {
        return SingleResult.of(transferDeliveryRecordQueryService.summarizeToday());
    }
}
