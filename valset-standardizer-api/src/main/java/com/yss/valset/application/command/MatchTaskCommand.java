package com.yss.valset.application.command;

import javax.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 请求主题匹配任务的有效负载。
 */
@Data
public class MatchTaskCommand {
    /**
     * 估值表数据源类型 (EXCEL, CSV, API, DB)，默认 EXCEL 保持向后兼容。
     */
    private String dataSourceType = "EXCEL";
    /**
     * 评估估值表的数据源URI或绝对路径。
     */
    @NotBlank
    private String workbookPath;

    /**
     * 是否强制重新生成匹配任务。
     */
    private Boolean forceRebuild = Boolean.FALSE;

    /**
     * 可选的原始数据文件标识。
     */
    private Long fileId;

    /**
     * 每个科目保留的考生数量。
     */
    private Integer topK = 5;
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
