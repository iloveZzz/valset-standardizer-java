package com.yss.valset.workflow.web;

import com.yss.cloud.dto.response.SingleResult;
import com.yss.valset.workflow.model.WorkflowErrorDTO;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowExceptionHandlerTest {

    private final WorkflowExceptionHandler handler = new WorkflowExceptionHandler();

    @Test
    void shouldWrapIllegalArgumentException() {
        ResponseEntity<SingleResult<WorkflowErrorDTO>> response = handler.handleIllegalArgumentException(
                new IllegalArgumentException("工作流定义不合法"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getCode()).isEqualTo("INVALID_REQUEST");
        assertThat(response.getBody().getData().getMessage()).isEqualTo("工作流定义不合法");
    }

    @Test
    void shouldWrapIllegalStateExceptionAsNotFound() {
        ResponseEntity<SingleResult<WorkflowErrorDTO>> response = handler.handleIllegalStateException(
                new IllegalStateException("未找到任务实例"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getCode()).isEqualTo("NOT_FOUND");
    }
}
