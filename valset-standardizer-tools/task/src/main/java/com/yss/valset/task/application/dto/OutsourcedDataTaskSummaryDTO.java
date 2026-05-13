package com.yss.valset.task.application.dto;

import lombok.Data;

import java.util.List;

/**
 * 估值表解析任务总览。
 */
@Data
public class OutsourcedDataTaskSummaryDTO implements java.io.Serializable{

    private String workflowCode;

    private String workflowId;

    private Integer versionNo;

    private long totalCount;

    private long runningCount;

    private long successCount;

    private long failedCount;

    private List<OutsourcedDataTaskStageSummaryDTO> stageCatalog;

    private List<OutsourcedDataTaskStageSummaryDTO> stepSummaries;
}
