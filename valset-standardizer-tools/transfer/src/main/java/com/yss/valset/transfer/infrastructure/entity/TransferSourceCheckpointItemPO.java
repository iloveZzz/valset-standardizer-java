package com.yss.valset.transfer.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 来源检查点去重记录持久化实体。
 */
@Data
@TableName("t_transfer_source_checkpoint_item")
public class TransferSourceCheckpointItemPO {

    /**
     * 去重记录主键。
     */
    @TableId(value = "checkpoint_item_id", type = IdType.ASSIGN_ID)
    private String checkpointItemId;

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
     * 去重键。
     */
    @TableField("item_key")
    private String itemKey;

    /**
     * 来源引用标识。
     */
    @TableField("item_ref")
    private String itemRef;

    /**
     * 条目名称。
     */
    @TableField("item_name")
    private String itemName;

    /**
     * 条目大小。
     */
    @TableField("item_size")
    private Long itemSize;

    /**
     * 条目 MIME 类型。
     */
    @TableField("item_mime_type")
    private String itemMimeType;

    /**
     * 条目指纹。
     */
    @TableField("item_fingerprint")
    private String itemFingerprint;

    /**
     * 条目元数据 JSON。
     */
    @TableField("item_meta_json")
    private String itemMetaJson;

    /**
     * 触发类型。
     */
    @TableField("trigger_type")
    private String triggerType;

    /**
     * 处理时间。
     */
    @TableField("processed_at")
    private LocalDateTime processedAt;

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
