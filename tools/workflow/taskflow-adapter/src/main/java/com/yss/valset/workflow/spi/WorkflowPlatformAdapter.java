package com.yss.valset.workflow.spi;

import com.yss.valset.workflow.model.WorkflowDefinitionDTO;
import com.yss.valset.workflow.model.WorkflowInstanceDTO;
import com.yss.valset.workflow.model.WorkflowLogQueryRequest;
import com.yss.valset.workflow.model.WorkflowPlatformExecutionResult;
import com.yss.valset.workflow.model.WorkflowRetryRequest;
import com.yss.valset.workflow.model.WorkflowStopRequest;
import com.yss.valset.workflow.model.WorkflowTriggerRequest;
import com.yss.valset.workflow.model.EtlPlatformType;

import java.util.List;

/**
 * 平台适配器。
 */
public interface WorkflowPlatformAdapter {

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
