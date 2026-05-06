package com.yss.valset.workflow.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yss.valset.workflow.model.EtlPlatformType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流定义持久化实体。
 */
@Data
@TableName("t_etl_workflow_definition")
public class WorkflowDefinitionPO {

    @TableId(value = "workflow_id", type = IdType.INPUT)
    private String workflowId;

    @TableField("workflow_code")
    private String workflowCode;

    @TableField("workflow_name")
    private String workflowName;

    @TableField("workflow_version_no")
    private Integer workflowVersionNo;

    @TableField("platform_type")
    private EtlPlatformType platformType;

    @TableField("description")
    private String description;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
