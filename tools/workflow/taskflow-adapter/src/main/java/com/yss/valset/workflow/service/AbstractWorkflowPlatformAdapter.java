package com.yss.valset.workflow.service;

import com.yss.valset.workflow.model.EtlPlatformType;
import com.yss.valset.workflow.model.WorkflowDefinitionDTO;
import com.yss.valset.workflow.model.WorkflowInstanceDTO;
import com.yss.valset.workflow.model.WorkflowLogQueryRequest;
import com.yss.valset.workflow.model.WorkflowPlatformExecutionResult;
import com.yss.valset.workflow.model.WorkflowRetryRequest;
import com.yss.valset.workflow.model.WorkflowStopRequest;
import com.yss.valset.workflow.model.WorkflowTriggerRequest;
import com.yss.valset.workflow.spi.WorkflowPlatformAdapter;
import com.yss.valset.workflow.spi.WorkflowPlatformClient;

import java.util.List;

/**
 * 平台适配器委派壳。
 */
public abstract class AbstractWorkflowPlatformAdapter implements WorkflowPlatformAdapter {

    private final WorkflowPlatformClient client;

    protected AbstractWorkflowPlatformAdapter(WorkflowPlatformClient client) {
        this.client = client;
    }

    @Override
    public EtlPlatformType platformType() {
        return client.platformType();
    }

    @Override
    public void validate(WorkflowDefinitionDTO definition) {
        client.validate(definition);
    }

    @Override
    public WorkflowPlatformExecutionResult trigger(WorkflowDefinitionDTO definition,
                                                   WorkflowInstanceDTO instance,
                                                   WorkflowTriggerRequest request) {
        return client.trigger(definition, instance, request);
    }

    @Override
    public WorkflowPlatformExecutionResult stop(WorkflowDefinitionDTO definition,
                                                WorkflowInstanceDTO instance,
                                                WorkflowStopRequest request) {
        return client.stop(definition, instance, request);
    }

    @Override
    public WorkflowPlatformExecutionResult retry(WorkflowDefinitionDTO definition,
                                                 WorkflowInstanceDTO instance,
                                                 WorkflowRetryRequest request) {
        return client.retry(definition, instance, request);
    }

    @Override
    public WorkflowPlatformExecutionResult query(WorkflowDefinitionDTO definition,
                                                 WorkflowInstanceDTO instance) {
        return client.query(definition, instance);
    }

    @Override
    public List<WorkflowPlatformExecutionResult> queryLogs(WorkflowDefinitionDTO definition,
                                                          WorkflowInstanceDTO instance,
                                                          WorkflowLogQueryRequest request) {
        return client.queryLogs(definition, instance, request);
    }
}
