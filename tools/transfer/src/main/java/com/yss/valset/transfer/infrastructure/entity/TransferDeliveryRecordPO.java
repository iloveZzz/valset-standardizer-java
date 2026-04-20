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

    @TableId(value = "delivery_id", type = IdType.ASSIGN_ID)
    private Long deliveryId;

    @TableField("route_id")
    private Long routeId;

    @TableField("transfer_id")
    private Long transferId;

    @TableField("target_type")
    private String targetType;

    @TableField("target_code")
    private String targetCode;

    @TableField("execute_status")
    private String executeStatus;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField("request_snapshot_json")
    private String requestSnapshotJson;

    @TableField("response_snapshot_json")
    private String responseSnapshotJson;

    @TableField("error_message")
    private String errorMessage;

    @TableField("delivered_at")
    private LocalDateTime deliveredAt;
}
