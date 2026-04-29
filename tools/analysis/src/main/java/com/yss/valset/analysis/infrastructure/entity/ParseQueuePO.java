package com.yss.valset.analysis.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 待解析任务持久化实体。
 */
@Data
@TableName("t_parse_queue")
public class ParseQueuePO {

    @TableId(value = "queue_id", type = IdType.ASSIGN_ID)
    private String queueId;

    @TableField("business_key")
    private String businessKey;

    @TableField("transfer_id")
    private String transferId;

    @TableField("original_name")
    private String originalName;

    @TableField("source_id")
    private String sourceId;

    @TableField("source_type")
    private String sourceType;

    @TableField("source_code")
    private String sourceCode;

    @TableField("route_id")
    private String routeId;

    @TableField("delivery_id")
    private String deliveryId;

    @TableField("tag_id")
    private String tagId;

    @TableField("tag_code")
    private String tagCode;

    @TableField("tag_name")
    private String tagName;

    @TableField("file_status")
    private String fileStatus;

    @TableField("delivery_status")
    private String deliveryStatus;

    @TableField("parse_status")
    private String parseStatus;

    @TableField("trigger_mode")
    private String triggerMode;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField("claimed_by")
    private String subscribedBy;

    @TableField("claimed_at")
    private LocalDateTime subscribedAt;

    @TableField("parsed_at")
    private LocalDateTime parsedAt;

    @TableField("last_error_message")
    private String lastErrorMessage;

    @TableField("object_snapshot_json")
    private String objectSnapshotJson;

    @TableField("delivery_snapshot_json")
    private String deliverySnapshotJson;

    @TableField("parse_request_json")
    private String parseRequestJson;

    @TableField("parse_result_json")
    private String parseResultJson;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
