package com.yss.valset.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 路由规则变更返回结果。
 */
@Data
@Builder
public class TransferRuleMutationResponse {

    /**
     * 操作类型。
     */
    private String operation;
    /**
     * 提示消息。
     */
    private String message;
    /**
     * 关联的表单模板名。
     */
    private String formTemplateName;
    /**
     * 路由规则详情。
     */
    private TransferRuleViewDTO rule;
}
