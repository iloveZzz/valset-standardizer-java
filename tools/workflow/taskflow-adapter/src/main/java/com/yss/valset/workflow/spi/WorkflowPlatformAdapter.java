package com.yss.valset.workflow.spi;

import com.yss.valset.workflow.model.WorkflowDefinitionDTO;
import com.yss.valset.workflow.model.WorkflowInstanceDTO;
import com.yss.valset.workflow.model.WorkflowInstanceQueryRequest;
import com.yss.valset.workflow.model.WorkflowInstanceViewDTO;
import com.yss.valset.workflow.model.WorkflowLogQueryRequest;
import com.yss.valset.workflow.model.WorkflowPauseRequest;
import com.yss.valset.workflow.model.WorkflowPlatformExecutionResult;
import com.yss.valset.workflow.model.WorkflowTaskListDTO;
import com.yss.valset.workflow.model.WorkflowRetryRequest;
import com.yss.valset.workflow.model.WorkflowStopRequest;
import com.yss.valset.workflow.model.WorkflowTriggerRequest;
import com.yss.valset.workflow.model.EtlPlatformType;
import com.yss.cloud.dto.response.PageResult;

import java.util.List;

/**
 * 平台适配器。
 */
public interface WorkflowPlatformAdapter {

    EtlPlatformType platformType();

    void validate(WorkflowDefinitionDTO definition);

    WorkflowDefinitionDTO syncDefinition(WorkflowDefinitionDTO definition);

    WorkflowDefinitionDTO onlineDefinition(WorkflowDefinitionDTO definition);

    WorkflowDefinitionDTO offlineDefinition(WorkflowDefinitionDTO definition);

    void deleteDefinition(WorkflowDefinitionDTO definition);

    default PageResult<WorkflowInstanceViewDTO> listInstances(WorkflowDefinitionDTO definition,
                                                              WorkflowInstanceQueryRequest request) {
        int pageIndex = request == null || request.getPageIndex() == null ? 0 : Math.max(request.getPageIndex(), 0);
        int pageSize = request == null || request.getPageSize() == null ? 20 : Math.max(request.getPageSize(), 1);
        return PageResult.of(List.of(), 0L, pageSize, pageIndex);
    }

    WorkflowPlatformExecutionResult trigger(WorkflowDefinitionDTO definition,
                                            WorkflowInstanceDTO instance,
                                            WorkflowTriggerRequest request);

    WorkflowPlatformExecutionResult stop(WorkflowDefinitionDTO definition,
                                         WorkflowInstanceDTO instance,
                                         WorkflowStopRequest request);

    default WorkflowPlatformExecutionResult pause(WorkflowDefinitionDTO definition,
                                                  WorkflowInstanceDTO instance,
                                                  WorkflowPauseRequest request) {
        throw new UnsupportedOperationException("当前平台不支持暂停工作流");
    }

    WorkflowPlatformExecutionResult retry(WorkflowDefinitionDTO definition,
                                          WorkflowInstanceDTO instance,
                                          WorkflowRetryRequest request);

    WorkflowPlatformExecutionResult query(WorkflowDefinitionDTO definition,
                                          WorkflowInstanceDTO instance);

    default List<WorkflowPlatformExecutionResult> queryLogs(WorkflowDefinitionDTO definition,
                                                            WorkflowInstanceDTO instance,
                                                            WorkflowLogQueryRequest request) {
        return List.of();
    }

    default WorkflowTaskListDTO queryTasks(WorkflowDefinitionDTO definition,
                                           WorkflowInstanceDTO instance) {
        return WorkflowTaskListDTO.builder().build();
    }
}
