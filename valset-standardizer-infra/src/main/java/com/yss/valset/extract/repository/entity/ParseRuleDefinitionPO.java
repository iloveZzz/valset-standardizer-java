package com.yss.valset.extract.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yss.valset.domain.rule.ParseRuleType;
import lombok.Data;

import javax.persistence.Id;
import java.time.LocalDateTime;

/**
 * 解析规则步骤对象。
 */
@Data
@TableName("t_file_parse_rule_step")
public class ParseRuleDefinitionPO {

    @Id
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("profile_id")
    private Long profileId;

    @TableField("rule_type")
    private ParseRuleType ruleType;

    @TableField("step_name")
    private String stepName;

    @TableField("priority")
    private Integer priority;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("expr_text")
    private String exprText;

    @TableField("expr_lang")
    private String exprLang;

    @TableField("input_schema_json")
    private String inputSchemaJson;

    @TableField("output_schema_json")
    private String outputSchemaJson;

    @TableField("error_policy")
    private String errorPolicy;

    @TableField("timeout_ms")
    private Long timeoutMs;

    @TableField("creater")
    private String creater;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("modifier")
    private String modifier;

    @TableField("modify_time")
    private LocalDateTime modifyTime;
}
