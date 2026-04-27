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

    /**
     * 文件主键。
     */
    @TableId(value = "transfer_id", type = IdType.ASSIGN_ID)
    private String transferId;

    /**
     * 来源主键。
     */
    @TableField("source_id")
    private String sourceId;

    /**
     * 来源类型。
     */
    @TableField("source_type")
    private String sourceType;

    /**
     * 来源编码。
     */
    @TableField("source_code")
    private String sourceCode;

    /**
     * 原始文件名。
     */
    @TableField("original_name")
    private String originalName;

    /**
     * 文件扩展名。
     */
    @TableField("extension")
    private String extension;

    /**
     * 文件类型。
     */
    @TableField("mime_type")
    private String mimeType;

    /**
     * 文件大小，单位字节。
     */
    @TableField("size_bytes")
    private Long sizeBytes;

    /**
     * 文件指纹。
     */
    @TableField("fingerprint")
    private String fingerprint;

    /**
     * 来源引用标识。
     */
    @TableField("source_ref")
    private String sourceRef;

    /**
     * 本地临时文件路径。
     */
    @TableField("local_temp_path")
    private String localTempPath;

    /**
     * 真实文件存储地址。
     */
    @TableField("real_storage_path")
    private String realStoragePath;

    /**
     * 文件状态。
     */
    @TableField("status")
    private String status;

    /**
     * 收取时间。
     */
    @TableField("received_at")
    private LocalDateTime receivedAt;

    /**
     * 落库时间。
     */
    @TableField("stored_at")
    private LocalDateTime storedAt;

    /**
     * 路由主键。
     */
    @TableField("route_id")
    private String routeId;

    /**
     * 错误信息。
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 探测结果 JSON。
     */
    @TableField("probe_result_json")
    private String probeResultJson;

    /**
     * 文件元数据 JSON。
     */
    @TableField("file_meta_json")
    private String fileMetaJson;
}
