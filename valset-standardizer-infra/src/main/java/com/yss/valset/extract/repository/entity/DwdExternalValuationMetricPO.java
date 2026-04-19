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
 * DWD 外部估值指标表。
 */
@Data
@TableName("t_stg_external_valuation_metric")
public class DwdExternalValuationMetricPO {
    @Id
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("valuation_id")
    private Long valuationId;

    @TableField("sheet_name")
    private String sheetName;

    @TableField("row_data_number")
    private Integer rowDataNumber;

    @TableField("metric_name")
    private String metricName;

    @TableField("metric_type")
    private String metricType;

    @TableField("metric_value")
    private String metricValue;

    @TableField("raw_values_json")
    private String rawValuesJson;
}
