package com.yss.valset.task.application.dto.workflow;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 工作流版本差异视图。
 */
@Data
public class WorkflowVersionDiffDTO {

    private String leftWorkflowId;
    private String rightWorkflowId;
    private Integer leftVersionNo;
    private Integer rightVersionNo;
    private String leftWorkflowCode;
    private String rightWorkflowCode;
    private List<WorkflowVersionDiffItemDTO> items = new ArrayList<>();
}
