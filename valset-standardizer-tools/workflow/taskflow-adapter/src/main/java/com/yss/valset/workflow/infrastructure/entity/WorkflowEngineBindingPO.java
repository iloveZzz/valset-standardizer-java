package com.yss.valset.workflow.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yss.valset.workflow.model.EtlPlatformType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流引擎绑定持久化实体。
 */
@Data
@TableName("t_etl_workflow_engine_binding")
public class WorkflowEngineBindingPO {

    @TableId(value = "binding_id", type = IdType.INPUT)
    private String bindingId;

    @TableField("workflow_id")
    private String workflowId;

    @TableField("platform_type")
    private EtlPlatformType platformType;

    @TableField("external_workflow_id")
    private String externalWorkflowId;

    @TableField("external_project_code")
    private String externalProjectCode;

    @TableField("external_namespace")
    private String externalNamespace;

    @TableField("external_job_group")
    private String externalJobGroup;

    @TableField("external_job_handler")
    private String externalJobHandler;

    @TableField("config_json")
    private String configJson;

    @TableField("attributes_json")
    private String attributesJson;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
