package com.yss.subjectmatch.application.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 请求解析任务的负载。
 */
@Data
public class ParseTaskCommand {
    /**
     * 估值表数据源类型 (EXCEL, CSV, API, DB)，默认 EXCEL 保持向后兼容。
     */
    private String dataSourceType = "EXCEL";
    /**
     * 要解析的数据源URI或绝对路径。
     */
    @NotBlank
    private String workbookPath;

    /**
     * 是否强制重新生成解析任务。
     */
    private Boolean forceRebuild = Boolean.FALSE;

    /**
     * 可选的原始数据文件标识。
     */
    private Long fileId;

    /**
     * 可选的创建者标识符。
     */
    private String createdBy;
}
