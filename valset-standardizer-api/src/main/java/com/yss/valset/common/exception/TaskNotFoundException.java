package com.yss.valset.common.exception;

public class TaskNotFoundException extends BizException {
    public TaskNotFoundException(Long taskId) {
        super("TASK_NOT_FOUND", "Task not found: " + taskId);
    }
}
