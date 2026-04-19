package com.yss.valset.application.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件主数据视图。
 */
@Data
@Builder
public class ValsetFileInfoViewDTO {
    private Long fileId;
    private String fileNameOriginal;
    private String fileNameNormalized;
    private String fileExtension;
    private String mimeType;
    private Long fileSizeBytes;
    private String fileFingerprint;
    private String sourceChannel;
    private String sourceUri;
    private String storageType;
    private String storageUri;
    private String fileFormat;
    private String fileStatus;
    private String createdBy;
    private LocalDateTime receivedAt;
    private LocalDateTime storedAt;
    private LocalDateTime lastProcessedAt;
    private Long lastTaskId;
    private String errorMessage;
    private String sourceMetaJson;
    private String storageMetaJson;
    private String remark;
}
