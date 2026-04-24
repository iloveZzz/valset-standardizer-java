package com.yss.valset.transfer.application.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 文件收发运行日志统计分析视图。
 */
@Data
@Builder
public class TransferRunLogAnalysisViewDTO {

    /**
     * 日志总数。
     */
    private Long totalCount;

    /**
     * 来源数量。
     */
    private Long sourceCount;

    /**
     * 路由数量。
     */
    private Long routeCount;

    /**
     * 目标数量。
     */
    private Long targetCount;

    /**
     * 阶段统计列表。
     */
    private List<TransferRunLogStageAnalysisViewDTO> stageAnalyses;
}
