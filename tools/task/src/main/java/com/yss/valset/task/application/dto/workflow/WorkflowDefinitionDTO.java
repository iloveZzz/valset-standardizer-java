package com.yss.valset.task.application.dto.workflow;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 工作流定义视图。
 */
@Data
public class WorkflowDefinitionDTO {

    private String workflowId;
    private String workflowCode;
    private String workflowName;
    private String businessType;
    private String engineType;
    private String parseFallbackStage;
    private String workflowFallbackStage;
    private Integer versionNo;
    private Boolean enabled;
    private String status;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<WorkflowStageDTO> stages = new ArrayList<>();
    private List<WorkflowStatusMappingDTO> statusMappings = new ArrayList<>();
    private List<WorkflowExecutorBindingDTO> executorBindings = new ArrayList<>();
    private List<String> ignoredParseLifecycleStages = new ArrayList<>();
    private List<String> ignoredWorkflowTaskTypes = new ArrayList<>();
}
