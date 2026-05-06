package com.yss.valset.workflow.service;

import com.yss.valset.workflow.model.EtlPlatformType;
import com.yss.valset.workflow.model.WorkflowCallbackRequest;
import com.yss.valset.workflow.model.WorkflowDefinitionDTO;
import com.yss.valset.workflow.model.WorkflowEngineBindingDTO;
import com.yss.valset.workflow.model.WorkflowInstanceDTO;
import com.yss.valset.workflow.model.WorkflowRetryRequest;
import com.yss.valset.workflow.model.WorkflowPlatformMetadataDTO;
import com.yss.valset.workflow.model.WorkflowStageDTO;
import com.yss.valset.workflow.model.WorkflowStatus;
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
                    public com.yss.valset.workflow.model.WorkflowPlatformExecutionResult trigger(WorkflowDefinitionDTO definition, WorkflowInstanceDTO instance, WorkflowTriggerRequest request) {
                        return build(definition, instance, "SUBMITTED", "已提交");
                    }

                    @Override
                    public com.yss.valset.workflow.model.WorkflowPlatformExecutionResult stop(WorkflowDefinitionDTO definition, WorkflowInstanceDTO instance, WorkflowStopRequest request) {
                        return build(definition, instance, "STOPPED", request == null ? "任务已停止" : request.getReason());
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

        WorkflowInstanceDTO callback = service.callback(triggered.getInstanceId(), WorkflowCallbackRequest.builder()
                .stageCode("PARSE")
                .rawStatus("success")
                .message("阶段已完成")
                .payload(Map.of("rows", 12))
                .build());
        assertThat(callback.getStatus()).isEqualTo(WorkflowStatus.SUCCEEDED);
        assertThat(service.listStageLogs(com.yss.valset.workflow.model.WorkflowLogQueryRequest.builder()
                .instanceId(triggered.getInstanceId())
                .stageCode("PARSE")
                .build())).isNotEmpty();
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
}
