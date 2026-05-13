package com.yss.valset.workflow.service;

import com.yss.valset.workflow.model.EtlPlatformType;
import com.yss.valset.workflow.model.WorkflowCallbackRequest;
import com.yss.valset.workflow.model.WorkflowDefinitionDTO;
import com.yss.valset.workflow.model.WorkflowEngineBindingDTO;
import com.yss.valset.workflow.model.WorkflowInstanceQueryRequest;
import com.yss.valset.workflow.model.WorkflowInstanceDTO;
import com.yss.valset.workflow.model.WorkflowInstanceViewDTO;
import com.yss.valset.workflow.model.WorkflowRetryRequest;
import com.yss.valset.workflow.model.WorkflowResumeRequest;
import com.yss.valset.workflow.model.WorkflowPlatformMetadataDTO;
import com.yss.valset.workflow.model.WorkflowStageDTO;
import com.yss.valset.workflow.model.WorkflowStatus;
import com.yss.valset.workflow.model.WorkflowSyncStatus;
import com.yss.cloud.dto.response.PageResult;
import com.yss.valset.workflow.model.WorkflowStopRequest;
import com.yss.valset.workflow.model.WorkflowTriggerRequest;
import com.yss.valset.workflow.spi.WorkflowPlatformAdapter;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultWorkflowApplicationServiceTest {

    @Test
    void shouldTriggerRetryStopAndCallbackForDefinition() {
        InMemoryWorkflowRuntimeStore store = new InMemoryWorkflowRuntimeStore();
        DefaultWorkflowApplicationService service = new DefaultWorkflowApplicationService(
                store,
                List.of(new WorkflowPlatformAdapter() {
                    @Override
                    public EtlPlatformType platformType() {
                        return EtlPlatformType.SPRING_BATCH;
                    }

                    @Override
                    public void validate(WorkflowDefinitionDTO definition) {
                    }

                    @Override
                    public WorkflowDefinitionDTO syncDefinition(WorkflowDefinitionDTO definition) {
                        return definition;
                    }

                    @Override
                    public WorkflowDefinitionDTO onlineDefinition(WorkflowDefinitionDTO definition) {
                        return definition;
                    }

                    @Override
                    public WorkflowDefinitionDTO offlineDefinition(WorkflowDefinitionDTO definition) {
                        return definition;
                    }

                    @Override
                    public void deleteDefinition(WorkflowDefinitionDTO definition) {
                    }

                    @Override
                    public com.yss.valset.workflow.model.WorkflowPlatformExecutionResult trigger(WorkflowDefinitionDTO definition, WorkflowInstanceDTO instance, WorkflowTriggerRequest request) {
                        return build(definition, instance, "SUBMITTED", "已提交");
                    }

                    @Override
                    public com.yss.valset.workflow.model.WorkflowPlatformExecutionResult stop(WorkflowDefinitionDTO definition, WorkflowInstanceDTO instance, WorkflowStopRequest request) {
                        return build(definition, instance, "STOPPED", request == null ? "任务已停止" : request.getReason());
                    }

                    @Override
                    public com.yss.valset.workflow.model.WorkflowPlatformExecutionResult resume(WorkflowDefinitionDTO definition, WorkflowInstanceDTO instance, WorkflowResumeRequest request) {
                        return build(definition, instance, "RUNNING", request == null ? "任务已恢复运行" : request.getReason());
                    }

                    @Override
                    public com.yss.valset.workflow.model.WorkflowPlatformExecutionResult retry(WorkflowDefinitionDTO definition, WorkflowInstanceDTO instance, WorkflowRetryRequest request) {
                        return build(definition, instance, "RETRYING", "任务已重新提交");
                    }

                    @Override
                    public com.yss.valset.workflow.model.WorkflowPlatformExecutionResult query(WorkflowDefinitionDTO definition, WorkflowInstanceDTO instance) {
                        return build(definition, instance, instance == null ? null : instance.getRawStatus(), instance == null ? null : instance.getMessage());
                    }

                    @Override
                    public List<com.yss.valset.workflow.model.WorkflowPlatformExecutionResult> queryLogs(WorkflowDefinitionDTO definition, WorkflowInstanceDTO instance, com.yss.valset.workflow.model.WorkflowLogQueryRequest request) {
                        return List.of(build(definition, instance, instance == null ? null : instance.getRawStatus(), "日志查询"));
                    }

                    private com.yss.valset.workflow.model.WorkflowPlatformExecutionResult build(WorkflowDefinitionDTO definition,
                                                                                               WorkflowInstanceDTO instance,
                                                                                               String rawStatus,
                                                                                               String message) {
                        Map<String, Object> payload = new LinkedHashMap<>();
                        payload.put("adapter", "fake");
                        payload.put("workflowCode", definition.getWorkflowCode());
                        payload.put("instanceId", instance.getInstanceId());
                        payload.put("context", instance.getContext());
                        return com.yss.valset.workflow.model.WorkflowPlatformExecutionResult.builder()
                                .platformType(platformType())
                                .externalWorkflowId(definition.getEngineBinding().getExternalWorkflowId())
                                .externalInstanceId(instance.getInstanceId() + "-ext")
                                .rawStatus(rawStatus)
                                .message(message)
                                .payload(payload)
                                .stageLogs(new ArrayList<>(List.of(
                                        com.yss.valset.workflow.model.WorkflowStageLogDTO.builder()
                                                .instanceId(instance.getInstanceId())
                                                .workflowCode(definition.getWorkflowCode())
                                                .workflowVersionNo(definition.getWorkflowVersionNo())
                                                .stageCode(definition.getStages().get(0).getStageCode())
                                                .stageName(definition.getStages().get(0).getStageName())
                                                .stageOrder(definition.getStages().get(0).getStageOrder())
                                                .status(WorkflowStatus.fromRawStatus(rawStatus))
                                                .rawStatus(rawStatus)
                                                .message(message)
                                                .startTime(LocalDateTime.now())
                                                .endTime(LocalDateTime.now())
                                                .payload(payload)
                                                .build())))
                                .build();
                    }
                }));

        WorkflowDefinitionDTO definition = WorkflowDefinitionDTO.builder()
                .workflowCode("valset-etl")
                .workflowName("估值ETL")
                .workflowVersionNo(1)
                .platformType(EtlPlatformType.SPRING_BATCH)
                .engineBinding(WorkflowEngineBindingDTO.builder()
                        .platformType(EtlPlatformType.SPRING_BATCH)
                        .externalWorkflowId("valset-job")
                        .build())
                .stages(List.of(
                        WorkflowStageDTO.builder().stageCode("EXTRACT").stageName("抽取").stageOrder(1).build(),
                        WorkflowStageDTO.builder().stageCode("PARSE").stageName("解析").stageOrder(2).build()))
                .build();

        WorkflowDefinitionDTO saved = service.saveDefinition(definition);
        assertThat(saved.getWorkflowCode()).isEqualTo("valset-etl");

        WorkflowInstanceDTO runSubmitted = service.runDefinition("valset-etl", 1);
        assertThat(runSubmitted.getPlatformType()).isEqualTo(EtlPlatformType.SPRING_BATCH);
        assertThat(runSubmitted.getStatus()).isEqualTo(WorkflowStatus.SUBMITTED);

        WorkflowInstanceDTO triggered = service.trigger(WorkflowTriggerRequest.builder()
                .workflowCode("valset-etl")
                .workflowVersionNo(1)
                .businessKey("file-1")
                .context(Map.of("fileId", "file-1"))
                .build());
        assertThat(triggered.getPlatformType()).isEqualTo(EtlPlatformType.SPRING_BATCH);
        assertThat(triggered.getStatus()).isEqualTo(WorkflowStatus.SUBMITTED);
        assertThat(triggered.getStageLogs()).isNotEmpty();

        WorkflowInstanceDTO retried = service.retry(triggered.getInstanceId(), WorkflowRetryRequest.builder()
                .stageCode("PARSE")
                .context(Map.of("retry", true))
                .build());
        assertThat(retried.getRawStatus()).isEqualTo("RETRYING");

        WorkflowInstanceDTO stopped = service.stop(triggered.getInstanceId(), WorkflowStopRequest.builder()
                .reason("人工停止")
                .build());
        assertThat(stopped.getStatus()).isEqualTo(WorkflowStatus.STOPPED);

        WorkflowInstanceDTO resumed = service.resume(triggered.getInstanceId(), WorkflowResumeRequest.builder()
                .reason("恢复运行")
                .build());
        assertThat(resumed.getStatus()).isEqualTo(WorkflowStatus.RUNNING);

        WorkflowInstanceDTO snapshotAfterControlActions = service.findInstance(triggered.getInstanceId())
                .orElseThrow();
        assertThat(snapshotAfterControlActions.getStageLogs()).isEmpty();

        WorkflowInstanceDTO callback = service.callback(triggered.getInstanceId(), WorkflowCallbackRequest.builder()
                .stageCode("PARSE")
                .rawStatus("success")
                .message("阶段已完成")
                .payload(Map.of("rows", 12))
                .build());
        assertThat(callback.getStatus()).isEqualTo(WorkflowStatus.SUCCEEDED);
        assertThat(callback.getCurrentStageCode()).isEqualTo("PARSE");
        assertThat(callback.getStageLogs()).isEmpty();
        assertThat(service.listStageLogs(com.yss.valset.workflow.model.WorkflowLogQueryRequest.builder()
                .instanceId(triggered.getInstanceId())
                .stageCode("PARSE")
                .build())).isNotEmpty();

        PageResult<WorkflowInstanceViewDTO> instances = service.listInstances(WorkflowInstanceQueryRequest.builder()
                .workflowCode("valset-etl")
                .pageIndex(0)
                .pageSize(10)
                .build());
        assertThat(instances.getTotalCount()).isEqualTo(2);
        assertThat(instances.getData()).hasSize(2);
        assertThat(instances.getData().get(0).getCurrentStageCode()).isEqualTo("PARSE");
    }

    @Test
    void shouldListRemoteInstancesAndHydrateDetailsThroughAdapter() {
        InMemoryWorkflowRuntimeStore store = new InMemoryWorkflowRuntimeStore();
        DefaultWorkflowApplicationService service = new DefaultWorkflowApplicationService(
                store,
                List.of(new WorkflowPlatformAdapter() {
                    @Override
                    public EtlPlatformType platformType() {
                        return EtlPlatformType.DOLPHIN_SCHEDULER;
                    }

                    @Override
                    public void validate(WorkflowDefinitionDTO definition) {
                    }

                    @Override
                    public WorkflowDefinitionDTO syncDefinition(WorkflowDefinitionDTO definition) {
                        return definition;
                    }

                    @Override
                    public WorkflowDefinitionDTO onlineDefinition(WorkflowDefinitionDTO definition) {
                        return definition;
                    }

                    @Override
                    public WorkflowDefinitionDTO offlineDefinition(WorkflowDefinitionDTO definition) {
                        return definition;
                    }

                    @Override
                    public void deleteDefinition(WorkflowDefinitionDTO definition) {
                    }

                    @Override
                    public PageResult<WorkflowInstanceViewDTO> listInstances(WorkflowDefinitionDTO definition, WorkflowInstanceQueryRequest request) {
                        return PageResult.of(List.of(
                                WorkflowInstanceViewDTO.builder()
                                        .instanceId("ds-remote-1")
                                        .workflowCode(definition.getWorkflowCode())
                                        .workflowVersionNo(definition.getWorkflowVersionNo())
                                        .platformType(platformType())
                                        .businessKey("biz-1")
                                        .externalInstanceId("remote-instance-1")
                                        .externalWorkflowId(definition.getEngineBinding().getExternalWorkflowId())
                                        .status(WorkflowStatus.RUNNING)
                                        .rawStatus("RUNNING")
                                        .currentStageCode("TASK-1")
                                        .currentStageName("准备")
                                        .triggerTime(LocalDateTime.of(2026, 5, 10, 10, 0))
                                        .startTime(LocalDateTime.of(2026, 5, 10, 10, 1))
                                        .message("运行中")
                                        .stageCount(1)
                                        .build()
                        ), 1L, 10, 0);
                    }

                    @Override
                    public com.yss.valset.workflow.model.WorkflowPlatformExecutionResult trigger(WorkflowDefinitionDTO definition, WorkflowInstanceDTO instance, WorkflowTriggerRequest request) {
                        return null;
                    }

                    @Override
                    public com.yss.valset.workflow.model.WorkflowPlatformExecutionResult stop(WorkflowDefinitionDTO definition, WorkflowInstanceDTO instance, WorkflowStopRequest request) {
                        return null;
                    }

                    @Override
                    public com.yss.valset.workflow.model.WorkflowPlatformExecutionResult retry(WorkflowDefinitionDTO definition, WorkflowInstanceDTO instance, WorkflowRetryRequest request) {
                        return null;
                    }

                    @Override
                    public com.yss.valset.workflow.model.WorkflowPlatformExecutionResult query(WorkflowDefinitionDTO definition, WorkflowInstanceDTO instance) {
                        return buildResult(definition, instance, "RUNNING", "详情已加载");
                    }

                    @Override
                    public List<com.yss.valset.workflow.model.WorkflowPlatformExecutionResult> queryLogs(WorkflowDefinitionDTO definition, WorkflowInstanceDTO instance, com.yss.valset.workflow.model.WorkflowLogQueryRequest request) {
                        return List.of(com.yss.valset.workflow.model.WorkflowPlatformExecutionResult.builder()
                                .platformType(platformType())
                                .externalWorkflowId(definition.getEngineBinding().getExternalWorkflowId())
                                .externalInstanceId(instance.getExternalInstanceId())
                                .rawStatus("RUNNING")
                                .message("任务日志")
                                .payload(Map.of("source", "remote"))
                                .stageLogs(new ArrayList<>(List.of(
                                        com.yss.valset.workflow.model.WorkflowStageLogDTO.builder()
                                                .instanceId(instance.getInstanceId())
                                                .workflowCode(definition.getWorkflowCode())
                                                .workflowVersionNo(definition.getWorkflowVersionNo())
                                                .stageCode("TASK-1")
                                                .stageName("准备")
                                                .stageOrder(1)
                                                .status(WorkflowStatus.RUNNING)
                                                .rawStatus("RUNNING")
                                                .message("任务日志")
                                                .startTime(LocalDateTime.of(2026, 5, 10, 10, 1))
                                                .endTime(LocalDateTime.of(2026, 5, 10, 10, 2))
                                                .payload(Map.of("source", "remote"))
                                                .build())))
                                .build());
                    }

                    private com.yss.valset.workflow.model.WorkflowPlatformExecutionResult buildResult(WorkflowDefinitionDTO definition,
                                                                                                       WorkflowInstanceDTO instance,
                                                                                                       String rawStatus,
                                                                                                       String message) {
                        return com.yss.valset.workflow.model.WorkflowPlatformExecutionResult.builder()
                                .platformType(platformType())
                                .externalWorkflowId(definition.getEngineBinding().getExternalWorkflowId())
                                .externalInstanceId(instance.getExternalInstanceId())
                                .rawStatus(rawStatus)
                                .message(message)
                                .payload(Map.of("source", "remote"))
                                .stageLogs(new ArrayList<>(List.of(
                                        com.yss.valset.workflow.model.WorkflowStageLogDTO.builder()
                                                .instanceId(instance.getInstanceId())
                                                .workflowCode(definition.getWorkflowCode())
                                                .workflowVersionNo(definition.getWorkflowVersionNo())
                                                .stageCode("TASK-1")
                                                .stageName("准备")
                                                .stageOrder(1)
                                                .status(WorkflowStatus.fromRawStatus(rawStatus))
                                                .rawStatus(rawStatus)
                                                .message(message)
                                                .startTime(LocalDateTime.of(2026, 5, 10, 10, 1))
                                                .endTime(LocalDateTime.of(2026, 5, 10, 10, 2))
                                                .payload(Map.of("source", "remote"))
                                                .build())))
                                .build();
                    }
                }));

        WorkflowDefinitionDTO definition = WorkflowDefinitionDTO.builder()
                .workflowCode("wadas")
                .workflowName("WADAS ETL")
                .workflowVersionNo(1)
                .platformType(EtlPlatformType.DOLPHIN_SCHEDULER)
                .engineBinding(WorkflowEngineBindingDTO.builder()
                        .platformType(EtlPlatformType.DOLPHIN_SCHEDULER)
                        .externalWorkflowId("8001")
                        .externalProjectCode("173038802481536")
                        .externalNamespace("wadas-project")
                        .build())
                .stages(List.of(WorkflowStageDTO.builder()
                        .stageCode("TASK-1")
                        .stageName("准备")
                        .stageOrder(1)
                        .build()))
                .build();

        service.saveDefinition(definition);

        PageResult<WorkflowInstanceViewDTO> page = service.listInstances(WorkflowInstanceQueryRequest.builder()
                .workflowCode("wadas")
                .workflowVersionNo(1)
                .pageIndex(0)
                .pageSize(10)
                .build());
        assertThat(page.getTotalCount()).isEqualTo(1);
        assertThat(page.getData()).hasSize(1);
        assertThat(page.getData().get(0).getExternalInstanceId()).isEqualTo("remote-instance-1");

        List<com.yss.valset.workflow.model.WorkflowStageLogDTO> logs = service.listStageLogs(com.yss.valset.workflow.model.WorkflowLogQueryRequest.builder()
                .instanceId("ds-remote-1")
                .stageCode("TASK-1")
                .build());
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getMessage()).isEqualTo("任务日志");

        WorkflowInstanceDTO hydrated = service.findInstance("ds-remote-1").orElseThrow();
        assertThat(hydrated.getExternalInstanceId()).isEqualTo("remote-instance-1");
        assertThat(hydrated.getStageLogs()).isNotEmpty();
    }

    @Test
    void shouldRejectInvalidDefinition() {
        InMemoryWorkflowRuntimeStore store = new InMemoryWorkflowRuntimeStore();
        DefaultWorkflowApplicationService service = new DefaultWorkflowApplicationService(store, List.of());

        assertThatThrownBy(() -> service.saveDefinition(WorkflowDefinitionDTO.builder().build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("工作流定义不合法");
    }

    @Test
    void shouldListPlatformsAndValidateDefinition() {
        InMemoryWorkflowRuntimeStore store = new InMemoryWorkflowRuntimeStore();
        DefaultWorkflowApplicationService service = new DefaultWorkflowApplicationService(
                store,
                List.of(new WorkflowPlatformAdapter() {
                    @Override
                    public EtlPlatformType platformType() {
                        return EtlPlatformType.XXL_JOB;
                    }

                    @Override
                    public void validate(WorkflowDefinitionDTO definition) {
                    }

                    @Override
                    public WorkflowDefinitionDTO syncDefinition(WorkflowDefinitionDTO definition) {
                        return definition;
                    }

                    @Override
                    public WorkflowDefinitionDTO onlineDefinition(WorkflowDefinitionDTO definition) {
                        return definition;
                    }

                    @Override
                    public WorkflowDefinitionDTO offlineDefinition(WorkflowDefinitionDTO definition) {
                        return definition;
                    }

                    @Override
                    public void deleteDefinition(WorkflowDefinitionDTO definition) {
                    }

                    @Override
                    public com.yss.valset.workflow.model.WorkflowPlatformExecutionResult trigger(WorkflowDefinitionDTO definition, WorkflowInstanceDTO instance, WorkflowTriggerRequest request) {
                        return null;
                    }

                    @Override
                    public com.yss.valset.workflow.model.WorkflowPlatformExecutionResult stop(WorkflowDefinitionDTO definition, WorkflowInstanceDTO instance, WorkflowStopRequest request) {
                        return null;
                    }

                    @Override
                    public com.yss.valset.workflow.model.WorkflowPlatformExecutionResult retry(WorkflowDefinitionDTO definition, WorkflowInstanceDTO instance, WorkflowRetryRequest request) {
                        return null;
                    }

                    @Override
                    public com.yss.valset.workflow.model.WorkflowPlatformExecutionResult query(WorkflowDefinitionDTO definition, WorkflowInstanceDTO instance) {
                        return null;
                    }

                    @Override
                    public List<com.yss.valset.workflow.model.WorkflowPlatformExecutionResult> queryLogs(WorkflowDefinitionDTO definition, WorkflowInstanceDTO instance, com.yss.valset.workflow.model.WorkflowLogQueryRequest request) {
                        return List.of();
                    }
                }));

        WorkflowDefinitionDTO normalized = service.validateDefinition(WorkflowDefinitionDTO.builder()
                .workflowCode("xxl-job")
                .workflowName("XXL Job")
                .workflowVersionNo(1)
                .platformType(EtlPlatformType.XXL_JOB)
                .engineBinding(WorkflowEngineBindingDTO.builder()
                        .platformType(EtlPlatformType.XXL_JOB)
                        .externalJobGroup("group-a")
                        .externalJobHandler("handler-a")
                        .build())
                .stages(List.of(WorkflowStageDTO.builder()
                        .stageCode("BEGIN")
                        .stageName("开始")
                        .stageOrder(1)
                        .build()))
                .build());

        assertThat(normalized.getStages()).hasSize(1);

        List<WorkflowPlatformMetadataDTO> platforms = service.listPlatforms();
        assertThat(platforms).hasSize(1);
        assertThat(platforms.get(0).getPlatformType()).isEqualTo(EtlPlatformType.XXL_JOB);
        assertThat(platforms.get(0).getRequiredBindingFields()).containsExactly("externalJobGroup", "externalJobHandler");
    }

    @Test
    void shouldSyncDefinitionAndPersistUpdatedBinding() {
        InMemoryWorkflowRuntimeStore store = new InMemoryWorkflowRuntimeStore();
        DefaultWorkflowApplicationService service = new DefaultWorkflowApplicationService(
                store,
                List.of(new WorkflowPlatformAdapter() {
                    @Override
                    public EtlPlatformType platformType() {
                        return EtlPlatformType.DOLPHIN_SCHEDULER;
                    }

                    @Override
                    public void validate(WorkflowDefinitionDTO definition) {
                    }

                    @Override
                    public WorkflowDefinitionDTO syncDefinition(WorkflowDefinitionDTO definition) {
                        return definition.toBuilder()
                                .engineBinding(definition.getEngineBinding().toBuilder()
                                        .externalProjectCode("9001")
                                        .externalWorkflowId("8001")
                                        .externalNamespace("demo-ns")
                                        .remoteWorkflowVersionNo(12)
                                        .build())
                                .build();
                    }

                    @Override
                    public WorkflowDefinitionDTO onlineDefinition(WorkflowDefinitionDTO definition) {
                        return definition;
                    }

                    @Override
                    public WorkflowDefinitionDTO offlineDefinition(WorkflowDefinitionDTO definition) {
                        return definition;
                    }

                    @Override
                    public void deleteDefinition(WorkflowDefinitionDTO definition) {
                    }

                    @Override
                    public com.yss.valset.workflow.model.WorkflowPlatformExecutionResult trigger(WorkflowDefinitionDTO definition, WorkflowInstanceDTO instance, WorkflowTriggerRequest request) {
                        return null;
                    }

                    @Override
                    public com.yss.valset.workflow.model.WorkflowPlatformExecutionResult stop(WorkflowDefinitionDTO definition, WorkflowInstanceDTO instance, WorkflowStopRequest request) {
                        return null;
                    }

                    @Override
                    public com.yss.valset.workflow.model.WorkflowPlatformExecutionResult retry(WorkflowDefinitionDTO definition, WorkflowInstanceDTO instance, WorkflowRetryRequest request) {
                        return null;
                    }

                    @Override
                    public com.yss.valset.workflow.model.WorkflowPlatformExecutionResult query(WorkflowDefinitionDTO definition, WorkflowInstanceDTO instance) {
                        return null;
                    }

                    @Override
                    public List<com.yss.valset.workflow.model.WorkflowPlatformExecutionResult> queryLogs(WorkflowDefinitionDTO definition, WorkflowInstanceDTO instance, com.yss.valset.workflow.model.WorkflowLogQueryRequest request) {
                        return List.of();
                    }
                }));

        WorkflowDefinitionDTO definition = WorkflowDefinitionDTO.builder()
                .workflowCode("sync-etl")
                .workflowName("同步ETL")
                .workflowVersionNo(1)
                .platformType(EtlPlatformType.DOLPHIN_SCHEDULER)
                .engineBinding(WorkflowEngineBindingDTO.builder()
                        .platformType(EtlPlatformType.DOLPHIN_SCHEDULER)
                        .build())
                .stages(List.of(WorkflowStageDTO.builder()
                        .stageCode("S1")
                        .stageName("阶段1")
                        .stageOrder(1)
                        .build()))
                .build();

        WorkflowDefinitionDTO synced = service.syncDefinition(definition);
        assertThat(synced.getEngineBinding().getExternalProjectCode()).isEqualTo("9001");
        assertThat(synced.getEngineBinding().getExternalWorkflowId()).isEqualTo("8001");
        assertThat(synced.getEngineBinding().getExternalNamespace()).isEqualTo("demo-ns");
        assertThat(synced.getEngineBinding().getRemoteWorkflowVersionNo()).isEqualTo(12);
        assertThat(synced.getEngineBinding().getSyncStatus()).isEqualTo(WorkflowSyncStatus.SYNCED);
        assertThat(synced.getEngineBinding().getFirstSyncedAt()).isNotNull();
        assertThat(synced.getEngineBinding().getLastSyncedAt()).isNotNull();
        assertThat(service.findDefinition("sync-etl", 1)).isPresent();
    }

    @Test
    void shouldPersistFailedSyncStatusWhenAdapterFails() {
        InMemoryWorkflowRuntimeStore store = new InMemoryWorkflowRuntimeStore();
        DefaultWorkflowApplicationService service = new DefaultWorkflowApplicationService(
                store,
                List.of(new WorkflowPlatformAdapter() {
                    @Override
                    public EtlPlatformType platformType() {
                        return EtlPlatformType.DOLPHIN_SCHEDULER;
                    }

                    @Override
                    public void validate(WorkflowDefinitionDTO definition) {
                    }

                    @Override
                    public WorkflowDefinitionDTO syncDefinition(WorkflowDefinitionDTO definition) {
                        throw new IllegalStateException("远端接口异常");
                    }

                    @Override
                    public WorkflowDefinitionDTO onlineDefinition(WorkflowDefinitionDTO definition) {
                        return definition;
                    }

                    @Override
                    public WorkflowDefinitionDTO offlineDefinition(WorkflowDefinitionDTO definition) {
                        return definition;
                    }

                    @Override
                    public void deleteDefinition(WorkflowDefinitionDTO definition) {
                    }

                    @Override
                    public com.yss.valset.workflow.model.WorkflowPlatformExecutionResult trigger(WorkflowDefinitionDTO definition, WorkflowInstanceDTO instance, WorkflowTriggerRequest request) {
                        return null;
                    }

                    @Override
                    public com.yss.valset.workflow.model.WorkflowPlatformExecutionResult stop(WorkflowDefinitionDTO definition, WorkflowInstanceDTO instance, WorkflowStopRequest request) {
                        return null;
                    }

                    @Override
                    public com.yss.valset.workflow.model.WorkflowPlatformExecutionResult retry(WorkflowDefinitionDTO definition, WorkflowInstanceDTO instance, WorkflowRetryRequest request) {
                        return null;
                    }

                    @Override
                    public com.yss.valset.workflow.model.WorkflowPlatformExecutionResult query(WorkflowDefinitionDTO definition, WorkflowInstanceDTO instance) {
                        return null;
                    }

                    @Override
                    public List<com.yss.valset.workflow.model.WorkflowPlatformExecutionResult> queryLogs(WorkflowDefinitionDTO definition, WorkflowInstanceDTO instance, com.yss.valset.workflow.model.WorkflowLogQueryRequest request) {
                        return List.of();
                    }
                }));

        WorkflowDefinitionDTO definition = WorkflowDefinitionDTO.builder()
                .workflowCode("sync-failed")
                .workflowName("同步失败ETL")
                .workflowVersionNo(1)
                .platformType(EtlPlatformType.DOLPHIN_SCHEDULER)
                .engineBinding(WorkflowEngineBindingDTO.builder()
                        .platformType(EtlPlatformType.DOLPHIN_SCHEDULER)
                        .externalProjectCode("9001")
                        .externalWorkflowId("8001")
                        .build())
                .stages(List.of(WorkflowStageDTO.builder()
                        .stageCode("S1")
                        .stageName("阶段1")
                        .stageOrder(1)
                        .build()))
                .build();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.syncDefinition(definition))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("远端接口异常");

        WorkflowDefinitionDTO failed = service.findDefinition("sync-failed", 1).orElseThrow();
        assertThat(failed.getEngineBinding().getSyncStatus()).isEqualTo(WorkflowSyncStatus.FAILED);
        assertThat(failed.getEngineBinding().getSyncFailureReason()).contains("远端接口异常");
        assertThat(failed.getEngineBinding().getLastSyncedAt()).isNotNull();
    }

    @Test
    void shouldOnlineAndOfflineDefinitionThroughAdapter() {
        InMemoryWorkflowRuntimeStore store = new InMemoryWorkflowRuntimeStore();
        List<String> operations = new ArrayList<>();
        DefaultWorkflowApplicationService service = new DefaultWorkflowApplicationService(
                store,
                List.of(new WorkflowPlatformAdapter() {
                    @Override
                    public EtlPlatformType platformType() {
                        return EtlPlatformType.DOLPHIN_SCHEDULER;
                    }

                    @Override
                    public void validate(WorkflowDefinitionDTO definition) {
                    }

                    @Override
                    public WorkflowDefinitionDTO syncDefinition(WorkflowDefinitionDTO definition) {
                        return definition;
                    }

                    @Override
                    public WorkflowDefinitionDTO onlineDefinition(WorkflowDefinitionDTO definition) {
                        operations.add("online");
                        return definition.toBuilder().enabled(true).build();
                    }

                    @Override
                    public WorkflowDefinitionDTO offlineDefinition(WorkflowDefinitionDTO definition) {
                        operations.add("offline");
                        return definition.toBuilder().enabled(false).build();
                    }

                    @Override
                    public void deleteDefinition(WorkflowDefinitionDTO definition) {
                    }

                    @Override
                    public com.yss.valset.workflow.model.WorkflowPlatformExecutionResult trigger(WorkflowDefinitionDTO definition, WorkflowInstanceDTO instance, WorkflowTriggerRequest request) {
                        return null;
                    }

                    @Override
                    public com.yss.valset.workflow.model.WorkflowPlatformExecutionResult stop(WorkflowDefinitionDTO definition, WorkflowInstanceDTO instance, WorkflowStopRequest request) {
                        return null;
                    }

                    @Override
                    public com.yss.valset.workflow.model.WorkflowPlatformExecutionResult retry(WorkflowDefinitionDTO definition, WorkflowInstanceDTO instance, WorkflowRetryRequest request) {
                        return null;
                    }

                    @Override
                    public com.yss.valset.workflow.model.WorkflowPlatformExecutionResult query(WorkflowDefinitionDTO definition, WorkflowInstanceDTO instance) {
                        return null;
                    }

                    @Override
                    public List<com.yss.valset.workflow.model.WorkflowPlatformExecutionResult> queryLogs(WorkflowDefinitionDTO definition, WorkflowInstanceDTO instance, com.yss.valset.workflow.model.WorkflowLogQueryRequest request) {
                        return List.of();
                    }
                }));

        WorkflowDefinitionDTO definition = WorkflowDefinitionDTO.builder()
                .workflowCode("online-offline-etl")
                .workflowName("上线下线ETL")
                .workflowVersionNo(1)
                .platformType(EtlPlatformType.DOLPHIN_SCHEDULER)
                .enabled(false)
                .engineBinding(WorkflowEngineBindingDTO.builder()
                        .platformType(EtlPlatformType.DOLPHIN_SCHEDULER)
                        .externalProjectCode("9001")
                        .externalWorkflowId("8001")
                        .build())
                .stages(List.of(WorkflowStageDTO.builder()
                        .stageCode("S1")
                        .stageName("阶段1")
                        .stageOrder(1)
                        .build()))
                .build();

        service.saveDefinition(definition);

        WorkflowDefinitionDTO online = service.onlineDefinition("online-offline-etl", 1);
        WorkflowDefinitionDTO offline = service.offlineDefinition("online-offline-etl", 1);

        assertThat(operations).containsExactly("online", "offline");
        assertThat(online.isEnabled()).isTrue();
        assertThat(offline.isEnabled()).isFalse();
        assertThat(online.getEngineBinding().getExternalOnline()).isTrue();
        assertThat(online.getEngineBinding().getExternalReleaseState()).isEqualTo("ONLINE");
        assertThat(offline.getEngineBinding().getExternalOnline()).isFalse();
        assertThat(offline.getEngineBinding().getExternalReleaseState()).isEqualTo("OFFLINE");
    }

    @Test
    void shouldDeleteRemoteWorkflowBeforeRemovingLocalDefinition() {
        InMemoryWorkflowRuntimeStore store = new InMemoryWorkflowRuntimeStore();
        List<String> deleted = new ArrayList<>();
        DefaultWorkflowApplicationService service = new DefaultWorkflowApplicationService(
                store,
                List.of(new WorkflowPlatformAdapter() {
                    @Override
                    public EtlPlatformType platformType() {
                        return EtlPlatformType.DOLPHIN_SCHEDULER;
                    }

                    @Override
                    public void validate(WorkflowDefinitionDTO definition) {
                    }

                    @Override
                    public WorkflowDefinitionDTO syncDefinition(WorkflowDefinitionDTO definition) {
                        return definition;
                    }

                    @Override
                    public WorkflowDefinitionDTO onlineDefinition(WorkflowDefinitionDTO definition) {
                        return definition;
                    }

                    @Override
                    public WorkflowDefinitionDTO offlineDefinition(WorkflowDefinitionDTO definition) {
                        return definition;
                    }

                    @Override
                    public void deleteDefinition(WorkflowDefinitionDTO definition) {
                        deleted.add(definition.getWorkflowCode() + ":" + definition.getWorkflowVersionNo());
                    }

                    @Override
                    public com.yss.valset.workflow.model.WorkflowPlatformExecutionResult trigger(WorkflowDefinitionDTO definition, WorkflowInstanceDTO instance, WorkflowTriggerRequest request) {
                        return null;
                    }

                    @Override
                    public com.yss.valset.workflow.model.WorkflowPlatformExecutionResult stop(WorkflowDefinitionDTO definition, WorkflowInstanceDTO instance, WorkflowStopRequest request) {
                        return null;
                    }

                    @Override
                    public com.yss.valset.workflow.model.WorkflowPlatformExecutionResult retry(WorkflowDefinitionDTO definition, WorkflowInstanceDTO instance, WorkflowRetryRequest request) {
                        return null;
                    }

                    @Override
                    public com.yss.valset.workflow.model.WorkflowPlatformExecutionResult query(WorkflowDefinitionDTO definition, WorkflowInstanceDTO instance) {
                        return null;
                    }

                    @Override
                    public List<com.yss.valset.workflow.model.WorkflowPlatformExecutionResult> queryLogs(WorkflowDefinitionDTO definition, WorkflowInstanceDTO instance, com.yss.valset.workflow.model.WorkflowLogQueryRequest request) {
                        return List.of();
                    }
                }));

        WorkflowDefinitionDTO definition = WorkflowDefinitionDTO.builder()
                .workflowCode("delete-etl")
                .workflowName("删除ETL")
                .workflowVersionNo(1)
                .platformType(EtlPlatformType.DOLPHIN_SCHEDULER)
                .engineBinding(WorkflowEngineBindingDTO.builder()
                        .platformType(EtlPlatformType.DOLPHIN_SCHEDULER)
                        .externalProjectCode("9001")
                        .externalWorkflowId("8001")
                        .build())
                .stages(List.of(WorkflowStageDTO.builder()
                        .stageCode("S1")
                        .stageName("阶段1")
                        .stageOrder(1)
                        .build()))
                .build();

        service.saveDefinition(definition);
        assertThat(service.deleteDefinition("delete-etl", 1)).isTrue();
        assertThat(deleted).containsExactly("delete-etl:1");
        assertThat(service.findDefinition("delete-etl", 1)).isNotPresent();
    }
}
