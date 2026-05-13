package com.yss.valset.workflow.dolphinscheduler;

import com.yss.valset.workflow.model.EtlPlatformType;
import com.yss.valset.workflow.model.WorkflowDefinitionDTO;
import com.yss.valset.workflow.model.WorkflowEngineBindingDTO;
import com.yss.valset.workflow.model.WorkflowStageDTO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.verify;
import org.springframework.util.MultiValueMap;

class DolphinSchedulerWorkflowSyncSupportHttpTest {

    @Test
    void shouldRecoverProjectCodeThenOnlineAndOfflineWorkflow() {
        DolphinSchedulerRemoteApi remoteApi = mock(DolphinSchedulerRemoteApi.class);
        when(remoteApi.getProject(123L)).thenReturn("""
                {"code":10018,"msg":"Project does not exist."}
                """);
        when(remoteApi.listProjects(anyMap())).thenReturn("""
                {"code":0,"msg":"success","data":{"totalList":[{"code":555,"name":"legacy_etl-project"}],"total":1}}
                """);
        when(remoteApi.getWorkflowDefinition(eq(555L), eq(777L))).thenReturn("""
                {"code":10018,"msg":"Workflow definition not exist."}
                """);
        when(remoteApi.getWorkflowDefinition(eq(555L), eq(888L))).thenReturn("""
                {"code":10018,"msg":"Workflow definition not exist."}
                """);
        when(remoteApi.createWorkflowDefinition(eq(555L), any(MultiValueMap.class))).thenReturn(
                """
                        {"code":0,"msg":"success","data":{"code":777,"version":1}}
                        """,
                """
                        {"code":0,"msg":"success","data":{"code":888,"version":1}}
                        """);
        when(remoteApi.releaseWorkflowDefinition(eq(555L), eq(888L), any(MultiValueMap.class))).thenReturn("""
                {"code":0,"msg":"success","data":true}
                """);
        when(remoteApi.releaseWorkflowDefinition(eq(555L), eq(777L), any(MultiValueMap.class))).thenReturn("""
                {"code":0,"msg":"success","data":true}
                """);

        DolphinSchedulerWorkflowSyncSupport support = new DolphinSchedulerWorkflowSyncSupport(remoteApi, new DolphinSchedulerResponseSupport());
        WorkflowDefinitionDTO synced = support.syncDefinition(baseDefinition().toBuilder()
                .engineBinding(baseBinding("123", "999").build())
                .build());

        assertThat(synced.getEngineBinding().getExternalProjectCode()).isEqualTo("555");
        assertThat(synced.getEngineBinding().getExternalWorkflowId()).isEqualTo("888");
        assertThat(synced.getEngineBinding().getExternalNamespace()).isEqualTo("default");
        assertThat(synced.getEngineBinding().getRemoteWorkflowVersionNo()).isEqualTo(1);

        support.onlineDefinition(synced);
        support.offlineDefinition(synced);

        ArgumentCaptor<MultiValueMap<String, String>> releaseCaptor = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(remoteApi, times(2)).releaseWorkflowDefinition(eq(555L), eq(888L), releaseCaptor.capture());
        assertThat(releaseCaptor.getAllValues()).hasSize(2);
        assertThat(releaseCaptor.getAllValues().get(0)).containsEntry("releaseState", List.of("ONLINE"));
        assertThat(releaseCaptor.getAllValues().get(0)).containsEntry("name", List.of("Legacy ETL"));
        assertThat(releaseCaptor.getAllValues().get(1)).containsEntry("releaseState", List.of("OFFLINE"));
        assertThat(releaseCaptor.getAllValues().get(1)).containsEntry("name", List.of("Legacy ETL"));
    }

    @Test
    void shouldResyncWorkflowDefinitionWhenRemoteStateIsMissing() {
        DolphinSchedulerRemoteApi remoteApi = mock(DolphinSchedulerRemoteApi.class);
        when(remoteApi.getProject(555L)).thenReturn("""
                {"code":0,"msg":"success","data":{"code":555,"name":"legacy_etl-project"}}
                """);
        when(remoteApi.getWorkflowDefinition(eq(555L), eq(888L))).thenReturn("""
                {"code":10018,"msg":"Workflow definition not exist."}
                """);
        when(remoteApi.getWorkflowDefinition(eq(555L), eq(999L))).thenReturn("""
                {"code":10018,"msg":"Workflow definition not exist."}
                """);
        when(remoteApi.createWorkflowDefinition(eq(555L), any(MultiValueMap.class))).thenReturn(
                """
                        {"code":0,"msg":"success","data":{"code":777,"version":1}}
                        """,
                """
                        {"code":0,"msg":"success","data":{"code":888,"version":1}}
                        """);

        DolphinSchedulerWorkflowSyncSupport support = new DolphinSchedulerWorkflowSyncSupport(remoteApi, new DolphinSchedulerResponseSupport());
        WorkflowDefinitionDTO synced = support.syncDefinition(baseDefinition().toBuilder()
                .engineBinding(baseBinding("555", "999")
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
                .build());

        assertThat(synced.getEngineBinding().getExternalProjectCode()).isEqualTo("555");
        assertThat(synced.getEngineBinding().getExternalWorkflowId()).isEqualTo("888");
        assertThat(synced.getEngineBinding().getRemoteWorkflowVersionNo()).isEqualTo(1);
    }

    @Test
    void shouldDeleteChildWorkflowsBeforeParentWorkflow() {
        DolphinSchedulerRemoteApi remoteApi = mock(DolphinSchedulerRemoteApi.class);
        when(remoteApi.getProject(555L)).thenReturn("""
                {"code":0,"msg":"success","data":{"code":555,"name":"legacy_etl-project"}}
                """);
        when(remoteApi.deleteWorkflowDefinition(eq(555L), eq(888L))).thenReturn("""
                {"code":0,"msg":"success","data":true}
                """);
        when(remoteApi.deleteWorkflowDefinition(eq(555L), eq(999L))).thenReturn("""
                {"code":0,"msg":"success","data":true}
                """);

        DolphinSchedulerWorkflowSyncSupport support = new DolphinSchedulerWorkflowSyncSupport(remoteApi, new DolphinSchedulerResponseSupport());
        support.deleteDefinition(baseDefinition().toBuilder()
                .engineBinding(baseBinding("555", "999")
                        .attributes(Map.of(
                                "dolphinschedulerSync", Map.of(
                                        "projectCode", "555",
                                        "parent", Map.of(
                                                "workflowDefinitionCode", "999"),
                                        "stages", Map.of(
                                                "S1", Map.of(
                                                        "workflowDefinitionCode", "888")))))
                        .build())
                .build());
    }

    @Test
    void shouldRetryTransientDeleteWorkflowFailures() {
        DolphinSchedulerRemoteApi remoteApi = mock(DolphinSchedulerRemoteApi.class);
        when(remoteApi.getProject(555L)).thenReturn("""
                {"code":0,"msg":"success","data":{"code":555,"name":"legacy_etl-project"}}
                """);
        when(remoteApi.deleteWorkflowDefinition(eq(555L), eq(888L))).thenReturn(
                """
                        {"code":10086,"msg":"delete workflow definition by code error:Lock wait timeout exceeded; try restarting transaction"}
                        """,
                """
                        {"code":0,"msg":"success","data":true}
                        """);

        DolphinSchedulerWorkflowSyncSupport support = new DolphinSchedulerWorkflowSyncSupport(remoteApi, new DolphinSchedulerResponseSupport());
        support.deleteDefinition(baseDefinition().toBuilder()
                .engineBinding(baseBinding("555", "888").build())
                .build());

        verify(remoteApi, times(2)).deleteWorkflowDefinition(eq(555L), eq(888L));
    }

    private WorkflowDefinitionDTO baseDefinition() {
        return WorkflowDefinitionDTO.builder()
                .workflowCode("legacy_etl")
                .workflowName("Legacy ETL")
                .workflowVersionNo(1)
                .platformType(EtlPlatformType.DOLPHIN_SCHEDULER)
                .description("legacy flow")
                .stages(List.of(WorkflowStageDTO.builder()
                        .stageCode("S1")
                        .stageName("阶段1")
                        .stageOrder(1)
                        .build()))
                .build();
    }

    private WorkflowEngineBindingDTO.WorkflowEngineBindingDTOBuilder baseBinding(String projectCode, String workflowCode) {
        return WorkflowEngineBindingDTO.builder()
                .platformType(EtlPlatformType.DOLPHIN_SCHEDULER)
                .externalProjectCode(projectCode)
                .externalWorkflowId(workflowCode)
                .externalNamespace("default");
    }
}
