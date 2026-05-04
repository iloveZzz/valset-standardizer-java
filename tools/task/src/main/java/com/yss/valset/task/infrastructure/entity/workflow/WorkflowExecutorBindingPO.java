package com.yss.valset.task.infrastructure.entity.workflow;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流执行平台绑定持久化实体。
 */
@Data
@TableName("t_workflow_executor_binding")
public class WorkflowExecutorBindingPO {

    @TableId(value = "binding_id", type = IdType.ASSIGN_ID)
    private String bindingId;

    @TableField("workflow_id")
    private String workflowId;

    @TableField("stage_id")
    private String stageId;

    @TableField("engine_type")
    private String engineType;

    @TableField("external_ref")
    private String externalRef;

    @TableField("config_json")
    private String configJson;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
