package com.yss.valset.application.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 请求原始数据提取任务的负载。
 */
@Data
public class ExtractDataTaskCommand {
    /**
     * 估值表数据源类型 (EXCEL, CSV)，默认 EXCEL。
     */
    private String dataSourceType = "EXCEL";

    /**
     * 要提取的工作簿或 CSV 文件路径。
     */
    @NotBlank
    private String workbookPath;

    /**
     * 文件内容指纹，用于识别是否为同一份原始文件。
     */
    private String fileFingerprint;

    /**
     * 文件主数据标识。
     */
    private Long fileId;

    /**
     * 是否强制重新生成提取任务。
     */
    private Boolean forceRebuild = Boolean.FALSE;

    /**
     * 可选的创建者标识符。
     */
    private String createdBy;
}
