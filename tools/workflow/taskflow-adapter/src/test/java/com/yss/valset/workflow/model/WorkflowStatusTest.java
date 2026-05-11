package com.yss.valset.workflow.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowStatusTest {

    @Test
    void shouldNormalizeCommonRawStatuses() {
        assertThat(WorkflowStatus.fromRawStatus("success")).isEqualTo(WorkflowStatus.SUCCEEDED);
        assertThat(WorkflowStatus.fromRawStatus("running")).isEqualTo(WorkflowStatus.RUNNING);
        assertThat(WorkflowStatus.fromRawStatus("RUNNING_EXECUTION")).isEqualTo(WorkflowStatus.RUNNING);
        assertThat(WorkflowStatus.fromRawStatus("SUBMITTED_SUCCESS")).isEqualTo(WorkflowStatus.SUBMITTED);
        assertThat(WorkflowStatus.fromRawStatus("READY_PAUSE")).isEqualTo(WorkflowStatus.STOPPED);
        assertThat(WorkflowStatus.fromRawStatus("READY_STOP")).isEqualTo(WorkflowStatus.STOPPED);
        assertThat(WorkflowStatus.fromRawStatus("retry")).isEqualTo(WorkflowStatus.RETRYING);
        assertThat(WorkflowStatus.fromRawStatus("unknown-value")).isEqualTo(WorkflowStatus.UNKNOWN);
    }
}
