package com.yss.valset.application.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 解析模板校验视图。
 */
@Data
@Builder
public class ParseRuleValidationViewDTO {
    private Long profileId;
    private String profileCode;
    private String version;
    private Boolean publishable;
    private Integer ruleCount;
    private Integer enabledRuleCount;
    private Integer caseCount;
    private List<String> issues;
}
