package com.yss.valset.workflow.dolphinscheduler;

import com.yss.valset.workflow.model.WorkflowScheduleDTO;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DolphinSchedulerWorkflowSchedulePlatformClientHttpTest {

    @Test
    void shouldCallRemoteScheduleEndpointsWithExpectedPayloads() {
        DolphinSchedulerWorkflowSchedulePlatformClient client = new DolphinSchedulerWorkflowSchedulePlatformClient();
        DolphinSchedulerApiSupport apiSupport = (DolphinSchedulerApiSupport) ReflectionTestUtils.getField(client, "apiSupport");
        assertThat(apiSupport).isNotNull();

        RestTemplate restTemplate = new RestTemplate();
        apiSupport.setBaseUrl("http://dolphin.example");
        apiSupport.setRestTemplate(restTemplate);

        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(false).build();

        server.expect(requestTo("http://dolphin.example/projects/100/schedules"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(containsString("workflowDefinitionCode=200")))
                .andExpect(content().string(containsString("warningType=NONE")))
                .andExpect(content().string(containsString("failureStrategy=CONTINUE")))
                .andExpect(content().string(containsString("workerGroup=default")))
                .andRespond(withSuccess("""
                        {"code":0,"msg":"success","data":{"id":11,"projectCode":100,"workflowDefinitionCode":200,"schedule":"{\\"crontab\\":\\"0 0 * * * ?\\"}","releaseState":"OFFLINE","online":false}}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://dolphin.example/projects/100/schedules/11"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(containsString("workflowDefinitionCode=200")))
                .andExpect(content().string(containsString("schedule=")))
                .andRespond(withSuccess("""
                        {"code":0,"msg":"success","data":{"id":11,"projectCode":100,"workflowDefinitionCode":200,"schedule":"{\\"crontab\\":\\"0 0 * * * ?\\"}","releaseState":"ONLINE","online":true}}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://dolphin.example/projects/100/schedules?workflowDefinitionCode=200&pageNo=1&pageSize=100"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"code":0,"msg":"success","data":[{"id":11,"projectCode":100,"workflowDefinitionCode":200,"schedule":"{\\"crontab\\":\\"0 0 * * * ?\\"}","releaseState":"ONLINE","online":true}]}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://dolphin.example/projects/100/schedules/preview"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(containsString("schedule=")))
                .andRespond(withSuccess("""
                        {"code":0,"msg":"success","data":["2026-05-07 10:00:00","2026-05-07 11:00:00"]}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://dolphin.example/projects/100/schedules/11/online"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"code\":0,\"msg\":\"success\",\"data\":{}}", MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://dolphin.example/projects/100/schedules/11/offline"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"code\":0,\"msg\":\"success\",\"data\":{}}", MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://dolphin.example/projects/100/schedules/11"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess("{\"code\":0,\"msg\":\"success\",\"data\":{}}", MediaType.APPLICATION_JSON));

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

        server.verify();
    }
}
