package com.yss.valset.workflow.web;

import com.yss.cloud.dto.result.SingleResult;
import com.yss.valset.workflow.model.WorkflowErrorDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 通用 ETL 工作流异常处理器。
 */
@RestControllerAdvice
public class WorkflowExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<SingleResult<WorkflowErrorDTO>> handleIllegalArgumentException(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(SingleResult.of(WorkflowErrorDTO.builder()
                .code("INVALID_REQUEST")
                .message(exception.getMessage())
                .detail(exception.getClass().getSimpleName())
                .build()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<SingleResult<WorkflowErrorDTO>> handleIllegalStateException(IllegalStateException exception) {
        HttpStatus status = exception.getMessage() != null && exception.getMessage().contains("未找到")
                ? HttpStatus.NOT_FOUND
                : HttpStatus.CONFLICT;
        return ResponseEntity.status(status).body(SingleResult.of(WorkflowErrorDTO.builder()
                .code(status == HttpStatus.NOT_FOUND ? "NOT_FOUND" : "ILLEGAL_STATE")
                .message(exception.getMessage())
                .detail(exception.getClass().getSimpleName())
                .build()));
    }
}
