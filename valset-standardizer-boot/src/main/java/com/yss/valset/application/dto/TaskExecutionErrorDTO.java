package com.yss.valset.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 任务执行失败时返回给前端的结构化错误信息。
 */
@Data
@Builder
public class TaskExecutionErrorDTO {
    private String code;
    private String message;
    private String taskType;
    private Long taskId;
    private String errorCode;
    private String failurePayload;
}
