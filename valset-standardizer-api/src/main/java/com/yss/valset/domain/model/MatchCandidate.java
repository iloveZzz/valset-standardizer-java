package com.yss.valset.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 候选标准科目及其分数细目。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchCandidate {
    /** 标准代码 */
    private String standardCode;
    /** 标准名称 */
    private String standardName;
    /** 总分 */
    private BigDecimal score;
    /** 名称匹配得分 */
    private BigDecimal scoreName;
    /** 路径匹配得分 */
    private BigDecimal scorePath;
    /** 关键词匹配得分 */
    private BigDecimal scoreKeyword;
    /** 代码匹配得分 */
    private BigDecimal scoreCode;
    /** 历史匹配得分 */
    private BigDecimal scoreHistory;
    /** Embedding匹配得分 */
    private BigDecimal scoreEmbedding;
    /** 是否历史匹配 */
    private Boolean matchedByHistory;
    /** 候选来源 */
    private List<String> candidateSources;
    /** 原因说明 */
    private List<String> reasons;
}
