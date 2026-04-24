package com.yss.valset.transfer.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件来源持久化实体。
 */
@Data
@TableName("t_transfer_source")
public class TransferSourcePO {

    /**
     * 来源主键。
     */
    @TableId(value = "source_id", type = IdType.ASSIGN_ID)
    private String sourceId;

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
     * 来源类型。
     */
    @TableField("source_type")
    private String sourceType;

    /**
     * 是否启用。
     */
    @TableField("enabled")
    private Boolean enabled;

    /**
     * 轮询表达式。
     */
    @TableField("poll_cron")
    private String pollCron;

    /**
     * 连接配置 JSON。
     */
    @TableField("connection_config_json")
    private String connectionConfigJson;

    /**
     * 来源元数据 JSON。
     */
    @TableField("source_meta_json")
    private String sourceMetaJson;

    /**
     * 收取状态。
     */
    @TableField("ingest_status")
    private String ingestStatus;

    /**
     * 收取开始时间。
     */
    @TableField("ingest_started_at")
    private LocalDateTime ingestStartedAt;

    /**
     * 收取完成时间。
     */
    @TableField("ingest_finished_at")
    private LocalDateTime ingestFinishedAt;

    /**
     * 创建时间。
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 修改时间。
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
