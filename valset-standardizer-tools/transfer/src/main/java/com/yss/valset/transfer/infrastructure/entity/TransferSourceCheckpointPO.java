package com.yss.valset.transfer.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 来源检查点持久化实体。
 */
@Data
@TableName("t_transfer_source_checkpoint")
public class TransferSourceCheckpointPO {

    /**
     * 检查点主键。
     */
    @TableId(value = "checkpoint_id", type = IdType.ASSIGN_ID)
    private String checkpointId;

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
     * 检查点键。
     */
    @TableField("checkpoint_key")
    private String checkpointKey;

    /**
     * 检查点值。
     */
    @TableField("checkpoint_value")
    private String checkpointValue;

    /**
     * 检查点 JSON。
     */
    @TableField("checkpoint_json")
    private String checkpointJson;

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
