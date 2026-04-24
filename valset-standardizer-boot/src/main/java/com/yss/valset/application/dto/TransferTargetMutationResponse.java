package com.yss.valset.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 投递目标变更返回结果。
 */
@Data
@Builder
public class TransferTargetMutationResponse {

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
     * 投递目标详情。
     */
    private TransferTargetViewDTO target;
}
