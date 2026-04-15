package com.yss.subjectmatch.application.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.subjectmatch.domain.gateway.TaskGateway;
import com.yss.subjectmatch.domain.model.TaskInfo;
import com.yss.subjectmatch.domain.model.TaskStatus;
import com.yss.subjectmatch.domain.model.TaskStage;
import com.yss.subjectmatch.domain.model.TaskType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultTaskQueryAppServiceTest {

    @Test
    void exposesExtractTaskMetricsInQueryView() {
        TaskGateway taskGateway = mock(TaskGateway.class);
        ObjectMapper objectMapper = new ObjectMapper();
        DefaultTaskQueryAppService service = new DefaultTaskQueryAppService(taskGateway, objectMapper);

        when(taskGateway.findById(99L)).thenReturn(TaskInfo.builder()
                .taskId(99L)
                .taskType(TaskType.EXTRACT_DATA)
                .taskStatus(TaskStatus.SUCCESS)
                .taskStage(TaskStage.EXTRACT)
                .businessKey("EXTRACT:/tmp/sample.csv")
                .resultPayload("{\"workbookPath\":\"/tmp/sample.csv\",\"dataSourceType\":\"CSV\",\"fileId\":99,\"rowCount\":114,\"fileSizeBytes\":53760,\"durationMs\":1234}")
                .build());

        var view = service.queryTask(99L);

        assertThat(view.getTaskType()).isEqualTo("EXTRACT_DATA");
        assertThat(view.getTaskStage()).isEqualTo("EXTRACT");
        assertThat(view.getRowCount()).isEqualTo(114L);
        assertThat(view.getFileSizeBytes()).isEqualTo(53760L);
        assertThat(view.getDurationMs()).isEqualTo(1234L);
        assertThat(view.getResultData()).containsEntry("rowCount", 114);
    }
}
