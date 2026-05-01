package com.yss.valset.task.infrastructure.gateway;

import com.yss.valset.application.event.lifecycle.ParseLifecycleEvent;
import com.yss.valset.application.event.lifecycle.ParseLifecycleStage;
import com.yss.valset.application.event.lifecycle.WorkflowTaskLifecycleEvent;
import com.yss.valset.domain.gateway.ValsetFileInfoGateway;
import com.yss.valset.domain.model.TaskStage;
import com.yss.valset.domain.model.TaskStatus;
import com.yss.valset.domain.model.TaskType;
import com.yss.valset.domain.model.ValsetFileInfo;
import com.yss.valset.task.domain.model.OutsourcedDataTaskStage;
import com.yss.valset.task.domain.model.OutsourcedDataTaskStatus;
import com.yss.valset.task.infrastructure.entity.OutsourcedDataTaskBatchPO;
import com.yss.valset.task.infrastructure.entity.OutsourcedDataTaskLogPO;
import com.yss.valset.task.infrastructure.entity.OutsourcedDataTaskStepPO;
import com.yss.valset.task.infrastructure.mapper.OutsourcedDataTaskBatchRepository;
import com.yss.valset.task.infrastructure.mapper.OutsourcedDataTaskLogRepository;
import com.yss.valset.task.infrastructure.mapper.OutsourcedDataTaskStepRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 委外数据任务持久化网关测试。
 */
class OutsourcedDataTaskGatewayImplTest {

    @Test
    void shouldArchiveParseLifecycleEventAsBatchStepsAndLog() {
        OutsourcedDataTaskBatchRepository batchRepository = mock(OutsourcedDataTaskBatchRepository.class);
        OutsourcedDataTaskStepRepository stepRepository = mock(OutsourcedDataTaskStepRepository.class);
        OutsourcedDataTaskLogRepository logRepository = mock(OutsourcedDataTaskLogRepository.class);
        ValsetFileInfoGateway fileInfoGateway = mock(ValsetFileInfoGateway.class);
        when(batchRepository.selectById(any())).thenReturn(null);
        when(stepRepository.selectById(any())).thenReturn(null);
        when(logRepository.selectById(any())).thenReturn(null);
        when(fileInfoGateway.findById(1001L)).thenReturn(ValsetFileInfo.builder()
                .fileId(1001L)
                .fileNameOriginal("委外产品5估值表.xlsx")
                .fileFingerprint("fingerprint-001")
                .sourceMetaJson("{\"filesysFileId\":\"FS-1001\"}")
                .build());
        AtomicReference<OutsourcedDataTaskBatchPO> insertedBatch = new AtomicReference<>();
        List<OutsourcedDataTaskStepPO> insertedSteps = new ArrayList<>();
        AtomicInteger batchSelectCount = new AtomicInteger();
        AtomicInteger stepSelectListCount = new AtomicInteger();
        org.mockito.Mockito.doAnswer(invocation -> {
            insertedBatch.set(invocation.getArgument(0));
            return 1;
        }).when(batchRepository).insert(any(OutsourcedDataTaskBatchPO.class));
        org.mockito.Mockito.doAnswer(invocation -> {
            insertedSteps.add(invocation.getArgument(0));
            return 1;
        }).when(stepRepository).insert(any(OutsourcedDataTaskStepPO.class));
        when(batchRepository.selectById(any())).thenAnswer(invocation -> batchSelectCount.incrementAndGet() == 2 ? insertedBatch.get() : null);
        when(stepRepository.selectList(any())).thenAnswer(invocation -> stepSelectListCount.incrementAndGet() == 5 ? insertedSteps : List.of());
        OutsourcedDataTaskGatewayImpl gateway = new OutsourcedDataTaskGatewayImpl(
                batchRepository,
                stepRepository,
                logRepository,
                fileInfoGateway
        );

        gateway.recordParseLifecycleEvent(ParseLifecycleEvent.builder()
                .eventId("event-standardized")
                .occurredAt(Instant.parse("2026-04-30T10:00:00Z"))
                .stage(ParseLifecycleStage.TASK_STANDARDIZED)
                .source("parse-execution")
                .taskId(11L)
                .fileId(1001L)
                .businessKey("WORKFLOW:PARSE:EXCEL:1001")
                .message("标准化完成")
                .build());

        ArgumentCaptor<OutsourcedDataTaskBatchPO> batchCaptor = ArgumentCaptor.forClass(OutsourcedDataTaskBatchPO.class);
        ArgumentCaptor<OutsourcedDataTaskBatchPO> batchUpdateCaptor = ArgumentCaptor.forClass(OutsourcedDataTaskBatchPO.class);
        ArgumentCaptor<OutsourcedDataTaskStepPO> stepCaptor = ArgumentCaptor.forClass(OutsourcedDataTaskStepPO.class);
        ArgumentCaptor<OutsourcedDataTaskLogPO> logCaptor = ArgumentCaptor.forClass(OutsourcedDataTaskLogPO.class);
        verify(batchRepository).insert(batchCaptor.capture());
        verify(batchRepository).updateById(batchUpdateCaptor.capture());
        verify(stepRepository, org.mockito.Mockito.times(2)).insert(stepCaptor.capture());
        verify(logRepository).insert(logCaptor.capture());

        OutsourcedDataTaskBatchPO batch = batchCaptor.getValue();
        assertThat(batch.getBatchId()).isEqualTo("FILE-1001");
        assertThat(batch.getFileId()).isEqualTo("1001");
        assertThat(batch.getFilesysFileId()).isEqualTo("FS-1001");
        assertThat(batch.getOriginalFileName()).isEqualTo("委外产品5估值表.xlsx");
        assertThat(batch.getFileFingerprint()).isEqualTo("fingerprint-001");

        OutsourcedDataTaskBatchPO aggregatedBatch = batchUpdateCaptor.getValue();
        assertThat(aggregatedBatch.getBatchId()).isEqualTo("FILE-1001");
        assertThat(aggregatedBatch.getCurrentStage()).isEqualTo(OutsourcedDataTaskStage.SUBJECT_RECOGNIZE.name());
        assertThat(aggregatedBatch.getStatus()).isEqualTo(OutsourcedDataTaskStatus.SUCCESS.name());

        List<String> stages = stepCaptor.getAllValues().stream()
                .map(OutsourcedDataTaskStepPO::getStage)
                .toList();
        assertThat(stages).containsExactly(
                OutsourcedDataTaskStage.STRUCTURE_STANDARDIZE.name(),
                OutsourcedDataTaskStage.SUBJECT_RECOGNIZE.name()
        );
        assertThat(stepCaptor.getAllValues())
                .allSatisfy(step -> assertThat(step.getStatus()).isEqualTo(OutsourcedDataTaskStatus.SUCCESS.name()));
        assertThat(stepCaptor.getAllValues())
                .allSatisfy(step -> {
                    assertThat(step.getCurrentFlag()).isTrue();
                    assertThat(step.getStepId()).endsWith("-1");
                });

        OutsourcedDataTaskLogPO log = logCaptor.getValue();
        assertThat(log.getLogId()).isEqualTo("event-standardized");
        assertThat(log.getBatchId()).isEqualTo("FILE-1001");
        assertThat(log.getStage()).isEqualTo(OutsourcedDataTaskStage.STRUCTURE_STANDARDIZE.name());
        assertThat(log.getMessage()).isEqualTo("标准化完成");
    }

    @Test
    void shouldArchiveNonParseWorkflowTaskEventAsDataProcessingStep() {
        OutsourcedDataTaskBatchRepository batchRepository = mock(OutsourcedDataTaskBatchRepository.class);
        OutsourcedDataTaskStepRepository stepRepository = mock(OutsourcedDataTaskStepRepository.class);
        OutsourcedDataTaskLogRepository logRepository = mock(OutsourcedDataTaskLogRepository.class);
        ValsetFileInfoGateway fileInfoGateway = mock(ValsetFileInfoGateway.class);
        when(batchRepository.selectById(any())).thenReturn(null);
        when(stepRepository.selectById(any())).thenReturn(null);
        when(logRepository.selectById(any())).thenReturn(null);
        AtomicReference<OutsourcedDataTaskBatchPO> insertedBatch = new AtomicReference<>();
        List<OutsourcedDataTaskStepPO> insertedSteps = new ArrayList<>();
        AtomicInteger batchSelectCount = new AtomicInteger();
        AtomicInteger stepSelectListCount = new AtomicInteger();
        org.mockito.Mockito.doAnswer(invocation -> {
            insertedBatch.set(invocation.getArgument(0));
            return 1;
        }).when(batchRepository).insert(any(OutsourcedDataTaskBatchPO.class));
        org.mockito.Mockito.doAnswer(invocation -> {
            insertedSteps.add(invocation.getArgument(0));
            return 1;
        }).when(stepRepository).insert(any(OutsourcedDataTaskStepPO.class));
        when(batchRepository.selectById(any())).thenAnswer(invocation -> batchSelectCount.incrementAndGet() == 2 ? insertedBatch.get() : null);
        when(stepRepository.selectList(any())).thenAnswer(invocation -> stepSelectListCount.incrementAndGet() == 3 ? insertedSteps : List.of());
        OutsourcedDataTaskGatewayImpl gateway = new OutsourcedDataTaskGatewayImpl(
                batchRepository,
                stepRepository,
                logRepository,
                fileInfoGateway
        );

        gateway.recordWorkflowTaskLifecycleEvent(WorkflowTaskLifecycleEvent.builder()
                .eventId("event-processing-success")
                .occurredAt(Instant.parse("2026-04-30T10:10:00Z"))
                .taskId(22L)
                .taskType(TaskType.EVALUATE_MAPPING)
                .taskStage(TaskStage.OTHER)
                .taskStatus(TaskStatus.SUCCESS)
                .businessKey("TODO:PROCESS:W213412")
                .fileId(1001L)
                .message("工作流任务执行成功")
                .build());

        ArgumentCaptor<OutsourcedDataTaskBatchPO> batchCaptor = ArgumentCaptor.forClass(OutsourcedDataTaskBatchPO.class);
        ArgumentCaptor<OutsourcedDataTaskBatchPO> batchUpdateCaptor = ArgumentCaptor.forClass(OutsourcedDataTaskBatchPO.class);
        ArgumentCaptor<OutsourcedDataTaskStepPO> stepCaptor = ArgumentCaptor.forClass(OutsourcedDataTaskStepPO.class);
        ArgumentCaptor<OutsourcedDataTaskLogPO> logCaptor = ArgumentCaptor.forClass(OutsourcedDataTaskLogPO.class);
        verify(batchRepository).insert(batchCaptor.capture());
        verify(batchRepository).updateById(batchUpdateCaptor.capture());
        verify(stepRepository).insert(stepCaptor.capture());
        verify(logRepository).insert(logCaptor.capture());

        assertThat(batchCaptor.getValue().getBatchId()).isEqualTo("FILE-1001");
        assertThat(batchUpdateCaptor.getValue().getCurrentStage()).isEqualTo(OutsourcedDataTaskStage.DATA_PROCESSING.name());
        assertThat(batchUpdateCaptor.getValue().getStatus()).isEqualTo(OutsourcedDataTaskStatus.SUCCESS.name());
        assertThat(stepCaptor.getValue().getStage()).isEqualTo(OutsourcedDataTaskStage.DATA_PROCESSING.name());
        assertThat(stepCaptor.getValue().getTaskType()).isEqualTo(TaskType.EVALUATE_MAPPING.name());
        assertThat(stepCaptor.getValue().getStatus()).isEqualTo(OutsourcedDataTaskStatus.SUCCESS.name());
        assertThat(stepCaptor.getValue().getCurrentFlag()).isTrue();
        assertThat(logCaptor.getValue().getStage()).isEqualTo(OutsourcedDataTaskStage.DATA_PROCESSING.name());
    }

    @Test
    void shouldResolveBatchByAttributeFileIdWhenLifecycleEventHasNoFileId() {
        OutsourcedDataTaskBatchRepository batchRepository = mock(OutsourcedDataTaskBatchRepository.class);
        OutsourcedDataTaskStepRepository stepRepository = mock(OutsourcedDataTaskStepRepository.class);
        OutsourcedDataTaskLogRepository logRepository = mock(OutsourcedDataTaskLogRepository.class);
        ValsetFileInfoGateway fileInfoGateway = mock(ValsetFileInfoGateway.class);
        when(batchRepository.selectById(any())).thenReturn(null);
        when(stepRepository.selectById(any())).thenReturn(null);
        when(logRepository.selectById(any())).thenReturn(null);
        OutsourcedDataTaskGatewayImpl gateway = new OutsourcedDataTaskGatewayImpl(
                batchRepository,
                stepRepository,
                logRepository,
                fileInfoGateway
        );

        gateway.recordParseLifecycleEvent(ParseLifecycleEvent.builder()
                .eventId("event-file-info-repaired")
                .occurredAt(Instant.parse("2026-04-30T10:20:00Z"))
                .stage(ParseLifecycleStage.QUEUE_FILE_INFO_REPAIR_COMPLETED)
                .queueId("3001")
                .businessKey("transfer-3001:VALUATION_TABLE")
                .attributes(Map.of("fileId", 1001L))
                .message("文件主数据自动修复完成")
                .build());

        ArgumentCaptor<OutsourcedDataTaskBatchPO> batchCaptor = ArgumentCaptor.forClass(OutsourcedDataTaskBatchPO.class);
        verify(batchRepository).insert(batchCaptor.capture());
        assertThat(batchCaptor.getValue().getBatchId()).isEqualTo("FILE-1001");
        assertThat(batchCaptor.getValue().getFileId()).isEqualTo("1001");
    }

    @Test
    void shouldCreateNewCurrentRunWhenTerminalStepIsRerun() {
        OutsourcedDataTaskBatchRepository batchRepository = mock(OutsourcedDataTaskBatchRepository.class);
        OutsourcedDataTaskStepRepository stepRepository = mock(OutsourcedDataTaskStepRepository.class);
        OutsourcedDataTaskLogRepository logRepository = mock(OutsourcedDataTaskLogRepository.class);
        ValsetFileInfoGateway fileInfoGateway = mock(ValsetFileInfoGateway.class);
        OutsourcedDataTaskBatchPO batch = new OutsourcedDataTaskBatchPO();
        batch.setBatchId("FILE-1001");
        batch.setBatchName("FILE-1001");
        batch.setCurrentStage(OutsourcedDataTaskStage.DATA_PROCESSING.name());
        batch.setStatus(OutsourcedDataTaskStatus.SUCCESS.name());
        OutsourcedDataTaskStepPO oldStep = new OutsourcedDataTaskStepPO();
        oldStep.setStepId("FILE-1001-DATA_PROCESSING-1");
        oldStep.setBatchId("FILE-1001");
        oldStep.setStage(OutsourcedDataTaskStage.DATA_PROCESSING.name());
        oldStep.setRunNo(1);
        oldStep.setCurrentFlag(true);
        oldStep.setStatus(OutsourcedDataTaskStatus.SUCCESS.name());
        List<OutsourcedDataTaskStepPO> insertedSteps = new ArrayList<>();
        AtomicInteger stepSelectListCount = new AtomicInteger();
        when(batchRepository.selectById(any())).thenReturn(batch);
        when(logRepository.selectById(any())).thenReturn(null);
        when(stepRepository.selectList(any())).thenAnswer(invocation -> {
            int count = stepSelectListCount.incrementAndGet();
            if (count == 1 || count == 2) {
                return List.of(oldStep);
            }
            return insertedSteps;
        });
        org.mockito.Mockito.doAnswer(invocation -> {
            insertedSteps.add(invocation.getArgument(0));
            return 1;
        }).when(stepRepository).insert(any(OutsourcedDataTaskStepPO.class));
        OutsourcedDataTaskGatewayImpl gateway = new OutsourcedDataTaskGatewayImpl(
                batchRepository,
                stepRepository,
                logRepository,
                fileInfoGateway
        );

        gateway.recordWorkflowTaskLifecycleEvent(WorkflowTaskLifecycleEvent.builder()
                .eventId("event-processing-rerun")
                .occurredAt(Instant.parse("2026-04-30T10:30:00Z"))
                .taskId(23L)
                .taskType(TaskType.EVALUATE_MAPPING)
                .taskStage(TaskStage.OTHER)
                .taskStatus(TaskStatus.RUNNING)
                .fileId(1001L)
                .message("工作流任务重新执行")
                .build());

        ArgumentCaptor<OutsourcedDataTaskStepPO> historicalStepCaptor = ArgumentCaptor.forClass(OutsourcedDataTaskStepPO.class);
        ArgumentCaptor<OutsourcedDataTaskStepPO> newStepCaptor = ArgumentCaptor.forClass(OutsourcedDataTaskStepPO.class);
        ArgumentCaptor<OutsourcedDataTaskBatchPO> batchUpdateCaptor = ArgumentCaptor.forClass(OutsourcedDataTaskBatchPO.class);
        verify(stepRepository).updateById(historicalStepCaptor.capture());
        verify(stepRepository).insert(newStepCaptor.capture());
        verify(batchRepository, org.mockito.Mockito.atLeastOnce()).updateById(batchUpdateCaptor.capture());

        assertThat(historicalStepCaptor.getValue().getCurrentFlag()).isFalse();
        assertThat(newStepCaptor.getValue().getStepId()).isEqualTo("FILE-1001-DATA_PROCESSING-2");
        assertThat(newStepCaptor.getValue().getRunNo()).isEqualTo(2);
        assertThat(newStepCaptor.getValue().getCurrentFlag()).isTrue();
        assertThat(batchUpdateCaptor.getAllValues().get(batchUpdateCaptor.getAllValues().size() - 1).getStatus())
                .isEqualTo(OutsourcedDataTaskStatus.RUNNING.name());
    }
}
