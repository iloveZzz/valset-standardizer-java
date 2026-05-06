package com.yss.valset.task.application.service.workflow.engine;

import com.yss.valset.task.application.dto.workflow.WorkflowExecutorBindingDTO;

import java.util.Map;
import java.util.Optional;

/**
 * 估值内部工作流执行适配器。
 *
 * <p>
 * 该接口只面向估值文件解析、结构标准化、标准科目匹配和结果落库等内部流程，
 * 不承载通用 ETL 平台编排能力。
 * </p>
 */
public interface WorkflowEngineAdapter {

    WorkflowEngineType engineType();

    void validate(WorkflowExecutorBindingDTO binding);

    void trigger(String workflowCode, String stageCode, Map<String, Object> context);

    void stop(String instanceId);

    void retry(String instanceId, String stageCode);

    Optional<Map<String, Object>> query(String instanceId);
}
