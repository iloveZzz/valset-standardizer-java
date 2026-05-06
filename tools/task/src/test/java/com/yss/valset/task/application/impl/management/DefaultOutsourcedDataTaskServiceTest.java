package com.yss.valset.task.application.impl.management;

import com.yss.cloud.dto.response.PageResult;
import com.yss.valset.task.application.command.OutsourcedDataTaskQueryCommand;
import com.yss.valset.task.application.dto.OutsourcedDataTaskBatchDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskStepDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskSummaryDTO;
import com.yss.valset.task.application.service.workflow.WorkflowRuntimeCatalog;
import com.yss.valset.task.domain.model.OutsourcedDataTaskStage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 默认估值表解析任务管理应用服务测试。
 */
class DefaultOutsourcedDataTaskServiceTest {

    private final DefaultOutsourcedDataTaskService service = new DefaultOutsourcedDataTaskService();

    DefaultOutsourcedDataTaskServiceTest() {
        WorkflowRuntimeCatalog stageCatalog = mock(WorkflowRuntimeCatalog.class);
        when(stageCatalog.stageSequence()).thenReturn(List.of(OutsourcedDataTaskStage.FILE_PARSE));
        when(stageCatalog.stageLabel(OutsourcedDataTaskStage.FILE_PARSE.name())).thenReturn("文件解析");
        when(stageCatalog.stageDescription(OutsourcedDataTaskStage.FILE_PARSE.name())).thenReturn("文件识别、Sheet 解析、结构化解析");
        when(stageCatalog.activeWorkflowDefinition()).thenReturn(Optional.empty());
        when(stageCatalog.activeWorkflowCode()).thenReturn("VALUATION_PARSE");
        service.setStageCatalog(stageCatalog);
    }

    @Test
    void shouldReturnEmptySummaryWithoutGateway() {
        OutsourcedDataTaskSummaryDTO summary = service.summary(new OutsourcedDataTaskQueryCommand());

        assertThat(summary.getTotalCount()).isZero();
        assertThat(summary.getStepSummaries()).isNotEmpty();
    }

    @Test
    void shouldReturnEmptyPageWithoutGateway() {
        OutsourcedDataTaskQueryCommand query = new OutsourcedDataTaskQueryCommand();
        query.setPageIndex(1);
        query.setPageSize(10);

        PageResult<OutsourcedDataTaskBatchDTO> page = service.pageTasks(query);

        assertThat(page.getData()).isEmpty();
        assertThat(page.getTotalCount()).isZero();
    }

    @Test
    void shouldReturnEmptyStepsWithoutGateway() {
        List<OutsourcedDataTaskStepDTO> steps = service.listSteps("BATCH-20250227-001");

        assertThat(steps).isEmpty();
    }
}
