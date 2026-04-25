package com.yss.valset.transfer.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件投递结果视图。
 */
@Data
@Builder
public class TransferDeliveryRecordViewDTO {

    /**
     * 投递记录主键。
     */
    private String deliveryId;
    /**
     * 路由主键。
     */
    private String routeId;
    /**
     * 文件主键。
     */
    private String transferId;
    /**
     * 目标类型。
     */
    private String targetType;
    /**
     * 目标编码。
     */
    private String targetCode;
    /**
     * 执行状态。
     */
    private String executeStatus;
    /**
     * 执行状态名称。
     */
    private String executeStatusLabel;
    /**
     * 请求快照 JSON。
     */
    private String requestSnapshotJson;
    /**
     * 响应快照 JSON。
     */
    private String responseSnapshotJson;
    /**
     * 错误信息。
     */
    private String errorMessage;
    /**
     * 投递时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime deliveredAt;
}
