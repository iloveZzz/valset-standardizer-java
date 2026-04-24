package com.yss.valset.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 分拣路由变更返回结果。
 */
@Data
@Builder
public class TransferRouteMutationResponse {

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
     * 路由详情。
     */
    private TransferRouteViewDTO route;
}
