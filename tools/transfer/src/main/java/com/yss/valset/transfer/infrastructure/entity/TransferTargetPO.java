package com.yss.valset.transfer.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 投递目标持久化实体。
 */
@Data
@TableName("t_transfer_target")
public class TransferTargetPO {

    /**
     * 目标主键。
     */
    @TableId(value = "target_id", type = IdType.ASSIGN_ID)
    private Long targetId;

    /**
     * 目标编码。
     */
    @TableField("target_code")
    private String targetCode;

    /**
     * 目标名称。
     */
    @TableField("target_name")
    private String targetName;

    /**
     * 目标类型。
     */
    @TableField("target_type")
    private String targetType;

    /**
     * 是否启用。
     */
    @TableField("enabled")
    private Boolean enabled;

    /**
     * 目标路径模板。
     */
    @TableField("target_path_template")
    private String targetPathTemplate;

    /**
     * 连接配置 JSON。
     */
    @TableField("connection_config_json")
    private String connectionConfigJson;

    /**
     * 目标元数据 JSON。
     */
    @TableField("target_meta_json")
    private String targetMetaJson;

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
