package com.yss.subjectmatch.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 上传估值表并完成 ODS 提取后的返回结果。
 */
@Data
@Builder
public class UploadValuationFileResponse {
    private Long fileId;
    private String workbookPath;
    private String dataSourceType;
    private Long fileSizeBytes;
    private String fileFingerprint;
    private Boolean reusedExistingExtractTask;
    private TaskViewDTO extractTask;
}
