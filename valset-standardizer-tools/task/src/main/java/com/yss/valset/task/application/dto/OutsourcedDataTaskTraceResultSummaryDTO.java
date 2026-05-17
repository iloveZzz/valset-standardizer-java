package com.yss.valset.task.application.dto;

import lombok.Data;

/**
 * 估值解析链路结果摘要。
 */
@Data
public class OutsourcedDataTaskTraceResultSummaryDTO implements java.io.Serializable {

    private String status;

    private String statusName;

    private String startedAt;

    private String endedAt;

    private Long durationMs;

    private String inputSummary;

    private String outputSummary;

    private String errorCode;

    private String errorMessage;
}
