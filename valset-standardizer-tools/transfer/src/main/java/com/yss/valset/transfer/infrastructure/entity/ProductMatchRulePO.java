package com.yss.valset.transfer.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 产品识别规则持久化实体。
 */
@Data
@TableName("tp_match_rules")
public class ProductMatchRulePO {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @TableField("file_type_name")
    private String fileTypeName;

    @TableField("pd_cd")
    private String pdCd;

    @TableField("pd_nm")
    private String pdNm;

    @TableField("org_cd")
    private String orgCd;

    @TableField("org_nm")
    private String orgNm;

    @TableField("pd_type")
    private String pdType;

    @TableField("file_type")
    private String fileType;

    @TableField("match_rules")
    private String matchRules;

    @TableField("creater")
    private String creater;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("modifier")
    private String modifier;

    @TableField("modify_time")
    private LocalDateTime modifyTime;

    @TableField("is_valid")
    private Integer isValid;

    @TableField("memo")
    private String memo;

    @TableField("debug_name")
    private String debugName;

    @TableField("job_name")
    private String jobName;

    @TableField("job_scene")
    private String jobScene;
}
