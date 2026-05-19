package com.yss.valset.extract.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yss.cloud.sankuai.GenerationTypeSeq;
import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("tp_match_rules")
public class MatchRulesPO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Integer id;

    /**
     * 文件类型名称
     */
    @TableField("file_type_name")
    private String fileTypeName;

    /**
     * 产品代码
     */
    @TableField("pd_cd")
    private String pdCd;

    /**
     * 产品类型：理财产品，委外产品。默认为委外产品
     */
    @TableField("pd_type")
    private String pdType;

    /**
     * 产品名称
     */
    @TableField("pd_nm")
    private String pdNm;

    /**
     * 机构代码
     */
    @TableField("org_cd")
    private String orgCd;

    /**
     * 机构名称
     */
    @TableField("org_nm")
    private String orgName;

    /**
     * 匹配规则
     */
    @TableField("match_rules")
    private String matchRules;

    /**
     * 文件类型
     */
    @TableField("file_type")
    private String fileType;

    /**
     * 创建人
     */
    @TableField("creater")
    private String creater;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 修改人
     */
    @TableField("modifier")
    private String modifier;

    /**
     * 修改时间
     */
    private LocalDateTime modifyTime;

    /**
     * 是否启用
     */
    @TableField("is_valid")
    private Integer isValid;

    /**
     * 备注
     */
    @TableField("memo")
    private String memo;

    /**
     * 调试名称
     */
    @TableField("debug_name")
    private String debugName;

    /**
     * 作业名称
     */
    @TableField("job_name")
    private String jobName;

    /**
     * 作业场景
     */
    @TableField("job_scene")
    private String jobScene;

}
