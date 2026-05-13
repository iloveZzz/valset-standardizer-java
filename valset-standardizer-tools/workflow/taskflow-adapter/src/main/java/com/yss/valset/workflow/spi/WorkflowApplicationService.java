package com.yss.valset.workflow.spi;

import com.yss.cloud.dto.result.PageResult;
import com.yss.valset.workflow.model.WorkflowCallbackRequest;
import com.yss.valset.workflow.model.WorkflowDefinitionDTO;
import com.yss.valset.workflow.model.WorkflowInstanceQueryRequest;
import com.yss.valset.workflow.model.WorkflowInstanceDTO;
import com.yss.valset.workflow.model.WorkflowInstanceViewDTO;
import com.yss.valset.workflow.model.WorkflowLogQueryRequest;
import com.yss.valset.workflow.model.WorkflowPauseRequest;
import com.yss.valset.workflow.model.WorkflowRetryRequest;
import com.yss.valset.workflow.model.WorkflowResumeRequest;
import com.yss.valset.workflow.model.WorkflowPlatformMetadataDTO;
import com.yss.valset.workflow.model.WorkflowStageLogDTO;
import com.yss.valset.workflow.model.WorkflowTaskInstancePageDTO;
import com.yss.valset.workflow.model.WorkflowTaskInstanceQueryRequest;
import com.yss.valset.workflow.model.WorkflowTaskListDTO;
import com.yss.valset.workflow.model.WorkflowStopRequest;
import com.yss.valset.workflow.model.WorkflowTriggerRequest;

import java.util.List;
import java.util.Optional;

/**
 * 通用 ETL 工作流应用服务。
 */
public interface WorkflowApplicationService {

    WorkflowDefinitionDTO saveDefinition(WorkflowDefinitionDTO definition);

    WorkflowDefinitionDTO syncDefinition(WorkflowDefinitionDTO definition);

    WorkflowDefinitionDTO onlineDefinition(String workflowCode, Integer workflowVersionNo);

    WorkflowDefinitionDTO offlineDefinition(String workflowCode, Integer workflowVersionNo);

    boolean deleteDefinition(String workflowCode, Integer workflowVersionNo);

    WorkflowDefinitionDTO validateDefinition(WorkflowDefinitionDTO definition);

    WorkflowInstanceDTO runDefinition(String workflowCode, Integer workflowVersionNo);

    List<WorkflowDefinitionDTO> listDefinitions();

    List<WorkflowPlatformMetadataDTO> listPlatforms();

    Optional<WorkflowDefinitionDTO> findDefinition(String workflowCode, Integer workflowVersionNo);

    WorkflowInstanceDTO trigger(WorkflowTriggerRequest request);

    WorkflowInstanceDTO stop(String instanceId, WorkflowStopRequest request);

    WorkflowInstanceDTO pause(String instanceId, WorkflowPauseRequest request);

    WorkflowInstanceDTO resume(String instanceId, WorkflowResumeRequest request);

    WorkflowInstanceDTO retry(String instanceId, WorkflowRetryRequest request);

    Optional<WorkflowInstanceDTO> findInstance(String instanceId);

    PageResult<WorkflowInstanceViewDTO> listInstances(WorkflowInstanceQueryRequest request);

    WorkflowTaskListDTO listTaskInstances(String instanceId);

    String queryTaskLog(String instanceId, Long taskInstanceId);

    WorkflowTaskInstancePageDTO listTaskInstances(WorkflowTaskInstanceQueryRequest request);

    void forceTaskSuccess(String workflowCode, Integer workflowVersionNo, Long taskInstanceId);

    String queryTaskLog(String workflowCode,
                        Integer workflowVersionNo,
                        Long taskInstanceId,
                        Integer skipLineNum,
                        Integer limit);

    List<WorkflowStageLogDTO> listStageLogs(WorkflowLogQueryRequest request);

    WorkflowInstanceDTO callback(String instanceId, WorkflowCallbackRequest request);
}
