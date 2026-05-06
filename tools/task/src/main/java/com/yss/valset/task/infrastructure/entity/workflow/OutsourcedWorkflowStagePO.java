package com.yss.valset.task.infrastructure.entity.workflow;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_outsourced_workflow_stage")
public class OutsourcedWorkflowStagePO {

    @TableId(value = "stage_id", type = IdType.ASSIGN_ID)
    private String stageId;

    @TableField("workflow_id")
    private String workflowId;

    @TableField("stage_code")
    private String stageCode;

    @TableField("stage_name")
    private String stageName;

    @TableField("stage_description")
    private String stageDescription;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
