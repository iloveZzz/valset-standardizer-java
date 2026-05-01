package com.yss.valset.task.application.dto;

import lombok.Data;

import java.util.List;

/**
 * 委外数据任务总览。
 */
@Data
public class OutsourcedDataTaskSummaryDTO {

    private long totalCount;

    private long runningCount;

    private long successCount;

    private long failedCount;

    private List<OutsourcedDataTaskStageSummaryDTO> stageSummaries;
}
