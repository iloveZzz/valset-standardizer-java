package com.yss.valset.extract.standardization.mapping;

import com.alibaba.qlexpress4.Express4Runner;
import com.alibaba.qlexpress4.InitOptions;
import com.alibaba.qlexpress4.QLOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.domain.model.MappingDecision;
import com.yss.valset.domain.rule.ParseRuleTraceContext;
import com.yss.valset.domain.rule.ParseRuleTraceContextHolder;
import com.yss.valset.domain.rule.ParseRuleTraceRecord;
import com.yss.valset.domain.rule.ParseRuleTraceRecorder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 QLExpress 的表头映射引擎。
 */
@Slf4j
@Component
@Primary
public class QlexpressHeaderMappingEngine implements HeaderMappingEngine {

    private static final String TRACE_TYPE_KEY = "__traceType";
    private static final String TRACE_STEP_KEY = "__traceStep";
    private static final String TRACE_TYPE_HEADER = "HEADER_MAPPING";

    private final Express4Runner runner;
    private final DefaultHeaderMappingEngine legacyEngine;
    private final ObjectMapper objectMapper;
    private final ParseRuleTraceRecorder traceRecorder;

    public QlexpressHeaderMappingEngine() {
        this(new ObjectMapper(), null);
    }

    @Autowired
    public QlexpressHeaderMappingEngine(ObjectMapper objectMapper, ParseRuleTraceRecorder traceRecorder) {
        this.runner = new Express4Runner(InitOptions.DEFAULT_OPTIONS);
        this.legacyEngine = new DefaultHeaderMappingEngine();
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.traceRecorder = traceRecorder;
        registerBuiltInFunctions();
    }

    @Override
    public Map<Integer, MappingDecision> map(List<HeaderMappingInput> inputs, HeaderMappingLookup lookup, String strategyExpr) {
        Map<Integer, MappingDecision> decisions = new LinkedHashMap<>();
        if (inputs == null || inputs.isEmpty()) {
            return decisions;
        }
        for (HeaderMappingInput input : inputs) {
            MappingDecision decision = mapOne(input, lookup, strategyExpr);
            if (decision != null && input != null && input.columnIndex() != null) {
                decisions.put(input.columnIndex(), decision);
            }
        }
        return decisions;
    }

    private MappingDecision mapOne(HeaderMappingInput input, HeaderMappingLookup lookup, String strategyExpr) {
        if (input == null || lookup == null) {
            return fallback(input, lookup);
        }

        HeaderMappingRuleSupport.ResolvedHeaderCandidate exactCandidate =
                HeaderMappingRuleSupport.resolveExactCandidate(input, lookup);
        HeaderMappingRuleSupport.ResolvedHeaderCandidate segmentCandidate =
                HeaderMappingRuleSupport.resolveSegmentCandidate(input, lookup);
        HeaderMappingRuleSupport.ResolvedHeaderCandidate aliasCandidate =
                HeaderMappingRuleSupport.resolveAliasCandidate(input, lookup);

        Map<String, Object> context = new HashMap<>();
        context.put("headerText", input.headerText());
        context.put("segments", HeaderMappingRuleSupport.normalizeSegments(input.segments()));
        context.put("exactCandidate", exactCandidate);
        context.put("segmentCandidate", segmentCandidate);
        context.put("aliasCandidate", aliasCandidate);
        context.put(TRACE_TYPE_KEY, TRACE_TYPE_HEADER);
        context.put(TRACE_STEP_KEY, "STRATEGY");

        String strategy = evaluateString(strategyExpr == null || strategyExpr.isBlank() ? HeaderMappingExpressions.STRATEGY_EXPR : strategyExpr, context);
        if ("exact_header".equals(strategy) && exactCandidate != null) {
            context.put(TRACE_STEP_KEY, "EXACT_MATCH");
            return buildDecision(input, exactCandidate, strategy, context);
        }
        if ("header_segment".equals(strategy) && segmentCandidate != null) {
            context.put(TRACE_STEP_KEY, "SEGMENT_MATCH");
            return buildDecision(input, segmentCandidate, strategy, context);
        }
        if ("alias_contains".equals(strategy) && aliasCandidate != null) {
            context.put(TRACE_STEP_KEY, "ALIAS_MATCH");
            return buildDecision(input, aliasCandidate, strategy, context);
        }
        return fallback(input, lookup);
    }

    private MappingDecision buildDecision(
            HeaderMappingInput input,
            HeaderMappingRuleSupport.ResolvedHeaderCandidate resolvedCandidate,
            String strategy,
            Map<String, Object> context
    ) {
        if (resolvedCandidate == null || resolvedCandidate.candidate() == null) {
            return fallback(input, null);
        }
        context.put(TRACE_STEP_KEY, "CONFIDENCE");
        Double confidence = evaluateConfidence(context);
        context.put(TRACE_STEP_KEY, "REASON");
        String reason = evaluateString(HeaderMappingExpressions.REASON_EXPR, context);
        if (reason == null || reason.isBlank()) {
            reason = "表头映射成功";
        }
        String matchedText = resolvedCandidate.matchedText() == null ? input.headerText() : resolvedCandidate.matchedText();
        return MappingDecision.builder()
                .columnIndex(input.columnIndex())
                .headerText(input.headerText())
                .standardCode(resolvedCandidate.candidate().standardCode())
                .matchedRuleId(resolvedCandidate.candidate().ruleId())
                .matchedSourceId(resolvedCandidate.candidate().sourceId())
                .strategy(strategy)
                .confidence(confidence)
                .reason(reason)
                .matchedText(matchedText)
                .matched(Boolean.TRUE)
                .build();
    }

    private Double evaluateConfidence(Map<String, Object> context) {
        Object result = evaluate(HeaderMappingExpressions.CONFIDENCE_EXPR, context);
        if (result instanceof Number number) {
            return number.doubleValue();
        }
        return 0D;
    }

    private Object evaluate(String expression, Map<String, Object> context) {
        long startedAt = System.currentTimeMillis();
        Map<String, Object> safeContext = context == null ? Map.of() : new HashMap<>(context);
        try {
            Object result = runner.execute(expression, safeContext, QLOptions.DEFAULT_OPTIONS).getResult();
            recordTraceIfNeeded(expression, safeContext, result, null, true, System.currentTimeMillis() - startedAt);
            return result;
        } catch (Exception exception) {
            recordTraceIfNeeded(expression, safeContext, null, exception, false, System.currentTimeMillis() - startedAt);
            log.warn("QLExpress 表头映射执行失败，expression={}", expression, exception);
            throw new IllegalStateException("QLExpress 表头映射执行失败: " + expression, exception);
        }
    }

    private String evaluateString(String expression, Map<String, Object> context) {
        Object result = evaluate(expression, context);
        return result == null ? "" : String.valueOf(result);
    }

    private void registerBuiltInFunctions() {
        runner.addVarArgsFunction("hasCandidate", params -> params != null
                && params.length > 0
                && params[0] != null);
        runner.addVarArgsFunction("headerContainsAnySegment", params -> HeaderMappingRuleSupport.headerContainsAnySegment(
                asString(params, 0, ""),
                asStringList(params, 1)
        ));
        runner.addVarArgsFunction("headerContainsAllSegments", params -> HeaderMappingRuleSupport.headerContainsAllSegments(
                asString(params, 0, ""),
                asStringList(params, 1)
        ));
    }

    private MappingDecision fallback(HeaderMappingInput input, HeaderMappingLookup lookup) {
        if (input == null) {
            return MappingDecision.builder()
                    .matched(Boolean.FALSE)
                    .strategy("fallback")
                    .confidence(0D)
                    .reason("没有可映射的输入")
                    .build();
        }
        if (lookup == null) {
            return MappingDecision.builder()
                    .columnIndex(input.columnIndex())
                    .headerText(input.headerText())
                    .matched(Boolean.FALSE)
                    .strategy("fallback")
                    .confidence(0D)
                    .reason("映射查找器不可用")
                    .build();
        }
        Map<Integer, MappingDecision> legacy = legacyEngine.map(List.of(input), lookup);
        MappingDecision decision = legacy.get(input.columnIndex());
        if (decision != null) {
            return decision;
        }
        return MappingDecision.builder()
                .columnIndex(input.columnIndex())
                .headerText(input.headerText())
                .matched(Boolean.FALSE)
                .strategy("fallback")
                .confidence(0D)
                .reason("未命中标准表头")
                .build();
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
        String traceType = asString(context.get(TRACE_TYPE_KEY), TRACE_TYPE_HEADER);
        String stepName = asString(context.get(TRACE_STEP_KEY), "HEADER_MAPPING");
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
            log.warn("记录表头映射追踪失败，stepName={}, traceType={}", stepName, traceType, traceException);
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
}
