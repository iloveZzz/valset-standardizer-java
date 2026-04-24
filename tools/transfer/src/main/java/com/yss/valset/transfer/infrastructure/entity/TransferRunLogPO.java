package com.yss.valset.transfer.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件收发运行日志持久化实体。
 */
@Data
@TableName("t_transfer_run_log")
public class TransferRunLogPO {

    /**
     * 运行日志主键。
     */
    @TableId(value = "run_log_id", type = IdType.ASSIGN_ID)
    private String runLogId;

    /**
     * 来源主键。
     */
    @TableField("source_id")
    private String sourceId;

    /**
     * 来源类型。
     */
    @TableField("source_type")
    private String sourceType;

    /**
     * 来源编码。
     */
    @TableField("source_code")
    private String sourceCode;

    /**
     * 来源名称。
     */
    @TableField("source_name")
    private String sourceName;

    /**
     * 文件主键。
     */
    @TableField("transfer_id")
    private String transferId;

    /**
     * 路由主键。
     */
    @TableField("route_id")
    private String routeId;

    /**
     * 触发类型。
     */
    @TableField("trigger_type")
    private String triggerType;

    /**
     * 运行阶段。
     */
    @TableField("run_stage")
    private String runStage;

    /**
     * 运行状态。
     */
    @TableField("run_status")
    private String runStatus;

    /**
     * 运行说明。
     */
    @TableField("log_message")
    private String logMessage;

    /**
     * 错误信息。
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 创建时间。
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
