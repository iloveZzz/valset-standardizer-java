package com.yss.valset.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 表头或字段映射决策结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MappingDecision {
    /** 列序号 */
    private Integer columnIndex;
    /** 表头文本 */
    private String headerText;
    /** 标准代码 */
    private String standardCode;
    /** 匹配规则ID */
    private Long matchedRuleId;
    /** 匹配数据源ID */
    private Long matchedSourceId;
    /** 匹配策略 */
    private String strategy;
    /** 置信度 */
    private Double confidence;
    /** 匹配原因 */
    private String reason;
    /** 匹配文本 */
    private String matchedText;
    /** 是否匹配成功 */
    private Boolean matched;
}
