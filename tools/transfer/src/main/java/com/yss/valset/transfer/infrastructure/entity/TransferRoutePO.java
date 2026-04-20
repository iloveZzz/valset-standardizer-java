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

    /**
     * 路由主键。
     */
    @TableId(value = "route_id", type = IdType.ASSIGN_ID)
    private Long routeId;

    /**
     * 文件主键。
     */
    @TableField("transfer_id")
    private Long transferId;

    /**
     * 规则主键。
     */
    @TableField("rule_id")
    private Long ruleId;

    /**
     * 目标类型。
     */
    @TableField("target_type")
    private String targetType;

    /**
     * 目标编码。
     */
    @TableField("target_code")
    private String targetCode;

    /**
     * 目标路径。
     */
    @TableField("target_path")
    private String targetPath;

    /**
     * 重命名模板。
     */
    @TableField("rename_pattern")
    private String renamePattern;

    /**
     * 路由状态。
     */
    @TableField("route_status")
    private String routeStatus;

    /**
     * 路由元数据 JSON。
     */
    @TableField("route_meta_json")
    private String routeMetaJson;
}
