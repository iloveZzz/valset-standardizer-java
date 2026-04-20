package com.yss.valset.transfer.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 投递目标持久化实体。
 */
@Data
@TableName("t_transfer_target")
public class TransferTargetPO {

    @TableId(value = "target_id", type = IdType.ASSIGN_ID)
    private Long targetId;

    @TableField("target_code")
    private String targetCode;

    @TableField("target_name")
    private String targetName;

    @TableField("target_type")
    private String targetType;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("target_path_template")
    private String targetPathTemplate;

    @TableField("connection_config_json")
    private String connectionConfigJson;

    @TableField("target_meta_json")
    private String targetMetaJson;
}
