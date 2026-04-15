package com.yss.subjectmatch.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文件主数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectMatchFileInfo {
    private Long fileId;
    private String fileNameOriginal;
    private String fileNameNormalized;
    private String fileExtension;
    private String mimeType;
    private Long fileSizeBytes;
    private String fileFingerprint;
    private SubjectMatchFileSourceChannel sourceChannel;
    private String sourceUri;
    private SubjectMatchFileStorageType storageType;
    private String storageUri;
    private String fileFormat;
    private SubjectMatchFileStatus fileStatus;
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
