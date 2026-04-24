package com.yss.valset.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 解析模板变更返回结果。
 */
@Data
@Builder
public class ParseRuleMutationResponse {
    /**
     * 模板详情。
     */
    private ParseRuleProfileViewDTO profile;
    /**
     * 校验结果。
     */
    private ParseRuleValidationViewDTO validation;
    /**
     * 发布日志。
     */
    private ParseRulePublishLogViewDTO publishLog;
}
