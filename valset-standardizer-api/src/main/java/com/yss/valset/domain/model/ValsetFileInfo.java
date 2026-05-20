package com.yss.valset.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 文件主数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValsetFileInfo {
    /** 文件主键ID */
    private Long fileId;
    /** 原始文件名 */
    private String fileNameOriginal;
    /** 规范化文件名 */
    private String fileNameNormalized;
    /** 文件扩展名 */
    private String fileExtension;
    /** MIME类型 */
    private String mimeType;
    /** 文件大小（字节） */
    private Long fileSizeBytes;
    /** 文件指纹 */
    private String fileFingerprint;
    /** 来源渠道 */
    private ValsetFileSourceChannel sourceChannel;
    /** 来源URI */
    private String sourceUri;
    /** 存储类型 */
    private ValsetFileStorageType storageType;
    /** 存储URI */
    private String storageUri;
    /** 本地临时路径 */
    private String localTempPath;
    /** 实际存储路径 */
    private String realStoragePath;
    /** 文件格式 */
    private String fileFormat;
    /** 文件状态 */
    private ValsetFileStatus fileStatus;
    /** 创建人 */
    private String createdBy;
    /** 接收时间 */
    private LocalDateTime receivedAt;
    /** 存储时间 */
    private LocalDateTime storedAt;
    /** 业务日期 */
    private LocalDate businessDate;
    /** 最后处理时间 */
    private LocalDateTime lastProcessedAt;
    /** 最后任务ID */
    private Long lastTaskId;
    /** 错误消息 */
    private String errorMessage;
    /** 来源元数据JSON */
    private String sourceMetaJson;
    /** 存储元数据JSON */
    private String storageMetaJson;
    /** 备注 */
    private String remark;
}
