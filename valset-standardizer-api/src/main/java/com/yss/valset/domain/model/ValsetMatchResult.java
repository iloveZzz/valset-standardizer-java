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
    /** 外部代码 */
    private String externalSubjectCode;
    /** 外部名称 */
    private String externalSubjectName;
    /** 外部层级 */
    private Integer externalLevel;
    /** 是否为叶子节点 */
    private Boolean externalIsLeaf;
    /** 锚点代码 */
    private String anchorSubjectCode;
    /** 锚点名称 */
    private String anchorSubjectName;
    /** 锚点层级 */
    private Integer anchorLevel;
    /** 锚点路径文本 */
    private String anchorPathText;
    /** 锚点原因 */
    private String anchorReason;
    /** 匹配标准代码 */
    private String matchedStandardCode;
    /** 匹配标准名称 */
    private String matchedStandardName;
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
    /** 置信度等级 */
    private ConfidenceLevel confidenceLevel;
    /** 是否需要人工复核 */
    private Boolean needsReview;
    /** 匹配原因 */
    private String matchReason;
    /** 候选数量 */
    private Integer candidateCount;
    /** 候选列表 */
    private List<MatchCandidate> topCandidates;
}
