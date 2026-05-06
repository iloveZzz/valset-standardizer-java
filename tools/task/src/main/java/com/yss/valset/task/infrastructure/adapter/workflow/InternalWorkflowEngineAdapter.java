package com.yss.valset.task.infrastructure.adapter.workflow;

import com.yss.valset.batch.scheduler.SchedulerService;
import com.yss.valset.domain.gateway.WorkflowTaskGateway;
import com.yss.valset.domain.model.WorkflowTask;
import com.yss.valset.task.application.dto.workflow.WorkflowExecutorBindingDTO;
import com.yss.valset.task.application.service.workflow.engine.WorkflowEngineAdapter;
import com.yss.valset.task.application.service.workflow.engine.WorkflowEngineType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 估值内部工作流执行适配器。
 */
@Component
public class InternalWorkflowEngineAdapter implements WorkflowEngineAdapter {

    private final SchedulerService schedulerService;
    private final WorkflowTaskGateway workflowTaskGateway;

    public InternalWorkflowEngineAdapter(SchedulerService schedulerService,
                                         WorkflowTaskGateway workflowTaskGateway) {
        this.schedulerService = schedulerService;
        this.workflowTaskGateway = workflowTaskGateway;
    }

    @Override
    public WorkflowEngineType engineType() {
        return WorkflowEngineType.INTERNAL;
    }

    @Override
    public void validate(WorkflowExecutorBindingDTO binding) {
        if (binding == null) {
            throw new IllegalArgumentException("平台绑定不能为空");
        }
        if (!StringUtils.hasText(binding.getEngineType())) {
            throw new IllegalArgumentException("平台绑定执行类型不能为空");
        }
        if (!WorkflowEngineType.INTERNAL.name().equalsIgnoreCase(binding.getEngineType())) {
            throw new IllegalArgumentException("内置适配器仅支持 INTERNAL 平台");
        }
    }

    @Override
    public void trigger(String workflowCode, String stageCode, Map<String, Object> context) {
        Long taskId = resolveTaskId(context);
        if (taskId == null) {
            throw new IllegalArgumentException("INTERNAL 平台触发缺少 taskId");
        }
        if (schedulerService == null) {
            throw new IllegalStateException("调度器未启用，无法触发内部工作流任务：" + taskId);
        }
        schedulerService.triggerNow(taskId);
    }

    @Override
    public void stop(String instanceId) {
        throw new UnsupportedOperationException("INTERNAL 平台暂未接入停止入口");
    }

    @Override
    public void retry(String instanceId, String stageCode) {
        Long taskId = resolveInstanceId(instanceId);
        if (taskId == null) {
            throw new IllegalArgumentException("INTERNAL 平台重试缺少实例标识");
        }
        if (workflowTaskGateway == null) {
            throw new IllegalStateException("工作流任务网关未启用，无法重试任务：" + taskId);
        }
        if (!workflowTaskGateway.markRetrying(taskId)) {
            throw new IllegalStateException("任务状态不允许重试：" + taskId);
        }
        if (schedulerService == null) {
            throw new IllegalStateException("调度器未启用，无法重试内部工作流任务：" + taskId);
        }
        schedulerService.triggerNow(taskId);
    }

    @Override
    public Optional<Map<String, Object>> query(String instanceId) {
        Long taskId = resolveInstanceId(instanceId);
        if (taskId == null || workflowTaskGateway == null) {
            return Optional.empty();
        }
        try {
            WorkflowTask task = workflowTaskGateway.findById(taskId);
            Map<String, Object> result = new HashMap<>();
            result.put("taskId", task.getTaskId());
            result.put("taskType", task.getTaskType() == null ? null : task.getTaskType().name());
            result.put("taskStatus", task.getTaskStatus() == null ? null : task.getTaskStatus().name());
            result.put("taskStage", task.getTaskStage() == null ? null : task.getTaskStage().name());
            result.put("businessKey", task.getBusinessKey());
            result.put("fileId", task.getFileId());
            result.put("inputPayload", task.getInputPayload());
            result.put("resultPayload", task.getResultPayload());
            result.put("taskStartTime", task.getTaskStartTime());
            result.put("parseTaskTimeMs", task.getParseTaskTimeMs());
            result.put("standardizeTimeMs", task.getStandardizeTimeMs());
            result.put("matchStandardSubjectTimeMs", task.getMatchStandardSubjectTimeMs());
            return Optional.of(result);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private Long resolveTaskId(Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return null;
        }
        Object value = context.get("taskId");
        if (value == null) {
            value = context.get("instanceId");
        }
        if (value == null) {
            value = context.get("batchId");
        }
        return resolveInstanceId(value == null ? null : String.valueOf(value));
    }

    private Long resolveInstanceId(String instanceId) {
        if (!StringUtils.hasText(instanceId)) {
            return null;
        }
        try {
            return Long.parseLong(instanceId.trim());
        } catch (Exception ignored) {
            return null;
        }
    }
}
