package com.yss.valset.task.application.dto;

import lombok.Data;

/**
 * 估值表解析任务步骤总览。
 */
@Data
public class OutsourcedDataTaskStageSummaryDTO {

    private String stage;

    private String step;

    private String stageName;

    private String stepName;

    private String stageDescription;

    private String stepDescription;

    private long totalCount;

    private long runningCount;

    private long failedCount;

    private long pendingCount;
}
