package com.yss.valset.transfer.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 文件路由持久化实体。
 */
@Data
@TableName("t_transfer_route")
public class TransferRoutePO {

    @TableId(value = "route_id", type = IdType.ASSIGN_ID)
    private Long routeId;

    @TableField("transfer_id")
    private Long transferId;

    @TableField("rule_id")
    private Long ruleId;

    @TableField("target_type")
    private String targetType;

    @TableField("target_code")
    private String targetCode;

    @TableField("target_path")
    private String targetPath;

    @TableField("rename_pattern")
    private String renamePattern;

    @TableField("route_status")
    private String routeStatus;

    @TableField("route_meta_json")
    private String routeMetaJson;
}
