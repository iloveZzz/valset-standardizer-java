package com.yss.valset.transfer.domain.rule;

import com.alibaba.qlexpress4.Express4Runner;
import com.alibaba.qlexpress4.InitOptions;
import com.alibaba.qlexpress4.QLOptions;
import com.yss.valset.transfer.domain.model.RuleContext;
import com.yss.valset.transfer.domain.model.RuleDefinition;
import com.yss.valset.transfer.domain.model.RuleEvaluationResult;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 脚本规则引擎适配器。
 */
@Component
public class ScriptRuleEngineAdapter implements RuleEngine {

    private final Express4Runner express4Runner = new Express4Runner(InitOptions.DEFAULT_OPTIONS);
    private final TransferRuleFunctions transferRuleFunctions = new TransferRuleFunctions();

    @Override
    public RuleEvaluationResult evaluate(RuleDefinition ruleDefinition, RuleContext context) {
        if (ruleDefinition == null || !ruleDefinition.enabled()) {
            return new RuleEvaluationResult(false, Collections.emptyList(), "规则未启用");
        }
        String script = ruleDefinition.scriptBody();
        if (script == null || script.isBlank()) {
            return new RuleEvaluationResult(false, Collections.emptyList(), "规则脚本为空");
        }

        Map<String, Object> variables = new LinkedHashMap<>();
        if (context != null && context.variables() != null) {
            variables.putAll(context.variables());
        }
        if (context != null && context.recognitionContext() != null) {
            variables.putIfAbsent("sourceType", context.recognitionContext().sourceType());
            variables.putIfAbsent("sourceCode", context.recognitionContext().sourceCode());
            variables.putIfAbsent("fileName", context.recognitionContext().fileName());
            variables.putIfAbsent("mimeType", context.recognitionContext().mimeType());
            variables.putIfAbsent("fileSize", context.recognitionContext().fileSize());
            variables.putIfAbsent("sender", context.recognitionContext().sender());
            variables.putIfAbsent("recipientsTo", context.recognitionContext().recipientsTo());
            variables.putIfAbsent("recipientsCc", context.recognitionContext().recipientsCc());
            variables.putIfAbsent("recipientsBcc", context.recognitionContext().recipientsBcc());
            variables.putIfAbsent("subject", context.recognitionContext().subject());
            variables.putIfAbsent("body", context.recognitionContext().body());
            variables.putIfAbsent("mailId", context.recognitionContext().mailId());
            variables.putIfAbsent("mailProtocol", context.recognitionContext().mailProtocol());
            variables.putIfAbsent("mailFolder", context.recognitionContext().mailFolder());
            variables.putIfAbsent("path", context.recognitionContext().path());
            if (context.recognitionContext().attributes() != null) {
                variables.putIfAbsent("attachmentName", context.recognitionContext().attributes().get("attachmentName"));
                variables.putIfAbsent("attachmentIndex", context.recognitionContext().attributes().get("attachmentIndex"));
                variables.putIfAbsent("attachmentContentType", context.recognitionContext().attributes().get("attachmentContentType"));
                variables.putIfAbsent("attachmentSize", context.recognitionContext().attributes().get("attachmentSize"));
                variables.putIfAbsent("attachmentCount", context.recognitionContext().attributes().get("attachmentCount"));
                variables.putIfAbsent("attachmentNames", context.recognitionContext().attributes().get("attachmentNames"));
                variables.putIfAbsent("attributes", context.recognitionContext().attributes());
            }
        }
        variables.putIfAbsent("fn", transferRuleFunctions);
        variables.putIfAbsent("rule", ruleDefinition);

        Object rawResult = express4Runner.execute(script, variables, QLOptions.DEFAULT_OPTIONS).getResult();
        if (rawResult instanceof Boolean matched) {
            return new RuleEvaluationResult(matched, Collections.emptyList(), matched ? "规则命中" : "规则未命中");
        }
        if (rawResult instanceof Map<?, ?> map) {
            boolean matched = resolveBoolean(map.get("matched"));
            String message = map.get("message") == null ? "规则执行完成" : String.valueOf(map.get("message"));
            List<?> routeRawList = map.get("routes") instanceof List<?> list ? list : List.of();
            return new RuleEvaluationResult(matched, Collections.emptyList(), message + ", routes=" + routeRawList.size());
        }
        if (rawResult == null) {
            return new RuleEvaluationResult(false, Collections.emptyList(), "规则未返回结果");
        }
        return new RuleEvaluationResult(resolveBoolean(rawResult), Collections.emptyList(), "规则执行完成");
    }

    private boolean resolveBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        if (value == null) {
            return false;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }
}
