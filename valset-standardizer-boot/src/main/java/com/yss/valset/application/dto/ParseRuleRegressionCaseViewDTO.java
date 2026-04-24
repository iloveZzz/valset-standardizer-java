package com.yss.valset.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 解析模板样例回归结果视图。
 */
@Data
@Builder
public class ParseRuleRegressionCaseViewDTO {
    /**
     * 样例主键。
     */
    private String caseId;
    /**
     * 样例文件主键。
     */
    private String sampleFileId;
    /**
     * 样例文件名称。
     */
    private String sampleFileName;
    /**
     * 期望输出哈希。
     */
    private String expectedOutputHash;
    /**
     * 实际输出哈希。
     */
    private String actualOutputHash;
    /**
     * 是否通过。
     */
    private Boolean passed;
    /**
     * 不通过原因。
     */
    private String reason;
    /**
     * 期望的表头行号。
     */
    private Integer expectedHeaderRow;
    /**
     * 实际的表头行号。
     */
    private Integer actualHeaderRow;
    /**
     * 期望的数据起始行号。
     */
    private Integer expectedDataStartRow;
    /**
     * 实际的数据起始行号。
     */
    private Integer actualDataStartRow;
    /**
     * 期望的科目数量。
     */
    private Integer expectedSubjectCount;
    /**
     * 实际的科目数量。
     */
    private Integer actualSubjectCount;
    /**
     * 期望的指标数量。
     */
    private Integer expectedMetricCount;
    /**
     * 实际的指标数量。
     */
    private Integer actualMetricCount;
}
