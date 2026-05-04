package com.yss.valset.task.application.dto.workflow;

import lombok.Data;

/**
 * 工作流执行平台绑定视图。
 */
@Data
public class WorkflowExecutorBindingDTO {

    private String bindingId;
    private String workflowId;
    private String stageId;
    private String stageCode;
    private String engineType;
    private String externalRef;
    private String configJson;
    private Boolean enabled;
}
