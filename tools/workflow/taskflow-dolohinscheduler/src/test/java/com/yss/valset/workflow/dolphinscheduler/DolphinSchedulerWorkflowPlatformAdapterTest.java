package com.yss.valset.workflow.dolphinscheduler;

import com.yss.valset.workflow.model.EtlPlatformType;
import com.yss.valset.workflow.model.WorkflowDefinitionDTO;
import com.yss.valset.workflow.model.WorkflowEngineBindingDTO;
import com.yss.valset.workflow.model.WorkflowInstanceDTO;
import com.yss.valset.workflow.model.WorkflowStageDTO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DolphinSchedulerWorkflowPlatformAdapterTest {

    private final DolphinSchedulerWorkflowPlatformAdapter adapter =
            new DolphinSchedulerWorkflowPlatformAdapter(new DolphinSchedulerWorkflowPlatformClient(null, new DolphinSchedulerResponseSupport()));

    @Test
    void shouldMapDolphinSchedulerPayloadAndLogs() {
        WorkflowDefinitionDTO definition = definition();
        adapter.validate(definition);
        WorkflowInstanceDTO instance = WorkflowInstanceDTO.builder()
                .instanceId("instance-2")
                .workflowCode("dolphin")
                .workflowVersionNo(2)
                .platformType(EtlPlatformType.DOLPHIN_SCHEDULER)
                .externalWorkflowId("ds-flow")
                .rawStatus("RUNNING")
                .context(Map.of("bizDate", "2026-05-06"))
                .build();

        assertThat(adapter.trigger(definition, instance, null).getPayload())
                .containsEntry("projectCode", "project-a")
                .containsEntry("workflowName", "ds-flow")
                .containsEntry("namespace", "default")
                .containsEntry("operationType", "TRIGGER");
        assertThat(adapter.queryLogs(definition, instance, null)).isEmpty();
    }

    private WorkflowDefinitionDTO definition() {
        return WorkflowDefinitionDTO.builder()
                .workflowCode("dolphin")
                .workflowName("Dolphin ETL")
                .workflowVersionNo(2)
                .platformType(EtlPlatformType.DOLPHIN_SCHEDULER)
                .engineBinding(WorkflowEngineBindingDTO.builder()
                        .platformType(EtlPlatformType.DOLPHIN_SCHEDULER)
                        .externalWorkflowId("ds-flow")
                        .externalProjectCode("project-a")
                        .externalNamespace("default")
                        .build())
                .stages(List.of(
                        WorkflowStageDTO.builder().stageCode("PREPARE").stageName("准备").stageOrder(1).build(),
                        WorkflowStageDTO.builder().stageCode("RUN").stageName("执行").stageOrder(2).build()))
                .build();
    }
}
