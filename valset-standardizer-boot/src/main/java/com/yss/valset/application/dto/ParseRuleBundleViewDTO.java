package com.yss.valset.application.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 解析模板完整视图。
 */
@Data
@Builder
public class ParseRuleBundleViewDTO {
    /**
     * 模板详情。
     */
    private ParseRuleProfileViewDTO profile;
    /**
     * 规则步骤列表。
     */
    private List<ParseRuleDefinitionViewDTO> rules;
    /**
     * 回归样例列表。
     */
    private List<ParseRuleCaseViewDTO> cases;
    /**
     * 发布日志列表。
     */
    private List<ParseRulePublishLogViewDTO> publishLogs;
}
