package com.yss.valset.task.application.port.workflow;

import com.yss.valset.task.application.command.workflow.WorkflowConfigAuditRecordCommand;
import com.yss.cloud.dto.response.PageResult;
import com.yss.valset.task.application.command.workflow.WorkflowConfigAuditQueryCommand;
import com.yss.valset.task.application.dto.workflow.WorkflowConfigAuditDTO;

import java.util.Optional;

/**
 * 工作流配置审计持久化网关。
 */
public interface WorkflowConfigAuditGateway {

    void record(WorkflowConfigAuditRecordCommand command);

    PageResult<WorkflowConfigAuditDTO> pageAudits(WorkflowConfigAuditQueryCommand query);

    Optional<WorkflowConfigAuditDTO> findById(String auditId);
}
