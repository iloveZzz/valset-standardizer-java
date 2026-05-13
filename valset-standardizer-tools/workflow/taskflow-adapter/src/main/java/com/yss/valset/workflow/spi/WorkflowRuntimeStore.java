package com.yss.valset.workflow.spi;

import com.yss.valset.workflow.model.WorkflowDefinitionDTO;
import com.yss.valset.workflow.model.WorkflowInstanceQueryRequest;
import com.yss.valset.workflow.model.WorkflowInstanceDTO;
import com.yss.valset.workflow.model.WorkflowInstanceViewDTO;
import com.yss.cloud.dto.result.PageResult;

import java.util.List;
import java.util.Optional;

/**
 * 工作流运行态存储。
 */
public interface WorkflowRuntimeStore {

    WorkflowDefinitionDTO saveDefinition(WorkflowDefinitionDTO definition);

    boolean deleteDefinition(String workflowCode, Integer workflowVersionNo);

    Optional<WorkflowDefinitionDTO> findDefinition(String workflowCode, Integer workflowVersionNo);

    List<WorkflowDefinitionDTO> listDefinitions();

    WorkflowInstanceDTO saveInstance(WorkflowInstanceDTO instance);

    Optional<WorkflowInstanceDTO> findInstance(String instanceId);

    PageResult<WorkflowInstanceViewDTO> listInstances(WorkflowInstanceQueryRequest request);
}
