package com.yss.valset.application.dto.workflow;

import lombok.Data;

/**
 * 工作流执行上下文。
 */
@Data
public class WorkflowExecutionContextDTO {

    private String workflowId;
    private String workflowCode;
    private Integer workflowVersionNo;
    private String workflowStageCode;
    private String workflowStageName;
    private String workflowStageDescription;
    private String engineType;
    private String externalRef;
    private String configJson;
    private String bindingId;
    private Boolean bindingResolved;
}
