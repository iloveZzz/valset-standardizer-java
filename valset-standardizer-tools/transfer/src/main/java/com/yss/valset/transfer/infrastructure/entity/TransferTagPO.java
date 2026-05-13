package com.yss.valset.transfer.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 标签配置持久化实体。
 */
@Data
@TableName("t_transfer_tag")
public class TransferTagPO {

    @TableId(value = "tag_id", type = IdType.ASSIGN_ID)
    private String tagId;

    @TableField("tag_code")
    private String tagCode;

    @TableField("tag_name")
    private String tagName;

    @TableField("tag_value")
    private String tagValue;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("priority")
    private Integer priority;

    @TableField("match_strategy")
    private String matchStrategy;

    @TableField("script_language")
    private String scriptLanguage;

    @TableField("script_body")
    private String scriptBody;

    @TableField("regex_pattern")
    private String regexPattern;

    @TableField("tag_meta_json")
    private String tagMetaJson;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
