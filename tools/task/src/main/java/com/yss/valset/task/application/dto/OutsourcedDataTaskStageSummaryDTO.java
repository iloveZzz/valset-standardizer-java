package com.yss.valset.task.application.dto;

import lombok.Data;

/**
 * 委外数据任务阶段总览。
 */
@Data
public class OutsourcedDataTaskStageSummaryDTO {

    private String stage;

    private String stageName;

    private String stageDescription;

    private long totalCount;

    private long runningCount;

    private long failedCount;

    private long pendingCount;
}
