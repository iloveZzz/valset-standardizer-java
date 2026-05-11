package com.yss.valset.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 在工作簿解析期间捕获的指标行。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricRecord {
    private String sheetName;
    private Integer rowDataNumber;
    private String metricName;
    private String metricType;
    private String value;
    private Map<String, Object> rawValues;
    private String standardCode;
    private String standardName;
    private String standardValueText;
    private BigDecimal standardValueNumber;
    private String standardValueUnit;
    private Map<String, Object> standardValues;
    private Long mappingRuleId;
    private Long mappingSourceId;
    private String mappingStatus;
    private String mappingReason;
    private Double mappingConfidence;
}
