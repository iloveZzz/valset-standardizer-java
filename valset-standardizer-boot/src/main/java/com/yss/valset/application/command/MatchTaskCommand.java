package com.yss.valset.application.command;

import jakarta.validation.constraints.NotBlank;
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
}
