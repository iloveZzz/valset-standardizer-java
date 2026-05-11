package com.yss.valset.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 上传估值表并完成 ODS 提取后的返回结果。
 */
@Data
@Builder
public class UploadValuationFileResponse {
    /**
     * 文件主键。
     */
    private String fileId;
    /**
     * 工作簿路径。
     */
    private String workbookPath;
    /**
     * 数据源类型。
     */
    private String dataSourceType;
    /**
     * 文件大小，单位字节。
     */
    private String fileSizeBytes;
    /**
     * 文件指纹。
     */
    private String fileFingerprint;
    /**
     * 文件服务任务标识。
     */
    private String filesysTaskId;
    /**
     * 文件服务文件标识。
     */
    private String filesysFileId;
    /**
     * 文件服务对象键。
     */
    private String filesysObjectKey;
    /**
     * 是否立即完成文件服务上传。
     */
    private Boolean filesysInstantUpload;
    /**
     * 是否复用已有提取任务。
     */
    private Boolean reusedExistingExtractTask;
    /**
     * 提取任务详情。
     */
    private TaskViewDTO extractTask;
}
