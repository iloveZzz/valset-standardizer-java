package com.yss.valset.task.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 委外数据任务阶段持久化实体。
 */
@Data
@TableName("t_outsourced_data_task_step")
public class OutsourcedDataTaskStepPO {

    @TableId(value = "step_id", type = IdType.ASSIGN_ID)
    private String stepId;

    @TableField("batch_id")
    private String batchId;

    @TableField("stage")
    private String stage;

    @TableField("task_id")
    private String taskId;

    @TableField("task_type")
    private String taskType;

    @TableField("run_no")
    private Integer runNo;

    @TableField("current_flag")
    private Boolean currentFlag;

    @TableField("trigger_mode")
    private String triggerMode;

    @TableField("status")
    private String status;

    @TableField("progress")
    private Integer progress;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("ended_at")
    private LocalDateTime endedAt;

    @TableField("duration_ms")
    private Long durationMs;

    @TableField("input_summary")
    private String inputSummary;

    @TableField("output_summary")
    private String outputSummary;

    @TableField("error_code")
    private String errorCode;

    @TableField("error_message")
    private String errorMessage;

    @TableField("log_ref")
    private String logRef;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
