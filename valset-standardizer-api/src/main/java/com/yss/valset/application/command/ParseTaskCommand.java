package com.yss.valset.application.command;

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
     * 可选的原始文件名。
     */
    private String fileNameOriginal;

    /**
     * 可选的创建者标识符。
     */
    private String createdBy;

    /**
     * 工作流编码。
     */
    private String workflowCode;

    /**
     * 工作流主键。
     */
    private String workflowId;

    /**
     * 工作流版本号。
     */
    private Integer workflowVersionNo;

    /**
     * 当前工作流阶段编码。
     */
    private String workflowStageCode;

    /**
     * 当前工作流阶段名称。
     */
    private String workflowStageName;

    /**
     * 执行平台类型。
     */
    private String workflowEngineType;

    /**
     * 外部执行标识。
     */
    private String workflowEngineExternalRef;

    /**
     * 执行平台扩展参数。
     */
    private String workflowEngineConfigJson;
}
