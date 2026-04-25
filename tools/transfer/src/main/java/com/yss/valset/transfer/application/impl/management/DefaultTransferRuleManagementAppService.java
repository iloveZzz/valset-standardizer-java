package com.yss.valset.transfer.application.impl.management;

import com.yss.valset.transfer.application.command.TransferRuleUpsertCommand;
import com.yss.valset.transfer.application.dto.TransferRuleMutationResponse;
import com.yss.valset.transfer.application.dto.TransferRuleViewDTO;
import com.yss.valset.transfer.application.service.TransferRuleManagementAppService;
import com.yss.valset.transfer.domain.form.TransferFormTemplateNames;
import com.yss.valset.transfer.domain.gateway.TransferRuleGateway;
import com.yss.valset.transfer.domain.model.RuleDefinition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 默认路由规则管理服务。
 */
@Service
@RequiredArgsConstructor
public class DefaultTransferRuleManagementAppService implements TransferRuleManagementAppService {

    private final TransferRuleGateway transferRuleGateway;

    @Override
    public List<TransferRuleViewDTO> listRules(String ruleCode, Boolean enabled, Integer limit) {
        return transferRuleGateway.listRules(ruleCode, enabled, limit)
                .stream()
                .map(this::toView)
                .toList();
    }

    @Override
    public TransferRuleViewDTO getRule(String ruleId) {
        return toView(transferRuleGateway.findById(ruleId)
                .orElseThrow(() -> new IllegalStateException("未找到路由规则，ruleId=" + ruleId)));
    }

    @Override
    public TransferRuleMutationResponse upsertRule(TransferRuleUpsertCommand command) {
        boolean createMode = command.getRuleId() == null;
        RuleDefinition definition = new RuleDefinition(
                command.getRuleId(),
                command.getRuleCode(),
                command.getRuleName(),
                command.getRuleVersion(),
                Boolean.TRUE.equals(command.getEnabled()),
                command.getPriority() == null ? 10 : command.getPriority(),
                command.getMatchStrategy(),
                command.getScriptLanguage(),
                command.getScriptBody(),
                toInstant(command.getEffectiveFrom()),
                toInstant(command.getEffectiveTo()),
                command.getRuleMeta() == null ? new HashMap<>() : command.getRuleMeta()
        );
        RuleDefinition saved = transferRuleGateway.save(definition);
        return TransferRuleMutationResponse.builder()
                .operation(createMode ? "create" : "update")
                .message("路由规则保存成功")
                .formTemplateName(TransferFormTemplateNames.TRANSFER_RULE)
                .rule(toView(saved))
                .build();
    }

    @Override
    public TransferRuleMutationResponse deleteRule(String ruleId) {
        RuleDefinition existing = transferRuleGateway.findById(ruleId)
                .orElseThrow(() -> new IllegalStateException("未找到路由规则，ruleId=" + ruleId));
        transferRuleGateway.deleteById(ruleId);
        return TransferRuleMutationResponse.builder()
                .operation("delete")
                .message("路由规则删除成功")
                .formTemplateName(TransferFormTemplateNames.TRANSFER_RULE)
                .rule(toView(existing))
                .build();
    }

    private TransferRuleViewDTO toView(RuleDefinition definition) {
        return TransferRuleViewDTO.builder()
                .ruleId(definition.ruleId() == null ? null : String.valueOf(definition.ruleId()))
                .ruleCode(definition.ruleCode())
                .ruleName(definition.ruleName())
                .ruleVersion(definition.ruleVersion())
                .enabled(definition.enabled())
                .priority(definition.priority())
                .matchStrategy(definition.matchStrategy())
                .scriptLanguage(definition.scriptLanguage())
                .scriptBody(definition.scriptBody())
                .effectiveFrom(toLocalDateTime(definition.effectiveFrom()))
                .effectiveTo(toLocalDateTime(definition.effectiveTo()))
                .ruleMeta(definition.ruleMeta())
                .formTemplateName(TransferFormTemplateNames.TRANSFER_RULE)
                .build();
    }

    private Instant toInstant(java.time.LocalDateTime value) {
        return value == null ? null : value.atZone(java.time.ZoneId.systemDefault()).toInstant();
    }

    private java.time.LocalDateTime toLocalDateTime(Instant value) {
        return value == null ? null : java.time.LocalDateTime.ofInstant(value, java.time.ZoneId.systemDefault());
    }
}
