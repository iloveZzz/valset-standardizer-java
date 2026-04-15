package com.yss.subjectmatch.domain.model;

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
    private String standardCode;
    private String standardName;
    private BigDecimal score;
    private BigDecimal scoreName;
    private BigDecimal scorePath;
    private BigDecimal scoreKeyword;
    private BigDecimal scoreCode;
    private BigDecimal scoreHistory;
    private BigDecimal scoreEmbedding;
    private Boolean matchedByHistory;
    private List<String> candidateSources;
    private List<String> reasons;
}
