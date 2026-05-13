package com.yss.valset.transfer.domain.rule;


/**
 * 工具类：从 JSON 解析规则树，生成 SQL 表达式
 */
public class ConditionRuleParser {
    public static ConditionRule fromJsonString(String jsonStr) {
        return JSONUtils.parseObject(jsonStr, ConditionRule.class);
    }
    public static String toJsonString(ConditionRule rule) {
        return JSONUtils.toJsonString(rule);
    }
}