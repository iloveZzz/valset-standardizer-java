package com.yss.valset.task.application.command.workflow;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流配置审计记录命令。
 */
@Data
public class WorkflowConfigAuditRecordCommand {

    private String workflowId;

    private String workflowCode;

    private Integer versionNo;

    private String actionType;

    private String actionResult;

    private String operatorName;

    private String operatorId;

    private String beforeJson;

    private String afterJson;

    private String remark;

    private LocalDateTime createdAt;
}
