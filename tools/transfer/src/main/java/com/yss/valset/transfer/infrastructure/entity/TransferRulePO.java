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

    @TableId(value = "rule_id", type = IdType.ASSIGN_ID)
    private Long ruleId;

    @TableField("rule_code")
    private String ruleCode;

    @TableField("rule_name")
    private String ruleName;

    @TableField("rule_version")
    private String ruleVersion;

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

    @TableField("effective_from")
    private LocalDateTime effectiveFrom;

    @TableField("effective_to")
    private LocalDateTime effectiveTo;

    @TableField("rule_meta_json")
    private String ruleMetaJson;
}
