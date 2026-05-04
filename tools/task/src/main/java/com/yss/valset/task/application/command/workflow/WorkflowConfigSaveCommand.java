package com.yss.valset.task.application.command.workflow;

import com.yss.valset.task.application.dto.workflow.WorkflowExecutorBindingDTO;
import com.yss.valset.task.application.dto.workflow.WorkflowStageDTO;
import com.yss.valset.task.application.dto.workflow.WorkflowStatusMappingDTO;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 工作流配置保存命令。
 */
@Data
public class WorkflowConfigSaveCommand {

    private String workflowId;

    @NotBlank(message = "工作流编码不能为空")
    private String workflowCode;

    @NotBlank(message = "工作流名称不能为空")
    private String workflowName;

    private String businessType;
    private String engineType;
    private String parseFallbackStage;
    private String workflowFallbackStage;
    private Integer versionNo;
    private Boolean enabled;
    private String status;
    private String description;
    private List<WorkflowStageDTO> stages = new ArrayList<>();
    private List<WorkflowStatusMappingDTO> statusMappings = new ArrayList<>();
    private List<WorkflowExecutorBindingDTO> executorBindings = new ArrayList<>();
    private List<String> ignoredParseLifecycleStages = new ArrayList<>();
    private List<String> ignoredWorkflowTaskTypes = new ArrayList<>();
}
