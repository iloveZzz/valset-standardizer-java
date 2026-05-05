package com.yss.valset.task.application.command.workflow;

import lombok.Data;

/**
 * 工作流配置审计查询命令。
 */
@Data
public class WorkflowConfigAuditQueryCommand {

    private String workflowCode;

    private Integer versionNo;

    private String actionType;

    private String actionResult;

    private Integer pageIndex;

    private Integer pageSize;
}
