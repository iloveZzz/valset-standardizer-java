package com.yss.valset.extract.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.persistence.Id;

/**
 * DWD 外部估值标准指标表。
 */
@Data
@TableName("t_dwd_external_valuation_metric")
public class DwdExternalValuationStandardMetricPO {
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

    @TableField("metric_name")
    private String metricName;

    @TableField("metric_type")
    private String metricType;

    @TableField("metric_code")
    private String metricCode;

    @TableField("metric_standard_name")
    private String metricStandardName;

    @TableField("standard_value_text")
    private String standardValueText;

    @TableField("standard_value_num")
    private java.math.BigDecimal standardValueNum;

    @TableField("standard_value_unit")
    private String standardValueUnit;

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
