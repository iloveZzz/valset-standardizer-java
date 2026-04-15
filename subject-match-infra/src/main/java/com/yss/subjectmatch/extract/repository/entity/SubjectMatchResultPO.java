package com.yss.subjectmatch.extract.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yss.cloud.sankuai.GenerationTypeSeq;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.math.BigDecimal;

@Data
@TableName("t_subject_match_result")
public class SubjectMatchResultPO {
    @Id
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("task_id")
    private Long taskId;

    @TableField("file_id")
    private Long fileId;

    @TableField("external_subject_code")
    private String externalSubjectCode;

    @TableField("external_subject_name")
    private String externalSubjectName;

    @TableField("external_level")
    private Integer externalLevel;

    @TableField("external_is_leaf")
    private Boolean externalIsLeaf;

    @TableField("anchor_subject_code")
    private String anchorSubjectCode;

    @TableField("anchor_subject_name")
    private String anchorSubjectName;

    @TableField("anchor_level")
    private Integer anchorLevel;

    @TableField("anchor_path_text")
    private String anchorPathText;

    @TableField("anchor_reason")
    private String anchorReason;

    @TableField("matched_standard_code")
    private String matchedStandardCode;

    @TableField("matched_standard_name")
    private String matchedStandardName;

    @TableField("score")
    private BigDecimal score;

    @TableField("score_name")
    private BigDecimal scoreName;

    @TableField("score_path")
    private BigDecimal scorePath;

    @TableField("score_keyword")
    private BigDecimal scoreKeyword;

    @TableField("score_code")
    private BigDecimal scoreCode;

    @TableField("score_history")
    private BigDecimal scoreHistory;

    @TableField("score_embedding")
    private BigDecimal scoreEmbedding;

    @TableField("confidence_level")
    private String confidenceLevel;

    @TableField("needs_review")
    private Boolean needsReview;

    @TableField("match_reason")
    private String matchReason;

    @TableField("candidate_count")
    private Integer candidateCount;

    @TableField("top_candidates_json")
    private String topCandidatesJson;
}
