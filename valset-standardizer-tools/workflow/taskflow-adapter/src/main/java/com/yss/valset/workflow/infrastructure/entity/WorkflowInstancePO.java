package com.yss.valset.workflow.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yss.valset.workflow.model.EtlPlatformType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流实例持久化实体。
 */
@Data
@TableName("t_etl_workflow_instance")
public class WorkflowInstancePO {

    @TableId(value = "instance_id", type = IdType.INPUT)
    private String instanceId;

    @TableField("workflow_id")
    private String workflowId;

    @TableField("workflow_code")
    private String workflowCode;

    @TableField("workflow_version_no")
    private Integer workflowVersionNo;

    @TableField("platform_type")
    private EtlPlatformType platformType;

    @TableField("business_key")
    private String businessKey;

    @TableField("current_stage_code")
    private String currentStageCode;

    @TableField("external_instance_id")
    private String externalInstanceId;

    @TableField("external_workflow_id")
    private String externalWorkflowId;

    @TableField("status")
    private String status;

    @TableField("raw_status")
    private String rawStatus;

    @TableField("trigger_time")
    private LocalDateTime triggerTime;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    @TableField("message")
    private String message;

    @TableField("context_json")
    private String contextJson;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
