package com.yss.valset.task.infrastructure.entity.workflow;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流阶段定义持久化实体。
 */
@Data
@TableName("t_workflow_stage")
public class WorkflowStagePO {

    @TableId(value = "stage_id", type = IdType.ASSIGN_ID)
    private String stageId;

    @TableField("workflow_id")
    private String workflowId;

    @TableField("stage_code")
    private String stageCode;

    @TableField("step_code")
    private String stepCode;

    @TableField("stage_name")
    private String stageName;

    @TableField("step_name")
    private String stepName;

    @TableField("stage_description")
    private String stageDescription;

    @TableField("step_description")
    private String stepDescription;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("retryable")
    private Boolean retryable;

    @TableField("skippable")
    private Boolean skippable;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
