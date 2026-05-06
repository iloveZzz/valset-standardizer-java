package com.yss.valset.workflow.spi;

import com.yss.valset.workflow.model.WorkflowDefinitionDTO;
import com.yss.valset.workflow.model.WorkflowInstanceDTO;
import com.yss.valset.workflow.model.WorkflowStageLogDTO;

import java.util.List;
import java.util.Optional;

/**
 * 工作流运行态存储。
 */
public interface WorkflowRuntimeStore {

    WorkflowDefinitionDTO saveDefinition(WorkflowDefinitionDTO definition);

    Optional<WorkflowDefinitionDTO> findDefinition(String workflowCode, Integer workflowVersionNo);

    List<WorkflowDefinitionDTO> listDefinitions();

    WorkflowInstanceDTO saveInstance(WorkflowInstanceDTO instance);

    Optional<WorkflowInstanceDTO> findInstance(String instanceId);

    WorkflowStageLogDTO saveStageLog(WorkflowStageLogDTO log);

    List<WorkflowStageLogDTO> listStageLogs(String instanceId, String stageCode);
}
