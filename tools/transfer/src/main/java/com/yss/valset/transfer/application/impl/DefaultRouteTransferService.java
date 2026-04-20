package com.yss.valset.transfer.application.impl;

import com.yss.valset.transfer.application.port.RouteTransferUseCase;
import com.yss.valset.transfer.domain.gateway.TransferObjectGateway;
import com.yss.valset.transfer.domain.gateway.TransferRouteGateway;
import com.yss.valset.transfer.domain.gateway.TransferRuleGateway;
import com.yss.valset.transfer.domain.model.ProbeResult;
import com.yss.valset.transfer.domain.model.RecognitionContext;
import com.yss.valset.transfer.domain.model.RuleContext;
import com.yss.valset.transfer.domain.model.RuleDefinition;
import com.yss.valset.transfer.domain.model.RuleEvaluationResult;
import com.yss.valset.transfer.domain.model.TargetType;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferRoute;
import com.yss.valset.transfer.domain.model.TransferStatus;
import com.yss.valset.transfer.domain.rule.RuleEngine;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 默认文件路由应用服务。
 */
@Service
public class DefaultRouteTransferService implements RouteTransferUseCase {

    private final TransferObjectGateway transferObjectGateway;
    private final TransferRuleGateway transferRuleGateway;
    private final TransferRouteGateway transferRouteGateway;
    private final RuleEngine ruleEngine;

    public DefaultRouteTransferService(
            TransferObjectGateway transferObjectGateway,
            TransferRuleGateway transferRuleGateway,
            TransferRouteGateway transferRouteGateway,
            RuleEngine ruleEngine
    ) {
        this.transferObjectGateway = transferObjectGateway;
        this.transferRuleGateway = transferRuleGateway;
        this.transferRouteGateway = transferRouteGateway;
        this.ruleEngine = ruleEngine;
    }

    @Override
    public void execute(Long transferId) {
        TransferObject transferObject = transferObjectGateway.findById(transferId)
                .orElseThrow(() -> new IllegalStateException("未找到文件记录，transferId=" + transferId));
        RecognitionContext context = toRecognitionContext(transferObject);
        ProbeResult probeResult = new ProbeResult(true, transferObject.extension(), transferObject.fileMeta());
        List<RuleDefinition> ruleDefinitions = transferRuleGateway.listEnabledRules();
        for (RuleDefinition ruleDefinition : ruleDefinitions) {
            RuleContext ruleContext = new RuleContext(context, probeResult, Map.of());
            RuleEvaluationResult evaluationResult = ruleEngine.evaluate(ruleDefinition, ruleContext);
            if (!evaluationResult.matched()) {
                continue;
            }
            TransferRoute route = buildRoute(transferObject, ruleDefinition, evaluationResult);
            transferRouteGateway.save(route);
            return;
        }
        throw new IllegalStateException("未匹配到可用的分拣规则，transferId=" + transferId);
    }

    private TransferRoute buildRoute(
            TransferObject transferObject,
            RuleDefinition ruleDefinition,
            RuleEvaluationResult evaluationResult
    ) {
        Map<String, Object> ruleMeta = ruleDefinition.ruleMeta();
        TargetType targetType = TargetType.valueOf(String.valueOf(ruleMeta.getOrDefault("targetType", TargetType.S3.name())));
        return new TransferRoute(
                null,
                transferObject.transferId(),
                ruleDefinition.ruleId(),
                targetType,
                String.valueOf(ruleMeta.getOrDefault("targetCode", "default-target")),
                String.valueOf(ruleMeta.getOrDefault("targetPath", "/transfer/inbox")),
                String.valueOf(ruleMeta.getOrDefault("renamePattern", "${fileName}")),
                TransferStatus.ROUTED,
                Map.of(
                        "ruleMessage", evaluationResult.message(),
                        "matchStrategy", ruleDefinition.matchStrategy()
                )
        );
    }

    private RecognitionContext toRecognitionContext(TransferObject transferObject) {
        Map<String, Object> fileMeta = transferObject.fileMeta() == null ? Map.of() : transferObject.fileMeta();
        return new RecognitionContext(
                resolveSourceType(fileMeta),
                String.valueOf(fileMeta.getOrDefault("sourceCode", transferObject.sourceId())),
                transferObject.originalName(),
                transferObject.mimeType(),
                transferObject.sizeBytes(),
                null,
                null,
                transferObject.localTempPath(),
                fileMeta
        );
    }

    private com.yss.valset.transfer.domain.model.SourceType resolveSourceType(Map<String, Object> fileMeta) {
        Object raw = fileMeta.get("sourceType");
        if (raw == null) {
            return null;
        }
        try {
            return com.yss.valset.transfer.domain.model.SourceType.valueOf(String.valueOf(raw));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
