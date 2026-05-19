package com.yss.valset.extract.rule;

import com.alibaba.qlexpress4.QLOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.qlexpress.domain.runtime.QlexpressExecutionContextEnhancer;
import com.yss.valset.qlexpress.domain.runtime.ManagedQlexpressRunner;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 唯一的 QLExpress 规则执行器，统一表达式执行和结果转换。
 */
@Slf4j
public class QlexpressRuleEngine {

    public static final String TRACE_TYPE_KEY = "__traceType";
    public static final String TRACE_STEP_KEY = "__traceStep";

    private final ManagedQlexpressRunner runner;
    private final String runnerScope;
    private final QlexpressExecutionContextEnhancer contextEnhancer;
    private final String defaultErrorPrefix;

    public QlexpressRuleEngine(ManagedQlexpressRunner runner,
                               ObjectMapper objectMapper,
                               String defaultErrorPrefix) {
        this(runner, null, QlexpressExecutionContextEnhancer.empty(), objectMapper, defaultErrorPrefix);
    }

    public QlexpressRuleEngine(ManagedQlexpressRunner runner,
                               String runnerScope,
                               QlexpressExecutionContextEnhancer contextEnhancer,
                               ObjectMapper objectMapper,
                               String defaultErrorPrefix) {
        this.runner = runner;
        this.runnerScope = runnerScope;
        this.contextEnhancer = contextEnhancer == null ? QlexpressExecutionContextEnhancer.empty() : contextEnhancer;
        this.defaultErrorPrefix = hasText(defaultErrorPrefix) ? defaultErrorPrefix : "QLExpress 规则执行失败";
    }

    public Object evaluate(String expression, Map<String, Object> context, String defaultTraceType, String defaultStepName) {
        return evaluate(expression, context, defaultTraceType, defaultStepName, defaultErrorPrefix);
    }

    public Object evaluate(String expression,
                           Map<String, Object> context,
                           String defaultTraceType,
                           String defaultStepName,
                           String errorPrefix) {
        if (!hasText(expression)) {
            return null;
        }
        long startedAt = System.currentTimeMillis();
        Map<String, Object> safeContext = contextEnhancer.enhance(runnerScope, safeContext(context));
        try {
            return runner.getRunner().execute(expression, safeContext, QLOptions.DEFAULT_OPTIONS).getResult();
        } catch (Exception exception) {
            String prefix = hasText(errorPrefix) ? errorPrefix : defaultErrorPrefix;
            log.warn("{}，expression={}, costMs={}", prefix, expression, System.currentTimeMillis() - startedAt, exception);
            throw new IllegalStateException(prefix + ": " + expression, exception);
        }
    }

    public boolean evaluateBoolean(String expression, Map<String, Object> context, String defaultTraceType, String defaultStepName) {
        Object result = evaluate(expression, context, defaultTraceType, defaultStepName);
        if (result instanceof Boolean) {
            return ((Boolean) result).booleanValue();
        }
        if (result instanceof Number) {
            return ((Number) result).intValue() != 0;
        }
        return result != null && !String.valueOf(result).trim().isEmpty() && !"false".equalsIgnoreCase(String.valueOf(result));
    }

    public String evaluateString(String expression, Map<String, Object> context, String defaultTraceType, String defaultStepName) {
        Object result = evaluate(expression, context, defaultTraceType, defaultStepName);
        return result == null ? "" : String.valueOf(result);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> evaluateMap(String expression, Map<String, Object> context, String defaultTraceType, String defaultStepName) {
        Object result = evaluate(expression, context, defaultTraceType, defaultStepName);
        if (result == null) {
            return Collections.emptyMap();
        }
        if (result instanceof Map<?, ?>) {
            return new HashMap<>((Map<String, Object>) result);
        }
        throw new IllegalStateException("QLExpress 规则输出类型必须是 Map，actualType=" + result.getClass().getName());
    }

    private Map<String, Object> safeContext(Map<String, Object> context) {
        return context == null ? Collections.emptyMap() : new HashMap<>(context);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
