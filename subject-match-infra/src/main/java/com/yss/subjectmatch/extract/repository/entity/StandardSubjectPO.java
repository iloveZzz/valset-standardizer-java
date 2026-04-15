package com.yss.subjectmatch.extract.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.persistence.Id;
import java.util.List;

/**
 * 标准科目落地表对象。
 */
@Data
@TableName("t_ods_standard_subject")
public class StandardSubjectPO {
    @Id
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("standard_code")
    private String standardCode;

    @TableField("standard_name")
    private String standardName;

    @TableField("parent_code")
    private String parentCode;

    @TableField("parent_name")
    private String parentName;

    @TableField("level")
    private Integer level;

    @TableField("root_code")
    private String rootCode;

    @TableField("segment_count")
    private Integer segmentCount;

    @TableField("path_codes_json")
    private String pathCodesJson;

    @TableField("path_names_json")
    private String pathNamesJson;

    @TableField("path_text")
    private String pathText;

    @TableField("normalized_name")
    private String normalizedName;

    @TableField("normalized_path_text")
    private String normalizedPathText;

    @TableField("placeholder")
    private Boolean placeholder;
}
