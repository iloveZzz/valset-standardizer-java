package com.yss.valset.workflow.dolphinscheduler;

import com.yss.valset.workflow.model.WorkflowScheduleDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.util.MultiValueMap;

class DolphinSchedulerWorkflowSchedulePlatformClientHttpTest {

    @Test
    void shouldCallRemoteScheduleEndpointsWithExpectedPayloads() {
        DolphinSchedulerRemoteApi remoteApi = mock(DolphinSchedulerRemoteApi.class);
        when(remoteApi.createSchedule(eq(100L), any(MultiValueMap.class))).thenReturn("""
                {"code":0,"msg":"success","data":{"id":11,"projectCode":100,"workflowDefinitionCode":200,"schedule":"{\\"crontab\\":\\"0 0 * * * ?\\"}","releaseState":"OFFLINE","online":false}}
                """);
        when(remoteApi.updateSchedule(eq(100L), eq(11L), any(MultiValueMap.class))).thenReturn("""
                {"code":0,"msg":"success","data":{"id":11,"projectCode":100,"workflowDefinitionCode":200,"schedule":"{\\"crontab\\":\\"0 0 * * * ?\\"}","releaseState":"ONLINE","online":true}}
                """);
        when(remoteApi.listSchedules(eq(100L), anyMap())).thenReturn("""
                {"code":0,"msg":"success","data":[{"id":11,"projectCode":100,"workflowDefinitionCode":200,"schedule":"{\\"crontab\\":\\"0 0 * * * ?\\"}","releaseState":"ONLINE","online":true}]}
                """);
        when(remoteApi.previewSchedule(eq(100L), any(MultiValueMap.class))).thenReturn("""
                {"code":0,"msg":"success","data":["2026-05-07 10:00:00","2026-05-07 11:00:00"]}
                """);
        when(remoteApi.onlineSchedule(eq(100L), eq(11L))).thenReturn("""
                {"code":0,"msg":"success","data":{}}
                """);
        when(remoteApi.offlineSchedule(eq(100L), eq(11L))).thenReturn("""
                {"code":0,"msg":"success","data":{}}
                """);
        when(remoteApi.deleteSchedule(eq(100L), eq(11L))).thenReturn("""
                {"code":0,"msg":"success","data":{}}
                """);

        DolphinSchedulerWorkflowSchedulePlatformClient client =
                new DolphinSchedulerWorkflowSchedulePlatformClient(remoteApi, new DolphinSchedulerResponseSupport());

        WorkflowScheduleDTO request = WorkflowScheduleDTO.builder()
                .projectCode(100L)
                .workflowDefinitionCode(200L)
                .workflowCode("dolphin-workflow")
                .workflowVersionNo(1)
                .scheduleJson("{\"crontab\":\"0 0 * * * ?\"}")
                .build();

        WorkflowScheduleDTO saved = client.saveSchedule(request);
        assertThat(saved.getScheduleId()).isEqualTo(11L);
        assertThat(saved.getReleaseState()).isEqualTo("OFFLINE");
        assertThat(saved.getOnline()).isFalse();

        WorkflowScheduleDTO updated = client.updateSchedule(request.toBuilder().scheduleId(11L).build());
        assertThat(updated.getScheduleId()).isEqualTo(11L);
        assertThat(updated.getOnline()).isTrue();

        List<WorkflowScheduleDTO> schedules = client.querySchedules(request);
        assertThat(schedules).hasSize(1);
        assertThat(schedules.get(0).getScheduleId()).isEqualTo(11L);
        assertThat(schedules.get(0).getOnline()).isTrue();

        assertThat(client.previewSchedule(com.yss.valset.workflow.model.WorkflowSchedulePreviewRequest.builder()
                .projectCode(100L)
                .scheduleJson("{\"crontab\":\"0 0 * * * ?\"}")
                .build())).containsExactly("2026-05-07 10:00:00", "2026-05-07 11:00:00");

        client.onlineSchedule(100L, 11L);
        client.offlineSchedule(100L, 11L);
        client.deleteSchedule(100L, 11L);
    }
}
