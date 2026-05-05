package com.yss.valset.task.application.impl.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.cloud.dto.response.PageResult;
import com.yss.valset.task.application.command.workflow.WorkflowConfigAuditQueryCommand;
import com.yss.valset.task.application.command.workflow.WorkflowConfigSaveCommand;
import com.yss.valset.task.application.service.workflow.WorkflowRuntimeCatalog;
import com.yss.valset.task.application.dto.workflow.WorkflowConfigAuditDTO;
import com.yss.valset.task.application.dto.workflow.WorkflowDefinitionDTO;
import com.yss.valset.task.application.dto.workflow.WorkflowStageDTO;
import com.yss.valset.task.application.event.workflow.WorkflowConfigRuntimeRefreshEvent;
import com.yss.valset.task.application.port.workflow.WorkflowConfigAuditGateway;
import com.yss.valset.task.application.port.workflow.WorkflowConfigGateway;
import com.yss.valset.task.application.service.workflow.engine.WorkflowEngineAdapter;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultWorkflowConfigServiceTest {

    @Mock
    private WorkflowConfigGateway workflowConfigGateway;

    @Mock
    private WorkflowConfigAuditGateway workflowConfigAuditGateway;

    @Mock
    private WorkflowRuntimeCatalog stageCatalog;

    @Mock
    private WorkflowEngineAdapter workflowEngineAdapter;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private DefaultWorkflowConfigService service;

    @BeforeEach
    void setUp() {
        service = new DefaultWorkflowConfigService(
                workflowConfigGateway,
                workflowConfigAuditGateway,
                stageCatalog,
                new ObjectMapper(),
                List.of(workflowEngineAdapter),
                applicationEventPublisher);
    }

    @Test
    void shouldPageAuditRecordsThroughGateway() {
        WorkflowConfigAuditQueryCommand query = new WorkflowConfigAuditQueryCommand();
        query.setWorkflowCode("VALUATION_PARSE");
        query.setPageIndex(1);
        query.setPageSize(10);

        PageResult<WorkflowConfigAuditDTO> expected = PageResult.of(List.of(), 0L, 10, 1);
        when(workflowConfigAuditGateway.pageAudits(query)).thenReturn(expected);

        PageResult<WorkflowConfigAuditDTO> result = service.pageAuditRecords(query);

        assertThat(result.getTotalCount()).isZero();
        verify(workflowConfigAuditGateway).pageAudits(query);
    }

    @Test
    void shouldReturnAuditRecordById() {
        WorkflowConfigAuditDTO audit = new WorkflowConfigAuditDTO();
        audit.setAuditId("AUDIT-1");
        when(workflowConfigAuditGateway.findById("AUDIT-1")).thenReturn(Optional.of(audit));

        WorkflowConfigAuditDTO result = service.getAuditRecord("AUDIT-1");

        assertThat(result.getAuditId()).isEqualTo("AUDIT-1");
    }

    @Test
    void shouldRefreshStageCatalogAfterPublish() {
        WorkflowDefinitionDTO before = buildDefinition("DRAFT");
        WorkflowDefinitionDTO saved = buildDefinition("PUBLISHED");
        saved.setEnabled(true);
        saved.setStatus("PUBLISHED");
        when(workflowConfigGateway.findById("WF-1")).thenReturn(Optional.of(before), Optional.of(saved));
        when(workflowConfigGateway.save(any())).thenReturn(saved);

        WorkflowDefinitionDTO result = service.publish("WF-1");

        assertThat(result.getStatus()).isEqualTo("PUBLISHED");
        verify(applicationEventPublisher).publishEvent(any(WorkflowConfigRuntimeRefreshEvent.class));
        verify(stageCatalog, never()).refreshActiveWorkflowDefinition();
        verify(workflowConfigAuditGateway).record(any());
        verify(workflowConfigGateway).disableOtherVersions("VALUATION_PARSE", "WF-1");
        verify(workflowConfigGateway).updateStatus("WF-1", "PUBLISHED", true);
    }

    @Test
    void shouldNotRefreshStageCatalogWhenSavingDraft() {
        WorkflowConfigSaveCommand command = buildSaveCommand();
        when(workflowConfigGateway.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.saveDraft(command);

        verify(stageCatalog, never()).refreshActiveWorkflowDefinition();
    }

    private WorkflowDefinitionDTO buildDefinition(String status) {
        WorkflowDefinitionDTO dto = new WorkflowDefinitionDTO();
        dto.setWorkflowId("WF-1");
        dto.setWorkflowCode("VALUATION_PARSE");
        dto.setWorkflowName("估值表解析工作流");
        dto.setBusinessType("VALUATION");
        dto.setEngineType("INTERNAL");
        dto.setParseFallbackStage("FILE_PARSE");
        dto.setWorkflowFallbackStage("STANDARD_LANDING");
        dto.setVersionNo(1);
        dto.setStatus(status);
        dto.setEnabled("PUBLISHED".equals(status));
        dto.setStages(List.of(buildStage("FILE_PARSE", "文件解析"), buildStage("STANDARD_LANDING", "标准落地")));
        return dto;
    }

    private WorkflowConfigSaveCommand buildSaveCommand() {
        WorkflowConfigSaveCommand command = new WorkflowConfigSaveCommand();
        command.setWorkflowId("WF-1");
        command.setWorkflowCode("VALUATION_PARSE");
        command.setWorkflowName("估值表解析工作流");
        command.setBusinessType("VALUATION");
        command.setEngineType("INTERNAL");
        command.setParseFallbackStage("FILE_PARSE");
        command.setWorkflowFallbackStage("STANDARD_LANDING");
        command.setVersionNo(1);
        command.setStages(List.of(buildStage("FILE_PARSE", "文件解析"), buildStage("STANDARD_LANDING", "标准落地")));
        return command;
    }

    private WorkflowStageDTO buildStage(String code, String name) {
        WorkflowStageDTO stage = new WorkflowStageDTO();
        stage.setStageCode(code);
        stage.setStepCode(code);
        stage.setStageName(name);
        stage.setStepName(name);
        stage.setSortOrder(1);
        stage.setRetryable(true);
        stage.setSkippable(false);
        stage.setEnabled(true);
        return stage;
    }
}
