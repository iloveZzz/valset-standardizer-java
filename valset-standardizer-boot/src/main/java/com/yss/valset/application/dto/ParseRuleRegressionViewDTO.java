package com.yss.valset.application.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 解析模板样例回归视图。
 */
@Data
@Builder
public class ParseRuleRegressionViewDTO {
    private Long profileId;
    private String profileCode;
    private String version;
    private Boolean passed;
    private Integer totalCases;
    private Integer passedCases;
    private List<String> issues;
    private List<ParseRuleRegressionCaseViewDTO> caseResults;
}
