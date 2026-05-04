package com.yss.valset.task.application.port.workflow;

import com.yss.cloud.dto.response.PageResult;
import com.yss.valset.task.application.command.workflow.WorkflowConfigQueryCommand;
import com.yss.valset.task.application.dto.workflow.WorkflowDefinitionDTO;

import java.util.Optional;

/**
 * 工作流配置持久化网关。
 */
public interface WorkflowConfigGateway {

    PageResult<WorkflowDefinitionDTO> pageDefinitions(WorkflowConfigQueryCommand query);

    Optional<WorkflowDefinitionDTO> findById(String workflowId);

    Optional<WorkflowDefinitionDTO> findActiveByCode(String workflowCode);

    WorkflowDefinitionDTO save(WorkflowDefinitionDTO definition);

    void disableOtherVersions(String workflowCode, String keepWorkflowId);

    void updateStatus(String workflowId, String status, Boolean enabled);
}
