package com.yss.valset.extract.rule;

/**
 * 规则表达式常量。
 */
public final class ParseRuleExpressions {

    /**
     * 表头识别表达式。
     */
    public static final String HEADER_ROW_EXPR = "isHeaderRow(row, requiredHeaders)";

    /**
     * 数据起始行表达式。
     */
    public static final String DATA_START_EXPR = "isDataStartRow(row)";

    /**
     * 行分类表达式。
     */
    public static final String ROW_CLASSIFY_EXPR = "classifyRowWithPattern(row, footerKeywords, subjectCodePattern)";

    private ParseRuleExpressions() {
    }
}
