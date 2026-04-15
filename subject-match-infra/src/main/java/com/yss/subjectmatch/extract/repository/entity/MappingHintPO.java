package com.yss.subjectmatch.extract.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.persistence.Id;
import java.math.BigDecimal;

/**
 * 历史映射经验落地表对象。
 */
@Data
@TableName("t_ods_mapping_hint")
public class MappingHintPO {
    @Id
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("source")
    private String source;

    @TableField("normalized_key")
    private String normalizedKey;

    @TableField("standard_code")
    private String standardCode;

    @TableField("standard_name")
    private String standardName;

    @TableField("support_count")
    private Integer supportCount;

    @TableField("confidence")
    private BigDecimal confidence;
}
