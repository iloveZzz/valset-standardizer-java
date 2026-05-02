package com.yss.valset.task.application.dto;

import lombok.Data;

/**
 * 估值表解析任务操作结果。
 */
@Data
public class OutsourcedDataTaskActionResultDTO {

    private String batchId;

    private String stepId;

    private boolean accepted;

    private String action;

    private String message;
}
