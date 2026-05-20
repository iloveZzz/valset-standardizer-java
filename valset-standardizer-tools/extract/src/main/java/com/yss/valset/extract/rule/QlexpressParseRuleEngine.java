package com.yss.valset.extract.rule;

import com.yss.valset.domain.rule.ParseRuleEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.qlexpress.domain.runtime.ManagedQlexpressRunner;
import com.yss.valset.qlexpress.domain.runtime.QlexpressCommonContextContributor;
import com.yss.valset.qlexpress.domain.runtime.QlexpressCommonFunctionFacade;
import com.yss.valset.qlexpress.domain.runtime.QlexpressExecutionContextEnhancer;
import com.yss.valset.qlexpress.domain.runtime.QlexpressFunctionScriptProvider;
import com.yss.valset.qlexpress.domain.runtime.QlexpressRunnerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 QLExpress 的解析规则执行器。
 */
@Slf4j
@Component
public class QlexpressParseRuleEngine implements ParseRuleEngine {

    private static final String TRACE_TYPE_KEY = "__traceType";
    private static final String TRACE_STEP_KEY = "__traceStep";
    private static final String TRACE_TYPE_PARSER = "PARSER";
    private static final String RUNNER_SCOPE = "extract.parse";

    private final QlexpressRuleEngine ruleEngine;

    public QlexpressParseRuleEngine() {
        this(new ObjectMapper(), Collections::emptyList);
    }

    public QlexpressParseRuleEngine(ObjectMapper objectMapper) {
        this(objectMapper, Collections::emptyList);
    }

    public QlexpressParseRuleEngine(ObjectMapper objectMapper,
                                    QlexpressFunctionScriptProvider scriptProvider) {
        QlexpressExecutionContextEnhancer contextEnhancer = new QlexpressExecutionContextEnhancer(
                java.util.Collections.singletonList(new QlexpressCommonContextContributor(new QlexpressCommonFunctionFacade()))
        );
        QlexpressRunnerRegistry registry = new QlexpressRunnerRegistry(scriptProvider, contextEnhancer);
        ManagedQlexpressRunner runner = registry.createManagedRunner(RUNNER_SCOPE);
        this.ruleEngine = new QlexpressRuleEngine(
                runner,
                RUNNER_SCOPE,
                contextEnhancer,
                objectMapper,
                "QLExpress 规则执行失败"
        );
    }

    @Autowired
    public QlexpressParseRuleEngine(ObjectMapper objectMapper,
                                    QlexpressRunnerRegistry qlexpressRunnerRegistry,
                                    QlexpressExecutionContextEnhancer contextEnhancer) {
        this.ruleEngine = new QlexpressRuleEngine(
                qlexpressRunnerRegistry.createManagedRunner(RUNNER_SCOPE),
                RUNNER_SCOPE,
                contextEnhancer,
                objectMapper,
                "QLExpress 规则执行失败"
        );
    }

    @Override
    public Object evaluate(String expression, Map<String, Object> context) {
        if (expression == null || expression.trim().isEmpty()) {
            return null;
        }
        return ruleEngine.evaluate(expression, context, TRACE_TYPE_PARSER, "EXPRESSION_EVAL");
    }

    @Override
    public boolean evaluateBoolean(String expression, Map<String, Object> context) {
        return ruleEngine.evaluateBoolean(expression, context, TRACE_TYPE_PARSER, "EXPRESSION_EVAL");
    }

    @Override
    public String evaluateString(String expression, Map<String, Object> context) {
        return ruleEngine.evaluateString(expression, context, TRACE_TYPE_PARSER, "EXPRESSION_EVAL");
    }

    /**
     * 执行表达式并要求返回 Map。
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> evaluateMap(String expression, Map<String, Object> context) {
        return ruleEngine.evaluateMap(expression, context, TRACE_TYPE_PARSER, "EXPRESSION_EVAL");
    }

    /**
     * 判断行是否是表头行。
     */
    public boolean matchesHeaderRow(List<Object> rowValues, List<String> requiredHeaders) {
        return matchesHeaderRow(rowValues, requiredHeaders, ParseRuleExpressions.HEADER_ROW_EXPR);
    }

    /**
     * 判断行是否是表头行。
     */
    public boolean matchesHeaderRow(List<Object> rowValues, List<String> requiredHeaders, String expression) {
        Map<String, Object> context = new HashMap<>();
        context.put("row", rowValues);
        context.put("requiredHeaders", normalizeKeywords(requiredHeaders));
        context.put(TRACE_TYPE_KEY, TRACE_TYPE_PARSER);
        context.put(TRACE_STEP_KEY, "HEADER_ROW");
        return evaluateBoolean(expression == null || expression.trim().isEmpty() ? ParseRuleExpressions.HEADER_ROW_EXPR : expression, context);
    }

    /**
     * 判断行是否是数据起始行。
     */
    public boolean matchesDataStartRow(List<Object> rowValues) {
        return matchesDataStartRow(rowValues, ParseRuleExpressions.DATA_START_EXPR);
    }

    /**
     * 判断行是否是数据起始行。
     */
    public boolean matchesDataStartRow(List<Object> rowValues, String expression) {
        Map<String, Object> context = buildRowContext(rowValues, null, "DATA_START");
        return evaluateBoolean(expression == null || expression.trim().isEmpty() ? ParseRuleExpressions.DATA_START_EXPR : expression, context);
    }

    /**
     * 判断当前行是否包含科目代码和科目名称。
     */
    public boolean matchesSubjectDataRow(List<Object> rowValues, String subjectCodePattern) {
        return evaluateBoolean("isSubjectRowWithPattern(row, subjectCodePattern)", buildRowContext(rowValues, subjectCodePattern, "SUBJECT_ROW"));
    }

    /**
     * 判断当前行是否是两列型指标数据。
     */
    public boolean matchesMetricDataRow(List<Object> rowValues, String subjectCodePattern) {
        return evaluateBoolean("isMetricDataRowWithPattern(row, subjectCodePattern)", buildRowContext(rowValues, subjectCodePattern, "METRIC_DATA_ROW"));
    }

    /**
     * 按表头定位到的“科目代码”列判断当前行是否是单值指标数据。
     */
    public boolean matchesMetricDataRowByColumn(List<Object> rowValues, Integer subjectCodeColumnIndex, String subjectCodePattern) {
        return evaluateBoolean(
                "isMetricDataRowByColumn(row, subjectCodeColumnIndex, subjectCodePattern)",
                buildColumnRowContext(rowValues, subjectCodeColumnIndex, subjectCodePattern, "METRIC_DATA_ROW_BY_COLUMN")
        );
    }

    /**
     * 判断当前行是否是多列型指标行。
     */
    public boolean matchesMetricRow(List<Object> rowValues, String subjectCodePattern) {
        return evaluateBoolean("isMetricRowWithPattern(row, subjectCodePattern)", buildRowContext(rowValues, subjectCodePattern, "METRIC_ROW"));
    }

    /**
     * 按表头定位到的“科目代码”列判断当前行是否是多值指标行。
     */
    public boolean matchesMetricRowByColumn(List<Object> rowValues, Integer subjectCodeColumnIndex, String subjectCodePattern) {
        return evaluateBoolean(
                "isMetricRowByColumn(row, subjectCodeColumnIndex, subjectCodePattern)",
                buildColumnRowContext(rowValues, subjectCodeColumnIndex, subjectCodePattern, "METRIC_ROW_BY_COLUMN")
        );
    }

    /**
     * 按表头定位到的“科目代码”列判断当前行是否是估值明细科目行。
     */
    public boolean matchesSubjectDetailRowByColumn(List<Object> rowValues, Integer subjectCodeColumnIndex, String subjectCodePattern) {
        return evaluateBoolean(
                "isSubjectDetailRowByColumn(row, subjectCodeColumnIndex, subjectCodePattern)",
                buildColumnRowContext(rowValues, subjectCodeColumnIndex, subjectCodePattern, "SUBJECT_DETAIL_ROW")
        );
    }

    /**
     * 按表头定位到的“科目代码”列判断当前行是否是指标行。
     */
    public boolean matchesMetricDetailRowByColumn(List<Object> rowValues, Integer subjectCodeColumnIndex, String subjectCodePattern) {
        return evaluateBoolean(
                "isMetricDetailRowByColumn(row, subjectCodeColumnIndex, subjectCodePattern)",
                buildColumnRowContext(rowValues, subjectCodeColumnIndex, subjectCodePattern, "METRIC_DETAIL_ROW")
        );
    }

    /**
     * 按表头定位到的“科目代码”列判断当前行是否属于估值数据区。
     */
    public boolean matchesValuationDataRowByColumn(List<Object> rowValues, Integer subjectCodeColumnIndex, String subjectCodePattern) {
        return evaluateBoolean(
                "isValuationDataRowByColumn(row, subjectCodeColumnIndex, subjectCodePattern)",
                buildColumnRowContext(rowValues, subjectCodeColumnIndex, subjectCodePattern, "VALUATION_DATA_ROW")
        );
    }

    /**
     * 获取行分类。
     */
    public String classifyRow(List<Object> rowValues, List<String> footerKeywords) {
        return classifyRow(rowValues, footerKeywords, null, ParseRuleExpressions.ROW_CLASSIFY_EXPR);
    }

    /**
     * 获取行分类。
     */
    public String classifyRow(List<Object> rowValues, List<String> footerKeywords, String expression) {
        return classifyRow(rowValues, footerKeywords, null, expression);
    }

    /**
     * 获取行分类。
     */
    public String classifyRow(List<Object> rowValues, List<String> footerKeywords, String subjectCodePattern, String expression) {
        Map<String, Object> context = new HashMap<>();
        context.put("row", rowValues);
        context.put("footerKeywords", normalizeKeywords(footerKeywords));
        context.put("subjectCodePattern", subjectCodePattern);
        context.put(TRACE_TYPE_KEY, TRACE_TYPE_PARSER);
        context.put(TRACE_STEP_KEY, "ROW_CLASSIFY");
        return evaluateString(expression == null || expression.trim().isEmpty() ? ParseRuleExpressions.ROW_CLASSIFY_EXPR : expression, context);
    }

    /**
     * 判断是否是页脚行。
     */
    public boolean matchesFooterRow(List<Object> rowValues, List<String> footerKeywords) {
        Map<String, Object> context = new HashMap<>();
        context.put("row", rowValues);
        context.put("footerKeywords", normalizeKeywords(footerKeywords));
        context.put(TRACE_TYPE_KEY, TRACE_TYPE_PARSER);
        context.put(TRACE_STEP_KEY, "FOOTER_ROW");
        return evaluateBoolean("isFooterRow(row, footerKeywords)", context);
    }

    private Map<String, Object> buildRowContext(List<Object> rowValues, String subjectCodePattern, String traceStep) {
        Map<String, Object> context = new HashMap<>();
        context.put("row", rowValues);
        context.put("subjectCodePattern", subjectCodePattern);
        context.put(TRACE_TYPE_KEY, TRACE_TYPE_PARSER);
        context.put(TRACE_STEP_KEY, traceStep);
        return context;
    }

    private Map<String, Object> buildColumnRowContext(List<Object> rowValues, Integer subjectCodeColumnIndex, String subjectCodePattern, String traceStep) {
        Map<String, Object> context = buildRowContext(rowValues, subjectCodePattern, traceStep);
        context.put("subjectCodeColumnIndex", subjectCodeColumnIndex);
        return context;
    }

    private List<String> normalizeKeywords(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        List<String> normalized = new java.util.ArrayList<>(keywords.size());
        for (String keyword : keywords) {
            if (keyword == null || keyword.trim().isEmpty()) {
                continue;
            }
            normalized.add(keyword.trim());
        }
        return normalized;
    }

}
