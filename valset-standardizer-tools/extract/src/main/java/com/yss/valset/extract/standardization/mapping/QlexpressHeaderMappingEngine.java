package com.yss.valset.extract.standardization.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.domain.model.MappingDecision;
import com.yss.valset.extract.rule.QlexpressRuleEngine;
import com.yss.valset.qlexpress.domain.runtime.ManagedQlexpressRunner;
import com.yss.valset.qlexpress.domain.runtime.QlexpressCommonContextContributor;
import com.yss.valset.qlexpress.domain.runtime.QlexpressCommonFunctionFacade;
import com.yss.valset.qlexpress.domain.runtime.QlexpressExecutionContextEnhancer;
import com.yss.valset.qlexpress.domain.runtime.QlexpressRunnerRegistry;
import com.yss.valset.qlexpress.domain.runtime.SystemQlexpressFunctionSeedScripts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Autowired;
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

    private static final String TRACE_TYPE_KEY = "__traceType";
    private static final String TRACE_STEP_KEY = "__traceStep";
    private static final String TRACE_TYPE_HEADER = "HEADER_MAPPING";
    private static final String RUNNER_SCOPE = "extract.headerMapping";

    private final QlexpressRuleEngine ruleEngine;
    private final DefaultHeaderMappingEngine legacyEngine;

    public QlexpressHeaderMappingEngine() {
        this(new ObjectMapper());
    }

    public QlexpressHeaderMappingEngine(ObjectMapper objectMapper) {
        QlexpressExecutionContextEnhancer contextEnhancer = new QlexpressExecutionContextEnhancer(java.util.Arrays.asList(
                new QlexpressCommonContextContributor(new QlexpressCommonFunctionFacade()),
                new QlexpressHeaderContextContributor(new QlexpressHeaderFunctionFacade())
        ));
        QlexpressRunnerRegistry registry = new QlexpressRunnerRegistry(SystemQlexpressFunctionSeedScripts::scripts, contextEnhancer);
        ManagedQlexpressRunner runner = registry.createManagedRunner(RUNNER_SCOPE);
        this.ruleEngine = new QlexpressRuleEngine(
                runner,
                RUNNER_SCOPE,
                contextEnhancer,
                objectMapper,
                "QLExpress 表头映射执行失败"
        );
        this.legacyEngine = new DefaultHeaderMappingEngine();
    }

    @Autowired
    public QlexpressHeaderMappingEngine(ObjectMapper objectMapper,
                                        QlexpressRunnerRegistry qlexpressRunnerRegistry,
                                        QlexpressExecutionContextEnhancer contextEnhancer) {
        this.ruleEngine = new QlexpressRuleEngine(
                qlexpressRunnerRegistry.createManagedRunner(RUNNER_SCOPE),
                RUNNER_SCOPE,
                contextEnhancer,
                objectMapper,
                "QLExpress 表头映射执行失败"
        );
        this.legacyEngine = new DefaultHeaderMappingEngine();
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

        String strategy = evaluateString(strategyExpr == null || strategyExpr.trim().isEmpty() ? HeaderMappingExpressions.STRATEGY_EXPR : strategyExpr, context);
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
        if (reason == null || reason.trim().isEmpty()) {
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
        if (result instanceof Number) {
            return ((Number) result).doubleValue();
        }
        return 0D;
    }

    private Object evaluate(String expression, Map<String, Object> context) {
        return ruleEngine.evaluate(expression, context, TRACE_TYPE_HEADER, "HEADER_MAPPING");
    }

    private String evaluateString(String expression, Map<String, Object> context) {
        return ruleEngine.evaluateString(expression, context, TRACE_TYPE_HEADER, "HEADER_MAPPING");
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
        Map<Integer, MappingDecision> legacy = legacyEngine.map(java.util.Arrays.asList(input), lookup);
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
