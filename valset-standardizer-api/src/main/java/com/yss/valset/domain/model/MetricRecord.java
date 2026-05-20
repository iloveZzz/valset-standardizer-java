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
    /** sheet名称 */
    private String sheetName;
    /** 数据行号 */
    private Integer rowDataNumber;
    /** 指标名称 */
    private String metricName;
    /** 指标类型 */
    private String metricType;
    /** 值 */
    private String value;
    /** 原始值映射 */
    private Map<String, Object> rawValues;
    /** 标准代码 */
    private String standardCode;
    /** 标准名称 */
    private String standardName;
    /** 标准值文本 */
    private String standardValueText;
    /** 标准值数字 */
    private BigDecimal standardValueNumber;
    /** 标准值单位 */
    private String standardValueUnit;
    /** 标准值映射 */
    private Map<String, Object> standardValues;
    /** 映射规则ID */
    private Long mappingRuleId;
    /** 映射数据源ID */
    private Long mappingSourceId;
    /** 映射状态 */
    private String mappingStatus;
    /** 映射原因 */
    private String mappingReason;
    /** 匹配置信度 */
    private Double mappingConfidence;
}
