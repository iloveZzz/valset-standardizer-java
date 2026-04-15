package com.yss.subjectmatch.application.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.subjectmatch.application.command.MatchTaskCommand;
import com.yss.subjectmatch.application.dto.TaskViewDTO;
import com.yss.subjectmatch.application.port.ExtractDataExecutionUseCase;
import com.yss.subjectmatch.application.port.MatchExecutionUseCase;
import com.yss.subjectmatch.application.port.ParseExecutionUseCase;
import com.yss.subjectmatch.application.service.TaskQueryAppService;
import com.yss.subjectmatch.application.support.TaskReuseService;
import com.yss.subjectmatch.application.support.UploadedFileStorageService;
import com.yss.subjectmatch.domain.gateway.SubjectMatchFileInfoGateway;
import com.yss.subjectmatch.domain.gateway.SubjectMatchFileIngestLogGateway;
import com.yss.subjectmatch.domain.gateway.TaskGateway;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DefaultValuationWorkflowAppServiceTest {

    @Test
    void matchCanBeSkippedByConfiguration() {
        DefaultValuationWorkflowAppService service = new DefaultValuationWorkflowAppService(
                mock(UploadedFileStorageService.class),
                mock(TaskGateway.class),
                mock(TaskQueryAppService.class),
                mock(ExtractDataExecutionUseCase.class),
                mock(ParseExecutionUseCase.class),
                mock(MatchExecutionUseCase.class),
                new ObjectMapper(),
                mock(TaskReuseService.class),
                mock(SubjectMatchFileInfoGateway.class),
                mock(SubjectMatchFileIngestLogGateway.class)
        );
        ReflectionTestUtils.setField(service, "enableMatchProcess", false);

        MatchTaskCommand command = new MatchTaskCommand();
        command.setWorkbookPath("/tmp/sample.xlsx");
        command.setFileId(123L);
        command.setDataSourceType("EXCEL");

        TaskViewDTO viewDTO = service.match(command);

        assertThat(viewDTO.getTaskStatus()).isEqualTo("SKIPPED");
        assertThat(viewDTO.getTaskType()).isEqualTo("MATCH_SUBJECT");
        assertThat(viewDTO.getTaskStage()).isEqualTo("MATCH");
        assertThat(viewDTO.getTaskId()).isNull();
        assertThat(viewDTO.getResultData()).containsEntry("skipped", true);
    }
}
