package com.yss.valset.task.application.service.workflow;

import com.yss.cloud.dto.response.PageResult;
import com.yss.valset.task.application.command.workflow.WorkflowConfigAuditQueryCommand;
import com.yss.valset.task.application.command.workflow.WorkflowConfigQueryCommand;
import com.yss.valset.task.application.command.workflow.WorkflowConfigSaveCommand;
import com.yss.valset.task.application.dto.workflow.WorkflowConfigAuditDTO;
import com.yss.valset.task.application.dto.workflow.WorkflowDefinitionDTO;
import com.yss.valset.task.application.dto.workflow.WorkflowVersionDiffDTO;

/**
 * 工作流配置应用服务。
 */
public interface WorkflowConfigService {

    PageResult<WorkflowDefinitionDTO> pageDefinitions(WorkflowConfigQueryCommand query);

    WorkflowDefinitionDTO getDefinition(String workflowId);

    WorkflowDefinitionDTO getActiveDefinition(String workflowCode);

    PageResult<WorkflowConfigAuditDTO> pageAuditRecords(WorkflowConfigAuditQueryCommand query);

    WorkflowConfigAuditDTO getAuditRecord(String auditId);

    WorkflowDefinitionDTO saveDraft(WorkflowConfigSaveCommand command);

    WorkflowDefinitionDTO importConfig(WorkflowConfigSaveCommand command);

    WorkflowDefinitionDTO copyVersion(String workflowId);

    WorkflowDefinitionDTO rollbackVersion(String workflowId, String sourceWorkflowId);

    void validate(WorkflowConfigSaveCommand command);

    WorkflowDefinitionDTO exportConfig(String workflowId);

    WorkflowVersionDiffDTO compareVersions(String leftWorkflowId, String rightWorkflowId);

    WorkflowDefinitionDTO publish(String workflowId);

    WorkflowDefinitionDTO disable(String workflowId);
}
