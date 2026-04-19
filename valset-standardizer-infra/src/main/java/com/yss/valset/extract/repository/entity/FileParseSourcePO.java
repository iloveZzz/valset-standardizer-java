package com.yss.valset.extract.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.persistence.Id;
import java.time.LocalDateTime;

/**
 * 解析来源映射表。
 */
@Data
@TableName("t_file_parse_source")
public class FileParseSourcePO {

    @Id
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("file_type")
    private String fileType;

    @TableField("column_map")
    private String columnMap;

    @TableField("column_name")
    private String columnName;

    @TableField("file_ext_info")
    private String fileExtInfo;

    @TableField("status")
    private Boolean status;

    @TableField("creater")
    private String creater;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("modifier")
    private String modifier;

    @TableField("modify_time")
    private LocalDateTime modifyTime;
}
