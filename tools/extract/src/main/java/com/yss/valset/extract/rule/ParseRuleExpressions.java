package com.yss.valset.extract.rule;

/**
 * 规则表达式常量。
 */
public final class ParseRuleExpressions {

    /**
     * 表头识别表达式。
     */
    public static final String HEADER_ROW_EXPR = "rowContainsAll(row, requiredHeaders)";

    /**
     * 数据起始行表达式。
     */
    public static final String DATA_START_EXPR = "isSubjectRow(row) || isMetricCandidate(row)";

    /**
     * 行分类表达式。
     */
    public static final String ROW_CLASSIFY_EXPR = "isSubjectRow(row) ? 'SUBJECT' : "
            + "(isMetricDataRow(row) ? 'METRIC_DATA' : "
            + "(isMetricRow(row) ? 'METRIC_ROW' : "
            + "(isFooterRow(row, footerKeywords) ? 'FOOTER' : 'IGNORE')))";

    private ParseRuleExpressions() {
    }
}
