package com.yss.valset.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工作流接口错误响应。
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowErrorDTO {

    private String code;

    private String message;

    private String detail;

    private String workflowCode;

    private Integer workflowVersionNo;

    private String instanceId;
}
