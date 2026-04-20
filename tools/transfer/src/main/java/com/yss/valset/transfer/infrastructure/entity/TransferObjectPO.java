package com.yss.valset.transfer.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件主对象持久化实体。
 */
@Data
@TableName("t_transfer_object")
public class TransferObjectPO {

    @TableId(value = "transfer_id", type = IdType.ASSIGN_ID)
    private Long transferId;

    @TableField("source_id")
    private Long sourceId;

    @TableField("source_type")
    private String sourceType;

    @TableField("source_code")
    private String sourceCode;

    @TableField("original_name")
    private String originalName;

    @TableField("normalized_name")
    private String normalizedName;

    @TableField("extension")
    private String extension;

    @TableField("mime_type")
    private String mimeType;

    @TableField("size_bytes")
    private Long sizeBytes;

    @TableField("fingerprint")
    private String fingerprint;

    @TableField("source_ref")
    private String sourceRef;

    @TableField("local_temp_path")
    private String localTempPath;

    @TableField("status")
    private String status;

    @TableField("received_at")
    private LocalDateTime receivedAt;

    @TableField("stored_at")
    private LocalDateTime storedAt;

    @TableField("route_id")
    private Long routeId;

    @TableField("error_message")
    private String errorMessage;

    @TableField("file_meta_json")
    private String fileMetaJson;
}
