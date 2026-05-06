package com.yss.valset.workflow.infrastructure.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.workflow.infrastructure.entity.WorkflowDefinitionPO;
import com.yss.valset.workflow.infrastructure.entity.WorkflowEngineBindingPO;
import com.yss.valset.workflow.infrastructure.entity.WorkflowInstancePO;
import com.yss.valset.workflow.infrastructure.entity.WorkflowStageLogPO;
import com.yss.valset.workflow.infrastructure.entity.WorkflowStagePO;
import com.yss.valset.workflow.infrastructure.mapper.WorkflowDefinitionRepository;
import com.yss.valset.workflow.infrastructure.mapper.WorkflowEngineBindingRepository;
import com.yss.valset.workflow.infrastructure.mapper.WorkflowInstanceRepository;
import com.yss.valset.workflow.infrastructure.mapper.WorkflowStageLogRepository;
import com.yss.valset.workflow.infrastructure.mapper.WorkflowStageRepository;
import com.yss.valset.workflow.infrastructure.support.WorkflowJsonCodec;
import com.yss.valset.workflow.model.EtlPlatformType;
import com.yss.valset.workflow.model.WorkflowDefinitionDTO;
import com.yss.valset.workflow.model.WorkflowEngineBindingDTO;
import com.yss.valset.workflow.model.WorkflowInstanceDTO;
import com.yss.valset.workflow.model.WorkflowLogQueryRequest;
import com.yss.valset.workflow.model.WorkflowStageDTO;
import com.yss.valset.workflow.model.WorkflowStageLogDTO;
import com.yss.valset.workflow.model.WorkflowStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DbWorkflowRuntimeStoreTest {

    @Test
    void shouldPersistAndReloadWorkflowDefinitionInstanceAndLogs() {
        WorkflowDefinitionRepository definitionRepository = mock(WorkflowDefinitionRepository.class);
        WorkflowStageRepository stageRepository = mock(WorkflowStageRepository.class);
        WorkflowEngineBindingRepository bindingRepository = mock(WorkflowEngineBindingRepository.class);
        WorkflowInstanceRepository instanceRepository = mock(WorkflowInstanceRepository.class);
        WorkflowStageLogRepository logRepository = mock(WorkflowStageLogRepository.class);
        WorkflowJsonCodec codec = new WorkflowJsonCodec(new ObjectMapper());
        DbWorkflowRuntimeStore store = new DbWorkflowRuntimeStore(
                definitionRepository,
                stageRepository,
                bindingRepository,
                instanceRepository,
                logRepository,
                codec);

        AtomicReference<WorkflowDefinitionPO> definitionState = new AtomicReference<>();
        AtomicReference<WorkflowEngineBindingPO> bindingState = new AtomicReference<>();
        AtomicReference<WorkflowInstancePO> instanceState = new AtomicReference<>();
        List<WorkflowStagePO> stageState = new ArrayList<>();
        List<WorkflowStageLogPO> logState = new ArrayList<>();

        when(definitionRepository.selectOne(any())).thenAnswer(invocation -> definitionState.get());
        when(definitionRepository.selectList(any())).thenAnswer(invocation -> {
            WorkflowDefinitionPO current = definitionState.get();
            return current == null ? List.of() : List.of(current);
        });
        when(bindingRepository.selectOne(any())).thenAnswer(invocation -> bindingState.get());
        when(stageRepository.selectList(any())).thenAnswer(invocation -> new ArrayList<>(stageState));
        when(instanceRepository.selectById(any())).thenAnswer(invocation -> {
            WorkflowInstancePO current = instanceState.get();
            String id = invocation.getArgument(0);
            return current != null && current.getInstanceId().equals(id) ? current : null;
        });
        when(logRepository.selectById(any())).thenReturn(null);
        when(logRepository.selectList(any())).thenAnswer(invocation -> {
            Object argument = invocation.getArgument(0);
            String criteria = argument == null ? "" : argument.toString();
            if (criteria.contains("missing")) {
                return List.of();
            }
            return new ArrayList<>(logState);
        });

        doAnswer(invocation -> {
            definitionState.set(invocation.getArgument(0));
            return 1;
        }).when(definitionRepository).insert(org.mockito.ArgumentMatchers.<WorkflowDefinitionPO>any());
        doAnswer(invocation -> {
            definitionState.set(invocation.getArgument(0));
            return 1;
        }).when(definitionRepository).updateById(org.mockito.ArgumentMatchers.<WorkflowDefinitionPO>any());
        doAnswer(invocation -> {
            stageState.clear();
            return 1;
        }).when(stageRepository).delete(any());
        doAnswer(invocation -> {
            stageState.add(invocation.getArgument(0));
            return 1;
        }).when(stageRepository).insert(org.mockito.ArgumentMatchers.<WorkflowStagePO>any());
        doAnswer(invocation -> {
            bindingState.set(invocation.getArgument(0));
            return 1;
        }).when(bindingRepository).insert(org.mockito.ArgumentMatchers.<WorkflowEngineBindingPO>any());
        doAnswer(invocation -> {
            bindingState.set(null);
            return 1;
        }).when(bindingRepository).delete(any());
        doAnswer(invocation -> {
            instanceState.set(invocation.getArgument(0));
            return 1;
        }).when(instanceRepository).insert(org.mockito.ArgumentMatchers.<WorkflowInstancePO>any());
        doAnswer(invocation -> {
            instanceState.set(invocation.getArgument(0));
            return 1;
        }).when(instanceRepository).updateById(org.mockito.ArgumentMatchers.<WorkflowInstancePO>any());
        doAnswer(invocation -> {
            logState.add(invocation.getArgument(0));
            return 1;
        }).when(logRepository).insert(org.mockito.ArgumentMatchers.<WorkflowStageLogPO>any());

        WorkflowDefinitionDTO definition = WorkflowDefinitionDTO.builder()
                .workflowCode("valset-etl")
                .workflowName("估值ETL")
                .workflowVersionNo(1)
                .platformType(EtlPlatformType.SPRING_BATCH)
                .description("说明")
                .enabled(true)
                .engineBinding(WorkflowEngineBindingDTO.builder()
                        .platformType(EtlPlatformType.SPRING_BATCH)
                        .externalWorkflowId("etl-job")
                        .externalProjectCode("project-1")
                        .externalNamespace("ns-1")
                        .externalJobGroup("group-1")
                        .externalJobHandler("handler-1")
                        .configJson("{\"foo\":\"bar\"}")
                        .attributes(Map.of("k", "v"))
                        .build())
                .stages(List.of(
                        WorkflowStageDTO.builder()
                                .stageCode("EXTRACT")
                                .stageName("抽取")
                                .stageOrder(2)
                                .description("抽取阶段")
                                .timeoutSeconds(60)
                                .retryable(true)
                                .build(),
                        WorkflowStageDTO.builder()
                                .stageCode("PARSE")
                                .stageName("解析")
                                .stageOrder(1)
                                .description("解析阶段")
                                .timeoutSeconds(120)
                                .retryable(false)
                                .build()))
                .build();

        WorkflowDefinitionDTO savedDefinition = store.saveDefinition(definition);
        assertThat(savedDefinition.getWorkflowCode()).isEqualTo("valset-etl");
        assertThat(savedDefinition.getStages()).extracting(WorkflowStageDTO::getStageCode)
                .containsExactly("PARSE", "EXTRACT");
        assertThat(savedDefinition.getEngineBinding().getAttributes()).containsEntry("k", "v");

        WorkflowInstanceDTO savedInstance = store.saveInstance(WorkflowInstanceDTO.builder()
                .instanceId("ins-1")
                .workflowCode("valset-etl")
                .workflowVersionNo(1)
                .platformType(EtlPlatformType.SPRING_BATCH)
                .businessKey("file-001")
                .externalWorkflowId("etl-job")
                .externalInstanceId("ext-001")
                .status(WorkflowStatus.SUBMITTED)
                .rawStatus("SUBMITTED")
                .triggerTime(LocalDateTime.now())
                .startTime(LocalDateTime.now())
                .message("已提交")
                .context(new LinkedHashMap<>(Map.of("fileId", "file-001")))
                .build());
        assertThat(savedInstance.getInstanceId()).isEqualTo("ins-1");
        assertThat(savedInstance.getBusinessKey()).isEqualTo("file-001");

        WorkflowStageLogDTO savedLog = store.saveStageLog(WorkflowStageLogDTO.builder()
                .instanceId("ins-1")
                .workflowCode("valset-etl")
                .workflowVersionNo(1)
                .stageCode("PARSE")
                .stageName("解析")
                .stageOrder(1)
                .status(WorkflowStatus.SUCCEEDED)
                .rawStatus("success")
                .message("完成")
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now())
                .payload(new LinkedHashMap<>(Map.of("rows", 10)))
                .build());
        assertThat(savedLog.getStageCode()).isEqualTo("PARSE");

        List<WorkflowStageLogDTO> stageLogs = store.listStageLogs("ins-1", "PARSE");
        assertThat(stageLogs).hasSize(1);
        assertThat(stageLogs.get(0).getPayload()).containsEntry("rows", 10);

        WorkflowInstanceDTO reloadedInstance = store.findInstance("ins-1").orElseThrow();
        assertThat(reloadedInstance.getStageLogs()).hasSize(1);
        assertThat(reloadedInstance.getStageLogs().get(0).getStatus()).isEqualTo(WorkflowStatus.SUCCEEDED);

        assertThat(store.findDefinition("valset-etl", 1)).isPresent();
        assertThat(store.listDefinitions()).hasSize(1);
        assertThat(store.findInstance("missing")).isEmpty();
    }
}
