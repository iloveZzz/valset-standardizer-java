package com.yss.valset.task.application.impl.workflow;

import com.yss.valset.application.dto.workflow.WorkflowExecutionContextDTO;
import com.yss.valset.batch.scheduler.SchedulerService;
import com.yss.valset.task.application.service.workflow.WorkflowEngineDispatchService;
import com.yss.valset.task.application.service.workflow.engine.WorkflowEngineAdapter;
import com.yss.valset.task.application.service.workflow.engine.WorkflowEngineType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 默认估值内部工作流分发服务。
 */
@Service
public class DefaultWorkflowEngineDispatchService implements WorkflowEngineDispatchService {

    private final SchedulerService schedulerService;
    private final List<WorkflowEngineAdapter> workflowEngineAdapters;

    public DefaultWorkflowEngineDispatchService(SchedulerService schedulerService,
                                                List<WorkflowEngineAdapter> workflowEngineAdapters) {
        this.schedulerService = schedulerService;
        this.workflowEngineAdapters = workflowEngineAdapters == null ? List.of() : workflowEngineAdapters;
    }

    @Override
    public void trigger(Long taskId, String stageCode, WorkflowExecutionContextDTO context) {
        if (taskId == null) {
            throw new IllegalArgumentException("任务id不能为空");
        }
        WorkflowEngineAdapter adapter = resolveAdapter(context);
        if (adapter == null) {
            triggerInternal(taskId);
            return;
        }
        adapter.trigger(resolveWorkflowCode(context), stageCode, buildContext(taskId, stageCode, context));
    }

    @Override
    public void retry(Long taskId, String stageCode, WorkflowExecutionContextDTO context) {
        if (taskId == null) {
            throw new IllegalArgumentException("任务id不能为空");
        }
        WorkflowEngineAdapter adapter = resolveAdapter(context);
        if (adapter == null) {
            triggerInternal(taskId);
            return;
        }
        adapter.retry(String.valueOf(taskId), stageCode);
    }

    @Override
    public Optional<Map<String, Object>> query(String instanceId, WorkflowExecutionContextDTO context) {
        WorkflowEngineAdapter adapter = resolveAdapter(context);
        if (adapter == null) {
            return Optional.empty();
        }
        try {
            return adapter.query(instanceId);
        } catch (UnsupportedOperationException exception) {
            return Optional.empty();
        }
    }

    private WorkflowEngineAdapter resolveAdapter(WorkflowExecutionContextDTO context) {
        if (context == null || !StringUtils.hasText(context.getEngineType())) {
            return null;
        }
        String engineType = context.getEngineType().trim();
        if (!WorkflowEngineType.INTERNAL.name().equalsIgnoreCase(engineType)) {
            return null;
        }
        return workflowEngineAdapters.stream()
                .filter(adapter -> adapter != null && adapter.engineType() == WorkflowEngineType.INTERNAL)
                .findFirst()
                .orElse(null);
    }

    private void triggerInternal(Long taskId) {
        if (schedulerService == null) {
            throw new IllegalStateException("调度器未启用，无法触发任务：" + taskId);
        }
        schedulerService.triggerNow(taskId);
    }

    private String resolveWorkflowCode(WorkflowExecutionContextDTO context) {
        if (context == null || !StringUtils.hasText(context.getWorkflowCode())) {
            return null;
        }
        return context.getWorkflowCode().trim();
    }

    private Map<String, Object> buildContext(Long taskId, String stageCode, WorkflowExecutionContextDTO context) {
        Map<String, Object> values = new HashMap<>();
        values.put("taskId", taskId);
        values.put("instanceId", taskId == null ? null : String.valueOf(taskId));
        values.put("stageCode", stageCode);
        values.put("workflowCode", context == null ? null : context.getWorkflowCode());
        values.put("workflowId", context == null ? null : context.getWorkflowId());
        values.put("workflowVersionNo", context == null ? null : context.getWorkflowVersionNo());
        values.put("workflowStageCode", context == null ? null : context.getWorkflowStageCode());
        values.put("workflowStageName", context == null ? null : context.getWorkflowStageName());
        values.put("workflowStageDescription", context == null ? null : context.getWorkflowStageDescription());
        values.put("workflowEngineType", context == null ? null : context.getEngineType());
        values.put("workflowEngineExternalRef", context == null ? null : context.getExternalRef());
        values.put("workflowEngineConfigJson", context == null ? null : context.getConfigJson());
        values.put("workflowBindingId", context == null ? null : context.getBindingId());
        values.put("workflowBindingResolved", context == null ? null : context.getBindingResolved());
        return values;
    }
}
