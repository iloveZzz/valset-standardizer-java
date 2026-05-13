package com.yss.valset.extract.application.command;

import javax.validation.constraints.NotBlank;
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
