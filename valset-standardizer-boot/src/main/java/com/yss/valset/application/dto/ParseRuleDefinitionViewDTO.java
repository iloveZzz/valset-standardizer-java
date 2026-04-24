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
    /**
     * 规则步骤主键。
     */
    private String id;
    /**
     * 模板主键。
     */
    private String profileId;
    /**
     * 规则类型。
     */
    private ParseRuleType ruleType;
    /**
     * 步骤名称。
     */
    private String stepName;
    /**
     * 优先级。
     */
    private Integer priority;
    /**
     * 是否启用。
     */
    private Boolean enabled;
    /**
     * 表达式内容。
     */
    private String exprText;
    /**
     * 表达式语言。
     */
    private String exprLang;
    /**
     * 输入结构 JSON。
     */
    private String inputSchemaJson;
    /**
     * 输出结构 JSON。
     */
    private String outputSchemaJson;
    /**
     * 错误策略。
     */
    private String errorPolicy;
    /**
     * 超时时间，单位毫秒。
     */
    private String timeoutMs;
}
