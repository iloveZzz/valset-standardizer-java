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
    private Integer columnIndex;
    private String headerText;
    private String standardCode;
    private Long matchedRuleId;
    private Long matchedSourceId;
    private String strategy;
    private Double confidence;
    private String reason;
    private String matchedText;
    private Boolean matched;
}
