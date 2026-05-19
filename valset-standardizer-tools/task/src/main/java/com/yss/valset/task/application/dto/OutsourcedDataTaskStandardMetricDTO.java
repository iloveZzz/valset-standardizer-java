package com.yss.valset.task.application.dto;

import lombok.Data;

import java.util.Map;

/**
 * 估值标准数据指标行。
 */
@Data
public class OutsourcedDataTaskStandardMetricDTO implements java.io.Serializable {

    private Long id;

    private Long valuationId;

    private String sheetName;

    private Integer rowDataNumber;

    private String metricName;

    private String metricType;

    private String metricValue;

    private String rawValuesJson;

    private Map<String, String> rawValues;
}
