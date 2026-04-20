package com.yss.valset.extract.rule;

import com.alibaba.qlexpress4.Express4Runner;
import com.alibaba.qlexpress4.InitOptions;
import com.alibaba.qlexpress4.QLOptions;
import com.yss.valset.domain.rule.ParseRuleEngine;
import com.yss.valset.domain.rule.ParseRuleTraceContext;
import com.yss.valset.domain.rule.ParseRuleTraceContextHolder;
import com.yss.valset.domain.rule.ParseRuleTraceRecord;
import com.yss.valset.domain.rule.ParseRuleTraceRecorder;
import com.yss.valset.extract.support.ExcelParsingSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 基于 QLExpress 的解析规则执行器。
 */
@Slf4j
@Component
public class QlexpressParseRuleEngine implements ParseRuleEngine {

    private static final String TRACE_TYPE_KEY = "__traceType";
    private static final String TRACE_STEP_KEY = "__traceStep";
    private static final String TRACE_TYPE_PARSER = "PARSER";

    private final Express4Runner runner;
    private final ObjectMapper objectMapper;
    private final ParseRuleTraceRecorder traceRecorder;

    public QlexpressParseRuleEngine() {
        this(new ObjectMapper(), null);
    }

    @Autowired
    public QlexpressParseRuleEngine(ObjectMapper objectMapper, ParseRuleTraceRecorder traceRecorder) {
        this.runner = new Express4Runner(InitOptions.DEFAULT_OPTIONS);
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.traceRecorder = traceRecorder;
        registerBuiltInFunctions();
    }

    @Override
    public Object evaluate(String expression, Map<String, Object> context) {
        if (expression == null || expression.isBlank()) {
            return null;
        }
        long startedAt = System.currentTimeMillis();
        Map<String, Object> safeContext = safeContext(context);
        try {
            Object result = runner.execute(expression, safeContext, QLOptions.DEFAULT_OPTIONS).getResult();
            recordTraceIfNeeded(expression, safeContext, result, null, true, System.currentTimeMillis() - startedAt);
            return result;
        } catch (Exception exception) {
            recordTraceIfNeeded(expression, safeContext, null, exception, false, System.currentTimeMillis() - startedAt);
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
        return matchesHeaderRow(rowValues, requiredHeaders, ParseRuleExpressions.HEADER_ROW_EXPR);
    }

    /**
     * 判断行是否是表头行。
     */
    public boolean matchesHeaderRow(List<Object> rowValues, List<String> requiredHeaders, String expression) {
        Map<String, Object> context = new HashMap<>();
        context.put("row", rowValues);
        context.put("requiredHeaders", ParseRuleSupport.normalizeKeywords(requiredHeaders));
        context.put(TRACE_TYPE_KEY, TRACE_TYPE_PARSER);
        context.put(TRACE_STEP_KEY, "HEADER_ROW");
        return evaluateBoolean(expression == null || expression.isBlank() ? ParseRuleExpressions.HEADER_ROW_EXPR : expression, context);
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
        Map<String, Object> context = new HashMap<>();
        context.put("row", rowValues);
        context.put(TRACE_TYPE_KEY, TRACE_TYPE_PARSER);
        context.put(TRACE_STEP_KEY, "DATA_START");
        return evaluateBoolean(expression == null || expression.isBlank() ? ParseRuleExpressions.DATA_START_EXPR : expression, context);
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
    public String classifyRow(List<Object> rowValues, List<String> footerKeywords, Pattern subjectCodePattern, String expression) {
        Map<String, Object> context = new HashMap<>();
        context.put("row", rowValues);
        context.put("footerKeywords", ParseRuleSupport.normalizeKeywords(footerKeywords));
        context.put("subjectCodePattern", subjectCodePattern == null ? null : subjectCodePattern.pattern());
        context.put(TRACE_TYPE_KEY, TRACE_TYPE_PARSER);
        context.put(TRACE_STEP_KEY, "ROW_CLASSIFY");
        return evaluateString(expression == null || expression.isBlank() ? ParseRuleExpressions.ROW_CLASSIFY_EXPR : expression, context);
    }

    /**
     * 判断是否是页脚行。
     */
    public boolean matchesFooterRow(List<Object> rowValues, List<String> footerKeywords) {
        Map<String, Object> context = new HashMap<>();
        context.put("row", rowValues);
        context.put("footerKeywords", ParseRuleSupport.normalizeKeywords(footerKeywords));
        context.put(TRACE_TYPE_KEY, TRACE_TYPE_PARSER);
        context.put(TRACE_STEP_KEY, "FOOTER_ROW");
        return evaluateBoolean("isFooterRow(row, footerKeywords)", context);
    }

    private void registerBuiltInFunctions() {
        runner.addVarArgsFunction("rowContainsAll", params -> ParseRuleSupport.rowContainsAll(
                asRow(params, 0),
                asStringList(params, 1)
        ));
        runner.addVarArgsFunction("rowContainsAny", params -> ParseRuleSupport.rowContainsAny(
                asRow(params, 0),
                asStringList(params, 1)
        ));
        runner.addVarArgsFunction("containsAny", params -> ParseRuleSupport.containsAny(
                asString(params, 0, ""),
                asStringList(params, 1)
        ));
        runner.addVarArgsFunction("containsAll", params -> ParseRuleSupport.containsAll(
                asString(params, 0, ""),
                asStringList(params, 1)
        ));
        runner.addVarArgsFunction("hasText", params -> ParseRuleSupport.hasText(
                params == null || params.length == 0 ? null : params[0]
        ));
        runner.addVarArgsFunction("rowHitCount", params -> ParseRuleSupport.rowHitCount(
                asRow(params, 0),
                asStringList(params, 1)
        ));
        runner.addVarArgsFunction("isHeaderRow", params -> ParseRuleSupport.isHeaderRow(
                asRow(params, 0),
                asStringList(params, 1)
        ));
        runner.addVarArgsFunction("isDataStartRow", params -> ParseRuleSupport.isDataStartRow(asRow(params, 0)));
        runner.addVarArgsFunction("isDataStartRowWithPattern", params -> ParseRuleSupport.isDataStartRow(
                asRow(params, 0),
                asPattern(params, 1)
        ));
        runner.addVarArgsFunction("isSubjectRow", params -> ParseRuleSupport.isSubjectRow(asRow(params, 0)));
        runner.addVarArgsFunction("isSubjectRowWithPattern", params -> ParseRuleSupport.isSubjectRow(
                asRow(params, 0),
                asPattern(params, 1)
        ));
        runner.addVarArgsFunction("isMetricCandidate", params -> ParseRuleSupport.isMetricCandidate(asRow(params, 0)));
        runner.addVarArgsFunction("isMetricCandidateWithPattern", params -> ParseRuleSupport.isMetricCandidate(
                asRow(params, 0),
                asPattern(params, 1)
        ));
        runner.addVarArgsFunction("isMetricDataRow", params -> ExcelParsingSupport.isMetricDataRow(asRow(params, 0)));
        runner.addVarArgsFunction("isMetricDataRowWithPattern", params -> ExcelParsingSupport.isMetricDataRow(
                asRow(params, 0),
                asPattern(params, 1)
        ));
        runner.addVarArgsFunction("isMetricRow", params -> ExcelParsingSupport.isMetricRow(asRow(params, 0)));
        runner.addVarArgsFunction("isMetricRowWithPattern", params -> ExcelParsingSupport.isMetricRow(
                asRow(params, 0),
                asPattern(params, 1)
        ));
        runner.addVarArgsFunction("isFooterRow", params -> ParseRuleSupport.isFooterRow(
                asRow(params, 0),
                asStringList(params, 1)
        ));
        runner.addVarArgsFunction("classifyRow", params -> ParseRuleSupport.classifyRow(
                asRow(params, 0),
                asStringList(params, 1)
        ));
        runner.addVarArgsFunction("classifyRowWithPattern", params -> ParseRuleSupport.classifyRow(
                asRow(params, 0),
                asStringList(params, 1),
                asPattern(params, 2)
        ));
        runner.addVarArgsFunction("firstMeaningfulTextContainsAny", params -> ParseRuleSupport.firstMeaningfulTextContainsAny(
                asRow(params, 0),
                asStringList(params, 1)
        ));
        runner.addVarArgsFunction("firstMeaningfulTextContainsAll", params -> ParseRuleSupport.firstMeaningfulTextContainsAll(
                asRow(params, 0),
                asStringList(params, 1)
        ));
        runner.addVarArgsFunction("hasAtLeastNonBlank", params -> ParseRuleSupport.hasAtLeastNonBlank(
                asRow(params, 0),
                asIntObject(params, 1)
        ));
        runner.addVarArgsFunction("textAt", params -> ExcelParsingSupport.textAt(asRow(params, 0), asInt(params, 1)));
        runner.addVarArgsFunction("valueAt", params -> ExcelParsingSupport.valueAt(asRow(params, 0), asInt(params, 1)));
        runner.addVarArgsFunction("rowNonBlankCount", params -> ParseRuleSupport.nonBlankCount(asRow(params, 0)));
        runner.addVarArgsFunction("firstMeaningfulText", params -> ParseRuleSupport.firstMeaningfulText(asRow(params, 0)));
    }

    private Map<String, Object> safeContext(Map<String, Object> context) {
        return context == null ? Map.of() : new HashMap<>(context);
    }

    private void recordTraceIfNeeded(String expression,
                                     Map<String, Object> context,
                                     Object result,
                                     Exception exception,
                                     boolean success,
                                     long costMs) {
        if (traceRecorder == null) {
            return;
        }
        ParseRuleTraceContext traceContext = ParseRuleTraceContextHolder.get();
        if (traceContext == null || !Boolean.TRUE.equals(traceContext.getTraceEnabled())) {
            return;
        }
        String traceType = asString(context.get(TRACE_TYPE_KEY), TRACE_TYPE_PARSER);
        String stepName = asString(context.get(TRACE_STEP_KEY), "EXPRESSION_EVAL");
        try {
            Map<String, Object> sanitizedContext = new HashMap<>(context);
            sanitizedContext.remove(TRACE_TYPE_KEY);
            sanitizedContext.remove(TRACE_STEP_KEY);
            ParseRuleTraceRecord record = ParseRuleTraceRecord.builder()
                    .traceScope(traceContext.getTraceScope())
                    .traceType(traceType)
                    .profileId(traceContext.getProfileId())
                    .profileCode(traceContext.getProfileCode())
                    .version(traceContext.getVersion())
                    .fileId(traceContext.getFileId())
                    .taskId(traceContext.getTaskId())
                    .stepName(stepName)
                    .expression(expression)
                    .inputJson(objectMapper.writeValueAsString(sanitizedContext))
                    .outputJson(result == null ? "null" : objectMapper.writeValueAsString(result))
                    .success(success)
                    .costMs(costMs)
                    .errorMessage(exception == null ? null : exception.getMessage())
                    .traceTime(LocalDateTime.now())
                    .build();
            traceRecorder.record(record);
        } catch (Exception traceException) {
            log.warn("记录 QLExpress 规则追踪失败，stepName={}, traceType={}", stepName, traceType, traceException);
        }
    }

    private String asString(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? defaultValue : text;
    }

    private String asString(Object[] params, int index, String defaultValue) {
        if (params == null || index < 0 || index >= params.length) {
            return defaultValue;
        }
        return asString(params[index], defaultValue);
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

    private Integer asIntObject(Object[] params, int index) {
        int value = asInt(params, index);
        return value < 0 ? null : value;
    }

    private Pattern asPattern(Object[] params, int index) {
        if (params == null || index < 0 || index >= params.length || params[index] == null) {
            return null;
        }
        Object value = params[index];
        try {
            if (value instanceof Pattern pattern) {
                return pattern;
            }
            String text = String.valueOf(value).trim();
            if (text.isBlank()) {
                return null;
            }
            return Pattern.compile(text);
        } catch (Exception exception) {
            return null;
        }
    }
}
