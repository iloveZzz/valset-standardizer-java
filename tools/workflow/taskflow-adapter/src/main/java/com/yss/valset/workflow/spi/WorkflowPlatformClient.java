package com.yss.valset.workflow.spi;

import com.yss.valset.workflow.model.EtlPlatformType;
import com.yss.valset.workflow.model.WorkflowDefinitionDTO;
import com.yss.valset.workflow.model.WorkflowInstanceDTO;
import com.yss.valset.workflow.model.WorkflowLogQueryRequest;
import com.yss.valset.workflow.model.WorkflowPlatformExecutionResult;
import com.yss.valset.workflow.model.WorkflowRetryRequest;
import com.yss.valset.workflow.model.WorkflowStopRequest;
import com.yss.valset.workflow.model.WorkflowTriggerRequest;

import java.util.List;

/**
 * 工作流平台客户端。
 */
public interface WorkflowPlatformClient {

    EtlPlatformType platformType();

    void validate(WorkflowDefinitionDTO definition);

    WorkflowPlatformExecutionResult trigger(WorkflowDefinitionDTO definition,
                                            WorkflowInstanceDTO instance,
                                            WorkflowTriggerRequest request);

    WorkflowPlatformExecutionResult stop(WorkflowDefinitionDTO definition,
                                         WorkflowInstanceDTO instance,
                                         WorkflowStopRequest request);

    WorkflowPlatformExecutionResult retry(WorkflowDefinitionDTO definition,
                                          WorkflowInstanceDTO instance,
                                          WorkflowRetryRequest request);

    WorkflowPlatformExecutionResult query(WorkflowDefinitionDTO definition,
                                          WorkflowInstanceDTO instance);

    List<WorkflowPlatformExecutionResult> queryLogs(WorkflowDefinitionDTO definition,
                                                    WorkflowInstanceDTO instance,
                                                    WorkflowLogQueryRequest request);
}
