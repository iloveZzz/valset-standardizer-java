package com.yss.valset.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件主数据视图。
 */
@Data
@Builder
public class ValsetFileInfoViewDTO {
    /**
     * 文件主键。
     */
    private String fileId;
    /**
     * 原始文件名。
     */
    private String fileNameOriginal;
    /**
     * 归一化文件名。
     */
    private String fileNameNormalized;
    /**
     * 文件扩展名。
     */
    private String fileExtension;
    /**
     * MIME 类型。
     */
    private String mimeType;
    /**
     * 文件大小，单位字节。
     */
    private String fileSizeBytes;
    /**
     * 文件指纹。
     */
    private String fileFingerprint;
    /**
     * 来源渠道。
     */
    private String sourceChannel;
    /**
     * 来源 URI。
     */
    private String sourceUri;
    /**
     * 存储类型。
     */
    private String storageType;
    /**
     * 存储 URI。
     */
    private String storageUri;
    /**
     * 文件格式。
     */
    private String fileFormat;
    /**
     * 文件状态。
     */
    private String fileStatus;
    /**
     * 创建人。
     */
    private String createdBy;
    /**
     * 接收时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime receivedAt;
    /**
     * 入库时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime storedAt;
    /**
     * 最近处理时间。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastProcessedAt;
    /**
     * 最近关联任务主键。
     */
    private String lastTaskId;
    /**
     * 错误信息。
     */
    private String errorMessage;
    /**
     * 来源元数据 JSON。
     */
    private String sourceMetaJson;
    /**
     * 存储元数据 JSON。
     */
    private String storageMetaJson;
    /**
     * 备注。
     */
    private String remark;
}
