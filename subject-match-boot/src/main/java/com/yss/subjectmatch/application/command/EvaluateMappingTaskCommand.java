package com.yss.subjectmatch.application.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 请求离线评估任务的有效负载。
 */
@Data
public class EvaluateMappingTaskCommand {
    /**
     * 历史映射示例的绝对工作簿路径。
     */
    @NotBlank
    private String mappingWorkbookPath;
    /**
     * 标准科目数据源类型 (EXCEL, CSV, API, DB)，默认 EXCEL 保持向后兼容。
     */
    private String standardSourceType = "EXCEL";
    /**
     * 标准主题工作簿的绝对工作簿路径或URI。
     */
    @NotBlank
    private String standardWorkbookPath;
    /**
     * 评估期间使用的样​​品分割模式。
     */
    private String splitMode = "org_holdout";
    /**
     * 在 K 顶部评估的候选人数量。
     */
    private Integer topK = 5;
    /**
     * 调整样本的最大数量。
     */
    private Integer maxTuningSamples = 1500;
    /**
     * 测试集大小的可选上限。
     */
    private Integer maxTestSamples;
    /**
     * 可选的创建者标识符。
     */
    private String createdBy;
}
