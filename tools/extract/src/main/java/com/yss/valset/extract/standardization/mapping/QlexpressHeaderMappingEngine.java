package com.yss.valset.extract.standardization.mapping;

import com.alibaba.qlexpress4.Express4Runner;
import com.alibaba.qlexpress4.InitOptions;
import com.alibaba.qlexpress4.QLOptions;
import com.yss.valset.domain.model.MappingDecision;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

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

    private final Express4Runner runner;
    private final DefaultHeaderMappingEngine legacyEngine;

    public QlexpressHeaderMappingEngine() {
        this.runner = new Express4Runner(InitOptions.DEFAULT_OPTIONS);
        this.legacyEngine = new DefaultHeaderMappingEngine();
    }

    @Override
    public Map<Integer, MappingDecision> map(List<HeaderMappingInput> inputs, HeaderMappingLookup lookup) {
        Map<Integer, MappingDecision> decisions = new LinkedHashMap<>();
        if (inputs == null || inputs.isEmpty()) {
            return decisions;
        }
        for (HeaderMappingInput input : inputs) {
            MappingDecision decision = mapOne(input, lookup);
            if (decision != null && input != null && input.columnIndex() != null) {
                decisions.put(input.columnIndex(), decision);
            }
        }
        return decisions;
    }

    private MappingDecision mapOne(HeaderMappingInput input, HeaderMappingLookup lookup) {
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

        String strategy = evaluateString(HeaderMappingExpressions.STRATEGY_EXPR, context);
        if ("exact_header".equals(strategy) && exactCandidate != null) {
            return buildDecision(input, exactCandidate, strategy, context);
        }
        if ("header_segment".equals(strategy) && segmentCandidate != null) {
            return buildDecision(input, segmentCandidate, strategy, context);
        }
        if ("alias_contains".equals(strategy) && aliasCandidate != null) {
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
        Double confidence = evaluateConfidence(context);
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
        try {
            return runner.execute(expression, context, QLOptions.DEFAULT_OPTIONS).getResult();
        } catch (Exception exception) {
            log.warn("QLExpress 表头映射执行失败，expression={}", expression, exception);
            throw new IllegalStateException("QLExpress 表头映射执行失败: " + expression, exception);
        }
    }

    private String evaluateString(String expression, Map<String, Object> context) {
        Object result = evaluate(expression, context);
        return result == null ? "" : String.valueOf(result);
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
}
