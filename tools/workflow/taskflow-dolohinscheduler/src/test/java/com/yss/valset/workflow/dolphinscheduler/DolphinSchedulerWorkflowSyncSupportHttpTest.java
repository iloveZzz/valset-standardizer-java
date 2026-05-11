package com.yss.valset.workflow.dolphinscheduler;

import com.yss.valset.workflow.model.EtlPlatformType;
import com.yss.valset.workflow.model.WorkflowDefinitionDTO;
import com.yss.valset.workflow.model.WorkflowEngineBindingDTO;
import com.yss.valset.workflow.model.WorkflowStageDTO;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DolphinSchedulerWorkflowSyncSupportHttpTest {

    @Test
    void shouldRecoverProjectCodeThenOnlineAndOfflineWorkflow() {
        DolphinSchedulerApiSupport apiSupport = new DolphinSchedulerApiSupport();
        RestTemplate restTemplate = new RestTemplate();
        apiSupport.setBaseUrl("http://dolphin.example");
        apiSupport.setRestTemplate(restTemplate);

        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

        server.expect(requestTo("http://dolphin.example/projects/123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"code":10018,"msg":"Project does not exist."}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://dolphin.example/projects?pageNo=1&pageSize=20&searchVal=legacy_etl-project"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"code":0,"msg":"success","data":{"totalList":[{"code":555,"name":"legacy_etl-project"}],"total":1}}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://dolphin.example/projects/555/workflow-definition"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andRespond(withSuccess("""
                        {"code":0,"msg":"success","data":{"code":777,"version":1}}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://dolphin.example/projects/555/workflow-definition"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andRespond(withSuccess("""
                        {"code":0,"msg":"success","data":{"code":888,"version":1}}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://dolphin.example/projects/555/workflow-definition/777/release"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=Legacy_ETL-S1")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("releaseState=ONLINE")))
                .andRespond(withSuccess("""
                        {"code":0,"msg":"success","data":{"code":888,"version":1}}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://dolphin.example/projects/555/workflow-definition/888/release"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=Legacy")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("releaseState=ONLINE")))
                .andRespond(withSuccess("""
                        {"code":0,"msg":"success","data":true}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://dolphin.example/projects/555/workflow-definition/888/offline"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andRespond(withSuccess("""
                        {"code":0,"msg":"success","data":true}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://dolphin.example/projects/555/workflow-definition/777/offline"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andRespond(withSuccess("""
                        {"code":0,"msg":"success","data":true}
                        """, MediaType.APPLICATION_JSON));

        DolphinSchedulerWorkflowSyncSupport support = new DolphinSchedulerWorkflowSyncSupport(apiSupport);
        WorkflowDefinitionDTO synced = support.syncDefinition(WorkflowDefinitionDTO.builder()
                .workflowCode("legacy_etl")
                .workflowName("Legacy ETL")
                .workflowVersionNo(1)
                .platformType(EtlPlatformType.DOLPHIN_SCHEDULER)
                .description("legacy flow")
                .engineBinding(WorkflowEngineBindingDTO.builder()
                        .platformType(EtlPlatformType.DOLPHIN_SCHEDULER)
                        .externalProjectCode("123")
                        .externalWorkflowId("999")
                        .externalNamespace("default")
                        .attributes(Map.of(
                                "dolphinschedulerSync", Map.of(
                                        "projectCode", "123",
                                        "namespace", "default")))
                        .build())
                .stages(List.of(WorkflowStageDTO.builder()
                        .stageCode("S1")
                        .stageName("阶段1")
                        .stageOrder(1)
                        .build()))
                .build());

        assertThat(synced.getEngineBinding().getExternalProjectCode()).isEqualTo("555");
        assertThat(synced.getEngineBinding().getExternalWorkflowId()).isEqualTo("888");
        assertThat(synced.getEngineBinding().getExternalNamespace()).isEqualTo("default");
        assertThat(synced.getEngineBinding().getRemoteWorkflowVersionNo()).isEqualTo(1);

        support.onlineDefinition(synced);
        support.offlineDefinition(synced);

        server.verify();
    }

    @Test
    void shouldResyncWorkflowDefinitionWhenRemoteStateIsMissing() {
        DolphinSchedulerApiSupport apiSupport = new DolphinSchedulerApiSupport();
        RestTemplate restTemplate = new RestTemplate();
        apiSupport.setBaseUrl("http://dolphin.example");
        apiSupport.setRestTemplate(restTemplate);

        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

        server.expect(requestTo("http://dolphin.example/projects/555"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"code":0,"msg":"success","data":{"code":555,"name":"legacy_etl-project"}}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://dolphin.example/projects/555/workflow-definition/888"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"code":10018,"msg":"Workflow definition not exist."}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://dolphin.example/projects/555/workflow-definition"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andRespond(withSuccess("""
                        {"code":0,"msg":"success","data":{"code":777,"version":1}}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://dolphin.example/projects/555/workflow-definition/999"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"code":10018,"msg":"Workflow definition not exist."}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://dolphin.example/projects/555/workflow-definition"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andRespond(withSuccess("""
                        {"code":0,"msg":"success","data":{"code":888,"version":1}}
                        """, MediaType.APPLICATION_JSON));

        DolphinSchedulerWorkflowSyncSupport support = new DolphinSchedulerWorkflowSyncSupport(apiSupport);
        WorkflowDefinitionDTO synced = support.syncDefinition(WorkflowDefinitionDTO.builder()
                .workflowCode("legacy_etl")
                .workflowName("Legacy ETL")
                .workflowVersionNo(1)
                .platformType(EtlPlatformType.DOLPHIN_SCHEDULER)
                .description("legacy flow")
                .engineBinding(WorkflowEngineBindingDTO.builder()
                        .platformType(EtlPlatformType.DOLPHIN_SCHEDULER)
                        .externalProjectCode("555")
                        .externalWorkflowId("999")
                        .externalNamespace("default")
                        .attributes(Map.of(
                                "dolphinschedulerSync", Map.of(
                                        "projectCode", "555",
                                        "namespace", "default",
                                        "parent", Map.of(
                                                "workflowDefinitionCode", 999,
                                                "workflowDefinitionVersion", 1,
                                                "fingerprint", "old-fingerprint",
                                                "taskCodes", Map.of("S1", 1)),
                                        "stages", Map.of(
                                                "S1", Map.of(
                                                        "workflowDefinitionCode", 888,
                                                        "workflowDefinitionVersion", 1,
                                                        "fingerprint", "old-fingerprint",
                                                        "taskCodes", Map.of(
                                                                "start", 11,
                                                                "end", 12))))))
                        .build())
                .stages(List.of(WorkflowStageDTO.builder()
                        .stageCode("S1")
                        .stageName("阶段1")
                        .stageOrder(1)
                        .build()))
                .build());

        assertThat(synced.getEngineBinding().getExternalProjectCode()).isEqualTo("555");
        assertThat(synced.getEngineBinding().getExternalWorkflowId()).isEqualTo("888");
        assertThat(synced.getEngineBinding().getRemoteWorkflowVersionNo()).isEqualTo(1);

        server.verify();
    }

    @Test
    void shouldDeleteChildWorkflowsBeforeParentWorkflow() {
        DolphinSchedulerApiSupport apiSupport = new DolphinSchedulerApiSupport();
        RestTemplate restTemplate = new RestTemplate();
        apiSupport.setBaseUrl("http://dolphin.example");
        apiSupport.setRestTemplate(restTemplate);

        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

        server.expect(requestTo("http://dolphin.example/projects/555"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"code":0,"msg":"success","data":{"code":555,"name":"legacy_etl-project"}}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://dolphin.example/projects/555/workflow-definition/888"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess("""
                        {"code":0,"msg":"success","data":true}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://dolphin.example/projects/555/workflow-definition/999"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess("""
                        {"code":0,"msg":"success","data":true}
                        """, MediaType.APPLICATION_JSON));

        DolphinSchedulerWorkflowSyncSupport support = new DolphinSchedulerWorkflowSyncSupport(apiSupport);
        support.deleteDefinition(WorkflowDefinitionDTO.builder()
                .workflowCode("legacy_etl")
                .workflowName("Legacy ETL")
                .workflowVersionNo(1)
                .platformType(EtlPlatformType.DOLPHIN_SCHEDULER)
                .engineBinding(WorkflowEngineBindingDTO.builder()
                        .platformType(EtlPlatformType.DOLPHIN_SCHEDULER)
                        .externalProjectCode("555")
                        .externalWorkflowId("999")
                        .attributes(Map.of(
                                "dolphinschedulerSync", Map.of(
                                        "projectCode", "555",
                                        "parent", Map.of(
                                                "workflowDefinitionCode", "999"),
                                        "stages", Map.of(
                                                "S1", Map.of(
                                                        "workflowDefinitionCode", "888")))))
                        .build())
                .stages(List.of(WorkflowStageDTO.builder()
                        .stageCode("S1")
                        .stageName("阶段1")
                        .stageOrder(1)
                        .build()))
                .build());

        server.verify();
    }
}
