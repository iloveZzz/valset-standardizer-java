package com.yss.valset.application.dto;

import com.yss.valset.domain.model.ValsetMatchResult;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 外部估值匹配结果视图。
 */
@Data
@Builder
public class MatchResultViewDTO {
    private Long fileId;
    private Integer matchedCount;
    private List<ValsetMatchResult> results;
}
