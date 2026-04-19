package com.yss.valset.application.dto;

import com.yss.valset.domain.rule.ParseRuleType;
import lombok.Builder;
import lombok.Data;

/**
 * 解析规则步骤视图。
 */
@Data
@Builder
public class ParseRuleDefinitionViewDTO {
    private Long id;
    private Long profileId;
    private ParseRuleType ruleType;
    private String stepName;
    private Integer priority;
    private Boolean enabled;
    private String exprText;
    private String exprLang;
    private String inputSchemaJson;
    private String outputSchemaJson;
    private String errorPolicy;
    private Long timeoutMs;
}
