package com.yss.valset.extract.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yss.cloud.sankuai.GenerationTypeSeq;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * DWD 外部估值明细表。
 */
@Data
@TableName("t_stg_external_valuation_subject")
public class DwdExternalValuationSubjectPO {
    @Id
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("valuation_id")
    private Long valuationId;

    @TableField("sheet_name")
    private String sheetName;

    @TableField("row_data_number")
    private Integer rowDataNumber;

    @TableField("subject_code")
    private String subjectCode;

    @TableField("subject_name")
    private String subjectName;

    @TableField("level_no")
    private Integer level;

    @TableField("parent_code")
    private String parentCode;

    @TableField("root_code")
    private String rootCode;

    @TableField("segment_count")
    private Integer segmentCount;

    @TableField("path_codes_json")
    private String pathCodesJson;

    @TableField("is_leaf")
    private Boolean leaf;

    @TableField("raw_values_json")
    private String rawValuesJson;
}
