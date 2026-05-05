package com.yss.valset.task.application.dto.workflow;

import lombok.Data;

/**
 * 工作流版本差异项。
 */
@Data
public class WorkflowVersionDiffItemDTO {

    private String path;
    private String leftValue;
    private String rightValue;
    private String changeType;
}
