package com.yss.valset.transfer.infrastructure.plugin;

import com.yss.valset.transfer.domain.gateway.TransferRuleGateway;
import com.yss.valset.transfer.domain.model.MatchResult;
import com.yss.valset.transfer.domain.model.ProbeResult;
import com.yss.valset.transfer.domain.model.RecognitionContext;
import com.yss.valset.transfer.domain.model.RuleContext;
import com.yss.valset.transfer.domain.model.RuleDefinition;
import com.yss.valset.transfer.domain.model.RuleEvaluationResult;
import com.yss.valset.transfer.domain.model.TargetType;
import com.yss.valset.transfer.domain.model.TransferRoute;
import com.yss.valset.transfer.domain.model.TransferStatus;
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
    private final RuleEngine ruleEngine;

    public ScriptRouteMatchPlugin(TransferRuleGateway transferRuleGateway, RuleEngine ruleEngine) {
        this.transferRuleGateway = transferRuleGateway;
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
        for (RuleDefinition ruleDefinition : ruleDefinitions) {
            RuleContext ruleContext = new RuleContext(context, probeResult, Map.of());
            RuleEvaluationResult evaluationResult = ruleEngine.evaluate(ruleDefinition, ruleContext);
            if (!evaluationResult.matched()) {
                continue;
            }
            TransferRoute route = buildRoute(context, ruleDefinition, evaluationResult);
            return new MatchResult(true, List.of(route), evaluationResult.message());
        }
        return new MatchResult(false, List.of(), "未匹配到可用规则");
    }

    private TransferRoute buildRoute(RecognitionContext context, RuleDefinition ruleDefinition, RuleEvaluationResult evaluationResult) {
        Map<String, Object> ruleMeta = ruleDefinition.ruleMeta();
        TargetType targetType = TargetType.valueOf(String.valueOf(ruleMeta.getOrDefault("targetType", TargetType.S3.name())));
        return new TransferRoute(
                null,
                null,
                ruleDefinition.ruleId(),
                targetType,
                String.valueOf(ruleMeta.getOrDefault("targetCode", "default-target")),
                String.valueOf(ruleMeta.getOrDefault("targetPath", "/transfer/inbox")),
                String.valueOf(ruleMeta.getOrDefault("renamePattern", "${fileName}")),
                TransferStatus.ROUTED,
                Map.of(
                        "ruleMessage", evaluationResult.message(),
                        "matchStrategy", ruleDefinition.matchStrategy(),
                        "sourceType", context.sourceType() == null ? null : context.sourceType().name(),
                        "sourceCode", context.sourceCode(),
                        "detectedType", probeType(context)
                )
        );
    }

    private String probeType(RecognitionContext context) {
        if (context.mailId() != null && !context.mailId().isBlank()) {
            return "EMAIL_ATTACHMENT";
        }
        return context.mimeType();
    }
}
