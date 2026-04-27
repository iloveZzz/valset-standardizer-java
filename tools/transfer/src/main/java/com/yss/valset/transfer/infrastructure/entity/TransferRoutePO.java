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
    private String routeId;

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
     * 来源编码。
     */
    @TableField("source_code")
    private String sourceCode;

    /**
     * 规则主键。
     */
    @TableField("rule_id")
    private String ruleId;

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
