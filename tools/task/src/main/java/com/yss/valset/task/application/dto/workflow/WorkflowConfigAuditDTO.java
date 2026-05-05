package com.yss.valset.task.application.dto.workflow;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流配置审计视图。
 */
@Data
public class WorkflowConfigAuditDTO {

    private String auditId;

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
