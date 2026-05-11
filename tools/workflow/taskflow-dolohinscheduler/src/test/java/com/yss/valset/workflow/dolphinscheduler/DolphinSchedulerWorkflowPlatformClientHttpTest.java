package com.yss.valset.workflow.dolphinscheduler;

import com.yss.valset.workflow.model.EtlPlatformType;
import com.yss.valset.workflow.model.WorkflowDefinitionDTO;
import com.yss.valset.workflow.model.WorkflowEngineBindingDTO;
import com.yss.valset.workflow.model.WorkflowInstanceDTO;
import com.yss.valset.workflow.model.WorkflowLogQueryRequest;
import com.yss.valset.workflow.model.WorkflowPauseRequest;
import com.yss.valset.workflow.model.WorkflowPlatformExecutionResult;
import com.yss.valset.workflow.model.WorkflowRetryRequest;
import com.yss.valset.workflow.model.WorkflowStageDTO;
import com.yss.valset.workflow.model.WorkflowStopRequest;
import com.yss.valset.workflow.model.WorkflowTriggerRequest;
import com.yss.valset.workflow.model.WorkflowTriggerMode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.test.web.client.MockRestServiceServer;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DolphinSchedulerWorkflowPlatformClientHttpTest {

    @Test
    void shouldQueryRemoteWorkflowInstances() {
        DolphinSchedulerWorkflowPlatformClient client = new DolphinSchedulerWorkflowPlatformClient();
        DolphinSchedulerApiSupport apiSupport = (DolphinSchedulerApiSupport) ReflectionTestUtils.getField(client, "apiSupport");
        assertThat(apiSupport).isNotNull();

        RestTemplate restTemplate = new RestTemplate();
        apiSupport.setBaseUrl("http://dolphin.example");
        apiSupport.setRestTemplate(restTemplate);

        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(false).build();

        server.expect(requestTo("http://dolphin.example/projects/173038802481536/workflow/instances?projectName=wadas-project&pageNo=1&pageSize=10"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"code":0,"msg":"success","data":{"records":[
                          {"id":"ds-instance-1","workflowInstanceId":"ds-exec-1","processDefinitionCode":"8001","state":"SUCCESS","message":"done","stageCode":"TASK-1","stageName":"准备","submitTime":"2026-05-10 10:00:00","startTime":"2026-05-10 10:01:00","endTime":"2026-05-10 10:02:00","businessKey":"biz-1"}
                        ],"totalCount":1}}
                        """, MediaType.APPLICATION_JSON));

        WorkflowDefinitionDTO listDefinition = WorkflowDefinitionDTO.builder()
                .workflowCode("wadas")
                .workflowName("WADAS ETL")
                .workflowVersionNo(1)
                .platformType(EtlPlatformType.DOLPHIN_SCHEDULER)
                .engineBinding(WorkflowEngineBindingDTO.builder()
                        .platformType(EtlPlatformType.DOLPHIN_SCHEDULER)
                        .externalWorkflowId("8001")
                        .externalProjectCode("173038802481536")
                        .build())
                .stages(List.of(
                        WorkflowStageDTO.builder().stageCode("TASK-1").stageName("准备").stageOrder(1).build()))
                .build();

        assertThat(client.listInstances(listDefinition, com.yss.valset.workflow.model.WorkflowInstanceQueryRequest.builder()
                .pageIndex(0)
                .pageSize(10)
                .build()).getData()).hasSize(1);

        server.verify();
    }

    @Test
    void shouldCallRemoteWorkflowEndpointsWithExpectedPayloads() {
        DolphinSchedulerWorkflowPlatformClient client = new DolphinSchedulerWorkflowPlatformClient();
        DolphinSchedulerApiSupport apiSupport = (DolphinSchedulerApiSupport) ReflectionTestUtils.getField(client, "apiSupport");
        assertThat(apiSupport).isNotNull();

        RestTemplate restTemplate = new RestTemplate();
        apiSupport.setBaseUrl("http://dolphin.example");
        apiSupport.setRestTemplate(restTemplate);

        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(false).build();

        server.expect(requestTo("http://dolphin.example/projects/100/executors/start-workflow-instance"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(containsString("workflowDefinitionCode=200")))
                .andExpect(content().string(containsString("execType=START_PROCESS")))
                .andExpect(content().string(containsString("workerGroup=default")))
                .andExpect(content().string(containsString("startParams=")))
                .andRespond(withSuccess("""
                        {"code":0,"msg":"success","data":9876}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://dolphin.example/projects/100/executors/start-workflow-instance"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(containsString("execType=START_FAILURE_TASK_PROCESS")))
                .andRespond(withSuccess("""
                        {"code":0,"msg":"success","data":9877}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://dolphin.example/projects/100/executors/execute"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(containsString("workflowInstanceId=ds-instance-2")))
                .andExpect(content().string(containsString("executeType=STOP")))
                .andRespond(withSuccess("""
                        {"code":0,"msg":"success","data":true}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://dolphin.example/projects/100/executors/execute"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(containsString("workflowInstanceId=ds-instance-2")))
                .andExpect(content().string(containsString("executeType=PAUSE")))
                .andRespond(withSuccess("""
                        {"code":0,"msg":"success","data":true}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://dolphin.example/projects/100/executors/execute"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(containsString("workflowInstanceId=ds-instance-2")))
                .andExpect(content().string(containsString("executeType=REPEAT_RUNNING")))
                .andRespond(withSuccess("""
                        {"code":0,"msg":"success","data":true}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://dolphin.example/projects/100/workflow-instances/ds-instance-2"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"code":0,"msg":"success","data":{"id":"ds-instance-2","state":"SUCCESS","message":"done","stageCode":"RUN","stageName":"执行"}}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://dolphin.example/projects/100/workflow-instances/ds-instance-2/tasks"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"code":0,"msg":"success","data":{"workflowInstanceState":"SUCCESS","taskList":[
                          {"taskCode":"TASK-1","taskName":"准备","state":"SUCCESS","stateDesc":"finished"},
                          {"taskCode":"TASK-2","taskName":"执行","state":"RUNNING","stateDesc":"running"}
                        ]}}
                        """, MediaType.APPLICATION_JSON));

        WorkflowDefinitionDTO definition = WorkflowDefinitionDTO.builder()
                .workflowCode("dolphin")
                .workflowName("Dolphin ETL")
                .workflowVersionNo(2)
                .platformType(EtlPlatformType.DOLPHIN_SCHEDULER)
                .engineBinding(WorkflowEngineBindingDTO.builder()
                        .platformType(EtlPlatformType.DOLPHIN_SCHEDULER)
                        .externalWorkflowId("200")
                        .externalProjectCode("100")
                        .externalNamespace("worker-a")
                        .build())
                .stages(List.of(
                        WorkflowStageDTO.builder().stageCode("TASK-1").stageName("准备").stageOrder(1).build(),
                        WorkflowStageDTO.builder().stageCode("TASK-2").stageName("执行").stageOrder(2).build()))
                .build();

        WorkflowInstanceDTO instance = WorkflowInstanceDTO.builder()
                .instanceId("instance-2")
                .externalInstanceId("ds-instance-2")
                .externalWorkflowId("200")
                .workflowCode("dolphin")
                .workflowVersionNo(2)
                .platformType(EtlPlatformType.DOLPHIN_SCHEDULER)
                .rawStatus("RUNNING")
                .context(Map.of("bizDate", "2026-05-06"))
                .build();

        WorkflowPlatformExecutionResult trigger = client.trigger(definition, instance, WorkflowTriggerRequest.builder()
                .workflowCode("dolphin")
                .workflowVersionNo(2)
                .context(Map.of("bizDate", "2026-05-06"))
                .build());
        assertThat(trigger.getRawStatus()).isEqualTo("RUNNING");
        assertThat(trigger.getExternalInstanceId()).isEqualTo("9876");
        assertThat(trigger.getPayload()).containsEntry("projectCode", "100");
        assertThat(trigger.getPayload()).containsEntry("workflowDefinitionCode", "200");

        WorkflowPlatformExecutionResult repeatFailed = client.trigger(definition, instance, WorkflowTriggerRequest.builder()
                .workflowCode("dolphin")
                .workflowVersionNo(2)
                .triggerMode(WorkflowTriggerMode.START_FAILURE_TASK_PROCESS)
                .build());
        assertThat(repeatFailed.getRawStatus()).isEqualTo("RUNNING");
        assertThat(repeatFailed.getMessage()).isEqualTo("已提交失败任务重跑");

        WorkflowPlatformExecutionResult stop = client.stop(definition, instance, WorkflowStopRequest.builder()
                .reason("manual stop")
                .build());
        assertThat(stop.getRawStatus()).isEqualTo("STOPPED");
        assertThat(stop.getMessage()).isEqualTo("manual stop");

        WorkflowPlatformExecutionResult pause = client.pause(definition, instance, WorkflowPauseRequest.builder()
                .reason("manual pause")
                .build());
        assertThat(pause.getRawStatus()).isEqualTo("STOPPED");
        assertThat(pause.getMessage()).isEqualTo("manual pause");

        WorkflowPlatformExecutionResult retry = client.retry(definition, instance, WorkflowRetryRequest.builder()
                .stageCode("TASK-2")
                .build());
        assertThat(retry.getRawStatus()).isEqualTo("RETRYING");
        assertThat(retry.getMessage()).isEqualTo("任务已重新提交");

        WorkflowPlatformExecutionResult query = client.query(definition, instance);
        assertThat(query.getRawStatus()).isEqualTo("SUCCESS");
        assertThat(query.getStageLogs()).hasSize(1);
        assertThat(query.getStageLogs().get(0).getStageCode()).isEqualTo("RUN");
        assertThat(query.getStageLogs().get(0).getStatus().name()).isEqualTo("SUCCEEDED");
        assertThat(query.getStageLogs().get(0).getMessage()).isEqualTo("done");

        List<WorkflowPlatformExecutionResult> logs = client.queryLogs(definition, instance, WorkflowLogQueryRequest.builder()
                .stageCode("TASK-1")
                .build());
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getRawStatus()).isEqualTo("SUCCESS");
        assertThat(logs.get(0).getMessage()).isEqualTo("finished");
        assertThat(logs.get(0).getStageLogs()).hasSize(1);
        assertThat(logs.get(0).getStageLogs().get(0).getStageCode()).isEqualTo("TASK-1");

        server.verify();
    }

    @Test
    void shouldQueryTaskLogFromRemotePlatform() {
        DolphinSchedulerWorkflowPlatformClient client = new DolphinSchedulerWorkflowPlatformClient();
        DolphinSchedulerApiSupport apiSupport = (DolphinSchedulerApiSupport) ReflectionTestUtils.getField(client, "apiSupport");
        assertThat(apiSupport).isNotNull();

        RestTemplate restTemplate = new RestTemplate();
        apiSupport.setBaseUrl("http://dolphin.example");
        apiSupport.setRestTemplate(restTemplate);

        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(false).build();

        server.expect(requestTo("http://dolphin.example/projects/100/workflow-instances/ds-instance-2/tasks"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"code":0,"msg":"success","data":{"workflowInstanceState":"SUCCESS","taskList":[
                          {"id":21,"name":"阶段33","taskType":"SUB_WORKFLOW","workflowInstanceId":10,"workflowInstanceName":"asd-20260510194358533","projectCode":173037995332480,"taskCode":239666485040597655,"taskDefinitionVersion":1,"state":"SUCCESS","firstSubmitTime":"2026-05-10 19:44:11","submitTime":"2026-05-10 19:44:11","startTime":"2026-05-10 19:44:11","endTime":"2026-05-10 19:44:17","host":"192.168.0.106:5678","logPath":"/Users/zhudaoming/dolphinscheduler/logs/20260510/173037997165440/1/10/21.log","retryTimes":0,"alertFlag":"NO","appLink":"subWorkflowInstanceId=13","flag":"YES","workerGroup":"default","environmentCode":-1,"executorId":2,"executorName":"system","delayTime":0,"taskParams":"simple","dryRun":0,"taskGroupId":0,"cpuQuota":-1,"memoryMax":-1,"taskExecuteType":"BATCH"}
                        ]}}
                        """, MediaType.APPLICATION_JSON));

        server.expect(requestTo("http://dolphin.example/dolphinscheduler/log/detail?taskInstanceId=21&limit=1000&skipLineNum=0"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"code":0,"msg":"success","data":{"lineNum":29,"message":"task log line 1\\nline 2"}}
                        """, MediaType.APPLICATION_JSON));

        WorkflowDefinitionDTO definition = WorkflowDefinitionDTO.builder()
                .workflowCode("dolphin")
                .workflowName("Dolphin ETL")
                .workflowVersionNo(2)
                .platformType(EtlPlatformType.DOLPHIN_SCHEDULER)
                .engineBinding(WorkflowEngineBindingDTO.builder()
                        .platformType(EtlPlatformType.DOLPHIN_SCHEDULER)
                        .externalWorkflowId("200")
                        .externalProjectCode("100")
                        .build())
                .stages(List.of(
                        WorkflowStageDTO.builder().stageCode("TASK-1").stageName("准备").stageOrder(1).build(),
                        WorkflowStageDTO.builder().stageCode("TASK-2").stageName("执行").stageOrder(2).build()))
                .build();

        WorkflowInstanceDTO instance = WorkflowInstanceDTO.builder()
                .instanceId("instance-2")
                .externalInstanceId("ds-instance-2")
                .externalWorkflowId("200")
                .workflowCode("dolphin")
                .workflowVersionNo(2)
                .platformType(EtlPlatformType.DOLPHIN_SCHEDULER)
                .rawStatus("RUNNING")
                .context(Map.of("bizDate", "2026-05-06"))
                .build();

        assertThat(client.queryTasks(definition, instance).getTaskList()).hasSize(1);
        assertThat(client.queryTaskLog(definition, instance, 21L)).contains("task log line 1");

        server.verify();
    }

    @Test
    void shouldQueryTaskLogWithoutDuplicatingPrefixWhenBaseUrlAlreadyContainsIt() {
        DolphinSchedulerWorkflowPlatformClient client = new DolphinSchedulerWorkflowPlatformClient();
        DolphinSchedulerApiSupport apiSupport = (DolphinSchedulerApiSupport) ReflectionTestUtils.getField(client, "apiSupport");
        assertThat(apiSupport).isNotNull();

        RestTemplate restTemplate = new RestTemplate();
        apiSupport.setBaseUrl("http://dolphin.example/dolphinscheduler");
        apiSupport.setRestTemplate(restTemplate);

        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(false).build();

        server.expect(requestTo("http://dolphin.example/dolphinscheduler/log/detail?taskInstanceId=21&limit=1000&skipLineNum=0"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"code":0,"msg":"success","data":{"lineNum":29,"message":"task log line 1\\nline 2"}}
                        """, MediaType.APPLICATION_JSON));

        WorkflowDefinitionDTO definition = WorkflowDefinitionDTO.builder()
                .workflowCode("dolphin")
                .workflowName("Dolphin ETL")
                .workflowVersionNo(2)
                .platformType(EtlPlatformType.DOLPHIN_SCHEDULER)
                .engineBinding(WorkflowEngineBindingDTO.builder()
                        .platformType(EtlPlatformType.DOLPHIN_SCHEDULER)
                        .externalWorkflowId("200")
                        .externalProjectCode("100")
                        .build())
                .build();

        WorkflowInstanceDTO instance = WorkflowInstanceDTO.builder()
                .instanceId("instance-2")
                .externalInstanceId("ds-instance-2")
                .externalWorkflowId("200")
                .workflowCode("dolphin")
                .workflowVersionNo(2)
                .platformType(EtlPlatformType.DOLPHIN_SCHEDULER)
                .rawStatus("RUNNING")
                .build();

        assertThat(client.queryTaskLog(definition, instance, 21L)).contains("task log line 1");

        server.verify();
    }
}
