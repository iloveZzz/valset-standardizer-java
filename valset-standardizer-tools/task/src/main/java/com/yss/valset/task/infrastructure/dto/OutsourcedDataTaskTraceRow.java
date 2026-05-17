package com.yss.valset.task.infrastructure.dto;

import lombok.Data;

/**
 * 估值解析链路查询行。
 */
@Data
public class OutsourcedDataTaskTraceRow {

    private Long executionId;

    private Long jobInstanceId;

    private String jobName;

    private String jobStatus;

    private String jobExitCode;

    private String jobExitMessage;

    private String jobCreateTime;

    private String jobStartTime;

    private String jobEndTime;

    private Long jobDurationMs;

    private Long taskId;

    private String fileId;

    private String businessKey;

    private String taskType;

    private String taskStage;

    private String inputPayload;

    private String queueId;

    private String queueStatus;

    private String queueDeliveryStatus;

    private String queueTriggerMode;

    private Integer queueRetryCount;

    private String queueClaimedBy;

    private String queueClaimedAt;

    private String queueParsedAt;

    private String queueErrorMessage;

    private String queueRequestJson;

    private String queueResultJson;

    private String transferId;

    private String transferStatus;

    private String deliveryExecuteStatus;

    private String deliveryDeliveredAt;

    private String deliveryErrorMessage;

    private String transferSourceType;

    private String transferSourceCode;

    private String transferOriginalName;

    private String transferReceivedAt;

    private String transferStoredAt;

    private String transferBusinessDate;

    private String transferErrorMessage;
}
