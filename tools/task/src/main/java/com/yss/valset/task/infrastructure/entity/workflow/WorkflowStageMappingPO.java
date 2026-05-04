package com.yss.valset.task.infrastructure.entity.workflow;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流阶段事件映射持久化实体。
 */
@Data
@TableName("t_workflow_stage_mapping")
public class WorkflowStageMappingPO {

    @TableId(value = "mapping_id", type = IdType.ASSIGN_ID)
    private String mappingId;

    @TableField("workflow_id")
    private String workflowId;

    @TableField("stage_id")
    private String stageId;

    @TableField("mapping_type")
    private String mappingType;

    @TableField("mapping_value")
    private String mappingValue;

    @TableField("ignored")
    private Boolean ignored;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
