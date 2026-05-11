package com.yss.valset.workflow.dolphinscheduler;

import com.yss.valset.workflow.model.EtlPlatformType;
import com.yss.valset.workflow.model.WorkflowScheduleDTO;
import com.yss.valset.workflow.model.WorkflowSchedulePreviewRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DolphinSchedulerWorkflowSchedulePlatformAdapterTest {

    private final DolphinSchedulerWorkflowSchedulePlatformAdapter adapter =
            new DolphinSchedulerWorkflowSchedulePlatformAdapter(
                    new DolphinSchedulerWorkflowSchedulePlatformClient(null, new DolphinSchedulerResponseSupport()));

    @Test
    void shouldValidateAndFallbackWithoutRemoteAddress() {
        WorkflowScheduleDTO schedule = WorkflowScheduleDTO.builder()
                .projectCode(100L)
                .workflowDefinitionCode(200L)
                .workflowCode("dolphin-workflow")
                .workflowVersionNo(1)
                .scheduleJson("{\"crontab\":\"0 0 * * * ?\"}")
                .build();

        adapter.validate(schedule);
        assertThat(adapter.platformType()).isEqualTo(EtlPlatformType.DOLPHIN_SCHEDULER);
        assertThat(adapter.saveSchedule(schedule).getScheduleJson()).contains("crontab");
        assertThat(adapter.querySchedules(schedule)).isEmpty();
        assertThat(adapter.previewSchedule(WorkflowSchedulePreviewRequest.builder()
                .projectCode(100L)
                .scheduleJson("{\"crontab\":\"0 0 * * * ?\"}")
                .build())).isEmpty();
    }
}
