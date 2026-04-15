package com.yss.subjectmatch.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
