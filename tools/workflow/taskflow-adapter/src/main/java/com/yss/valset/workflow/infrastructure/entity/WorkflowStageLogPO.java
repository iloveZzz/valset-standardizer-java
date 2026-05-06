package com.yss.valset.workflow.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yss.valset.workflow.model.WorkflowStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流阶段日志持久化实体。
 */
@Data
@TableName("t_etl_workflow_stage_log")
public class WorkflowStageLogPO {

    @TableId(value = "log_id", type = IdType.INPUT)
    private String logId;

    @TableField("instance_id")
    private String instanceId;

    @TableField("workflow_id")
    private String workflowId;

    @TableField("workflow_code")
    private String workflowCode;

    @TableField("workflow_version_no")
    private Integer workflowVersionNo;

    @TableField("stage_code")
    private String stageCode;

    @TableField("stage_name")
    private String stageName;

    @TableField("stage_order")
    private Integer stageOrder;

    @TableField("status")
    private WorkflowStatus status;

    @TableField("raw_status")
    private String rawStatus;

    @TableField("message")
    private String message;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    @TableField("payload_json")
    private String payloadJson;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
