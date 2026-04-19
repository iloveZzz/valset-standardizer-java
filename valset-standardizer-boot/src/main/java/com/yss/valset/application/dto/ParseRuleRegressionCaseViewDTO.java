package com.yss.valset.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 解析模板样例回归结果视图。
 */
@Data
@Builder
public class ParseRuleRegressionCaseViewDTO {
    private Long caseId;
    private Long sampleFileId;
    private String sampleFileName;
    private String expectedOutputHash;
    private String actualOutputHash;
    private Boolean passed;
    private String reason;
    private Integer expectedHeaderRow;
    private Integer actualHeaderRow;
    private Integer expectedDataStartRow;
    private Integer actualDataStartRow;
    private Integer expectedSubjectCount;
    private Integer actualSubjectCount;
    private Integer expectedMetricCount;
    private Integer actualMetricCount;
}
