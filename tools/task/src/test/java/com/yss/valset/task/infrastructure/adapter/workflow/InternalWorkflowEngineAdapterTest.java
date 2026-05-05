package com.yss.valset.task.infrastructure.adapter.workflow;

import com.yss.valset.batch.scheduler.SchedulerService;
import com.yss.valset.domain.gateway.WorkflowTaskGateway;
import com.yss.valset.domain.model.TaskStage;
import com.yss.valset.domain.model.TaskStatus;
import com.yss.valset.domain.model.TaskType;
import com.yss.valset.domain.model.WorkflowTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalWorkflowEngineAdapterTest {

    @Mock
    private SchedulerService schedulerService;

    @Mock
    private WorkflowTaskGateway workflowTaskGateway;

    @Test
    void shouldTriggerInternalTaskByTaskId() {
        InternalWorkflowEngineAdapter adapter = new InternalWorkflowEngineAdapter(schedulerService, workflowTaskGateway);

        adapter.trigger("VALUATION_PARSE", "PARSE", Map.of("taskId", 11L));

        verify(workflowTaskGateway, never()).markRunning(eq(11L), eq("PARSE"), org.mockito.ArgumentMatchers.any());
        verify(schedulerService).triggerNow(11L);
    }

    @Test
    void shouldRetryInternalTaskByInstanceId() {
        InternalWorkflowEngineAdapter adapter = new InternalWorkflowEngineAdapter(schedulerService, workflowTaskGateway);
        when(workflowTaskGateway.markRetrying(22L)).thenReturn(true);

        adapter.retry("22", "PARSE");

        verify(workflowTaskGateway).markRetrying(22L);
        verify(schedulerService).triggerNow(22L);
    }

    @Test
    void shouldQueryInternalTaskState() {
        InternalWorkflowEngineAdapter adapter = new InternalWorkflowEngineAdapter(schedulerService, workflowTaskGateway);
        when(workflowTaskGateway.findById(33L)).thenReturn(buildTask(33L, TaskStatus.RUNNING));

        Map<String, Object> result = adapter.query("33").orElseThrow();

        assertThat(result.get("taskId")).isEqualTo(33L);
        assertThat(result.get("taskStatus")).isEqualTo("RUNNING");
        assertThat(result.get("taskStage")).isEqualTo(TaskStage.PARSE.name());
    }

    @Test
    void shouldRejectMissingTaskIdOnTrigger() {
        InternalWorkflowEngineAdapter adapter = new InternalWorkflowEngineAdapter(schedulerService, workflowTaskGateway);
        assertThrows(IllegalArgumentException.class, () -> adapter.trigger("VALUATION_PARSE", "PARSE", Map.of()));
    }

    private WorkflowTask buildTask(Long taskId, TaskStatus status) {
        return WorkflowTask.builder()
                .taskId(taskId)
                .taskType(TaskType.PARSE_WORKBOOK)
                .taskStatus(status)
                .taskStage(TaskStage.PARSE)
                .businessKey("PARSE:EXCEL:1")
                .fileId(1L)
                .inputPayload("{}")
                .build();
    }
}
