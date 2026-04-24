package com.yss.valset.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 文件来源变更返回结果。
 */
@Data
@Builder
public class TransferSourceMutationResponse {

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
     * 文件来源详情。
     */
    private TransferSourceViewDTO source;
}
