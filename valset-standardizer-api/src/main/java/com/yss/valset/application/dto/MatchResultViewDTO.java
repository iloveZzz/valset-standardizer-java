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
    /**
     * 文件主键。
     */
    private String fileId;
    /**
     * 已匹配条数。
     */
    private Integer matchedCount;
    /**
     * 匹配结果列表。
     */
    private List<ValsetMatchResult> results;
}
