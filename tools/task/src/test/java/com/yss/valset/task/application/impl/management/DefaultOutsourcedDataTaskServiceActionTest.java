package com.yss.valset.task.application.impl.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.domain.gateway.WorkflowTaskGateway;
import com.yss.valset.domain.model.TaskStage;
import com.yss.valset.domain.model.TaskStatus;
import com.yss.valset.domain.model.TaskType;
import com.yss.valset.domain.model.WorkflowTask;
import com.yss.valset.task.application.command.OutsourcedDataTaskActionCommand;
import com.yss.valset.task.application.command.OutsourcedDataTaskBatchCommand;
import com.yss.valset.task.application.dto.OutsourcedDataTaskActionResultDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskBatchDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskStepDTO;
import com.yss.valset.task.application.port.OutsourcedDataTaskGateway;
import com.yss.valset.batch.scheduler.SchedulerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultOutsourcedDataTaskServiceActionTest {

    @Mock
    private OutsourcedDataTaskGateway outsourcedDataTaskGateway;

    private WorkflowTaskGateway workflowTaskGateway;

    @Mock
    private SchedulerService schedulerService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private DefaultOutsourcedDataTaskService service;

    @BeforeEach
    void setUp() {
        workflowTaskGateway = new WorkflowTaskGatewayStub();
        service = new DefaultOutsourcedDataTaskService(
                outsourcedDataTaskGateway,
                workflowTaskGateway,
                schedulerService,
                objectMapper);
    }

    @Test
    void shouldResumeFailedTaskByRetryingCurrentWorkflowTask() {
        when(outsourcedDataTaskGateway.findTask("BATCH-1")).thenReturn(Optional.of(buildBatch("BATCH-1", "FAILED")));
        when(outsourcedDataTaskGateway.listSteps("BATCH-1")).thenReturn(List.of(buildStep("BATCH-1", "STEP-1", "FAILED", true, "11")));
        ((WorkflowTaskGatewayStub) workflowTaskGateway).findByIdResult = buildWorkflowTask(11L, TaskStatus.FAILED);

        OutsourcedDataTaskActionResultDTO result = service.execute("BATCH-1", new OutsourcedDataTaskActionCommand());

        assertThat(result.isAccepted()).isTrue();
        assertThat(result.getAction()).isEqualTo("EXECUTE");
        assertThat(result.getStepId()).isEqualTo("STEP-1");
        assertThat(((WorkflowTaskGatewayStub) workflowTaskGateway).markRetryingCalled).isTrue();
        assertThat(((WorkflowTaskGatewayStub) workflowTaskGateway).markRetryingTaskId).isEqualTo(11L);
        verify(schedulerService).triggerNow(11L);
    }

    @Test
    void shouldCreateNewTaskForFullRerun() {
        when(outsourcedDataTaskGateway.findTask("BATCH-2")).thenReturn(Optional.of(buildBatch("BATCH-2", "FAILED")));
        when(outsourcedDataTaskGateway.listSteps("BATCH-2")).thenReturn(List.of(buildStep("BATCH-2", "STEP-2", "FAILED", true, "21")));
        ((WorkflowTaskGatewayStub) workflowTaskGateway).findByIdResult = buildWorkflowTask(21L, TaskStatus.FAILED);
        ((WorkflowTaskGatewayStub) workflowTaskGateway).saveResult = 22L;

        OutsourcedDataTaskActionResultDTO result = service.retry("BATCH-2", new OutsourcedDataTaskActionCommand());

        assertThat(result.isAccepted()).isTrue();
        assertThat(result.getAction()).isEqualTo("RETRY");
        assertThat(result.getStepId()).isEqualTo("STEP-2");
        WorkflowTask taskCaptor = ((WorkflowTaskGatewayStub) workflowTaskGateway).savedWorkflowTask;
        assertThat(taskCaptor).isNotNull();
        verify(schedulerService).triggerNow(22L);
        assertThat(taskCaptor.getTaskType()).isEqualTo(TaskType.PARSE_WORKBOOK);
        assertThat(taskCaptor.getTaskStatus()).isEqualTo(TaskStatus.PENDING);
        assertThat(taskCaptor.getTaskStage()).isEqualTo(TaskStage.PARSE);
    }

    @Test
    void shouldRetryStepByCloningCurrentWorkflowTask() {
        when(outsourcedDataTaskGateway.listSteps("BATCH-3")).thenReturn(List.of(buildStep("BATCH-3", "STEP-3", "FAILED", true, "31")));
        ((WorkflowTaskGatewayStub) workflowTaskGateway).findByIdResult = buildWorkflowTask(31L, TaskStatus.FAILED);
        ((WorkflowTaskGatewayStub) workflowTaskGateway).saveResult = 32L;

        OutsourcedDataTaskActionResultDTO result = service.retryStep("BATCH-3", "STEP-3", new OutsourcedDataTaskActionCommand());

        assertThat(result.isAccepted()).isTrue();
        assertThat(result.getAction()).isEqualTo("RETRY_STEP");
        assertThat(result.getStepId()).isEqualTo("STEP-3");
        verify(schedulerService).triggerNow(32L);
    }

    @Test
    void shouldBatchExecuteSelectedBatches() {
        when(outsourcedDataTaskGateway.findTask("BATCH-4")).thenReturn(Optional.of(buildBatch("BATCH-4", "FAILED")));
        when(outsourcedDataTaskGateway.listSteps("BATCH-4")).thenReturn(List.of(buildStep("BATCH-4", "STEP-4", "FAILED", true, "41")));
        ((WorkflowTaskGatewayStub) workflowTaskGateway).findByIdResult = buildWorkflowTask(41L, TaskStatus.FAILED);

        OutsourcedDataTaskBatchCommand command = new OutsourcedDataTaskBatchCommand();
        command.setBatchIds(List.of("BATCH-4"));

        List<OutsourcedDataTaskActionResultDTO> results = service.batchExecute(command);

        assertThat(results).hasSize(1);
        assertThat(((WorkflowTaskGatewayStub) workflowTaskGateway).markRetryingCalled).isTrue();
        verify(schedulerService).triggerNow(41L);
    }

    private static OutsourcedDataTaskBatchDTO buildBatch(String batchId, String status) {
        OutsourcedDataTaskBatchDTO batch = new OutsourcedDataTaskBatchDTO();
        batch.setBatchId(batchId);
        batch.setBatchName(batchId);
        batch.setStatus(status);
        batch.setCurrentStep("FILE_PARSE");
        batch.setCurrentStepName("文件解析");
        return batch;
    }

    private static OutsourcedDataTaskStepDTO buildStep(String batchId,
                                                       String stepId,
                                                       String status,
                                                       boolean currentFlag,
                                                       String taskId) {
        OutsourcedDataTaskStepDTO step = new OutsourcedDataTaskStepDTO();
        step.setBatchId(batchId);
        step.setStepId(stepId);
        step.setStage("FILE_PARSE");
        step.setStep("FILE_PARSE");
        step.setStageName("文件解析");
        step.setStepName("文件解析");
        step.setStatus(status);
        step.setCurrentFlag(currentFlag);
        step.setTaskId(taskId);
        step.setTaskType(TaskType.PARSE_WORKBOOK.name());
        return step;
    }

    private static WorkflowTask buildWorkflowTask(Long taskId, TaskStatus status) {
        return WorkflowTask.builder()
                .taskId(taskId)
                .taskType(TaskType.PARSE_WORKBOOK)
                .taskStatus(status)
                .taskStage(TaskStage.PARSE)
                .businessKey("PARSE:EXCEL:11")
                .fileId(11L)
                .inputPayload("{\"workbookPath\":\"/tmp/a.xlsx\"}")
                .build();
    }

    public static class WorkflowTaskGatewayStub implements WorkflowTaskGateway {
        private WorkflowTask findByIdResult;
        private Long saveResult = 99L;
        private WorkflowTask savedWorkflowTask;
        private boolean markRetryingCalled;
        private Long markRetryingTaskId;

        @Override
        public Long save(WorkflowTask workflowTask) {
            savedWorkflowTask = workflowTask;
            workflowTask.setTaskId(saveResult);
            return saveResult;
        }

        @Override
        public WorkflowTask findById(Long taskId) {
            return findByIdResult;
        }

        @Override
        public WorkflowTask findLatestSuccessfulTask(TaskType taskType, String businessKey) {
            return null;
        }

        @Override
        public boolean markRunning(Long taskId, String taskStage, java.time.LocalDateTime taskStartTime) {
            return true;
        }

        public boolean markRetrying(Long taskId) {
            markRetryingCalled = true;
            markRetryingTaskId = taskId;
            return true;
        }

        @Override
        public void updateTaskTimings(Long taskId, Long parseTaskTimeMs, Long standardizeTimeMs, Long matchStandardSubjectTimeMs) {
        }

        @Override
        public void markSuccess(Long taskId, String resultPayload) {
        }

        @Override
        public void markFailed(Long taskId, String errorMessage) {
        }
    }
}
