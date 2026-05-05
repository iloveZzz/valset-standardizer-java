package com.yss.valset.task.application.impl.workflow;

import com.yss.valset.application.command.workflow.WorkflowRuntimeParamSaveCommand;
import com.yss.valset.application.dto.workflow.WorkflowRuntimeParamDTO;
import com.yss.valset.common.support.WorkflowRuntimeParamProperties;
import com.yss.valset.task.application.port.workflow.WorkflowRuntimeParamGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultWorkflowRuntimeParamServiceTest {

    @Mock
    private WorkflowRuntimeParamGateway runtimeParamGateway;

    private WorkflowRuntimeParamProperties properties;

    private DefaultWorkflowRuntimeParamService service;

    @BeforeEach
    void setUp() {
        properties = new WorkflowRuntimeParamProperties();
        properties.setSkipExcelStyleParsing(false);
        properties.setEnableMatchProcess(true);
        properties.setPersistStandardizedDwdDetails(false);
        service = new DefaultWorkflowRuntimeParamService(runtimeParamGateway, properties);
    }

    @Test
    void shouldFallbackToPropertiesWhenDatabaseMissing() {
        when(runtimeParamGateway.findByNamespace("subject.match.workflow")).thenReturn(Optional.empty());

        WorkflowRuntimeParamDTO result = service.getRuntimeParam();

        assertThat(result.getParamNamespace()).isEqualTo("subject.match.workflow");
        assertThat(result.getEnableMatchProcess()).isTrue();
        assertThat(result.getSkipExcelStyleParsing()).isFalse();
        assertThat(result.getPersistStandardizedDwdDetails()).isFalse();
    }

    @Test
    void shouldPersistRuntimeParamAndRefreshCache() {
        WorkflowRuntimeParamSaveCommand command = new WorkflowRuntimeParamSaveCommand();
        command.setParamNamespace("subject.match.workflow");
        command.setSkipExcelStyleParsing(true);
        command.setEnableMatchProcess(false);
        command.setPersistStandardizedDwdDetails(true);
        command.setDescription("测试参数");

        WorkflowRuntimeParamDTO saved = new WorkflowRuntimeParamDTO();
        saved.setRuntimeParamId("1");
        saved.setParamNamespace("subject.match.workflow");
        saved.setSkipExcelStyleParsing(true);
        saved.setEnableMatchProcess(false);
        saved.setPersistStandardizedDwdDetails(true);
        saved.setDescription("测试参数");
        when(runtimeParamGateway.save(command)).thenReturn(saved);

        WorkflowRuntimeParamDTO result = service.saveRuntimeParam(command);

        assertThat(result.getEnableMatchProcess()).isFalse();
        assertThat(service.getRuntimeParam().getPersistStandardizedDwdDetails()).isTrue();
        verify(runtimeParamGateway).save(command);
    }
}
