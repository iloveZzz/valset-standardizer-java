package com.yss.valset.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 单个外部主题的匹配结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValsetMatchResult {
    private String externalSubjectCode;
    private String externalSubjectName;
    private Integer externalLevel;
    private Boolean externalIsLeaf;
    private String anchorSubjectCode;
    private String anchorSubjectName;
    private Integer anchorLevel;
    private String anchorPathText;
    private String anchorReason;
    private String matchedStandardCode;
    private String matchedStandardName;
    private BigDecimal score;
    private BigDecimal scoreName;
    private BigDecimal scorePath;
    private BigDecimal scoreKeyword;
    private BigDecimal scoreCode;
    private BigDecimal scoreHistory;
    private BigDecimal scoreEmbedding;
    private ConfidenceLevel confidenceLevel;
    private Boolean needsReview;
    private String matchReason;
    private Integer candidateCount;
    private List<MatchCandidate> topCandidates;
}
