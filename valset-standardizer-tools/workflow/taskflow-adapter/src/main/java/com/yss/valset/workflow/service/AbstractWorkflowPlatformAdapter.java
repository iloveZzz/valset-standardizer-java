package com.yss.valset.workflow.service;

import com.yss.valset.workflow.model.EtlPlatformType;
import com.yss.valset.workflow.model.WorkflowDefinitionDTO;
import com.yss.valset.workflow.model.WorkflowInstanceDTO;
import com.yss.valset.workflow.model.WorkflowInstanceQueryRequest;
import com.yss.valset.workflow.model.WorkflowInstanceViewDTO;
import com.yss.valset.workflow.model.WorkflowLogQueryRequest;
import com.yss.valset.workflow.model.WorkflowPauseRequest;
import com.yss.valset.workflow.model.WorkflowPlatformExecutionResult;
import com.yss.valset.workflow.model.WorkflowTaskInstancePageDTO;
import com.yss.valset.workflow.model.WorkflowTaskInstanceQueryRequest;
import com.yss.valset.workflow.model.WorkflowTaskListDTO;
import com.yss.valset.workflow.model.WorkflowRetryRequest;
import com.yss.valset.workflow.model.WorkflowResumeRequest;
import com.yss.valset.workflow.model.WorkflowStopRequest;
import com.yss.valset.workflow.model.WorkflowTriggerRequest;
import com.yss.valset.workflow.spi.WorkflowPlatformAdapter;
import com.yss.valset.workflow.spi.WorkflowPlatformClient;
import com.yss.cloud.dto.result.PageResult;

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
    public WorkflowDefinitionDTO syncDefinition(WorkflowDefinitionDTO definition) {
        return client.syncDefinition(definition);
    }

    @Override
    public WorkflowDefinitionDTO onlineDefinition(WorkflowDefinitionDTO definition) {
        return client.onlineDefinition(definition);
    }

    @Override
    public WorkflowDefinitionDTO offlineDefinition(WorkflowDefinitionDTO definition) {
        return client.offlineDefinition(definition);
    }

    @Override
    public void deleteDefinition(WorkflowDefinitionDTO definition) {
        client.deleteDefinition(definition);
    }

    @Override
    public PageResult<WorkflowInstanceViewDTO> listInstances(WorkflowDefinitionDTO definition,
                                                             WorkflowInstanceQueryRequest request) {
        return client.listInstances(definition, request);
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
    public WorkflowPlatformExecutionResult pause(WorkflowDefinitionDTO definition,
                                                 WorkflowInstanceDTO instance,
                                                 WorkflowPauseRequest request) {
        return client.pause(definition, instance, request);
    }

    @Override
    public WorkflowPlatformExecutionResult resume(WorkflowDefinitionDTO definition,
                                                  WorkflowInstanceDTO instance,
                                                  WorkflowResumeRequest request) {
        return client.resume(definition, instance, request);
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
    public String queryTaskLog(WorkflowDefinitionDTO definition,
                               WorkflowInstanceDTO instance,
                               Long taskInstanceId,
                               Integer skipLineNum,
                               Integer limit) {
        return client.queryTaskLog(definition, instance, taskInstanceId, skipLineNum, limit);
    }

    @Override
    public java.util.List<WorkflowPlatformExecutionResult> queryLogs(WorkflowDefinitionDTO definition,
                                                                      WorkflowInstanceDTO instance,
                                                                      WorkflowLogQueryRequest request) {
        return client.queryLogs(definition, instance, request);
    }

    @Override
    public WorkflowTaskListDTO queryTasks(WorkflowDefinitionDTO definition,
                                          WorkflowInstanceDTO instance) {
        return client.queryTasks(definition, instance);
    }

    @Override
    public WorkflowTaskInstancePageDTO listTaskInstances(WorkflowDefinitionDTO definition,
                                                         WorkflowTaskInstanceQueryRequest request) {
        return client.listTaskInstances(definition, request);
    }

    @Override
    public void forceTaskSuccess(WorkflowDefinitionDTO definition, Long taskInstanceId) {
        client.forceTaskSuccess(definition, taskInstanceId);
    }

    @Override
    public String queryTaskLog(WorkflowDefinitionDTO definition,
                               Long taskInstanceId,
                               Integer skipLineNum,
                               Integer limit) {
        return client.queryTaskLog(definition, taskInstanceId, skipLineNum, limit);
    }
}
