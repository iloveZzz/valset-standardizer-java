package com.yss.subjectmatch.extract.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.persistence.Id;

/**
 * DWD 外部估值标准明细表。
 */
@Data
@TableName("t_dwd_external_valuation_subject")
public class DwdExternalValuationStandardSubjectPO {
    @Id
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("valuation_id")
    private Long valuationId;

    @TableField("file_id")
    private Long fileId;

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

    @TableField("standard_code")
    private String standardCode;

    @TableField("standard_name")
    private String standardName;

    @TableField("standard_values_json")
    private String standardValuesJson;

    @TableField("mapping_rule_id")
    private Long mappingRuleId;

    @TableField("mapping_source_id")
    private Long mappingSourceId;

    @TableField("mapping_status")
    private String mappingStatus;

    @TableField("mapping_reason")
    private String mappingReason;

    @TableField("mapping_confidence")
    private java.math.BigDecimal mappingConfidence;

    @TableField("raw_values_json")
    private String rawValuesJson;
}
