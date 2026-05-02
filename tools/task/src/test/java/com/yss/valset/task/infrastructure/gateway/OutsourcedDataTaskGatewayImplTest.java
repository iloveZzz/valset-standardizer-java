package com.yss.valset.task.infrastructure.gateway;

import com.yss.valset.application.event.lifecycle.ParseLifecycleEvent;
import com.yss.valset.application.event.lifecycle.ParseLifecycleStage;
import com.yss.valset.application.event.lifecycle.WorkflowTaskLifecycleEvent;
import com.yss.valset.domain.gateway.ValsetFileInfoGateway;
import com.yss.valset.domain.model.TaskStage;
import com.yss.valset.domain.model.TaskStatus;
import com.yss.valset.domain.model.TaskType;
import com.yss.valset.domain.model.ValsetFileInfo;
import com.yss.valset.task.application.config.OutsourcedDataTaskStageCatalog;
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
import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 估值表解析任务持久化网关测试。
 */
class OutsourcedDataTaskGatewayImplTest {

    @Test
    void shouldResolveMissingFileNameFromFileInfoWhenLoadingBatch() {
        OutsourcedDataTaskBatchRepository batchRepository = mock(OutsourcedDataTaskBatchRepository.class);
        OutsourcedDataTaskStepRepository stepRepository = mock(OutsourcedDataTaskStepRepository.class);
        OutsourcedDataTaskLogRepository logRepository = mock(OutsourcedDataTaskLogRepository.class);
        ValsetFileInfoGateway fileInfoGateway = mock(ValsetFileInfoGateway.class);
        when(batchRepository.selectById("FILE-2001")).thenReturn(batch("FILE-2001",
                "FILE-2001",
                "2026-04-30",
                "2026-04-30",
                "W2001",
                "委外产品X",
                "临时机构",
                "2001",
                null,
                null,
                "FILESYS",
                OutsourcedDataTaskStage.VERIFY_ARCHIVE,
                OutsourcedDataTaskStatus.SUCCESS,
                100,
                null,
                null));
        when(fileInfoGateway.findById(2001L)).thenReturn(ValsetFileInfo.builder()
                .fileId(2001L)
                .fileNameOriginal("委外产品X估值表.xlsx")
                .sourceMetaJson("{\"filesysFileId\":\"FS-2001\"}")
                .build());
        OutsourcedDataTaskGatewayImpl gateway = new OutsourcedDataTaskGatewayImpl(
                batchRepository,
                stepRepository,
                logRepository,
                fileInfoGateway
        );

        Optional<com.yss.valset.task.application.dto.OutsourcedDataTaskBatchDTO> batch = gateway.findTask("FILE-2001");

        assertThat(batch).isPresent();
        assertThat(batch.get().getOriginalFileName()).isEqualTo("委外产品X估值表.xlsx");
        assertThat(batch.get().getFilesysFileId()).isEqualTo("FS-2001");
        assertThat(batch.get().getFileId()).isEqualTo("2001");
    }

    @Test
    void shouldSynthesizeMissingStepsIntoFullChain() {
        OutsourcedDataTaskBatchRepository batchRepository = mock(OutsourcedDataTaskBatchRepository.class);
        OutsourcedDataTaskStepRepository stepRepository = mock(OutsourcedDataTaskStepRepository.class);
        OutsourcedDataTaskLogRepository logRepository = mock(OutsourcedDataTaskLogRepository.class);
        ValsetFileInfoGateway fileInfoGateway = mock(ValsetFileInfoGateway.class);
        when(batchRepository.selectById("FILE-3001")).thenReturn(batch("FILE-3001",
                "FILE-3001",
                "2026-04-30",
                "2026-04-30",
                "W3001",
                "委外产品Y",
                "临时机构",
                "3001",
                "FS-3001",
                "委外产品Y估值表.xlsx",
                "EMAIL",
                OutsourcedDataTaskStage.VERIFY_ARCHIVE,
                OutsourcedDataTaskStatus.SUCCESS,
                100,
                null,
                null));
        when(stepRepository.selectList(any())).thenReturn(List.of(
                step("FILE-3001", OutsourcedDataTaskStage.FILE_PARSE, OutsourcedDataTaskStatus.SUCCESS, 1),
                step("FILE-3001", OutsourcedDataTaskStage.VERIFY_ARCHIVE, OutsourcedDataTaskStatus.SUCCESS, 1)
        ));
        OutsourcedDataTaskGatewayImpl gateway = new OutsourcedDataTaskGatewayImpl(
                batchRepository,
                stepRepository,
                logRepository,
                fileInfoGateway
        );

        List<com.yss.valset.task.application.dto.OutsourcedDataTaskStepDTO> steps = gateway.listSteps("FILE-3001");

        assertThat(steps).hasSize(OutsourcedDataTaskStage.values().length - 1);
        assertThat(steps).extracting("stage").containsExactly(
                OutsourcedDataTaskStage.FILE_PARSE.name(),
                OutsourcedDataTaskStage.STRUCTURE_STANDARDIZE.name(),
                OutsourcedDataTaskStage.SUBJECT_RECOGNIZE.name(),
                OutsourcedDataTaskStage.STANDARD_LANDING.name(),
                OutsourcedDataTaskStage.DATA_PROCESSING.name(),
                OutsourcedDataTaskStage.VERIFY_ARCHIVE.name()
        );
        assertThat(steps).allSatisfy(step -> assertThat(((com.yss.valset.task.application.dto.OutsourcedDataTaskStepDTO) step).getStatus())
                .isEqualTo(OutsourcedDataTaskStatus.SUCCESS.name()));
        assertThat(steps).allSatisfy(step -> {
            com.yss.valset.task.application.dto.OutsourcedDataTaskStepDTO dto =
                    (com.yss.valset.task.application.dto.OutsourcedDataTaskStepDTO) step;
            assertThat(dto.getDurationText()).isNotEqualTo("已完成");
        });
        assertThat(steps.stream()
                .filter(step -> OutsourcedDataTaskStage.DATA_PROCESSING.name().equals(((com.yss.valset.task.application.dto.OutsourcedDataTaskStepDTO) step).getStage()))
                .map(step -> ((com.yss.valset.task.application.dto.OutsourcedDataTaskStepDTO) step).getDurationText())
                .toList())
                .containsOnly("-");
    }

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
        when(batchRepository.selectById(any())).thenAnswer(invocation -> batchSelectCount.incrementAndGet() >= 2 ? insertedBatch.get() : null);
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
        when(batchRepository.selectById(any())).thenAnswer(invocation -> batchSelectCount.incrementAndGet() >= 2 ? insertedBatch.get() : null);
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
    void shouldResolveWorkflowStageFromConfiguredCatalogBinding() {
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
        when(batchRepository.selectById(any())).thenAnswer(invocation -> batchSelectCount.incrementAndGet() >= 2 ? insertedBatch.get() : null);
        when(stepRepository.selectList(any())).thenAnswer(invocation -> stepSelectListCount.incrementAndGet() == 3 ? insertedSteps : List.of());

        OutsourcedDataTaskStageCatalog catalog = new OutsourcedDataTaskStageCatalog();
        catalog.getStages().stream()
                .filter(stage -> Objects.equals(stage.getStage(), OutsourcedDataTaskStage.STANDARD_LANDING.name()))
                .findFirst()
                .orElseThrow()
                .getTaskTypes()
                .add(TaskType.EVALUATE_MAPPING.name());

        OutsourcedDataTaskGatewayImpl gateway = new OutsourcedDataTaskGatewayImpl(
                batchRepository,
                stepRepository,
                logRepository,
                fileInfoGateway
        );
        gateway.setStageCatalog(catalog);

        gateway.recordWorkflowTaskLifecycleEvent(WorkflowTaskLifecycleEvent.builder()
                .eventId("event-configured-stage")
                .occurredAt(Instant.parse("2026-04-30T10:50:00Z"))
                .taskId(25L)
                .taskType(TaskType.EVALUATE_MAPPING)
                .taskStage(TaskStage.OTHER)
                .taskStatus(TaskStatus.SUCCESS)
                .fileId(1001L)
                .message("配置映射后的工作流任务执行成功")
                .build());

        ArgumentCaptor<OutsourcedDataTaskBatchPO> batchCaptor = ArgumentCaptor.forClass(OutsourcedDataTaskBatchPO.class);
        ArgumentCaptor<OutsourcedDataTaskBatchPO> batchUpdateCaptor = ArgumentCaptor.forClass(OutsourcedDataTaskBatchPO.class);
        ArgumentCaptor<OutsourcedDataTaskStepPO> stepCaptor = ArgumentCaptor.forClass(OutsourcedDataTaskStepPO.class);
        ArgumentCaptor<OutsourcedDataTaskLogPO> logCaptor = ArgumentCaptor.forClass(OutsourcedDataTaskLogPO.class);
        verify(batchRepository).insert(batchCaptor.capture());
        verify(batchRepository).updateById(batchUpdateCaptor.capture());
        verify(stepRepository).insert(stepCaptor.capture());
        verify(logRepository).insert(logCaptor.capture());

        assertThat(batchUpdateCaptor.getValue().getCurrentStage()).isEqualTo(OutsourcedDataTaskStage.STANDARD_LANDING.name());
        assertThat(stepCaptor.getValue().getStage()).isEqualTo(OutsourcedDataTaskStage.STANDARD_LANDING.name());
        assertThat(logCaptor.getValue().getStage()).isEqualTo(OutsourcedDataTaskStage.STANDARD_LANDING.name());
    }

    @Test
    void shouldRespectConfiguredWorkflowIgnoreAndStatusRules() {
        OutsourcedDataTaskBatchRepository batchRepository = mock(OutsourcedDataTaskBatchRepository.class);
        OutsourcedDataTaskStepRepository stepRepository = mock(OutsourcedDataTaskStepRepository.class);
        OutsourcedDataTaskLogRepository logRepository = mock(OutsourcedDataTaskLogRepository.class);
        ValsetFileInfoGateway fileInfoGateway = mock(ValsetFileInfoGateway.class);
        when(batchRepository.selectById(any())).thenReturn(null);
        when(stepRepository.selectById(any())).thenReturn(null);
        when(logRepository.selectById(any())).thenReturn(null);

        OutsourcedDataTaskStageCatalog catalog = new OutsourcedDataTaskStageCatalog();
        catalog.setIgnoredWorkflowTaskTypes(List.of());
        catalog.setSuccessTaskStatuses(List.of(TaskStatus.RETRYING.name()));
        catalog.setRunningTaskStatuses(List.of(TaskStatus.RUNNING.name()));
        catalog.getStages().stream()
                .filter(stage -> Objects.equals(stage.getStage(), OutsourcedDataTaskStage.FILE_PARSE.name()))
                .findFirst()
                .orElseThrow()
                .getTaskTypes()
                .add(TaskType.PARSE_WORKBOOK.name());
        catalog.getStages().stream()
                .filter(stage -> Objects.equals(stage.getStage(), OutsourcedDataTaskStage.FILE_PARSE.name()))
                .findFirst()
                .orElseThrow()
                .getTaskStages()
                .add(TaskStage.PARSE.name());

        OutsourcedDataTaskGatewayImpl gateway = new OutsourcedDataTaskGatewayImpl(
                batchRepository,
                stepRepository,
                logRepository,
                fileInfoGateway
        );
        gateway.setStageCatalog(catalog);

        gateway.recordWorkflowTaskLifecycleEvent(WorkflowTaskLifecycleEvent.builder()
                .eventId("event-parse-workbook-configurable")
                .occurredAt(Instant.parse("2026-04-30T10:55:00Z"))
                .taskId(26L)
                .taskType(TaskType.PARSE_WORKBOOK)
                .taskStage(TaskStage.PARSE)
                .taskStatus(TaskStatus.RETRYING)
                .fileId(1002L)
                .message("解析任务按配置进入任务页")
                .build());

        ArgumentCaptor<OutsourcedDataTaskBatchPO> batchCaptor = ArgumentCaptor.forClass(OutsourcedDataTaskBatchPO.class);
        ArgumentCaptor<OutsourcedDataTaskStepPO> stepCaptor = ArgumentCaptor.forClass(OutsourcedDataTaskStepPO.class);
        verify(batchRepository).insert(batchCaptor.capture());
        verify(stepRepository).insert(stepCaptor.capture());

        assertThat(batchCaptor.getValue().getCurrentStage()).isEqualTo(OutsourcedDataTaskStage.FILE_PARSE.name());
        assertThat(stepCaptor.getValue().getStatus()).isEqualTo(OutsourcedDataTaskStatus.SUCCESS.name());
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

    @Test
    void shouldPromoteEarlierRunningStepWhenLaterStagesAreCompleted() {
        OutsourcedDataTaskBatchRepository batchRepository = mock(OutsourcedDataTaskBatchRepository.class);
        OutsourcedDataTaskStepRepository stepRepository = mock(OutsourcedDataTaskStepRepository.class);
        OutsourcedDataTaskLogRepository logRepository = mock(OutsourcedDataTaskLogRepository.class);
        ValsetFileInfoGateway fileInfoGateway = mock(ValsetFileInfoGateway.class);
        OutsourcedDataTaskBatchPO batch = new OutsourcedDataTaskBatchPO();
        batch.setBatchId("FILE-1001");
        batch.setBatchName("FILE-1001");
        batch.setCurrentStage(OutsourcedDataTaskStage.FILE_PARSE.name());
        batch.setStatus(OutsourcedDataTaskStatus.RUNNING.name());

        List<OutsourcedDataTaskStepPO> currentSteps = new ArrayList<>();
        currentSteps.add(step("FILE-1001", OutsourcedDataTaskStage.FILE_PARSE, OutsourcedDataTaskStatus.RUNNING, 1));
        currentSteps.add(step("FILE-1001", OutsourcedDataTaskStage.STRUCTURE_STANDARDIZE, OutsourcedDataTaskStatus.SUCCESS, 1));
        currentSteps.add(step("FILE-1001", OutsourcedDataTaskStage.SUBJECT_RECOGNIZE, OutsourcedDataTaskStatus.SUCCESS, 1));
        currentSteps.add(step("FILE-1001", OutsourcedDataTaskStage.STANDARD_LANDING, OutsourcedDataTaskStatus.SUCCESS, 1));

        AtomicInteger stepSelectCount = new AtomicInteger();
        List<OutsourcedDataTaskStepPO> insertedSteps = new ArrayList<>(currentSteps);
        when(batchRepository.selectById(any())).thenReturn(batch);
        when(logRepository.selectById(any())).thenReturn(null);
        when(stepRepository.selectList(any())).thenAnswer(invocation -> {
            if (stepSelectCount.incrementAndGet() == 1) {
                return List.of();
            }
            return new ArrayList<>(insertedSteps);
        });
        org.mockito.Mockito.doAnswer(invocation -> {
            OutsourcedDataTaskStepPO value = invocation.getArgument(0);
            insertedSteps.add(value);
            return 1;
        }).when(stepRepository).insert(any(OutsourcedDataTaskStepPO.class));
        OutsourcedDataTaskGatewayImpl gateway = new OutsourcedDataTaskGatewayImpl(
                batchRepository,
                stepRepository,
                logRepository,
                fileInfoGateway
        );

        gateway.recordWorkflowTaskLifecycleEvent(WorkflowTaskLifecycleEvent.builder()
                .eventId("event-archive-success")
                .occurredAt(Instant.parse("2026-04-30T10:40:00Z"))
                .taskId(24L)
                .taskType(TaskType.EXPORT_RESULT)
                .taskStage(TaskStage.OTHER)
                .taskStatus(TaskStatus.SUCCESS)
                .fileId(1001L)
                .message("归档完成")
                .build());

        ArgumentCaptor<OutsourcedDataTaskStepPO> stepCaptor = ArgumentCaptor.forClass(OutsourcedDataTaskStepPO.class);
        ArgumentCaptor<OutsourcedDataTaskBatchPO> batchUpdateCaptor = ArgumentCaptor.forClass(OutsourcedDataTaskBatchPO.class);
        verify(stepRepository, org.mockito.Mockito.atLeastOnce()).updateById(stepCaptor.capture());
        verify(batchRepository, org.mockito.Mockito.atLeastOnce()).updateById(batchUpdateCaptor.capture());

        OutsourcedDataTaskStepPO promotedStep = stepCaptor.getAllValues().stream()
                .filter(step -> OutsourcedDataTaskStage.FILE_PARSE.name().equals(step.getStage()))
                .reduce((first, second) -> second)
                .orElseThrow();
        assertThat(promotedStep.getStatus()).isEqualTo(OutsourcedDataTaskStatus.SUCCESS.name());
        assertThat(promotedStep.getEndedAt()).isNotNull();

        OutsourcedDataTaskBatchPO aggregatedBatch = batchUpdateCaptor.getAllValues().get(batchUpdateCaptor.getAllValues().size() - 1);
        assertThat(aggregatedBatch.getCurrentStage()).isEqualTo(OutsourcedDataTaskStage.VERIFY_ARCHIVE.name());
        assertThat(aggregatedBatch.getStatus()).isEqualTo(OutsourcedDataTaskStatus.SUCCESS.name());
    }

    @Test
    void shouldPromoteFileParseStepAfterParseTaskSucceeds() {
        OutsourcedDataTaskBatchRepository batchRepository = mock(OutsourcedDataTaskBatchRepository.class);
        OutsourcedDataTaskStepRepository stepRepository = mock(OutsourcedDataTaskStepRepository.class);
        OutsourcedDataTaskLogRepository logRepository = mock(OutsourcedDataTaskLogRepository.class);
        ValsetFileInfoGateway fileInfoGateway = mock(ValsetFileInfoGateway.class);
        when(batchRepository.selectById(any())).thenReturn(null);
        when(logRepository.selectById(any())).thenReturn(null);
        AtomicReference<OutsourcedDataTaskBatchPO> insertedBatch = new AtomicReference<>();
        List<OutsourcedDataTaskStepPO> insertedSteps = new ArrayList<>();
        AtomicInteger batchSelectCount = new AtomicInteger();
        AtomicInteger stepSelectCount = new AtomicInteger();
        org.mockito.Mockito.doAnswer(invocation -> {
            insertedBatch.set(invocation.getArgument(0));
            return 1;
        }).when(batchRepository).insert(any(OutsourcedDataTaskBatchPO.class));
        org.mockito.Mockito.doAnswer(invocation -> {
            insertedSteps.add(invocation.getArgument(0));
            return 1;
        }).when(stepRepository).insert(any(OutsourcedDataTaskStepPO.class));
        when(batchRepository.selectById(any())).thenAnswer(invocation -> {
            batchSelectCount.incrementAndGet();
            return insertedBatch.get();
        });
        when(stepRepository.selectList(any())).thenAnswer(invocation -> {
            int count = stepSelectCount.incrementAndGet();
            if (count == 1 || count == 2 || count == 4 || count == 5) {
                return List.of();
            }
            return new ArrayList<>(insertedSteps);
        });
        OutsourcedDataTaskGatewayImpl gateway = new OutsourcedDataTaskGatewayImpl(
                batchRepository,
                stepRepository,
                logRepository,
                fileInfoGateway
        );

        gateway.recordParseLifecycleEvent(ParseLifecycleEvent.builder()
                .eventId("event-started")
                .occurredAt(Instant.parse("2026-04-30T10:00:00Z"))
                .stage(ParseLifecycleStage.TASK_EXECUTION_STARTED)
                .source("parse-execution")
                .taskId(31L)
                .fileId(1001L)
                .businessKey("WORKFLOW:PARSE:EXCEL:1001")
                .message("开始执行解析任务")
                .build());

        gateway.recordParseLifecycleEvent(ParseLifecycleEvent.builder()
                .eventId("event-succeeded")
                .occurredAt(Instant.parse("2026-04-30T10:05:00Z"))
                .stage(ParseLifecycleStage.TASK_SUCCEEDED)
                .source("parse-execution")
                .taskId(31L)
                .fileId(1001L)
                .businessKey("WORKFLOW:PARSE:EXCEL:1001")
                .message("解析任务执行成功")
                .build());

        ArgumentCaptor<OutsourcedDataTaskStepPO> stepCaptor = ArgumentCaptor.forClass(OutsourcedDataTaskStepPO.class);
        ArgumentCaptor<OutsourcedDataTaskBatchPO> batchUpdateCaptor = ArgumentCaptor.forClass(OutsourcedDataTaskBatchPO.class);
        verify(stepRepository, org.mockito.Mockito.atLeastOnce()).updateById(stepCaptor.capture());
        verify(batchRepository, org.mockito.Mockito.atLeastOnce()).updateById(batchUpdateCaptor.capture());

        OutsourcedDataTaskStepPO fileParseStep = stepCaptor.getAllValues().stream()
                .filter(step -> OutsourcedDataTaskStage.FILE_PARSE.name().equals(step.getStage()))
                .reduce((first, second) -> second)
                .orElseThrow();
        assertThat(fileParseStep.getStatus()).isEqualTo(OutsourcedDataTaskStatus.SUCCESS.name());
        assertThat(fileParseStep.getEndedAt()).isNotNull();

        OutsourcedDataTaskBatchPO aggregatedBatch = batchUpdateCaptor.getAllValues().get(batchUpdateCaptor.getAllValues().size() - 1);
        assertThat(aggregatedBatch.getCurrentStage()).isEqualTo(OutsourcedDataTaskStage.VERIFY_ARCHIVE.name());
        assertThat(aggregatedBatch.getStatus()).isEqualTo(OutsourcedDataTaskStatus.SUCCESS.name());
    }

    private static OutsourcedDataTaskStepPO step(String batchId,
                                                 OutsourcedDataTaskStage stage,
                                                 OutsourcedDataTaskStatus status,
                                                 int runNo) {
        OutsourcedDataTaskStepPO step = new OutsourcedDataTaskStepPO();
        step.setStepId(batchId + "-" + stage.name() + "-" + runNo);
        step.setBatchId(batchId);
        step.setStage(stage.name());
        step.setRunNo(runNo);
        step.setCurrentFlag(true);
        step.setStatus(status.name());
        step.setProgress(status == OutsourcedDataTaskStatus.SUCCESS ? 100 : 50);
        step.setStartedAt(Instant.parse("2026-04-30T09:30:00Z").atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
        if (status == OutsourcedDataTaskStatus.SUCCESS) {
            step.setEndedAt(Instant.parse("2026-04-30T09:32:00Z").atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
            step.setDurationMs(120000L);
        }
        return step;
    }

    private static OutsourcedDataTaskBatchPO batch(String batchId,
                                                   String batchName,
                                                   String businessDate,
                                                   String valuationDate,
                                                   String productCode,
                                                   String productName,
                                                   String managerName,
                                                   String fileId,
                                                   String filesysFileId,
                                                   String originalFileName,
                                                   String sourceType,
                                                   OutsourcedDataTaskStage currentStage,
                                                   OutsourcedDataTaskStatus status,
                                                   Integer progress,
                                                   String errorCode,
                                                   String errorMessage) {
        OutsourcedDataTaskBatchPO batch = new OutsourcedDataTaskBatchPO();
        batch.setBatchId(batchId);
        batch.setBatchName(batchName);
        batch.setBusinessDate(java.time.LocalDate.parse(businessDate));
        batch.setValuationDate(java.time.LocalDate.parse(valuationDate));
        batch.setProductCode(productCode);
        batch.setProductName(productName);
        batch.setManagerName(managerName);
        batch.setFileId(fileId);
        batch.setFilesysFileId(filesysFileId);
        batch.setOriginalFileName(originalFileName);
        batch.setSourceType(sourceType);
        batch.setCurrentStage(currentStage.name());
        batch.setStatus(status.name());
        batch.setProgress(progress);
        batch.setStartedAt(Instant.parse("2026-04-30T09:30:00Z").atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
        if (status != OutsourcedDataTaskStatus.RUNNING) {
            batch.setEndedAt(Instant.parse("2026-04-30T09:42:00Z").atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
            batch.setDurationMs(720000L);
        }
        batch.setLastErrorCode(errorCode);
        batch.setLastErrorMessage(errorMessage);
        return batch;
    }
}
