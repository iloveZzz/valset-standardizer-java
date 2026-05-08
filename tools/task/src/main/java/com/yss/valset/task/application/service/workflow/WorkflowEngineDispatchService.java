package com.yss.valset.task.application.service.workflow;

import com.yss.valset.application.dto.workflow.WorkflowExecutionContextDTO;

import java.util.Map;
import java.util.Optional;

/**
 * 估值内部工作流分发服务。
 *
 * <p>
 * 这一层屏蔽具体调度实现，统一对外提供触发、重试和查询三个能力。
 * 对外传入的上下文会被转换成工作流引擎可识别的参数集合。
 * </p>
 */
public interface WorkflowEngineDispatchService {

    /**
     * 触发工作流任务。
     *
     * @param taskId 任务标识。
     * @param stageCode 阶段编码。
     * @param context 工作流执行上下文。
     */
    void trigger(Long taskId, String stageCode, WorkflowExecutionContextDTO context);

    /**
     * 重试工作流任务。
     *
     * @param taskId 任务标识。
     * @param stageCode 阶段编码。
     * @param context 工作流执行上下文。
     */
    void retry(Long taskId, String stageCode, WorkflowExecutionContextDTO context);

    /**
     * 查询工作流任务状态。
     *
     * @param instanceId 实例标识。
     * @param context 工作流执行上下文。
     */
    Optional<Map<String, Object>> query(String instanceId, WorkflowExecutionContextDTO context);
}
