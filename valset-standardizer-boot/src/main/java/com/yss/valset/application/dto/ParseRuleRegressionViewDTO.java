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
    /**
     * 模板主键。
     */
    private String profileId;
    /**
     * 模板编码。
     */
    private String profileCode;
    /**
     * 模板版本。
     */
    private String version;
    /**
     * 回归是否通过。
     */
    private Boolean passed;
    /**
     * 样例总数。
     */
    private Integer totalCases;
    /**
     * 通过样例数。
     */
    private Integer passedCases;
    /**
     * 回归问题列表。
     */
    private List<String> issues;
    /**
     * 每条样例的执行结果。
     */
    private List<ParseRuleRegressionCaseViewDTO> caseResults;
}
