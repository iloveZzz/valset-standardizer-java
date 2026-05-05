package com.yss.valset.task.infrastructure.entity.workflow;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流配置审计持久化实体。
 */
@Data
@TableName("t_workflow_config_audit")
public class WorkflowConfigAuditPO {

    @TableId(value = "audit_id", type = IdType.ASSIGN_ID)
    private String auditId;

    @TableField("workflow_id")
    private String workflowId;

    @TableField("workflow_code")
    private String workflowCode;

    @TableField("version_no")
    private Integer versionNo;

    @TableField("action_type")
    private String actionType;

    @TableField("action_result")
    private String actionResult;

    @TableField("operator_name")
    private String operatorName;

    @TableField("operator_id")
    private String operatorId;

    @TableField("before_json")
    private String beforeJson;

    @TableField("after_json")
    private String afterJson;

    @TableField("remark")
    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
