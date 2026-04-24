package com.yss.valset.transfer.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件投递结果持久化实体。
 */
@Data
@TableName("t_transfer_delivery_record")
public class TransferDeliveryRecordPO {

    /**
     * 投递记录主键。
     */
    @TableId(value = "delivery_id", type = IdType.ASSIGN_ID)
    private String deliveryId;

    /**
     * 路由主键。
     */
    @TableField("route_id")
    private String routeId;

    /**
     * 文件主键。
     */
    @TableField("transfer_id")
    private String transferId;

    /**
     * 目标类型。
     */
    @TableField("target_type")
    private String targetType;

    /**
     * 目标编码。
     */
    @TableField("target_code")
    private String targetCode;

    /**
     * 执行状态。
     */
    @TableField("execute_status")
    private String executeStatus;

    /**
     * 重试次数。
     */
    @TableField("retry_count")
    private Integer retryCount;

    /**
     * 请求快照 JSON。
     */
    @TableField("request_snapshot_json")
    private String requestSnapshotJson;

    /**
     * 响应快照 JSON。
     */
    @TableField("response_snapshot_json")
    private String responseSnapshotJson;

    /**
     * 错误信息。
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 投递时间。
     */
    @TableField("delivered_at")
    private LocalDateTime deliveredAt;
}
