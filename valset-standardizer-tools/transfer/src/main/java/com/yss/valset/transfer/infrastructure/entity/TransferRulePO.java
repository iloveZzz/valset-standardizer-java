package com.yss.valset.transfer.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件规则持久化实体。
 */
@Data
@TableName("t_transfer_rule")
public class TransferRulePO {

    /**
     * 规则主键。
     */
    @TableId(value = "rule_id", type = IdType.ASSIGN_ID)
    private String ruleId;

    /**
     * 规则编码。
     */
    @TableField("rule_code")
    private String ruleCode;

    /**
     * 规则名称。
     */
    @TableField("rule_name")
    private String ruleName;

    /**
     * 规则版本。
     */
    @TableField("rule_version")
    private String ruleVersion;

    /**
     * 是否启用。
     */
    @TableField("enabled")
    private Boolean enabled;

    /**
     * 优先级。
     */
    @TableField("priority")
    private Integer priority;

    /**
     * 匹配策略。
     */
    @TableField("match_strategy")
    private String matchStrategy;

    /**
     * 脚本语言。
     */
    @TableField("script_language")
    private String scriptLanguage;

    /**
     * 脚本文本。
     */
    @TableField("script_body")
    private String scriptBody;

    /**
     * 生效开始时间。
     */
    @TableField("effective_from")
    private LocalDateTime effectiveFrom;

    /**
     * 生效结束时间。
     */
    @TableField("effective_to")
    private LocalDateTime effectiveTo;

    /**
     * 规则元数据 JSON。
     */
    @TableField("rule_meta_json")
    private String ruleMetaJson;
}
