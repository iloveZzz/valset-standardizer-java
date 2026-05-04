package com.yss.valset.task.application.service.workflow;

import com.yss.cloud.dto.response.PageResult;
import com.yss.valset.task.application.command.workflow.WorkflowConfigQueryCommand;
import com.yss.valset.task.application.command.workflow.WorkflowConfigSaveCommand;
import com.yss.valset.task.application.dto.workflow.WorkflowDefinitionDTO;

/**
 * 工作流配置应用服务。
 */
public interface WorkflowConfigService {

    PageResult<WorkflowDefinitionDTO> pageDefinitions(WorkflowConfigQueryCommand query);

    WorkflowDefinitionDTO getDefinition(String workflowId);

    WorkflowDefinitionDTO getActiveDefinition(String workflowCode);

    WorkflowDefinitionDTO saveDraft(WorkflowConfigSaveCommand command);

    WorkflowDefinitionDTO publish(String workflowId);

    WorkflowDefinitionDTO disable(String workflowId);
}
