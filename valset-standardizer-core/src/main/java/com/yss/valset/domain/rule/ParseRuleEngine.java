package com.yss.valset.domain.rule;

import java.util.Map;

/**
 * 解析规则执行器抽象。
 */
public interface ParseRuleEngine {

    /**
     * 执行表达式并返回原始结果。
     */
    Object evaluate(String expression, Map<String, Object> context);

    /**
     * 执行表达式并返回布尔结果。
     */
    boolean evaluateBoolean(String expression, Map<String, Object> context);

    /**
     * 执行表达式并返回文本结果。
     */
    String evaluateString(String expression, Map<String, Object> context);
}
