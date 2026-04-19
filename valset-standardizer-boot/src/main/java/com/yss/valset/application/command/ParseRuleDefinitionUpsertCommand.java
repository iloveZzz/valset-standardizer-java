package com.yss.valset.application.command;

import com.yss.valset.domain.rule.ParseRuleType;
import lombok.Data;

/**
 * 解析规则步骤新增或更新请求。
 */
@Data
public class ParseRuleDefinitionUpsertCommand {
    private Long id;
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
