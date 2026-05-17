package com.yss.valset.workflow.springbatch;

import com.yss.valset.workflow.model.EtlPlatformType;
import com.yss.valset.workflow.model.WorkflowDefinitionDTO;
import com.yss.valset.workflow.model.WorkflowEngineBindingDTO;
import com.yss.valset.workflow.model.WorkflowInstanceDTO;
import com.yss.valset.workflow.model.WorkflowPlatformExecutionResult;
import com.yss.valset.workflow.model.WorkflowStageDTO;
import com.yss.valset.workflow.model.WorkflowTriggerRequest;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SpringBatchWorkflowPlatformAdapterTest {

    @Test
    void shouldRunRealSpringBatchJobAndMapStatuses() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(SpringBatchWorkflowRuntimeConfiguration.class)) {
            SpringBatchWorkflowPlatformAdapter adapter = context.getBean(SpringBatchWorkflowPlatformAdapter.class);

            WorkflowDefinitionDTO definition = definition();
            adapter.validate(definition);
            WorkflowInstanceDTO instance = WorkflowInstanceDTO.builder()
                    .instanceId("instance-1")
                    .workflowCode("spring-batch")
                    .workflowVersionNo(1)
                    .platformType(EtlPlatformType.SPRING_BATCH)
                    .externalWorkflowId("job-a")
                    .rawStatus("RUNNING")
                    .context(new java.util.HashMap<String, Object>() {{
                        put("input", "demo");
                    }})
                    .build();

            assertThat(adapter.trigger(definition, instance, WorkflowTriggerRequest.builder().context(new java.util.HashMap<String, Object>() {{
                put("input", "demo");
            }}).build())
                    .getPayload())
                    .containsEntry("jobName", "spring-batch-v1")
                    .containsEntry("batchInfrastructure", "resourceless")
                    .containsEntry("operationType", "TRIGGER")
                    .containsEntry("batchStatus", "COMPLETED");

            assertThat(adapter.query(definition, instance).getRawStatus()).isEqualTo("COMPLETED");
            List<WorkflowPlatformExecutionResult> logs = adapter.queryLogs(definition, instance, null);
            assertThat(logs).hasSize(2);
            Map<String, Object> stagePayload = logs.get(0).getStageLogs().get(0).getPayload();
            assertThat(stagePayload)
                    .containsEntry("executionStatus", "SUCCEEDED")
                    .containsKey("input")
                    .containsKey("output")
                    .containsKey("metadata");
            assertThat(stagePayload.get("output")).isInstanceOf(Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> output = (Map<String, Object>) stagePayload.get("output");
            assertThat(output)
                    .containsEntry("businessStage", "FILE_PARSE")
                    .containsEntry("stageFamily", "EXTRACT")
                    .containsKey("stagePlan")
                    .containsKey("qualityChecks")
                    .containsKey("targetTables");
        }
    }

    private WorkflowDefinitionDTO definition() {
        return WorkflowDefinitionDTO.builder()
                .workflowCode("spring-batch")
                .workflowName("批量任务 ETL")
                .workflowVersionNo(1)
                .platformType(EtlPlatformType.SPRING_BATCH)
                .engineBinding(WorkflowEngineBindingDTO.builder()
                        .platformType(EtlPlatformType.SPRING_BATCH)
                        .externalWorkflowId("job-a")
                        .build())
                .stages(java.util.Arrays.asList(
                        WorkflowStageDTO.builder().stageCode("EXTRACT").stageName("抽取").stageOrder(1).build(),
                        WorkflowStageDTO.builder().stageCode("LOAD").stageName("装载").stageOrder(2).build()))
                .build();
    }
}
