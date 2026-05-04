package com.yss.valset.task.application.dto;

import lombok.Data;

/**
 * 估值表解析任务批次视图。
 */
@Data
public class OutsourcedDataTaskBatchDTO {

    private String batchId;

    private String batchName;

    private String businessDate;

    private String productCode;

    private String productName;

    private String managerName;

    private String fileId;

    private String filesysFileId;

    private String originalFileName;

    private String sourceType;

    private String currentStage;

    private String currentStep;

    private String currentStageName;

    private String currentStepName;

    private String status;

    private String statusName;

    private Integer progress;

    private String startedAt;

    private String endedAt;

    private Long durationMs;

    private String durationText;

    private String lastErrorCode;

    private String lastErrorMessage;
}
