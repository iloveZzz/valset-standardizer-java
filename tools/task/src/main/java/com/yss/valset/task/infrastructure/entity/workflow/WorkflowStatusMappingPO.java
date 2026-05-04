package com.yss.valset.task.infrastructure.entity.workflow;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流状态映射持久化实体。
 */
@Data
@TableName("t_workflow_status_mapping")
public class WorkflowStatusMappingPO {

    @TableId(value = "mapping_id", type = IdType.ASSIGN_ID)
    private String mappingId;

    @TableField("workflow_id")
    private String workflowId;

    @TableField("source_type")
    private String sourceType;

    @TableField("source_status")
    private String sourceStatus;

    @TableField("target_status")
    private String targetStatus;

    @TableField("status_label")
    private String statusLabel;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
