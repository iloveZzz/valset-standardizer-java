package com.yss.valset.controller;

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

    @ExceptionHandler(TaskExecutionException.class)
    public ResponseEntity<TaskExecutionErrorDTO> handleTaskExecutionException(TaskExecutionException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(TaskExecutionErrorDTO.builder()
                .code(exception.getCode())
                .message(exception.getErrorMessage())
                .taskType(exception.getTaskType())
                .taskId(exception.getTaskId())
                .errorCode(exception.getErrorCode())
                .failurePayload(exception.getFailurePayload())
                .build());
    }
}
