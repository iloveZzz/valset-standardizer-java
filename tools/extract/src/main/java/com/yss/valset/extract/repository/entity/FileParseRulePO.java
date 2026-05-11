package com.yss.valset.extract.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.persistence.Id;
import java.time.LocalDateTime;

/**
 * 解析标准规则表。
 */
@Data
@TableName("t_file_parse_rule")
public class FileParseRulePO {

    @Id
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("creater")
    private String creater;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("modifier")
    private String modifier;

    @TableField("modify_time")
    private LocalDateTime modifyTime;

    @TableField("file_scene")
    private String fileScene;

    @TableField("file_type_name")
    private String fileTypeName;

    @TableField("region_name")
    private String regionName;

    @TableField("column_map")
    private String columnMap;

    @TableField("column_map_name")
    private String columnMapName;

    @TableField("status")
    private Boolean status;

    @TableField("multi_index")
    private Boolean multiIndex;

    @TableField("required")
    private Boolean required;
}
