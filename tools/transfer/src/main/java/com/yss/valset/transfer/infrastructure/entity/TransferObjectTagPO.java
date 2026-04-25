package com.yss.valset.transfer.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件对象标签结果持久化实体。
 */
@Data
@TableName("t_transfer_object_tag")
public class TransferObjectTagPO {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    @TableField("transfer_id")
    private String transferId;

    @TableField("tag_id")
    private String tagId;

    @TableField("tag_code")
    private String tagCode;

    @TableField("tag_name")
    private String tagName;

    @TableField("tag_value")
    private String tagValue;

    @TableField("match_strategy")
    private String matchStrategy;

    @TableField("match_reason")
    private String matchReason;

    @TableField("matched_field")
    private String matchedField;

    @TableField("matched_value")
    private String matchedValue;

    @TableField("match_snapshot_json")
    private String matchSnapshotJson;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
