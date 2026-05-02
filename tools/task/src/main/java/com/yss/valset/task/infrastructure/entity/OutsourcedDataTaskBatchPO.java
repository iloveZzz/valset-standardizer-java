package com.yss.valset.task.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 估值表解析任务批次持久化实体。
 */
@Data
@TableName("t_outsourced_data_task_batch")
public class OutsourcedDataTaskBatchPO {

    @TableId(value = "batch_id", type = IdType.ASSIGN_ID)
    private String batchId;

    @TableField("batch_name")
    private String batchName;

    @TableField("business_date")
    private LocalDate businessDate;

    @TableField("valuation_date")
    private LocalDate valuationDate;

    @TableField("product_code")
    private String productCode;

    @TableField("product_name")
    private String productName;

    @TableField("manager_name")
    private String managerName;

    @TableField("file_id")
    private String fileId;

    @TableField("filesys_file_id")
    private String filesysFileId;

    @TableField("file_fingerprint")
    private String fileFingerprint;

    @TableField("original_file_name")
    private String originalFileName;

    @TableField("source_type")
    private String sourceType;

    @TableField("current_stage")
    private String currentStage;

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

    @TableField("last_error_code")
    private String lastErrorCode;

    @TableField("last_error_message")
    private String lastErrorMessage;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
