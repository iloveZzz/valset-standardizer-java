package com.yss.valset.workflow.web;

import com.yss.cloud.dto.response.SingleResult;
import com.yss.valset.workflow.spi.WorkflowApplicationService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkflowControllerTest {

    @Test
    void shouldPassLogPagingParamsToApplicationService() {
        WorkflowApplicationService service = mock(WorkflowApplicationService.class);
        WorkflowController controller = new WorkflowController(service);

        when(service.queryTaskLog("dolphin", 2, 21L, 12, 34)).thenReturn("task log");

        SingleResult<String> result = controller.getTaskLog(21L, "dolphin", 2, 12, 34);

        assertThat(result.getData()).isEqualTo("task log");
        verify(service).queryTaskLog("dolphin", 2, 21L, 12, 34);
    }
}
