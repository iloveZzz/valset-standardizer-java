package com.yss.valset.domain.rule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 解析规则步骤定义。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParseRuleDefinition {

    private Long id;

    /**
     * 关联模板主表。
     */
    private Long profileId;

    /**
     * 规则类型。
     */
    private ParseRuleType ruleType;

    /**
     * 步骤名称。
     */
    private String stepName;

    /**
     * 执行优先级。
     */
    private Integer priority;

    /**
     * 是否启用。
     */
    private Boolean enabled;

    /**
     * 表达式文本。
     */
    private String exprText;

    /**
     * 表达式语言。
     */
    private String exprLang;

    /**
     * 输入结构说明。
     */
    private String inputSchemaJson;

    /**
     * 输出结构说明。
     */
    private String outputSchemaJson;

    /**
     * 异常策略。
     */
    private String errorPolicy;

    /**
     * 超时时间毫秒数。
     */
    private Long timeoutMs;
}
