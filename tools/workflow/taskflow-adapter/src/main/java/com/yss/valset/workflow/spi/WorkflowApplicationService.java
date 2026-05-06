package com.yss.valset.workflow.spi;

import com.yss.valset.workflow.model.WorkflowCallbackRequest;
import com.yss.valset.workflow.model.WorkflowDefinitionDTO;
import com.yss.valset.workflow.model.WorkflowInstanceDTO;
import com.yss.valset.workflow.model.WorkflowLogQueryRequest;
import com.yss.valset.workflow.model.WorkflowRetryRequest;
import com.yss.valset.workflow.model.WorkflowPlatformMetadataDTO;
import com.yss.valset.workflow.model.WorkflowStageLogDTO;
import com.yss.valset.workflow.model.WorkflowStopRequest;
import com.yss.valset.workflow.model.WorkflowTriggerRequest;

import java.util.List;
import java.util.Optional;

/**
 * 通用 ETL 工作流应用服务。
 */
public interface WorkflowApplicationService {

    WorkflowDefinitionDTO saveDefinition(WorkflowDefinitionDTO definition);

    WorkflowDefinitionDTO validateDefinition(WorkflowDefinitionDTO definition);

    List<WorkflowDefinitionDTO> listDefinitions();

    List<WorkflowPlatformMetadataDTO> listPlatforms();

    Optional<WorkflowDefinitionDTO> findDefinition(String workflowCode, Integer workflowVersionNo);

    WorkflowInstanceDTO trigger(WorkflowTriggerRequest request);

    WorkflowInstanceDTO stop(String instanceId, WorkflowStopRequest request);

    WorkflowInstanceDTO retry(String instanceId, WorkflowRetryRequest request);

    Optional<WorkflowInstanceDTO> findInstance(String instanceId);

    List<WorkflowStageLogDTO> listStageLogs(WorkflowLogQueryRequest request);

    WorkflowInstanceDTO callback(String instanceId, WorkflowCallbackRequest request);
}
