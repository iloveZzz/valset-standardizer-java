package com.yss.valset.task.application.dto;

import lombok.Data;

/**
 * 估值表解析任务日志视图。
 */
@Data
public class OutsourcedDataTaskLogDTO {

    private String logId;

    private String batchId;

    private String stepId;

    private String stage;

    private String logLevel;

    private String message;

    private String occurredAt;
}
