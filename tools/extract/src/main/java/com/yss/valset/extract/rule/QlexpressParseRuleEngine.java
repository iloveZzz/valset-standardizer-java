package com.yss.valset.extract.rule;

import com.alibaba.qlexpress4.Express4Runner;
import com.alibaba.qlexpress4.InitOptions;
import com.alibaba.qlexpress4.QLOptions;
import com.yss.valset.domain.rule.ParseRuleEngine;
import com.yss.valset.extract.support.ExcelParsingSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 QLExpress 的解析规则执行器。
 */
@Slf4j
@Component
public class QlexpressParseRuleEngine implements ParseRuleEngine {

    private final Express4Runner runner;

    public QlexpressParseRuleEngine() {
        this.runner = new Express4Runner(InitOptions.DEFAULT_OPTIONS);
        registerBuiltInFunctions();
    }

    @Override
    public Object evaluate(String expression, Map<String, Object> context) {
        if (expression == null || expression.isBlank()) {
            return null;
        }
        try {
            return runner.execute(expression, safeContext(context), QLOptions.DEFAULT_OPTIONS).getResult();
        } catch (Exception exception) {
            log.warn("QLExpress 规则执行失败，expression={}", expression, exception);
            throw new IllegalStateException("QLExpress 规则执行失败: " + expression, exception);
        }
    }

    @Override
    public boolean evaluateBoolean(String expression, Map<String, Object> context) {
        Object result = evaluate(expression, context);
        if (result instanceof Boolean bool) {
            return bool;
        }
        if (result instanceof Number number) {
            return number.intValue() != 0;
        }
        return result != null && !String.valueOf(result).isBlank() && !"false".equalsIgnoreCase(String.valueOf(result));
    }

    @Override
    public String evaluateString(String expression, Map<String, Object> context) {
        Object result = evaluate(expression, context);
        return result == null ? "" : String.valueOf(result);
    }

    /**
     * 判断行是否是表头行。
     */
    public boolean matchesHeaderRow(List<Object> rowValues, List<String> requiredHeaders) {
        Map<String, Object> context = new HashMap<>();
        context.put("row", rowValues);
        context.put("requiredHeaders", ParseRuleSupport.normalizeKeywords(requiredHeaders));
        return evaluateBoolean(ParseRuleExpressions.HEADER_ROW_EXPR, context);
    }

    /**
     * 判断行是否是数据起始行。
     */
    public boolean matchesDataStartRow(List<Object> rowValues) {
        Map<String, Object> context = new HashMap<>();
        context.put("row", rowValues);
        return evaluateBoolean(ParseRuleExpressions.DATA_START_EXPR, context);
    }

    /**
     * 获取行分类。
     */
    public String classifyRow(List<Object> rowValues, List<String> footerKeywords) {
        Map<String, Object> context = new HashMap<>();
        context.put("row", rowValues);
        context.put("footerKeywords", ParseRuleSupport.normalizeKeywords(footerKeywords));
        return evaluateString(ParseRuleExpressions.ROW_CLASSIFY_EXPR, context);
    }

    /**
     * 判断是否是页脚行。
     */
    public boolean matchesFooterRow(List<Object> rowValues, List<String> footerKeywords) {
        Map<String, Object> context = new HashMap<>();
        context.put("row", rowValues);
        context.put("footerKeywords", ParseRuleSupport.normalizeKeywords(footerKeywords));
        return evaluateBoolean("isFooterRow(row, footerKeywords)", context);
    }

    private void registerBuiltInFunctions() {
        runner.addVarArgsFunction("rowContainsAll", params -> ParseRuleSupport.rowContainsAll(
                asRow(params, 0),
                asStringList(params, 1)
        ));
        runner.addVarArgsFunction("rowHitCount", params -> ParseRuleSupport.rowHitCount(
                asRow(params, 0),
                asStringList(params, 1)
        ));
        runner.addVarArgsFunction("isSubjectRow", params -> ParseRuleSupport.isSubjectRow(asRow(params, 0)));
        runner.addVarArgsFunction("isMetricCandidate", params -> ParseRuleSupport.isMetricCandidate(asRow(params, 0)));
        runner.addVarArgsFunction("isMetricDataRow", params -> ExcelParsingSupport.isMetricDataRow(asRow(params, 0)));
        runner.addVarArgsFunction("isMetricRow", params -> ExcelParsingSupport.isMetricRow(asRow(params, 0)));
        runner.addVarArgsFunction("isFooterRow", params -> ParseRuleSupport.isFooterRow(
                asRow(params, 0),
                asStringList(params, 1)
        ));
        runner.addVarArgsFunction("textAt", params -> ExcelParsingSupport.textAt(asRow(params, 0), asInt(params, 1)));
        runner.addVarArgsFunction("valueAt", params -> ExcelParsingSupport.valueAt(asRow(params, 0), asInt(params, 1)));
        runner.addVarArgsFunction("rowNonBlankCount", params -> ParseRuleSupport.nonBlankCount(asRow(params, 0)));
        runner.addVarArgsFunction("firstMeaningfulText", params -> ParseRuleSupport.firstMeaningfulText(asRow(params, 0)));
    }

    private Map<String, Object> safeContext(Map<String, Object> context) {
        return context == null ? Map.of() : new HashMap<>(context);
    }

    @SuppressWarnings("unchecked")
    private List<Object> asRow(Object[] params, int index) {
        if (params == null || index < 0 || index >= params.length) {
            return List.of();
        }
        Object value = params[index];
        if (value instanceof List<?> list) {
            return (List<Object>) list;
        }
        return List.of(value);
    }

    @SuppressWarnings("unchecked")
    private List<String> asStringList(Object[] params, int index) {
        if (params == null || index < 0 || index >= params.length) {
            return List.of();
        }
        Object value = params[index];
        if (value instanceof List<?> list) {
            return list.stream().map(item -> item == null ? "" : String.valueOf(item)).toList();
        }
        if (value == null) {
            return List.of();
        }
        return List.of(String.valueOf(value));
    }

    private int asInt(Object[] params, int index) {
        if (params == null || index < 0 || index >= params.length || params[index] == null) {
            return -1;
        }
        Object value = params[index];
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return -1;
        }
    }
}
