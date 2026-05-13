package com.yss.valset.workflow.xxljob;

import com.yss.valset.workflow.model.EtlPlatformType;
import com.yss.valset.workflow.model.WorkflowDefinitionDTO;
import com.yss.valset.workflow.model.WorkflowEngineBindingDTO;
import com.yss.valset.workflow.model.WorkflowInstanceDTO;
import com.yss.valset.workflow.model.WorkflowStageDTO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class XxlJobWorkflowPlatformAdapterTest {

    private final XxlJobWorkflowPlatformAdapter adapter = new XxlJobWorkflowPlatformAdapter(new XxlJobWorkflowPlatformClient());

    @Test
    void shouldMapXxlJobPayloadAndRetryStatus() {
        WorkflowDefinitionDTO definition = definition();
        adapter.validate(definition);
        WorkflowInstanceDTO instance = WorkflowInstanceDTO.builder()
                .instanceId("instance-3")
                .workflowCode("xxl-job")
                .workflowVersionNo(3)
                .platformType(EtlPlatformType.XXL_JOB)
                .externalWorkflowId("xxl-flow")
                .rawStatus("RUNNING")
                .context(new java.util.HashMap<String, Object>() {{
                    put("businessKey", "bk-1");
                }})
                .build();

        assertThat(adapter.trigger(definition, instance, null).getPayload())
                .containsEntry("jobGroup", "group-a")
                .containsEntry("jobHandler", "handler-a")
                .containsEntry("executorRouteStrategy", "FIRST")
                .containsEntry("operationType", "TRIGGER");
        assertThat(adapter.retry(definition, instance, null).getRawStatus()).isEqualTo("RUNNING");
    }

    private WorkflowDefinitionDTO definition() {
        return WorkflowDefinitionDTO.builder()
                .workflowCode("xxl-job")
                .workflowName("XXL Job ETL")
                .workflowVersionNo(3)
                .platformType(EtlPlatformType.XXL_JOB)
                .engineBinding(WorkflowEngineBindingDTO.builder()
                        .platformType(EtlPlatformType.XXL_JOB)
                        .externalWorkflowId("xxl-flow")
                        .externalJobGroup("group-a")
                        .externalJobHandler("handler-a")
                        .build())
                .stages(java.util.Arrays.asList(
                        WorkflowStageDTO.builder().stageCode("BEGIN").stageName("开始").stageOrder(1).build(),
                        WorkflowStageDTO.builder().stageCode("END").stageName("结束").stageOrder(2).build()))
                .build();
    }
}
