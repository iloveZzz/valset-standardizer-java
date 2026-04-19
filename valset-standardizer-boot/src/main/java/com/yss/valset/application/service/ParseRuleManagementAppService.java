package com.yss.valset.application.service;

import com.yss.valset.application.command.ParseRuleProfileUpsertCommand;
import com.yss.valset.application.command.ParseRulePublishCommand;
import com.yss.valset.application.command.ParseRuleRollbackCommand;
import com.yss.valset.application.dto.ParseRuleBundleViewDTO;
import com.yss.valset.application.dto.ParseRuleMutationResponse;
import com.yss.valset.application.dto.ParseRuleProfileViewDTO;
import com.yss.valset.application.dto.ParseRuleValidationViewDTO;

import java.util.List;

/**
 * 解析模板管理服务。
 */
public interface ParseRuleManagementAppService {

    List<ParseRuleProfileViewDTO> listProfiles(String status, String profileCode, Integer limit);

    ParseRuleBundleViewDTO getProfile(Long profileId);

    ParseRuleMutationResponse upsertProfile(ParseRuleProfileUpsertCommand command);

    ParseRuleValidationViewDTO validateProfile(Long profileId);

    ParseRuleMutationResponse publishProfile(Long profileId, ParseRulePublishCommand command);

    ParseRuleMutationResponse rollbackProfile(Long profileId, ParseRuleRollbackCommand command);
}
