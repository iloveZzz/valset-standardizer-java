package com.yss.valset.task.application.service.workflow;

import com.yss.valset.application.event.lifecycle.ParseLifecycleStage;
import com.yss.valset.domain.model.TaskStage;
import com.yss.valset.domain.model.TaskStatus;
import com.yss.valset.domain.model.TaskType;
import com.yss.valset.task.application.dto.workflow.WorkflowDefinitionDTO;
import com.yss.valset.task.application.dto.workflow.WorkflowStageDTO;
import com.yss.valset.task.application.dto.workflow.WorkflowStatusMappingDTO;
import com.yss.valset.task.application.port.workflow.WorkflowConfigGateway;
import com.yss.valset.task.domain.model.OutsourcedDataTaskStage;
import com.yss.valset.task.domain.model.OutsourcedDataTaskStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 工作流运行态阶段目录测试。
 */
class WorkflowRuntimeCatalogTest {

    @Test
    void shouldResolveDefaultWorkflowRules() {
        WorkflowRuntimeCatalog catalog = new WorkflowRuntimeCatalog();

        assertThat(catalog.ignoreWorkflowTaskType(TaskType.PARSE_WORKBOOK)).isTrue();
        assertThat(catalog.resolveWorkflowStatus(TaskStatus.SUCCESS)).isEqualTo(OutsourcedDataTaskStatus.SUCCESS);
        assertThat(catalog.resolveWorkflowStatus(TaskStatus.RUNNING)).isEqualTo(OutsourcedDataTaskStatus.RUNNING);
        assertThat(catalog.resolveParseStepStatus(ParseLifecycleStage.TASK_STANDARDIZED)).isEqualTo(OutsourcedDataTaskStatus.SUCCESS);
        assertThat(catalog.statusLabel("SUCCESS")).isEqualTo("已完成");
    }

    @Test
    void shouldResolveDatabaseWorkflowRules() {
        WorkflowConfigGateway gateway = mock(WorkflowConfigGateway.class);
        WorkflowDefinitionDTO definition = new WorkflowDefinitionDTO();
        definition.setWorkflowCode("VALUATION_PARSE");
        definition.setWorkflowId("wf-test");
        definition.setVersionNo(2);
        definition.setStages(List.of(stage(TaskType.PARSE_WORKBOOK.name(), TaskStage.PARSE.name())));
        definition.setIgnoredWorkflowTaskTypes(List.of());
        definition.setStatusMappings(List.of(
                status("WORKFLOW_TASK", TaskStatus.RETRYING.name(), OutsourcedDataTaskStatus.SUCCESS.name(), "重试视为成功")
        ));
        when(gateway.findActiveByCode("VALUATION_PARSE")).thenReturn(Optional.of(definition));

        WorkflowRuntimeCatalog catalog = new WorkflowRuntimeCatalog();
        catalog.setWorkflowConfigGateway(gateway);

        assertThat(catalog.ignoreWorkflowTaskType(TaskType.PARSE_WORKBOOK)).isFalse();
        assertThat(catalog.resolveWorkflowStatus(TaskStatus.RETRYING)).isEqualTo(OutsourcedDataTaskStatus.SUCCESS);
        assertThat(catalog.resolveWorkflowStage(TaskType.PARSE_WORKBOOK, null)).isEqualTo(OutsourcedDataTaskStage.FILE_PARSE);
        assertThat(catalog.activeWorkflowId()).isEqualTo("wf-test");
        assertThat(catalog.activeWorkflowVersionNo()).isEqualTo(2);
    }

    private static WorkflowStageDTO stage(String taskType, String taskStage) {
        WorkflowStageDTO stage = new WorkflowStageDTO();
        stage.setStageCode(OutsourcedDataTaskStage.FILE_PARSE.name());
        stage.setStepCode(OutsourcedDataTaskStage.FILE_PARSE.name());
        stage.setStageName("文件解析");
        stage.setStepName("文件解析");
        stage.setEnabled(Boolean.TRUE);
        stage.setTaskTypes(List.of(taskType));
        stage.setTaskStages(List.of(taskStage));
        return stage;
    }

    private static WorkflowStatusMappingDTO status(String sourceType, String sourceStatus, String targetStatus, String label) {
        WorkflowStatusMappingDTO mapping = new WorkflowStatusMappingDTO();
        mapping.setSourceType(sourceType);
        mapping.setSourceStatus(sourceStatus);
        mapping.setTargetStatus(targetStatus);
        mapping.setStatusLabel(label);
        return mapping;
    }
}
