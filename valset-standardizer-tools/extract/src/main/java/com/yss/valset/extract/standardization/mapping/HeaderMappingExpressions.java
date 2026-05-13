package com.yss.valset.extract.standardization.mapping;

/**
 * 表头映射规则表达式。
 */
public final class HeaderMappingExpressions {

    /**
     * 表头策略选择表达式。
     */
    public static final String STRATEGY_EXPR = "exactCandidate != null ? 'exact_header' : "
            + "(segmentCandidate != null ? 'header_segment' : "
            + "(aliasCandidate != null ? 'alias_contains' : 'fallback'))";

    /**
     * 表头匹配置信度表达式。
     */
    public static final String CONFIDENCE_EXPR = "exactCandidate != null ? 0.98 : "
            + "(segmentCandidate != null ? 0.92 : "
            + "(aliasCandidate != null ? 0.80 : 0.0))";

    /**
     * 表头匹配原因表达式。
     */
    public static final String REASON_EXPR = "exactCandidate != null ? '精确表头匹配' : "
            + "(segmentCandidate != null ? '按表头分段匹配' : "
            + "(aliasCandidate != null ? '按别名白名单匹配' : '未命中标准表头'))";

    private HeaderMappingExpressions() {
    }
}
