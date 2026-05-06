package com.yss.valset.task.application.service.workflow;

import com.yss.valset.application.dto.workflow.WorkflowExecutionContextDTO;

import java.util.Map;
import java.util.Optional;

/**
 * 估值内部工作流分发服务。
 */
public interface WorkflowEngineDispatchService {

    /**
     * 触发工作流任务。
     */
    void trigger(Long taskId, String stageCode, WorkflowExecutionContextDTO context);

    /**
     * 重试工作流任务。
     */
    void retry(Long taskId, String stageCode, WorkflowExecutionContextDTO context);

    /**
     * 查询工作流任务状态。
     */
    Optional<Map<String, Object>> query(String instanceId, WorkflowExecutionContextDTO context);
}
