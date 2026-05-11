package com.yss.valset.controller;

import com.yss.valset.workflow.dolphinscheduler.DolphinSchedulerResponseSupport;
import com.yss.valset.workflow.dolphinscheduler.DolphinSchedulerRemoteApi;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DolphinScheduler 代理接口测试。
 */
class DolphinSchedulerProxyControllerTest {

    private final DolphinSchedulerResponseSupport responseSupport = new DolphinSchedulerResponseSupport();
    private final DolphinSchedulerRemoteApi remoteApi = mock(DolphinSchedulerRemoteApi.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new DolphinSchedulerProxyController(responseSupport, remoteApi))
            .build();

    @Test
    void shouldProxyTaskStateCountToDolphinScheduler() throws Exception {
        when(remoteApi.countTaskState(any())).thenReturn("""
                {"code":0,"msg":"success","data":{"totalCount":3,"taskInstanceStatusCounts":[{"state":"SUCCESS","count":2}]}}
                """);

        mockMvc.perform(get("/dolphinscheduler/projects/analysis/task-state-count")
                        .param("startDate", "2026-05-11 00:00:00")
                        .param("endDate", "2026-05-11 21:35:47")
                        .param("projectCode", "173116188828288")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"code":0,"msg":"success","data":{"totalCount":3,"taskInstanceStatusCounts":[{"state":"SUCCESS","count":2}]}}
                        """));

        verify(remoteApi).countTaskState(any());
    }

    @Test
    void shouldProxyWorkflowStateCountToDolphinScheduler() throws Exception {
        when(remoteApi.countWorkflowState(any())).thenReturn("""
                {"code":0,"msg":"success","data":{"totalCount":5,"workflowInstanceStatusCounts":[{"state":"SUCCESS","count":4}]}}
                """);

        mockMvc.perform(get("/dolphinscheduler/projects/analysis/workflow-state-count")
                        .param("startDate", "2026-05-11 00:00:00")
                        .param("endDate", "2026-05-11 21:35:47")
                        .param("projectCode", "173116188828288")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"code":0,"msg":"success","data":{"totalCount":5,"workflowInstanceStatusCounts":[{"state":"SUCCESS","count":4}]}}
                        """));

        verify(remoteApi).countWorkflowState(any());
    }
}
