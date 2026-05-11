package com.yss.valset.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 匹配器使用的权重配置。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchWeights {
    private BigDecimal nameWeight;
    private BigDecimal pathWeight;
    private BigDecimal keywordWeight;
    private BigDecimal codeWeight;
    private BigDecimal historyWeight;
    private BigDecimal embeddingWeight;
}
