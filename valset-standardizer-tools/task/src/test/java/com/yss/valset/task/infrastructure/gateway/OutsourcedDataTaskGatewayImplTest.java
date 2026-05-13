package com.yss.valset.task.infrastructure.gateway;

import com.yss.valset.application.event.lifecycle.ParseLifecycleEvent;
import com.yss.valset.application.event.lifecycle.WorkflowTaskLifecycleEvent;
import com.yss.valset.common.support.DatabaseDialectSupport;
import com.yss.valset.domain.gateway.ValsetFileInfoGateway;
import com.yss.valset.domain.model.TaskStage;
import com.yss.valset.domain.model.TaskStatus;
import com.yss.valset.domain.model.TaskType;
import com.yss.valset.domain.model.ValsetFileInfo;
import com.yss.valset.task.application.service.workflow.WorkflowRuntimeCatalog;
import com.yss.valset.task.domain.model.OutsourcedDataTaskStage;
import com.yss.valset.task.domain.model.OutsourcedDataTaskStatus;
import com.yss.valset.task.infrastructure.entity.OutsourcedDataTaskBatchPO;
import com.yss.valset.task.infrastructure.entity.OutsourcedDataTaskLogPO;
import com.yss.valset.task.infrastructure.entity.OutsourcedDataTaskStepPO;
import com.yss.valset.task.infrastructure.mapper.OutsourcedDataTaskBatchRepository;
import com.yss.valset.task.infrastructure.mapper.OutsourcedDataTaskLogRepository;
import com.yss.valset.task.infrastructure.mapper.OutsourcedDataTaskStepRepository;
import com.yss.valset.task.infrastructure.entity.workflow.OutsourcedWorkflowDefinitionPO;
import com.yss.valset.task.infrastructure.entity.workflow.OutsourcedWorkflowStagePO;
import com.yss.valset.task.infrastructure.mapper.workflow.OutsourcedWorkflowDefinitionRepository;
import com.yss.valset.task.infrastructure.mapper.workflow.OutsourcedWorkflowStageRepository;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
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
                                OutsourcedDataTaskStage.STANDARD_LANDING,
                                OutsourcedDataTaskStatus.SUCCESS,
                                100,
                                null,
                                null));
                when(fileInfoGateway.findById(2001L)).thenReturn(ValsetFileInfo.builder()
                                .fileId(2001L)
                                .fileNameOriginal("委外产品X估值表.xlsx")
                                .businessDate(java.time.LocalDate.parse("2026-04-29"))
                                .sourceMetaJson("{\"filesysFileId\":\"FS-2001\"}")
                                .build());
                OutsourcedDataTaskGatewayImpl gateway = newGateway(
                                batchRepository,
                                stepRepository,
                                logRepository,
                                fileInfoGateway);
                WorkflowRuntimeCatalog stageCatalog = defaultCatalog();
                assertThat(stageCatalog.normalizeStage("TASK_SUCCEEDED"))
                                .isEqualTo(OutsourcedDataTaskStage.STANDARD_LANDING);
                gateway.setStageCatalog(stageCatalog);

                Optional<com.yss.valset.task.application.dto.OutsourcedDataTaskBatchDTO> batch = gateway
                                .findTask("FILE-2001");

                assertThat(batch).isPresent();
                assertThat(batch.get().getOriginalFileName()).isEqualTo("委外产品X估值表.xlsx");
                assertThat(batch.get().getFilesysFileId()).isEqualTo("FS-2001");
                assertThat(batch.get().getFileId()).isEqualTo("2001");
                assertThat(batch.get().getBusinessDate()).isEqualTo("2026-04-29");
        }

        @Test
        void shouldResolveBatchDurationFromFileParseStartToVerifyArchiveEnd() {
                OutsourcedDataTaskBatchRepository batchRepository = mock(OutsourcedDataTaskBatchRepository.class);
                OutsourcedDataTaskStepRepository stepRepository = mock(OutsourcedDataTaskStepRepository.class);
                OutsourcedDataTaskLogRepository logRepository = mock(OutsourcedDataTaskLogRepository.class);
                ValsetFileInfoGateway fileInfoGateway = mock(ValsetFileInfoGateway.class);
                when(batchRepository.selectById("FILE-2002")).thenReturn(batch("FILE-2002",
                                "FILE-2002",
                                "2026-04-30",
                                "2026-04-30",
                                "W2002",
                                "委外产品Z",
                                "临时机构",
                                "2002",
                                null,
                                null,
                                "EMAIL",
                                OutsourcedDataTaskStage.STANDARD_LANDING,
                                OutsourcedDataTaskStatus.SUCCESS,
                                100,
                                null,
                                null));
                OutsourcedDataTaskStepPO fileParseStep = new OutsourcedDataTaskStepPO();
                fileParseStep.setStepId("FILE-2002-FILE_PARSE-1");
                fileParseStep.setBatchId("FILE-2002");
                fileParseStep.setStage(OutsourcedDataTaskStage.FILE_PARSE.name());
                fileParseStep.setRunNo(1);
                fileParseStep.setCurrentFlag(true);
                fileParseStep.setStatus(OutsourcedDataTaskStatus.SUCCESS.name());
                fileParseStep.setStartedAt(Instant.parse("2026-04-30T10:00:00Z")
                                .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
                fileParseStep.setEndedAt(Instant.parse("2026-04-30T10:01:00Z").atZone(java.time.ZoneId.systemDefault())
                                .toLocalDateTime());
                fileParseStep.setDurationMs(60000L);

                OutsourcedDataTaskStepPO landingStep = new OutsourcedDataTaskStepPO();
                landingStep.setStepId("FILE-2002-STANDARD_LANDING-1");
                landingStep.setBatchId("FILE-2002");
                landingStep.setStage(OutsourcedDataTaskStage.STANDARD_LANDING.name());
                landingStep.setRunNo(1);
                landingStep.setCurrentFlag(true);
                landingStep.setStatus(OutsourcedDataTaskStatus.SUCCESS.name());
                landingStep.setStartedAt(Instant.parse("2026-04-30T10:10:00Z")
                                .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
                landingStep.setEndedAt(Instant.parse("2026-04-30T10:12:00Z")
                                .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
                landingStep.setDurationMs(120000L);
                when(stepRepository.selectList(any())).thenReturn(List.of(fileParseStep, landingStep));

                OutsourcedDataTaskGatewayImpl gateway = newGateway(
                                batchRepository,
                                stepRepository,
                                logRepository,
                                fileInfoGateway);

                Optional<com.yss.valset.task.application.dto.OutsourcedDataTaskBatchDTO> batch = gateway
                                .findTask("FILE-2002");

                assertThat(batch).isPresent();
                assertThat(batch.get().getStartedAt()).isEqualTo("2026-04-30 18:00:00");
                assertThat(batch.get().getDurationMs()).isEqualTo(720000L);
                assertThat(batch.get().getDurationText()).isEqualTo("12m");
        }

        @Test
        void shouldFilterTasksByTaskDateUsingBatchStartedAt() {
                OutsourcedDataTaskBatchRepository batchRepository = mock(OutsourcedDataTaskBatchRepository.class);
                OutsourcedDataTaskStepRepository stepRepository = mock(OutsourcedDataTaskStepRepository.class);
                OutsourcedDataTaskLogRepository logRepository = mock(OutsourcedDataTaskLogRepository.class);
                ValsetFileInfoGateway fileInfoGateway = mock(ValsetFileInfoGateway.class);
                if (TableInfoHelper.getTableInfo(OutsourcedDataTaskBatchPO.class) == null) {
                        TableInfoHelper.initTableInfo(
                                        new org.apache.ibatis.builder.MapperBuilderAssistant(new MybatisConfiguration(),
                                                        "test"),
                                        OutsourcedDataTaskBatchPO.class);
                }
                when(batchRepository.selectPage(any(), any())).thenAnswer(invocation -> {
                        Wrapper<?> wrapper = invocation.getArgument(1);
                        assertThat(wrapper.getSqlSegment()).contains("started_at");
                        assertThat(wrapper.getSqlSegment()).doesNotContain("business_date");
                        Page<OutsourcedDataTaskBatchPO> page = new Page<>();
                        page.setRecords(List.of());
                        page.setTotal(0);
                        return page;
                });
                OutsourcedDataTaskGatewayImpl gateway = newGateway(
                                batchRepository,
                                stepRepository,
                                logRepository,
                                fileInfoGateway);

                com.yss.valset.task.application.command.OutsourcedDataTaskQueryCommand query = new com.yss.valset.task.application.command.OutsourcedDataTaskQueryCommand();
                query.setTaskDate("2026-04-30");
                gateway.pageTasks(query);
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
                                OutsourcedDataTaskStage.STANDARD_LANDING,
                                OutsourcedDataTaskStatus.SUCCESS,
                                100,
                                null,
                                null));
                when(stepRepository.selectList(any())).thenReturn(List.of(
                                step("FILE-3001", OutsourcedDataTaskStage.FILE_PARSE, OutsourcedDataTaskStatus.SUCCESS,
                                                1),
                                step("FILE-3001", OutsourcedDataTaskStage.STANDARD_LANDING,
                                                OutsourcedDataTaskStatus.SUCCESS, 1)));
                OutsourcedDataTaskGatewayImpl gateway = newGateway(
                                batchRepository,
                                stepRepository,
                                logRepository,
                                fileInfoGateway);

                List<com.yss.valset.task.application.dto.OutsourcedDataTaskStepDTO> steps = gateway
                                .listSteps("FILE-3001");

                assertThat(steps).hasSize(3);
                assertThat(steps).extracting("stage").containsExactly(
                                OutsourcedDataTaskStage.FILE_PARSE.name(),
                                OutsourcedDataTaskStage.STRUCTURE_STANDARDIZE.name(),
                                OutsourcedDataTaskStage.STANDARD_LANDING.name());
                assertThat(steps).allSatisfy(step -> assertThat(
                                ((com.yss.valset.task.application.dto.OutsourcedDataTaskStepDTO) step).getStatus())
                                .isEqualTo(OutsourcedDataTaskStatus.SUCCESS.name()));
                assertThat(steps).allSatisfy(step -> {
                        com.yss.valset.task.application.dto.OutsourcedDataTaskStepDTO dto = (com.yss.valset.task.application.dto.OutsourcedDataTaskStepDTO) step;
                        assertThat(dto.getDurationText()).isNotEqualTo("已完成");
                });
                assertThat(steps.stream()
                                .filter(step -> OutsourcedDataTaskStage.STRUCTURE_STANDARDIZE.name().equals(
                                                ((com.yss.valset.task.application.dto.OutsourcedDataTaskStepDTO) step)
                                                                .getStage()))
                                .map(step -> ((com.yss.valset.task.application.dto.OutsourcedDataTaskStepDTO) step)
                                                .getDurationText())
                                .toList())
                                .containsOnly("-");
                assertThat(steps.stream()
                                .filter(step -> OutsourcedDataTaskStage.STRUCTURE_STANDARDIZE.name().equals(
                                                ((com.yss.valset.task.application.dto.OutsourcedDataTaskStepDTO) step)
                                                                .getStage()))
                                .map(step -> ((com.yss.valset.task.application.dto.OutsourcedDataTaskStepDTO) step)
                                                .getStartedAt())
                                .toList())
                                .containsOnlyNulls();
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
                                .businessDate(java.time.LocalDate.parse("2026-04-28"))
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
                when(batchRepository.selectById(any())).thenAnswer(
                                invocation -> batchSelectCount.incrementAndGet() >= 2 ? insertedBatch.get() : null);
                when(stepRepository.selectList(any())).thenAnswer(invocation -> {
                        int count = stepSelectListCount.incrementAndGet();
                        if (count == 1 || insertedSteps.isEmpty()) {
                                return List.of();
                        }
                        return new ArrayList<>(insertedSteps);
                });
                OutsourcedDataTaskGatewayImpl gateway = newGateway(
                                batchRepository,
                                stepRepository,
                                logRepository,
                                fileInfoGateway);

                gateway.recordParseLifecycleEvent(ParseLifecycleEvent.builder()
                                .eventId("event-standardized")
                                .occurredAt(Instant.parse("2026-04-30T10:00:00Z"))
                                .stage(com.yss.valset.application.event.lifecycle.ParseLifecycleStage.STRUCTURE_STANDARDIZE)
                                .source("parse-execution")
                                .taskId(11L)
                                .fileId(1001L)
                                .businessKey("WORKFLOW:PARSE:EXCEL:1001")
                                .message("标准化完成")
                                .build());

                ArgumentCaptor<OutsourcedDataTaskBatchPO> batchCaptor = ArgumentCaptor
                                .forClass(OutsourcedDataTaskBatchPO.class);
                ArgumentCaptor<OutsourcedDataTaskBatchPO> batchUpdateCaptor = ArgumentCaptor
                                .forClass(OutsourcedDataTaskBatchPO.class);
                ArgumentCaptor<OutsourcedDataTaskStepPO> stepCaptor = ArgumentCaptor
                                .forClass(OutsourcedDataTaskStepPO.class);
                ArgumentCaptor<OutsourcedDataTaskLogPO> logCaptor = ArgumentCaptor
                                .forClass(OutsourcedDataTaskLogPO.class);
                verify(batchRepository).insert(batchCaptor.capture());
                verify(batchRepository).updateById(batchUpdateCaptor.capture());
                verify(stepRepository, org.mockito.Mockito.times(1)).insert(stepCaptor.capture());
                verify(logRepository).insert(logCaptor.capture());

                OutsourcedDataTaskBatchPO batch = batchCaptor.getValue();
                assertThat(batch.getBatchId()).isEqualTo("FILE-1001");
                assertThat(batch.getFileId()).isEqualTo("1001");
                assertThat(batch.getFilesysFileId()).isEqualTo("FS-1001");
                assertThat(batch.getOriginalFileName()).isEqualTo("委外产品5估值表.xlsx");
                assertThat(batch.getFileFingerprint()).isEqualTo("fingerprint-001");
                assertThat(batch.getBusinessDate()).isEqualTo(java.time.LocalDate.parse("2026-04-28"));

                OutsourcedDataTaskBatchPO aggregatedBatch = batchUpdateCaptor.getValue();
                assertThat(aggregatedBatch.getBatchId()).isEqualTo("FILE-1001");
                assertThat(aggregatedBatch.getCurrentStage())
                                .isEqualTo(OutsourcedDataTaskStage.STRUCTURE_STANDARDIZE.name());
                assertThat(aggregatedBatch.getStatus()).isEqualTo(OutsourcedDataTaskStatus.RUNNING.name());

                List<String> stages = stepCaptor.getAllValues().stream()
                                .map(OutsourcedDataTaskStepPO::getStage)
                                .toList();
                assertThat(stages).containsExactly(OutsourcedDataTaskStage.STRUCTURE_STANDARDIZE.name());
                assertThat(stepCaptor.getAllValues())
                                .allSatisfy(step -> assertThat(step.getStatus())
                                                .isEqualTo(OutsourcedDataTaskStatus.SUCCESS.name()));
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
                when(batchRepository.selectById(any())).thenAnswer(
                                invocation -> batchSelectCount.incrementAndGet() >= 2 ? insertedBatch.get() : null);
                when(stepRepository.selectList(any())).thenAnswer(invocation -> {
                        stepSelectListCount.incrementAndGet();
                        return insertedSteps.isEmpty() ? List.of() : new ArrayList<>(insertedSteps);
                });
                OutsourcedDataTaskGatewayImpl gateway = newGateway(
                                batchRepository,
                                stepRepository,
                                logRepository,
                                fileInfoGateway);

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

                ArgumentCaptor<OutsourcedDataTaskBatchPO> batchCaptor = ArgumentCaptor
                                .forClass(OutsourcedDataTaskBatchPO.class);
                ArgumentCaptor<OutsourcedDataTaskBatchPO> batchUpdateCaptor = ArgumentCaptor
                                .forClass(OutsourcedDataTaskBatchPO.class);
                ArgumentCaptor<OutsourcedDataTaskStepPO> stepCaptor = ArgumentCaptor
                                .forClass(OutsourcedDataTaskStepPO.class);
                ArgumentCaptor<OutsourcedDataTaskLogPO> logCaptor = ArgumentCaptor
                                .forClass(OutsourcedDataTaskLogPO.class);
                verify(batchRepository).insert(batchCaptor.capture());
                verify(batchRepository).updateById(batchUpdateCaptor.capture());
                verify(stepRepository).insert(stepCaptor.capture());
                verify(logRepository).insert(logCaptor.capture());

                assertThat(batchCaptor.getValue().getBatchId()).isEqualTo("FILE-1001");
                assertThat(batchUpdateCaptor.getValue().getCurrentStage())
                                .isEqualTo(OutsourcedDataTaskStage.STANDARD_LANDING.name());
                assertThat(batchUpdateCaptor.getValue().getStatus()).isEqualTo(OutsourcedDataTaskStatus.RUNNING.name());
                assertThat(stepCaptor.getValue().getStage()).isEqualTo(OutsourcedDataTaskStage.STANDARD_LANDING.name());
                assertThat(stepCaptor.getValue().getTaskType()).isEqualTo(TaskType.EVALUATE_MAPPING.name());
                assertThat(stepCaptor.getValue().getStatus()).isEqualTo(OutsourcedDataTaskStatus.SUCCESS.name());
                assertThat(stepCaptor.getValue().getCurrentFlag()).isTrue();
                assertThat(logCaptor.getValue().getStage()).isEqualTo(OutsourcedDataTaskStage.STANDARD_LANDING.name());
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
                when(batchRepository.selectById(any())).thenAnswer(
                                invocation -> batchSelectCount.incrementAndGet() >= 2 ? insertedBatch.get() : null);
                when(stepRepository.selectList(any())).thenAnswer(
                                invocation -> stepSelectListCount.incrementAndGet() == 3 ? insertedSteps : List.of());

                WorkflowRuntimeCatalog catalog = defaultCatalog();

                OutsourcedDataTaskGatewayImpl gateway = newGateway(
                                batchRepository,
                                stepRepository,
                                logRepository,
                                fileInfoGateway);
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

                ArgumentCaptor<OutsourcedDataTaskBatchPO> batchCaptor = ArgumentCaptor
                                .forClass(OutsourcedDataTaskBatchPO.class);
                ArgumentCaptor<OutsourcedDataTaskBatchPO> batchUpdateCaptor = ArgumentCaptor
                                .forClass(OutsourcedDataTaskBatchPO.class);
                ArgumentCaptor<OutsourcedDataTaskStepPO> stepCaptor = ArgumentCaptor
                                .forClass(OutsourcedDataTaskStepPO.class);
                ArgumentCaptor<OutsourcedDataTaskLogPO> logCaptor = ArgumentCaptor
                                .forClass(OutsourcedDataTaskLogPO.class);
                verify(batchRepository).insert(batchCaptor.capture());
                verify(batchRepository).updateById(batchUpdateCaptor.capture());
                verify(stepRepository).insert(stepCaptor.capture());
                verify(logRepository).insert(logCaptor.capture());

                assertThat(batchUpdateCaptor.getValue().getCurrentStage())
                                .isEqualTo(OutsourcedDataTaskStage.STANDARD_LANDING.name());
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

                WorkflowRuntimeCatalog catalog = defaultCatalog();

                OutsourcedDataTaskGatewayImpl gateway = newGateway(
                                batchRepository,
                                stepRepository,
                                logRepository,
                                fileInfoGateway);
                gateway.setStageCatalog(catalog);

                gateway.recordWorkflowTaskLifecycleEvent(WorkflowTaskLifecycleEvent.builder()
                                .eventId("event-extract-retrying")
                                .occurredAt(Instant.parse("2026-04-30T10:55:00Z"))
                                .taskId(26L)
                                .taskType(TaskType.EXTRACT_DATA)
                                .taskStage(TaskStage.EXTRACT)
                                .taskStatus(TaskStatus.RETRYING)
                                .fileId(1002L)
                                .message("委外抽取任务重试")
                                .build());

                ArgumentCaptor<OutsourcedDataTaskBatchPO> batchCaptor = ArgumentCaptor
                                .forClass(OutsourcedDataTaskBatchPO.class);
                ArgumentCaptor<OutsourcedDataTaskStepPO> stepCaptor = ArgumentCaptor
                                .forClass(OutsourcedDataTaskStepPO.class);
                verify(batchRepository).insert(batchCaptor.capture());
                verify(stepRepository).insert(stepCaptor.capture());

                assertThat(batchCaptor.getValue().getCurrentStage())
                                .isEqualTo(OutsourcedDataTaskStage.FILE_PARSE.name());
                assertThat(stepCaptor.getValue().getStatus()).isEqualTo(OutsourcedDataTaskStatus.RUNNING.name());
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
                OutsourcedDataTaskGatewayImpl gateway = newGateway(
                                batchRepository,
                                stepRepository,
                                logRepository,
                                fileInfoGateway);

                gateway.recordParseLifecycleEvent(ParseLifecycleEvent.builder()
                                .eventId("event-file-info-repaired")
                                .occurredAt(Instant.parse("2026-04-30T10:20:00Z"))
                                .stage(com.yss.valset.application.event.lifecycle.ParseLifecycleStage.FILE_PARSE)
                                .queueId("3001")
                                .businessKey("transfer-3001:VALUATION_TABLE")
                                .attributes(Map.of("fileId", 1001L))
                                .message("文件主数据自动修复完成")
                                .build());

                ArgumentCaptor<OutsourcedDataTaskBatchPO> batchCaptor = ArgumentCaptor
                                .forClass(OutsourcedDataTaskBatchPO.class);
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
                batch.setCurrentStage(OutsourcedDataTaskStage.STANDARD_LANDING.name());
                batch.setStatus(OutsourcedDataTaskStatus.SUCCESS.name());
                OutsourcedDataTaskStepPO oldStep = new OutsourcedDataTaskStepPO();
                oldStep.setStepId("FILE-1001-STANDARD_LANDING-1");
                oldStep.setBatchId("FILE-1001");
                oldStep.setStage(OutsourcedDataTaskStage.STANDARD_LANDING.name());
                oldStep.setRunNo(1);
                oldStep.setCurrentFlag(true);
                oldStep.setStatus(OutsourcedDataTaskStatus.SUCCESS.name());
                List<OutsourcedDataTaskStepPO> insertedSteps = new ArrayList<>();
                AtomicInteger stepSelectListCount = new AtomicInteger();
                when(batchRepository.selectById(any())).thenReturn(batch);
                when(logRepository.selectById(any())).thenReturn(null);
                when(stepRepository.selectList(any())).thenAnswer(invocation -> {
                        stepSelectListCount.incrementAndGet();
                        List<OutsourcedDataTaskStepPO> steps = new ArrayList<>();
                        steps.add(oldStep);
                        steps.addAll(insertedSteps);
                        return steps;
                });
                org.mockito.Mockito.doAnswer(invocation -> {
                        insertedSteps.add(invocation.getArgument(0));
                        return 1;
                }).when(stepRepository).insert(any(OutsourcedDataTaskStepPO.class));
                OutsourcedDataTaskGatewayImpl gateway = newGateway(
                                batchRepository,
                                stepRepository,
                                logRepository,
                                fileInfoGateway);

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

                ArgumentCaptor<OutsourcedDataTaskStepPO> historicalStepCaptor = ArgumentCaptor
                                .forClass(OutsourcedDataTaskStepPO.class);
                ArgumentCaptor<OutsourcedDataTaskStepPO> newStepCaptor = ArgumentCaptor
                                .forClass(OutsourcedDataTaskStepPO.class);
                ArgumentCaptor<OutsourcedDataTaskBatchPO> batchUpdateCaptor = ArgumentCaptor
                                .forClass(OutsourcedDataTaskBatchPO.class);
                verify(stepRepository).updateById(historicalStepCaptor.capture());
                verify(stepRepository).insert(newStepCaptor.capture());
                verify(batchRepository, org.mockito.Mockito.atLeastOnce()).updateById(batchUpdateCaptor.capture());

                assertThat(historicalStepCaptor.getValue().getCurrentFlag()).isFalse();
                assertThat(newStepCaptor.getValue().getStepId()).isEqualTo("FILE-1001-STANDARD_LANDING-2");
                assertThat(newStepCaptor.getValue().getRunNo()).isEqualTo(2);
                assertThat(newStepCaptor.getValue().getCurrentFlag()).isTrue();
                assertThat(batchUpdateCaptor.getAllValues().get(batchUpdateCaptor.getAllValues().size() - 1)
                                .getStatus())
                                .isEqualTo(OutsourcedDataTaskStatus.RUNNING.name());
        }

        @Test
        void shouldCreateNewRunWhenRerunUsesDifferentTaskIdEvenIfStageIsSuccess() {
                OutsourcedDataTaskBatchRepository batchRepository = mock(OutsourcedDataTaskBatchRepository.class);
                OutsourcedDataTaskStepRepository stepRepository = mock(OutsourcedDataTaskStepRepository.class);
                OutsourcedDataTaskLogRepository logRepository = mock(OutsourcedDataTaskLogRepository.class);
                ValsetFileInfoGateway fileInfoGateway = mock(ValsetFileInfoGateway.class);
                OutsourcedDataTaskBatchPO batch = new OutsourcedDataTaskBatchPO();
                batch.setBatchId("FILE-1002");
                batch.setBatchName("FILE-1002");
                batch.setCurrentStage(OutsourcedDataTaskStage.STRUCTURE_STANDARDIZE.name());
                batch.setStatus(OutsourcedDataTaskStatus.SUCCESS.name());
                OutsourcedDataTaskStepPO oldStep = new OutsourcedDataTaskStepPO();
                oldStep.setStepId("FILE-1002-STRUCTURE_STANDARDIZE-1");
                oldStep.setBatchId("FILE-1002");
                oldStep.setStage(OutsourcedDataTaskStage.STRUCTURE_STANDARDIZE.name());
                oldStep.setTaskId("31");
                oldStep.setRunNo(1);
                oldStep.setCurrentFlag(true);
                oldStep.setStatus(OutsourcedDataTaskStatus.SUCCESS.name());
                oldStep.setStartedAt(Instant.parse("2026-04-30T09:00:00Z").atZone(java.time.ZoneId.systemDefault())
                                .toLocalDateTime());
                oldStep.setEndedAt(Instant.parse("2026-04-30T09:03:00Z").atZone(java.time.ZoneId.systemDefault())
                                .toLocalDateTime());
                oldStep.setDurationMs(180000L);
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
                OutsourcedDataTaskGatewayImpl gateway = newGateway(
                                batchRepository,
                                stepRepository,
                                logRepository,
                                fileInfoGateway);

                gateway.recordParseLifecycleEvent(ParseLifecycleEvent.builder()
                                .eventId("event-structure-rerun")
                                .occurredAt(Instant.parse("2026-04-30T10:30:00Z"))
                                .stage(com.yss.valset.application.event.lifecycle.ParseLifecycleStage.STRUCTURE_STANDARDIZE)
                                .source("parse-execution")
                                .taskId(32L)
                                .fileId(1002L)
                                .businessKey("WORKFLOW:PARSE:EXCEL:1002")
                                .message("标准化完成")
                                .build());

                ArgumentCaptor<OutsourcedDataTaskStepPO> newStepCaptor = ArgumentCaptor
                                .forClass(OutsourcedDataTaskStepPO.class);
                verify(stepRepository, org.mockito.Mockito.atLeastOnce())
                                .updateById(any(OutsourcedDataTaskStepPO.class));
                verify(stepRepository).insert(newStepCaptor.capture());

                assertThat(newStepCaptor.getValue().getStepId()).isEqualTo("FILE-1002-STRUCTURE_STANDARDIZE-2");
                assertThat(newStepCaptor.getValue().getTaskId()).isEqualTo("32");
                assertThat(newStepCaptor.getValue().getRunNo()).isEqualTo(2);
                assertThat(newStepCaptor.getValue().getStartedAt()).isEqualTo(Instant.parse("2026-04-30T10:30:00Z")
                                .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
                assertThat(newStepCaptor.getValue().getEndedAt()).isNotNull();
                assertThat(newStepCaptor.getValue().getStatus()).isEqualTo(OutsourcedDataTaskStatus.SUCCESS.name());
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
                currentSteps.add(step("FILE-1001", OutsourcedDataTaskStage.FILE_PARSE, OutsourcedDataTaskStatus.RUNNING,
                                1));
                currentSteps.add(step("FILE-1001", OutsourcedDataTaskStage.STRUCTURE_STANDARDIZE,
                                OutsourcedDataTaskStatus.SUCCESS, 1));
                currentSteps.add(step("FILE-1001", OutsourcedDataTaskStage.STANDARD_LANDING,
                                OutsourcedDataTaskStatus.SUCCESS, 1));

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
                OutsourcedDataTaskGatewayImpl gateway = newGateway(
                                batchRepository,
                                stepRepository,
                                logRepository,
                                fileInfoGateway);

                gateway.recordWorkflowTaskLifecycleEvent(WorkflowTaskLifecycleEvent.builder()
                                .eventId("event-archive-success")
                                .occurredAt(Instant.parse("2026-04-30T10:40:00Z"))
                                .taskId(24L)
                                .taskType(TaskType.EVALUATE_MAPPING)
                                .taskStage(TaskStage.OTHER)
                                .taskStatus(TaskStatus.SUCCESS)
                                .fileId(1001L)
                                .message("归档完成")
                                .build());

                ArgumentCaptor<OutsourcedDataTaskStepPO> stepCaptor = ArgumentCaptor
                                .forClass(OutsourcedDataTaskStepPO.class);
                ArgumentCaptor<OutsourcedDataTaskBatchPO> batchUpdateCaptor = ArgumentCaptor
                                .forClass(OutsourcedDataTaskBatchPO.class);
                verify(stepRepository, org.mockito.Mockito.atLeastOnce()).updateById(stepCaptor.capture());
                verify(batchRepository, org.mockito.Mockito.atLeastOnce()).updateById(batchUpdateCaptor.capture());

                OutsourcedDataTaskStepPO promotedStep = stepCaptor.getAllValues().stream()
                                .filter(step -> OutsourcedDataTaskStage.FILE_PARSE.name().equals(step.getStage()))
                                .reduce((first, second) -> second)
                                .orElseThrow();
                assertThat(promotedStep.getStatus()).isEqualTo(OutsourcedDataTaskStatus.SUCCESS.name());
                assertThat(promotedStep.getEndedAt()).isNotNull();

                OutsourcedDataTaskBatchPO aggregatedBatch = batchUpdateCaptor.getAllValues()
                                .get(batchUpdateCaptor.getAllValues().size() - 1);
                assertThat(aggregatedBatch.getCurrentStage())
                                .isEqualTo(OutsourcedDataTaskStage.STANDARD_LANDING.name());
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
                        return insertedBatch.get();
                });
                when(stepRepository.selectList(any())).thenAnswer(invocation -> {
                        stepSelectCount.incrementAndGet();
                        return new ArrayList<>(insertedSteps);
                });
                OutsourcedDataTaskGatewayImpl gateway = newGateway(
                                batchRepository,
                                stepRepository,
                                logRepository,
                                fileInfoGateway);

                gateway.recordParseLifecycleEvent(ParseLifecycleEvent.builder()
                                .eventId("event-started")
                                .occurredAt(Instant.parse("2026-04-30T10:00:00Z"))
                                .stage(com.yss.valset.application.event.lifecycle.ParseLifecycleStage.FILE_PARSE)
                                .source("parse-execution")
                                .taskId(31L)
                                .fileId(1001L)
                                .businessKey("WORKFLOW:PARSE:EXCEL:1001")
                                .message("开始执行解析任务")
                                .build());

                gateway.recordParseLifecycleEvent(ParseLifecycleEvent.builder()
                                .eventId("event-succeeded")
                                .occurredAt(Instant.parse("2026-04-30T10:05:00Z"))
                                .stage(com.yss.valset.application.event.lifecycle.ParseLifecycleStage.STANDARD_LANDING)
                                .source("parse-execution")
                                .taskId(31L)
                                .fileId(1001L)
                                .businessKey("WORKFLOW:PARSE:EXCEL:1001")
                                .message("解析任务执行成功")
                                .build());

                ArgumentCaptor<OutsourcedDataTaskStepPO> stepCaptor = ArgumentCaptor
                                .forClass(OutsourcedDataTaskStepPO.class);
                ArgumentCaptor<OutsourcedDataTaskStepPO> stepInsertCaptor = ArgumentCaptor
                                .forClass(OutsourcedDataTaskStepPO.class);
                ArgumentCaptor<OutsourcedDataTaskBatchPO> batchUpdateCaptor = ArgumentCaptor
                                .forClass(OutsourcedDataTaskBatchPO.class);
                verify(stepRepository, org.mockito.Mockito.atLeastOnce()).updateById(stepCaptor.capture());
                verify(stepRepository, org.mockito.Mockito.atLeastOnce()).insert(stepInsertCaptor.capture());
                verify(batchRepository, org.mockito.Mockito.atLeastOnce()).updateById(batchUpdateCaptor.capture());
                assertThat(stepInsertCaptor.getAllValues().stream().map(OutsourcedDataTaskStepPO::getStage).toList())
                                .contains(OutsourcedDataTaskStage.FILE_PARSE.name());

                OutsourcedDataTaskStepPO fileParseStep = stepCaptor.getAllValues().stream()
                                .filter(step -> OutsourcedDataTaskStage.FILE_PARSE.name().equals(step.getStage()))
                                .reduce((first, second) -> second)
                                .orElseThrow();
                assertThat(fileParseStep.getStatus()).isEqualTo(OutsourcedDataTaskStatus.SUCCESS.name());
                assertThat(fileParseStep.getEndedAt()).isNotNull();

                OutsourcedDataTaskBatchPO aggregatedBatch = batchUpdateCaptor.getAllValues()
                                .get(batchUpdateCaptor.getAllValues().size() - 1);
                assertThat(aggregatedBatch.getCurrentStage())
                                .isEqualTo(OutsourcedDataTaskStage.FILE_PARSE.name());
                assertThat(aggregatedBatch.getStatus()).isEqualTo(OutsourcedDataTaskStatus.RUNNING.name());
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
                step.setStartedAt(Instant.parse("2026-04-30T09:30:00Z").atZone(java.time.ZoneId.systemDefault())
                                .toLocalDateTime());
                if (status == OutsourcedDataTaskStatus.SUCCESS) {
                        step.setEndedAt(Instant.parse("2026-04-30T09:32:00Z").atZone(java.time.ZoneId.systemDefault())
                                        .toLocalDateTime());
                        step.setDurationMs(120000L);
                }
                return step;
        }

        private static WorkflowRuntimeCatalog defaultCatalog() {
                return catalogWithStages("wf-test", List.of(
                                OutsourcedDataTaskStage.FILE_PARSE.name(),
                                OutsourcedDataTaskStage.STRUCTURE_STANDARDIZE.name(),
                                OutsourcedDataTaskStage.STANDARD_LANDING.name()));
        }

        private static WorkflowRuntimeCatalog catalogWithStages(String workflowId, List<String> stageCodes) {
                OutsourcedWorkflowDefinitionRepository definitionRepository = mock(
                                OutsourcedWorkflowDefinitionRepository.class);
                OutsourcedWorkflowStageRepository stageRepository = mock(OutsourcedWorkflowStageRepository.class);

                OutsourcedWorkflowDefinitionPO definitionPO = new OutsourcedWorkflowDefinitionPO();
                definitionPO.setWorkflowId(firstText(workflowId, "wf-test"));
                definitionPO.setWorkflowCode("VALUATION_PARSE");
                definitionPO.setWorkflowName("估值表解析工作流");
                definitionPO.setEnabled(true);
                definitionPO.setVersionNo(1);
                definitionPO.setParseFallbackStage(OutsourcedDataTaskStage.FILE_PARSE.name());
                definitionPO.setWorkflowFallbackStage(OutsourcedDataTaskStage.STANDARD_LANDING.name());
                when(definitionRepository.selectOne(any())).thenReturn(definitionPO);

                List<OutsourcedWorkflowStagePO> stagePOs = new ArrayList<>();
                int index = 0;
                List<String> codes = stageCodes == null ? List.of() : stageCodes;
                for (String stageCode : codes) {
                        OutsourcedWorkflowStagePO po = new OutsourcedWorkflowStagePO();
                        po.setStageId("stage-" + index);
                        po.setWorkflowId(definitionPO.getWorkflowId());
                        po.setStageCode(stageCode);
                        po.setStageName(stageCode);
                        po.setStageDescription(stageCode);
                        po.setSortOrder(index + 1);
                        po.setEnabled(Boolean.TRUE);
                        stagePOs.add(po);
                        index++;
                }
                when(stageRepository.selectList(any())).thenReturn(stagePOs);

                WorkflowRuntimeCatalog catalog = new WorkflowRuntimeCatalog();
                catalog.setDefinitionRepository(definitionRepository);
                catalog.setStageRepository(stageRepository);
                return catalog;
        }

        private static String firstText(String value, String fallback) {
                return value == null || value.isBlank() ? fallback : value;
        }

        private static OutsourcedDataTaskGatewayImpl newGateway(OutsourcedDataTaskBatchRepository batchRepository,
                        OutsourcedDataTaskStepRepository stepRepository,
                        OutsourcedDataTaskLogRepository logRepository,
                        ValsetFileInfoGateway fileInfoGateway) {
                OutsourcedDataTaskGatewayImpl gateway = new OutsourcedDataTaskGatewayImpl(
                                batchRepository,
                                stepRepository,
                                logRepository,
                                fileInfoGateway,
                                databaseDialectSupport());
                gateway.setStageCatalog(defaultCatalog());
                return gateway;
        }

        private static DatabaseDialectSupport databaseDialectSupport() {
                try {
                        DataSource dataSource = mock(DataSource.class);
                        Connection connection = mock(Connection.class);
                        DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
                        when(dataSource.getConnection()).thenReturn(connection);
                        when(connection.getMetaData()).thenReturn(databaseMetaData);
                        when(databaseMetaData.getDatabaseProductName()).thenReturn("MySQL");
                        doNothing().when(connection).close();
                        return new DatabaseDialectSupport(dataSource);
                } catch (Exception exception) {
                        throw new IllegalStateException("构造测试数据源失败", exception);
                }
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
                batch.setStartedAt(Instant.parse("2026-04-30T09:30:00Z").atZone(java.time.ZoneId.systemDefault())
                                .toLocalDateTime());
                if (status != OutsourcedDataTaskStatus.RUNNING) {
                        batch.setEndedAt(Instant.parse("2026-04-30T09:42:00Z").atZone(java.time.ZoneId.systemDefault())
                                        .toLocalDateTime());
                        batch.setDurationMs(720000L);
                }
                batch.setLastErrorCode(errorCode);
                batch.setLastErrorMessage(errorMessage);
                return batch;
        }
}
