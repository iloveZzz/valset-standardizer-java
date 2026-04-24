package com.yss.valset.transfer.application.service;

import com.yss.valset.transfer.application.command.TransferRuleUpsertCommand;
import com.yss.valset.transfer.application.dto.TransferRuleMutationResponse;
import com.yss.valset.transfer.application.dto.TransferRuleViewDTO;

import java.util.List;

/**
 * 路由规则管理服务。
 */
public interface TransferRuleManagementAppService {

    List<TransferRuleViewDTO> listRules(String ruleCode, Boolean enabled, Integer limit);

    TransferRuleViewDTO getRule(String ruleId);

    TransferRuleMutationResponse upsertRule(TransferRuleUpsertCommand command);

    TransferRuleMutationResponse deleteRule(String ruleId);
}
