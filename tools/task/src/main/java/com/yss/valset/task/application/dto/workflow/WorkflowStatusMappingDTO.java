package com.yss.valset.task.application.dto.workflow;

import lombok.Data;

/**
 * 工作流状态映射视图。
 */
@Data
public class WorkflowStatusMappingDTO {

    private String mappingId;
    private String workflowId;
    private String sourceType;
    private String sourceStatus;
    private String targetStatus;
    private String statusLabel;
}
