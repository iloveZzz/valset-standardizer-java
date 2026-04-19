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
    private ParseRuleProfileViewDTO profile;
    private List<ParseRuleDefinitionViewDTO> rules;
    private List<ParseRuleCaseViewDTO> cases;
    private List<ParseRulePublishLogViewDTO> publishLogs;
}
