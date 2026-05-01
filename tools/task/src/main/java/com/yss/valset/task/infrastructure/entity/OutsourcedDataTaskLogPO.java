package com.yss.valset.task.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 委外数据任务日志持久化实体。
 */
@Data
@TableName("t_outsourced_data_task_log")
public class OutsourcedDataTaskLogPO {

    @TableId(value = "log_id", type = IdType.ASSIGN_ID)
    private String logId;

    @TableField("batch_id")
    private String batchId;

    @TableField("step_id")
    private String stepId;

    @TableField("stage")
    private String stage;

    @TableField("log_level")
    private String logLevel;

    @TableField("message")
    private String message;

    @TableField("occurred_at")
    private LocalDateTime occurredAt;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
