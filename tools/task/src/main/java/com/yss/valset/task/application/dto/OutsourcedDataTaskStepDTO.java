package com.yss.valset.task.application.dto;

import lombok.Data;

/**
 * 委外数据任务阶段明细。
 */
@Data
public class OutsourcedDataTaskStepDTO {

    private String stepId;

    private String batchId;

    private String stage;

    private String stageName;

    private String taskId;

    private String taskType;

    private Integer runNo;

    private Boolean currentFlag;

    private String triggerMode;

    private String triggerModeName;

    private String status;

    private String statusName;

    private Integer progress;

    private String startedAt;

    private String endedAt;

    private Long durationMs;

    private String durationText;

    private String inputSummary;

    private String outputSummary;

    private String errorCode;

    private String errorMessage;

    private String logRef;
}
