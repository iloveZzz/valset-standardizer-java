package com.yss.valset.workflow.springbatch;

import com.yss.valset.workflow.model.EtlPlatformType;
import com.yss.valset.workflow.model.WorkflowDefinitionDTO;
import com.yss.valset.workflow.model.WorkflowEngineBindingDTO;
import com.yss.valset.workflow.model.WorkflowInstanceDTO;
import com.yss.valset.workflow.model.WorkflowOperationType;
import com.yss.valset.workflow.model.WorkflowPlatformCommand;
import com.yss.valset.workflow.model.WorkflowStageDTO;
import com.yss.valset.workflow.model.WorkflowStatus;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultSpringBatchStageProcessorTest {

    private final DefaultSpringBatchStageProcessor processor = new DefaultSpringBatchStageProcessor();

    @Test
    void shouldBuildBusinessAwareStandardizeStageResult() {
        WorkflowDefinitionDTO definition = WorkflowDefinitionDTO.builder()
                .workflowCode("valset-etl")
                .workflowName("估值ETL")
                .workflowVersionNo(1)
                .platformType(EtlPlatformType.SPRING_BATCH)
                .engineBinding(WorkflowEngineBindingDTO.builder()
                        .platformType(EtlPlatformType.SPRING_BATCH)
                        .externalWorkflowId("job-a")
                        .build())
                .stages(List.of(WorkflowStageDTO.builder()
                        .stageCode("STRUCTURE_STANDARDIZE")
                        .stageName("结构标准化")
                        .stageOrder(2)
                        .description("字段映射、数据清洗、STG 结构转换")
                        .retryable(true)
                        .timeoutSeconds(300)
                        .build()))
                .build();

        WorkflowInstanceDTO instance = WorkflowInstanceDTO.builder()
                .instanceId("ins-1")
                .workflowCode("valset-etl")
                .workflowVersionNo(1)
                .businessKey("FILE-1001")
                .platformType(EtlPlatformType.SPRING_BATCH)
                .externalWorkflowId("job-a")
                .rawStatus("RUNNING")
                .context(Map.of("dataSourceType", "EXCEL"))
                .build();

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("dataSourceType", "CSV");
        context.put("fileId", 1001);
        context.put("workbookPath", "/tmp/valuation.csv");
        context.put("createdBy", "zhudaoming");
        context.put("forceRebuild", true);
        context.put("topK", 8);

        WorkflowPlatformCommand command = WorkflowPlatformCommand.builder()
                .operationType(WorkflowOperationType.TRIGGER)
                .platformType(EtlPlatformType.SPRING_BATCH)
                .workflowCode("valset-etl")
                .workflowVersionNo(1)
                .workflowName("估值ETL")
                .instanceId("ins-1")
                .externalWorkflowId("job-a")
                .businessKey("FILE-1001")
                .stageCode("STRUCTURE_STANDARDIZE")
                .stageName("结构标准化")
                .stageOrder(2)
                .context(context)
                .build();

        SpringBatchStageExecutionResult result = processor.process(
                definition,
                instance,
                definition.getStages().get(0),
                command);

        assertThat(result.getStatus()).isEqualTo(WorkflowStatus.SUCCEEDED);
        assertThat(result.getRawStatus()).isEqualTo("SUCCEEDED");
        assertThat(result.getMessage()).isEqualTo("字段映射与结构标准化已完成");
        assertThat(result.getInput()).containsEntry("platformType", "SPRING_BATCH")
                .containsEntry("stageCode", "STRUCTURE_STANDARDIZE")
                .containsEntry("retryable", true)
                .containsEntry("timeoutSeconds", 300);
        assertThat(result.getOutput())
                .containsEntry("businessStage", "STRUCTURE_STANDARDIZE")
                .containsEntry("stageFamily", "STANDARDIZE")
                .containsEntry("result", "STRUCTURE_STANDARDIZE")
                .containsEntry("businessKey", "FILE-1001");
        assertThat(result.getOutput().get("stagePlan")).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> stagePlan = (Map<String, Object>) result.getOutput().get("stagePlan");
        assertThat(stagePlan)
                .containsEntry("businessStage", "STRUCTURE_STANDARDIZE")
                .containsEntry("sourceType", "CSV")
                .containsEntry("fileId", 1001L)
                .containsEntry("createdBy", "zhudaoming")
                .containsEntry("forceRebuild", true)
                .containsEntry("topK", 8L);
        assertThat(result.getMetadata())
                .containsEntry("businessStage", "STRUCTURE_STANDARDIZE")
                .containsEntry("normalizedStageCode", "STRUCTURE_STANDARDIZE")
                .containsEntry("dataSourceType", "CSV")
                .containsEntry("fileId", 1001L)
                .containsEntry("targetTables", List.of("t_stg_external_valuation", "t_stg_external_valuation_detail"));
    }
}
