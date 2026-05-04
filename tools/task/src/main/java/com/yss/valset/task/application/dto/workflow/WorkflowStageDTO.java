package com.yss.valset.task.application.dto.workflow;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 工作流阶段视图。
 */
@Data
public class WorkflowStageDTO {

    private String stageId;
    private String workflowId;
    private String stageCode;
    private String stepCode;
    private String stageName;
    private String stepName;
    private String stageDescription;
    private String stepDescription;
    private Integer sortOrder;
    private Boolean retryable;
    private Boolean skippable;
    private Boolean enabled;
    private List<String> taskTypes = new ArrayList<>();
    private List<String> taskStages = new ArrayList<>();
    private List<String> parseLifecycleStages = new ArrayList<>();
}
