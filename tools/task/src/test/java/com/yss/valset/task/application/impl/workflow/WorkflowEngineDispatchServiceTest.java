package com.yss.valset.task.application.impl.workflow;

import com.yss.valset.application.dto.workflow.WorkflowExecutionContextDTO;
import com.yss.valset.batch.scheduler.SchedulerService;
import com.yss.valset.task.application.service.workflow.engine.WorkflowEngineAdapter;
import com.yss.valset.task.application.service.workflow.engine.WorkflowEngineType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowEngineDispatchServiceTest {

    @Mock
    private SchedulerService schedulerService;

    @Mock
    private WorkflowEngineAdapter internalAdapter;

    @Test
    void shouldDispatchToInternalAdapterWhenEngineTypeIsInternal() {
        when(internalAdapter.engineType()).thenReturn(WorkflowEngineType.INTERNAL);
        DefaultWorkflowEngineDispatchService service = new DefaultWorkflowEngineDispatchService(schedulerService, List.of(internalAdapter));
        WorkflowExecutionContextDTO context = new WorkflowExecutionContextDTO();
        context.setWorkflowCode("VALUATION_PARSE");
        context.setEngineType("INTERNAL");
        context.setWorkflowStageCode("PARSE");

        service.trigger(11L, "PARSE", context);

        verify(internalAdapter).trigger(eq("VALUATION_PARSE"), eq("PARSE"), anyMap());
    }

    @Test
    void shouldFallbackToSchedulerWhenContextMissing() {
        DefaultWorkflowEngineDispatchService service = new DefaultWorkflowEngineDispatchService(schedulerService, List.of());

        service.trigger(22L, "PARSE", null);

        verify(schedulerService).triggerNow(22L);
    }

    @Test
    void shouldFallbackToSchedulerWhenEngineTypeIsNotInternal() {
        DefaultWorkflowEngineDispatchService service = new DefaultWorkflowEngineDispatchService(schedulerService, List.of(internalAdapter));
        WorkflowExecutionContextDTO context = new WorkflowExecutionContextDTO();
        context.setWorkflowCode("VALUATION_PARSE");
        context.setEngineType("EXTERNAL");

        service.trigger(33L, "PARSE", context);

        verify(schedulerService).triggerNow(33L);
    }
}
