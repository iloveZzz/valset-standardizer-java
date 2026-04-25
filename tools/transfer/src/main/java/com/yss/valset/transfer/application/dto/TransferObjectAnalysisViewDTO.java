package com.yss.valset.transfer.application.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 文件主对象统计分析视图。
 */
@Data
@Builder
public class TransferObjectAnalysisViewDTO {

    /**
     * 总数。
     */
    private Long totalCount;

    /**
     * 按来源类型统计列表。
     */
    private List<TransferObjectSourceAnalysisViewDTO> sourceAnalyses;

    /**
     * 文件大小统计。
     */
    private TransferObjectSizeAnalysisViewDTO sizeAnalysis;
}
