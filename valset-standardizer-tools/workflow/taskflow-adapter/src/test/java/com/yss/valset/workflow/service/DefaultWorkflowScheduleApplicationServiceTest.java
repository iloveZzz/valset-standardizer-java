package com.yss.valset.workflow.service;

import com.yss.valset.workflow.model.EtlPlatformType;
import com.yss.valset.workflow.model.WorkflowDefinitionDTO;
import com.yss.valset.workflow.model.WorkflowEngineBindingDTO;
import com.yss.valset.workflow.model.WorkflowScheduleDTO;
import com.yss.valset.workflow.model.WorkflowSchedulePreviewRequest;
import com.yss.valset.workflow.model.WorkflowStageDTO;
import com.yss.valset.workflow.spi.WorkflowSchedulePlatformAdapter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultWorkflowScheduleApplicationServiceTest {

    @Test
    void shouldSaveQueryPreviewAndOperateSchedule() {
        InMemoryWorkflowRuntimeStore store = new InMemoryWorkflowRuntimeStore();
        store.saveDefinition(WorkflowDefinitionDTO.builder()
                .workflowCode("dolphin-workflow")
                .workflowName("Dolphin Workflow")
                .workflowVersionNo(1)
                .platformType(EtlPlatformType.DOLPHIN_SCHEDULER)
                .engineBinding(WorkflowEngineBindingDTO.builder()
                        .platformType(EtlPlatformType.DOLPHIN_SCHEDULER)
                        .externalProjectCode("100")
                        .externalWorkflowId("200")
                        .build())
                .stages(List.of(WorkflowStageDTO.builder()
                        .stageCode("BEGIN")
                        .stageName("开始")
                        .stageOrder(1)
                        .build()))
                .build());

        DefaultWorkflowScheduleApplicationService service = new DefaultWorkflowScheduleApplicationService(
                store,
                List.of(new WorkflowSchedulePlatformAdapter() {
                    @Override
                    public EtlPlatformType platformType() {
                        return EtlPlatformType.DOLPHIN_SCHEDULER;
                    }

                    @Override
                    public void validate(WorkflowScheduleDTO schedule) {
                    }

                    @Override
                    public WorkflowScheduleDTO saveSchedule(WorkflowScheduleDTO schedule) {
                        return schedule.toBuilder().scheduleId(11L).message("created").build();
                    }

                    @Override
                    public WorkflowScheduleDTO updateSchedule(WorkflowScheduleDTO schedule) {
                        return schedule.toBuilder().message("updated").build();
                    }

                    @Override
                    public void deleteSchedule(Long projectCode, Long scheduleId) {
                    }

                    @Override
                    public void onlineSchedule(Long projectCode, Long scheduleId) {
                    }

                    @Override
                    public void offlineSchedule(Long projectCode, Long scheduleId) {
                    }

                    @Override
                    public List<WorkflowScheduleDTO> querySchedules(WorkflowScheduleDTO schedule) {
                        return List.of(schedule.toBuilder().scheduleId(11L).releaseState("ONLINE").online(true).build());
                    }

                    @Override
                    public List<String> previewSchedule(WorkflowSchedulePreviewRequest request) {
                        return List.of("2026-05-07 10:00:00", "2026-05-07 11:00:00");
                    }
                }));

        WorkflowScheduleDTO saved = service.saveSchedule(WorkflowScheduleDTO.builder()
                .workflowCode("dolphin-workflow")
                .workflowVersionNo(1)
                .scheduleJson("{\"crontab\":\"0 0 * * * ?\"}")
                .build());
        assertThat(saved.getScheduleId()).isEqualTo(11L);
        assertThat(saved.getProjectCode()).isEqualTo(100L);
        assertThat(saved.getWorkflowDefinitionCode()).isEqualTo(200L);

        WorkflowScheduleDTO updated = service.updateSchedule(11L, WorkflowScheduleDTO.builder()
                .workflowCode("dolphin-workflow")
                .workflowVersionNo(1)
                .scheduleJson("{\"crontab\":\"0 0 * * * ?\"}")
                .build());
        assertThat(updated.getMessage()).isEqualTo("updated");

        List<WorkflowScheduleDTO> schedules = service.querySchedules(WorkflowScheduleDTO.builder()
                .workflowCode("dolphin-workflow")
                .workflowVersionNo(1)
                .scheduleJson("{\"crontab\":\"0 0 * * * ?\"}")
                .build());
        assertThat(schedules).hasSize(1);
        assertThat(schedules.get(0).getOnline()).isTrue();

        List<String> preview = service.previewSchedule(WorkflowSchedulePreviewRequest.builder()
                .projectCode(100L)
                .scheduleJson("{\"crontab\":\"0 0 * * * ?\"}")
                .build());
        assertThat(preview).containsExactly("2026-05-07 10:00:00", "2026-05-07 11:00:00");
    }
}
