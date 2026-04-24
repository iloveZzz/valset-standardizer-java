package com.yss.valset.application.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 解析模板校验视图。
 */
@Data
@Builder
public class ParseRuleValidationViewDTO {
    /**
     * 模板主键。
     */
    private String profileId;
    /**
     * 模板编码。
     */
    private String profileCode;
    /**
     * 模板版本。
     */
    private String version;
    /**
     * 是否可发布。
     */
    private Boolean publishable;
    /**
     * 规则步骤数量。
     */
    private Integer ruleCount;
    /**
     * 已启用规则步骤数量。
     */
    private Integer enabledRuleCount;
    /**
     * 回归样例数量。
     */
    private Integer caseCount;
    /**
     * 校验问题列表。
     */
    private List<String> issues;
}
