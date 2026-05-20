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
    /** 名称匹配权重 */
    private BigDecimal nameWeight;
    /** 路径匹配权重 */
    private BigDecimal pathWeight;
    /** 关键词匹配权重 */
    private BigDecimal keywordWeight;
    /** 代码匹配权重 */
    private BigDecimal codeWeight;
    /** 历史匹配权重 */
    private BigDecimal historyWeight;
    /** Embedding匹配权重 */
    private BigDecimal embeddingWeight;
}
