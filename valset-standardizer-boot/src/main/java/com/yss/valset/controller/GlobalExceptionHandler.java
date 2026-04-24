package com.yss.valset.controller;

import com.yss.cloud.dto.response.SingleResult;
import com.yss.valset.application.dto.TaskExecutionErrorDTO;
import com.yss.valset.common.exception.TaskExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理任务执行异常。
     *
     * @param exception 任务执行异常
     * @return 统一错误响应
     */
    @ExceptionHandler(TaskExecutionException.class)
    public ResponseEntity<SingleResult<TaskExecutionErrorDTO>> handleTaskExecutionException(TaskExecutionException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(SingleResult.of(TaskExecutionErrorDTO.builder()
                .code(exception.getCode())
                .message(exception.getErrorMessage())
                .taskType(exception.getTaskType())
                .taskId(exception.getTaskId() == null ? null : String.valueOf(exception.getTaskId()))
                .errorCode(exception.getErrorCode())
                .failurePayload(exception.getFailurePayload())
                .build()));
    }
}
