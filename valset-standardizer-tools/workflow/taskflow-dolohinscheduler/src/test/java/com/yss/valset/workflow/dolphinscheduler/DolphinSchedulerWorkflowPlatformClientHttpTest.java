package com.yss.valset.workflow.dolphinscheduler;

import com.yss.valset.workflow.model.EtlPlatformType;
import com.yss.valset.workflow.model.WorkflowDefinitionDTO;
import com.yss.valset.workflow.model.WorkflowEngineBindingDTO;
import com.yss.valset.workflow.model.WorkflowInstanceDTO;
import com.yss.valset.workflow.model.WorkflowInstanceQueryRequest;
import com.yss.valset.workflow.model.WorkflowLogQueryRequest;
import com.yss.valset.workflow.model.WorkflowPauseRequest;
import com.yss.valset.workflow.model.WorkflowPlatformExecutionResult;
import com.yss.valset.workflow.model.WorkflowRetryRequest;
import com.yss.valset.workflow.model.WorkflowResumeRequest;
import com.yss.valset.workflow.model.WorkflowStageDTO;
import com.yss.valset.workflow.model.WorkflowStopRequest;
import com.yss.valset.workflow.model.WorkflowTriggerMode;
import com.yss.valset.workflow.model.WorkflowTriggerRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.util.MultiValueMap;

class DolphinSchedulerWorkflowPlatformClientHttpTest {

    @Test
    void shouldQueryRemoteWorkflowInstances() {
        DolphinSchedulerRemoteApi remoteApi = mock(DolphinSchedulerRemoteApi.class);
        when(remoteApi.listWorkflowInstances(eq(173038802481536L), anyMap())).thenReturn("""
                {"code":0,"msg":"success","data":{"records":[
                  {"id":"ds-instance-1","workflowInstanceId":"ds-exec-1","processDefinitionCode":"8001","state":"SUCCESS","message":"done","stageCode":"TASK-1","stageName":"准备","submitTime":"2026-05-10 10:00:00","startTime":"2026-05-10 10:01:00","endTime":"2026-05-10 10:02:00","duration":"2分钟","businessKey":"biz-1"}
                ],"totalCount":1}}
                """);

        DolphinSchedulerWorkflowPlatformClient client = newClient(remoteApi);

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

        var page = client.listInstances(listDefinition, com.yss.valset.workflow.model.WorkflowInstanceQueryRequest.builder()
                .workflowName("WADAS ETL")
                .triggerTimeFrom("2026-05-10 00:00:00")
                .triggerTimeTo("2026-05-11 23:59:59")
                .pageIndex(0)
                .pageSize(10)
                .build());
        assertThat(page.getData()).hasSize(1);
        assertThat(page.getData().get(0).getDuration()).isEqualTo("2分钟");

        ArgumentCaptor<Map<String, String>> queryCaptor = ArgumentCaptor.forClass(Map.class);
        verify(remoteApi).listWorkflowInstances(eq(173038802481536L), queryCaptor.capture());
        assertThat(queryCaptor.getValue()).containsEntry("workflowName", "WADAS ETL");
        assertThat(queryCaptor.getValue()).containsEntry("startDate", "2026-05-10 00:00:00");
        assertThat(queryCaptor.getValue()).containsEntry("endDate", "2026-05-11 23:59:59");
    }

    @Test
    void shouldFilterRemoteWorkflowInstancesByNormalizedStatus() {
        DolphinSchedulerRemoteApi remoteApi = mock(DolphinSchedulerRemoteApi.class);
        when(remoteApi.listWorkflowInstances(eq(173038802481536L), anyMap())).thenReturn("""
                {"code":0,"msg":"success","data":{"records":[
                  {"id":"ds-instance-2","workflowInstanceId":"ds-exec-2","processDefinitionCode":"8002","state":"SUBMITTED_SUCCESS","message":"submitted","stageCode":"TASK-1","stageName":"准备","submitTime":"2026-05-10 10:00:00","startTime":"2026-05-10 10:01:00","endTime":"2026-05-10 10:02:00","duration":"2分钟","businessKey":"biz-2"}
                ],"totalCount":1}}
                """);

        DolphinSchedulerWorkflowPlatformClient client = newClient(remoteApi);

        WorkflowDefinitionDTO definition = WorkflowDefinitionDTO.builder()
                .workflowCode("wadas")
                .workflowName("WADAS ETL")
                .workflowVersionNo(1)
                .platformType(EtlPlatformType.DOLPHIN_SCHEDULER)
                .engineBinding(WorkflowEngineBindingDTO.builder()
                        .platformType(EtlPlatformType.DOLPHIN_SCHEDULER)
                        .externalWorkflowId("8002")
                        .externalProjectCode("173038802481536")
                        .build())
                .stages(List.of(
                        WorkflowStageDTO.builder().stageCode("TASK-1").stageName("准备").stageOrder(1).build()))
                .build();

        var page = client.listInstances(definition, WorkflowInstanceQueryRequest.builder()
                .workflowName("WADAS ETL")
                .status("SUBMITTED")
                .pageIndex(0)
                .pageSize(10)
                .build());

        assertThat(page.getData()).hasSize(1);
        assertThat(page.getData().get(0).getStatus().name()).isEqualTo("SUBMITTED");
    }

    @Test
    void shouldCallRemoteWorkflowEndpointsWithExpectedPayloads() {
        DolphinSchedulerRemoteApi remoteApi = mock(DolphinSchedulerRemoteApi.class);
        when(remoteApi.startWorkflowInstance(eq(100L), any(MultiValueMap.class))).thenReturn("""
                {"code":0,"msg":"success","data":9876}
                """, """
                {"code":0,"msg":"success","data":9877}
                """);
        when(remoteApi.executeWorkflow(eq(100L), any(MultiValueMap.class))).thenReturn("""
                {"code":0,"msg":"success","data":true}
                """);
        when(remoteApi.getWorkflowInstance(eq(100L), eq("ds-instance-2"))).thenReturn("""
                {"code":0,"msg":"success","data":{"id":"ds-instance-2","state":"SUCCESS","message":"done","stageCode":"RUN","stageName":"执行"}}
                """);
        when(remoteApi.listWorkflowInstanceTasks(eq(100L), eq("ds-instance-2"))).thenReturn("""
                {"code":0,"msg":"success","data":{"workflowInstanceState":"SUCCESS","taskList":[
                  {"taskCode":"TASK-1","taskName":"准备","state":"SUCCESS","stateDesc":"finished"},
                  {"taskCode":"TASK-2","taskName":"执行","state":"RUNNING","stateDesc":"running"}
                ]}}
                """);

        DolphinSchedulerWorkflowPlatformClient client = newClient(remoteApi);

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

        WorkflowPlatformExecutionResult trigger = client.trigger(definition, instance, WorkflowTriggerRequest.builder()
                .workflowCode("dolphin")
                .workflowVersionNo(2)
                .context(Map.of("bizDate", "2026-05-06"))
                .build());
        assertThat(trigger.getRawStatus()).isEqualTo("RUNNING");
        assertThat(trigger.getExternalInstanceId()).isEqualTo("9876");

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

        WorkflowPlatformExecutionResult resume = client.resume(definition, instance, WorkflowResumeRequest.builder()
                .reason("manual resume")
                .build());
        assertThat(resume.getRawStatus()).isEqualTo("RUNNING");
        assertThat(resume.getMessage()).isEqualTo("manual resume");

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
    }

    @Test
    void shouldUseDefaultTenantCodeOnlyOnceWhenTriggeringWorkflow() {
        DolphinSchedulerRemoteApi remoteApi = mock(DolphinSchedulerRemoteApi.class);
        when(remoteApi.startWorkflowInstance(eq(100L), any(MultiValueMap.class))).thenReturn("""
                {"code":0,"msg":"success","data":9876}
                """);

        DolphinSchedulerWorkflowPlatformClient client = newClient(remoteApi);

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
                        WorkflowStageDTO.builder().stageCode("TASK-1").stageName("准备").stageOrder(1).build()))
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

        client.trigger(definition, instance, WorkflowTriggerRequest.builder()
                .workflowCode("dolphin")
                .workflowVersionNo(2)
                .build());

        ArgumentCaptor<MultiValueMap> paramsCaptor = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(remoteApi).startWorkflowInstance(eq(100L), paramsCaptor.capture());
        MultiValueMap<String, String> params = paramsCaptor.getValue();
        assertThat(params.get("workflowDefinitionCode")).containsExactly("200");
        assertThat(params.get("version")).containsExactly("2");
        assertThat(params.get("execType")).containsExactly("START_PROCESS");
        assertThat(params.get("startNodeList")).containsExactly("");
        assertThat(params.get("taskDependType")).containsExactly("TASK_POST");
        assertThat(params.get("complementDependentMode")).containsExactly("OFF_MODE");
        assertThat(params.get("runMode")).containsExactly("RUN_MODE_SERIAL");
        assertThat(params.get("executionOrder")).containsExactly("DESC_ORDER");
        assertThat(params.get("dryRun")).containsExactly("0");
        assertThat(params.get("allLevelDependent")).containsExactly("false");
        assertThat(params.get("tenantCode")).containsExactly("default");
        assertThat(params.get("tenantCode")).hasSize(1);
        assertThat(params.get("workerGroup")).containsExactly("default");
        assertThat(params.get("workerGroup")).hasSize(1);
    }

    @Test
    void shouldQueryRemoteWorkflowTasksAndLogs() {
        DolphinSchedulerRemoteApi remoteApi = mock(DolphinSchedulerRemoteApi.class);
        when(remoteApi.listWorkflowInstanceTasks(eq(100L), eq("ds-instance-2"))).thenReturn("""
                {"code":0,"msg":"success","data":{"workflowInstanceState":"SUCCESS","taskList":[
                  {"id":21,"name":"阶段33","taskType":"SUB_WORKFLOW","workflowInstanceId":10,"workflowInstanceName":"asd-20260510194358533","projectCode":173037995332480,"taskCode":239666485040597655,"taskDefinitionVersion":1,"state":"SUCCESS","firstSubmitTime":"2026-05-10 19:44:11","submitTime":"2026-05-10 19:44:11","startTime":"2026-05-10 19:44:11","endTime":"2026-05-10 19:44:17","host":"192.168.0.106:5678","logPath":"/Users/zhudaoming/dolphinscheduler/logs/20260510/173037997165440/1/10/21.log","retryTimes":0,"alertFlag":"NO","appLink":"subWorkflowInstanceId=13","flag":"YES","workerGroup":"default","environmentCode":-1,"executorId":2,"executorName":"system","delayTime":0,"taskParams":"simple","dryRun":0,"taskGroupId":0,"cpuQuota":-1,"memoryMax":-1,"taskExecuteType":"BATCH"}
                ]}}
                """);
        when(remoteApi.listTaskInstances(eq(100L), anyMap())).thenReturn("""
                {"code":0,"msg":"success","data":{"totalCount":2,"totalList":[
                  {"id":5,"name":"阶段01","taskType":"SUB_WORKFLOW","workflowInstanceId":10,"workflowInstanceName":"asd-20260510194358533","projectCode":173037995332480,"taskCode":239666485040597654,"taskDefinitionVersion":1,"state":"RUNNING","firstSubmitTime":"2026-05-10 19:43:11","submitTime":"2026-05-10 19:43:11","startTime":"2026-05-10 19:43:11","endTime":"2026-05-10 19:43:17","host":"192.168.0.105:5678","workerGroup":"default","executorName":"system","taskExecuteType":"BATCH"},
                  {"id":21,"name":"阶段33","taskType":"SUB_WORKFLOW","workflowInstanceId":10,"workflowInstanceName":"asd-20260510194358533","projectCode":173037995332480,"taskCode":239666485040597655,"taskDefinitionVersion":1,"state":"SUCCESS","firstSubmitTime":"2026-05-10 19:44:11","submitTime":"2026-05-10 19:44:11","startTime":"2026-05-10 19:44:11","endTime":"2026-05-10 19:44:17","host":"192.168.0.106:5678","workerGroup":"default","executorName":"system","taskExecuteType":"BATCH"}
                ]}}
                """);
        when(remoteApi.forceTaskSuccess(eq(100L), eq(21))).thenReturn("""
                {"code":0,"msg":"success","data":true}
                """);
        when(remoteApi.getTaskLogByProject(eq(100L), anyMap())).thenReturn("""
                {"code":0,"msg":"success","data":"task log line 1\\nline 2"}
                """);

        DolphinSchedulerWorkflowPlatformClient client = newClient(remoteApi);

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
                .build();

        assertThat(client.queryTasks(definition, instance).getTaskList()).hasSize(1);
        assertThat(client.queryTaskLog(definition, instance, 21L, 12, 34)).contains("task log line 1");
        assertThat(client.listTaskInstances(definition, com.yss.valset.workflow.model.WorkflowTaskInstanceQueryRequest.builder()
                .workflowCode("dolphin")
                .workflowVersionNo(2)
                .taskName("阶段33")
                .status("SUCCESS")
                .pageIndex(0)
                .pageSize(10)
                .build()).getTaskList())
                .extracting(com.yss.valset.workflow.model.WorkflowTaskInstanceDTO::getId)
                .containsExactly(5L, 21L);
        client.forceTaskSuccess(definition, 21L);

        ArgumentCaptor<Map<String, String>> logCaptor = ArgumentCaptor.forClass(Map.class);
        verify(remoteApi).getTaskLogByProject(eq(100L), logCaptor.capture());
        assertThat(logCaptor.getValue()).containsEntry("taskInstanceId", "21");
        assertThat(logCaptor.getValue()).containsEntry("skipLineNum", "12");
        assertThat(logCaptor.getValue()).containsEntry("limit", "34");
    }

    @Test
    void shouldQueryTaskLogWithoutDuplicatingPrefixWhenBaseUrlAlreadyContainsIt() {
        DolphinSchedulerRemoteApi remoteApi = mock(DolphinSchedulerRemoteApi.class);
        when(remoteApi.getTaskLogByProject(eq(100L), anyMap())).thenReturn("""
                {"code":0,"msg":"success","data":"task log line 1\\nline 2"}
                """);

        DolphinSchedulerWorkflowPlatformClient client = newClient(remoteApi);

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

        assertThat(client.queryTaskLog(definition, instance, 21L, 5, 6)).contains("task log line 1");
    }

    private DolphinSchedulerWorkflowPlatformClient newClient(DolphinSchedulerRemoteApi remoteApi) {
        DolphinSchedulerResponseSupport responseSupport = new DolphinSchedulerResponseSupport();
        return new DolphinSchedulerWorkflowPlatformClient(remoteApi, responseSupport);
    }
}
