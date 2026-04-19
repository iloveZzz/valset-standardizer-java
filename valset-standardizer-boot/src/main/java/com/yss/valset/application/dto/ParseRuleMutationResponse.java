package com.yss.valset.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 解析模板变更返回结果。
 */
@Data
@Builder
public class ParseRuleMutationResponse {
    private ParseRuleProfileViewDTO profile;
    private ParseRuleValidationViewDTO validation;
    private ParseRulePublishLogViewDTO publishLog;
}
