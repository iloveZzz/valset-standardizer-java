package com.yss.valset.task.application.impl.workflow;

import com.yss.valset.application.command.workflow.WorkflowRuntimeParamSaveCommand;
import com.yss.valset.application.dto.workflow.WorkflowRuntimeParamDTO;
import com.yss.valset.common.support.WorkflowRuntimeParamProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultWorkflowRuntimeParamServiceTest {

    private WorkflowRuntimeParamProperties properties;

    private DefaultWorkflowRuntimeParamService service;

    @BeforeEach
    void setUp() {
        properties = new WorkflowRuntimeParamProperties();
        properties.setSkipExcelStyleParsing(false);
        properties.setEnableMatchProcess(true);
        properties.setPersistStandardizedDwdDetails(false);
        service = new DefaultWorkflowRuntimeParamService(properties);
    }

    @Test
    void shouldFallbackToPropertiesWhenCacheEmpty() {
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

        WorkflowRuntimeParamDTO result = service.saveRuntimeParam(command);

        assertThat(result.getEnableMatchProcess()).isFalse();
        assertThat(service.getRuntimeParam().getPersistStandardizedDwdDetails()).isTrue();
    }
}
