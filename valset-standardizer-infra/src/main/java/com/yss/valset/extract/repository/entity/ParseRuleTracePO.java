package com.yss.valset.extract.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.persistence.Id;
import java.time.LocalDateTime;

/**
 * 解析规则追踪对象。
 */
@Data
@TableName("t_file_parse_rule_trace")
public class ParseRuleTracePO {

    @Id
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("trace_scope")
    private String traceScope;

    @TableField("trace_type")
    private String traceType;

    @TableField("profile_id")
    private Long profileId;

    @TableField("profile_code")
    private String profileCode;

    @TableField("version")
    private String version;

    @TableField("file_id")
    private Long fileId;

    @TableField("task_id")
    private Long taskId;

    @TableField("step_name")
    private String stepName;

    @TableField("expression")
    private String expression;

    @TableField("input_json")
    private String inputJson;

    @TableField("output_json")
    private String outputJson;

    @TableField("success")
    private Boolean success;

    @TableField("cost_ms")
    private Long costMs;

    @TableField("error_message")
    private String errorMessage;

    @TableField("trace_time")
    private LocalDateTime traceTime;
}
