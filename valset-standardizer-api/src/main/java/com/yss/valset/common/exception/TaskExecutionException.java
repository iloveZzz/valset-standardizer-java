package com.yss.valset.common.exception;

/**
 * 任务执行失败异常。
 */
public class TaskExecutionException extends BizException {

    private final String taskType;
    private final Long taskId;
    private final String errorCode;
    private final String errorMessage;
    private final String failurePayload;

    public TaskExecutionException(String taskType, Long taskId, String errorCode, String errorMessage, String failurePayload, Throwable cause) {
        super("TASK_EXECUTION_FAILED", errorMessage);
        this.taskType = taskType;
        this.taskId = taskId;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.failurePayload = failurePayload;
        initCause(cause);
    }

    public String getTaskType() {
        return taskType;
    }

    public Long getTaskId() {
        return taskId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getFailurePayload() {
        return failurePayload;
    }
}
