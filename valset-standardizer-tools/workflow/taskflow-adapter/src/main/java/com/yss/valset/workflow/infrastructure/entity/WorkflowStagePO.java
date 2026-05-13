package com.yss.valset.workflow.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流阶段持久化实体。
 */
@Data
@TableName("t_etl_workflow_stage")
public class WorkflowStagePO {

    @TableId(value = "stage_id", type = IdType.INPUT)
    private String stageId;

    @TableField("workflow_id")
    private String workflowId;

    @TableField("stage_code")
    private String stageCode;

    @TableField("stage_name")
    private String stageName;

    @TableField("stage_order")
    private Integer stageOrder;

    @TableField("description")
    private String description;

    @TableField("retryable")
    private Boolean retryable;

    @TableField("timeout_seconds")
    private Integer timeoutSeconds;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
