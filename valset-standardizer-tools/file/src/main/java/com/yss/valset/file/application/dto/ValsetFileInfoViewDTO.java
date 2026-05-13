package com.yss.valset.file.application.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件主数据视图。
 */
@Data
@Builder
public class ValsetFileInfoViewDTO implements java.io.Serializable {
    private String fileId;
    private String fileNameOriginal;
    private String fileNameNormalized;
    private String fileExtension;
    private String mimeType;
    private String fileSizeBytes;
    private String fileFingerprint;
    private String sourceChannel;
    private String sourceUri;
    private String storageType;
    private String storageUri;
    private String localTempPath;
    private String realStoragePath;
    private String fileFormat;
    private String fileStatus;
    private String createdBy;
    private LocalDateTime receivedAt;
    private LocalDateTime storedAt;
    private LocalDateTime lastProcessedAt;
    private String lastTaskId;
    private String errorMessage;
    private String sourceMetaJson;
    private String storageMetaJson;
    private String remark;
}
