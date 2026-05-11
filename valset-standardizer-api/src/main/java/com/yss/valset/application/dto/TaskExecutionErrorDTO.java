package com.yss.valset.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 任务执行失败时返回给前端的结构化错误信息。
 */
@Data
@Builder
public class TaskExecutionErrorDTO {
    /**
     * 错误编码。
     */
    private String code;
    /**
     * 错误消息。
     */
    private String message;
    /**
     * 任务类型。
     */
    private String taskType;
    /**
     * 任务主键。
     */
    private String taskId;
    /**
     * 任务错误码。
     */
    private String errorCode;
    /**
     * 失败载荷。
     */
    private String failurePayload;
}
