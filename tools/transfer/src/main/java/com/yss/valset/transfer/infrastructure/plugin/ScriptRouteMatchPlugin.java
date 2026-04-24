package com.yss.valset.transfer.infrastructure.plugin;

import com.yss.valset.transfer.domain.gateway.TransferRuleGateway;
import com.yss.valset.transfer.domain.gateway.TransferRouteGateway;
import com.yss.valset.transfer.domain.model.MatchResult;
import com.yss.valset.transfer.domain.model.ProbeResult;
import com.yss.valset.transfer.domain.model.RecognitionContext;
import com.yss.valset.transfer.domain.model.RuleContext;
import com.yss.valset.transfer.domain.model.RuleDefinition;
import com.yss.valset.transfer.domain.model.RuleEvaluationResult;
import com.yss.valset.transfer.domain.model.TransferRoute;
import com.yss.valset.transfer.domain.model.config.TransferConfigKeys;
import com.yss.valset.transfer.domain.plugin.RouteMatchPlugin;
import com.yss.valset.transfer.domain.rule.RuleEngine;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 基于脚本的路由匹配插件。
 */
@Component
public class ScriptRouteMatchPlugin implements RouteMatchPlugin {

    private final TransferRuleGateway transferRuleGateway;
    private final TransferRouteGateway transferRouteGateway;
    private final RuleEngine ruleEngine;

    public ScriptRouteMatchPlugin(TransferRuleGateway transferRuleGateway,
                                 TransferRouteGateway transferRouteGateway,
                                 RuleEngine ruleEngine) {
        this.transferRuleGateway = transferRuleGateway;
        this.transferRouteGateway = transferRouteGateway;
        this.ruleEngine = ruleEngine;
    }

    @Override
    public String type() {
        return "SCRIPT_RULE";
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public boolean supports(RecognitionContext context) {
        return true;
    }

    @Override
    public MatchResult match(RecognitionContext context, ProbeResult probeResult) {
        List<RuleDefinition> ruleDefinitions = transferRuleGateway.listEnabledRules();
        boolean matchedAnyRule = false;
        for (RuleDefinition ruleDefinition : ruleDefinitions) {
            RuleContext ruleContext = new RuleContext(context, probeResult, Map.of());
            RuleEvaluationResult evaluationResult = evaluate(ruleDefinition, ruleContext);
            if (!evaluationResult.matched()) {
                continue;
            }
            matchedAnyRule = true;
            List<TransferRoute> routes = transferRouteGateway.listRoutes(
                    null,
                    context == null || context.sourceType() == null ? null : context.sourceType().name(),
                    context == null ? null : context.sourceCode(),
                    ruleDefinition.ruleId(),
                    null,
                    null,
                    null,
                    null
            );
            if (!routes.isEmpty()) {
                return new MatchResult(true, routes, evaluationResult.message());
            }
        }
        if (matchedAnyRule) {
            return new MatchResult(true, List.of(), "规则已命中，但未找到对应的手动分拣路由配置");
        }
        return new MatchResult(false, List.of(), "未匹配到可用规则");
    }

    private RuleEvaluationResult evaluate(RuleDefinition ruleDefinition, RuleContext ruleContext) {
        if (ruleDefinition == null) {
            return new RuleEvaluationResult(false, List.of(), "规则为空");
        }
        String matchStrategy = ruleDefinition.matchStrategy();
        if (matchStrategy == null || matchStrategy.isBlank() || "ALL".equalsIgnoreCase(matchStrategy)) {
            return new RuleEvaluationResult(true, List.of(), "默认全部命中，跳过规则脚本");
        }
        if ("SCRIPT_RULE".equalsIgnoreCase(matchStrategy)) {
            return ruleEngine.evaluate(ruleDefinition, ruleContext);
        }
        return new RuleEvaluationResult(false, List.of(), "不支持的匹配策略：" + matchStrategy);
    }

}
