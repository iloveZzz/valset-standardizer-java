package com.yss.valset.application.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 文件收发运行日志阶段统计视图。
 */
@Data
@Builder
public class TransferRunLogStageAnalysisViewDTO {

    /**
     * 运行阶段。
     */
    private String runStage;

    /**
     * 阶段名称。
     */
    private String stageLabel;

    /**
     * 阶段日志总数。
     */
    private Long totalCount;

    /**
     * 阶段下的状态统计。
     */
    private List<TransferRunLogStatusCountViewDTO> statusCounts;
}
