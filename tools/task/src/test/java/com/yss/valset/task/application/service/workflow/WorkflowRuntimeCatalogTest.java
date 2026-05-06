package com.yss.valset.task.application.service.workflow;

import com.yss.valset.application.event.lifecycle.ParseLifecycleStage;
import com.yss.valset.domain.model.TaskStage;
import com.yss.valset.domain.model.TaskStatus;
import com.yss.valset.domain.model.TaskType;
import com.yss.valset.task.domain.model.OutsourcedDataTaskStage;
import com.yss.valset.task.domain.model.OutsourcedDataTaskStatus;
import com.yss.valset.task.infrastructure.entity.workflow.OutsourcedWorkflowDefinitionPO;
import com.yss.valset.task.infrastructure.entity.workflow.OutsourcedWorkflowStagePO;
import com.yss.valset.task.infrastructure.mapper.workflow.OutsourcedWorkflowDefinitionRepository;
import com.yss.valset.task.infrastructure.mapper.workflow.OutsourcedWorkflowStageRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 工作流运行态阶段目录测试。
 */
class WorkflowRuntimeCatalogTest {

    @Test
    void shouldRequireActiveWorkflowDefinitionForStageRules() {
        WorkflowRuntimeCatalog catalog = new WorkflowRuntimeCatalog();

        assertThat(catalog.resolveWorkflowStatus(TaskStatus.SUCCESS)).isEqualTo(OutsourcedDataTaskStatus.SUCCESS);
        assertThat(catalog.resolveWorkflowStatus(TaskStatus.RUNNING)).isEqualTo(OutsourcedDataTaskStatus.RUNNING);
        assertThat(catalog.resolveParseStepStatus(ParseLifecycleStage.TASK_STANDARDIZED))
                .isEqualTo(OutsourcedDataTaskStatus.SUCCESS);
        assertThat(catalog.statusLabel("SUCCESS")).isEqualTo("已完成");
        assertThatThrownBy(catalog::getStages)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("VALUATION_PARSE");
    }

    @Test
    void shouldResolveDatabaseWorkflowRules() {
        OutsourcedWorkflowDefinitionRepository definitionRepository = mock(
                OutsourcedWorkflowDefinitionRepository.class);
        OutsourcedWorkflowStageRepository stageRepository = mock(OutsourcedWorkflowStageRepository.class);
        OutsourcedWorkflowDefinitionPO definition = new OutsourcedWorkflowDefinitionPO();
        definition.setWorkflowId("wf-test");
        definition.setWorkflowCode("VALUATION_PARSE");
        definition.setWorkflowName("估值表解析工作流");
        definition.setEnabled(true);
        definition.setVersionNo(2);
        definition.setParseFallbackStage("FILE_PARSE");
        definition.setWorkflowFallbackStage("STANDARD_LANDING");
        OutsourcedWorkflowStagePO fileParse = stage("wf-test", OutsourcedDataTaskStage.FILE_PARSE.name(), 1);
        OutsourcedWorkflowStagePO structure = stage("wf-test", OutsourcedDataTaskStage.STRUCTURE_STANDARDIZE.name(), 2);
        OutsourcedWorkflowStagePO landing = stage("wf-test", OutsourcedDataTaskStage.STANDARD_LANDING.name(), 3);
        when(definitionRepository.selectOne(org.mockito.ArgumentMatchers.any())).thenReturn(definition);
        when(stageRepository.selectList(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(fileParse, structure, landing));

        WorkflowRuntimeCatalog catalog = new WorkflowRuntimeCatalog();
        catalog.setDefinitionRepository(definitionRepository);
        catalog.setStageRepository(stageRepository);

        assertThat(catalog.ignoreWorkflowTaskType(TaskType.PARSE_WORKBOOK)).isTrue();
        assertThat(catalog.resolveWorkflowStatus(TaskStatus.RETRYING)).isEqualTo(OutsourcedDataTaskStatus.RUNNING);
        assertThat(catalog.resolveWorkflowStage(TaskType.EXTRACT_DATA, TaskStage.EXTRACT))
                .isEqualTo(OutsourcedDataTaskStage.FILE_PARSE);
        assertThat(catalog.resolveParseLifecycleStage(ParseLifecycleStage.TASK_PERSISTED))
                .isEqualTo(OutsourcedDataTaskStage.STANDARD_LANDING);
        assertThat(catalog.activeWorkflowId()).isEqualTo("wf-test");
        assertThat(catalog.activeWorkflowVersionNo()).isEqualTo(2);
    }

    private static OutsourcedWorkflowStagePO stage(String workflowId, String stageCode, int sortOrder) {
        OutsourcedWorkflowStagePO stage = new OutsourcedWorkflowStagePO();
        stage.setStageId("stage-" + stageCode);
        stage.setWorkflowId(workflowId);
        stage.setStageCode(stageCode);
        stage.setStageName(stageCode);
        stage.setStageDescription(stageCode);
        stage.setSortOrder(sortOrder);
        stage.setEnabled(true);
        return stage;
    }
}
