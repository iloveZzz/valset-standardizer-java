package com.yss.valset.extract.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.persistence.Id;

/**
 * 映射样例落地表对象。
 */
@Data
@TableName("t_ods_mapping_sample")
public class MappingSamplePO {
    @Id
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("org_name")
    private String orgName;

    @TableField("org_id")
    private String orgId;

    @TableField("external_code")
    private String externalCode;

    @TableField("external_name")
    private String externalName;

    @TableField("standard_code")
    private String standardCode;

    @TableField("standard_name")
    private String standardName;

    @TableField("standard_system")
    private String standardSystem;

    @TableField("system_name")
    private String systemName;
}
