package com.yss.subjectmatch.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 任务创建后返回响应。
 */
@Data
@Builder
public class TaskCreateResponse {
    /**
     * 唯一的任务标识符。
     */
    private Long taskId;
    /**
     * 任务类型名称。
     */
    private String taskType;
    /**
     * 初始任务状态。
     */
    private String taskStatus;
    /**
     * 用于追溯的业务密钥。
     */
    private String businessKey;
    /**
     * 是否复用了已成功完成的任务。
     */
    private Boolean reusedExistingTask;
}
