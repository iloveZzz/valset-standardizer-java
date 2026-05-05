package com.yss.valset.task.application.service.workflow.engine;

import com.yss.valset.task.application.dto.workflow.WorkflowExecutorBindingDTO;

import java.util.Map;
import java.util.Optional;

/**
 * 工作流执行平台适配器。
 */
public interface WorkflowEngineAdapter {

    WorkflowEngineType engineType();

    void validate(WorkflowExecutorBindingDTO binding);

    void trigger(String workflowCode, String stageCode, Map<String, Object> context);

    void stop(String instanceId);

    void retry(String instanceId, String stageCode);

    Optional<Map<String, Object>> query(String instanceId);
}
