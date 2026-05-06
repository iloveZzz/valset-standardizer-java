package com.yss.valset.task.infrastructure.entity.workflow;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_outsourced_workflow_definition")
public class OutsourcedWorkflowDefinitionPO {

    @TableId(value = "workflow_id", type = IdType.ASSIGN_ID)
    private String workflowId;

    @TableField("workflow_code")
    private String workflowCode;

    @TableField("workflow_name")
    private String workflowName;

    @TableField("parse_fallback_stage")
    private String parseFallbackStage;

    @TableField("workflow_fallback_stage")
    private String workflowFallbackStage;

    @TableField("version_no")
    private Integer versionNo;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("status")
    private String status;

    @TableField("description")
    private String description;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
