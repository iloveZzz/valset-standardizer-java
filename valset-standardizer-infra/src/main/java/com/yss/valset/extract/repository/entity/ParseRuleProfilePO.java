package com.yss.valset.extract.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.persistence.Id;
import java.time.LocalDateTime;

/**
 * 解析模板主表对象。
 */
@Data
@TableName("t_file_parse_profile")
public class ParseRuleProfilePO {

    @Id
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("profile_code")
    private String profileCode;

    @TableField("profile_name")
    private String profileName;

    @TableField("version")
    private String version;

    @TableField("file_scene")
    private String fileScene;

    @TableField("file_type_name")
    private String fileTypeName;

    @TableField("source_channel")
    private String sourceChannel;

    @TableField("status")
    private String status;

    @TableField("priority")
    private Integer priority;

    @TableField("match_expr")
    private String matchExpr;

    @TableField("header_expr")
    private String headerExpr;

    @TableField("row_classify_expr")
    private String rowClassifyExpr;

    @TableField("field_map_expr")
    private String fieldMapExpr;

    @TableField("transform_expr")
    private String transformExpr;

    @TableField("trace_enabled")
    private Boolean traceEnabled;

    @TableField("timeout_ms")
    private Long timeoutMs;

    @TableField("checksum")
    private String checksum;

    @TableField("creater")
    private String creater;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("modifier")
    private String modifier;

    @TableField("modify_time")
    private LocalDateTime modifyTime;

    @TableField("published_time")
    private LocalDateTime publishedTime;
}
