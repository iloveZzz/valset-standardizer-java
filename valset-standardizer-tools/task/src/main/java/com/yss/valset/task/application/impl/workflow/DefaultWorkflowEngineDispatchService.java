package com.yss.valset.task.application.impl.workflow;

import com.yss.valset.application.dto.workflow.WorkflowContextKeys;
import com.yss.valset.application.dto.workflow.WorkflowExecutionContextDTO;
import com.yss.valset.application.dto.workflow.WorkflowContextPayloadSupport;
import com.yss.valset.batch.scheduler.SchedulerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.task.application.service.workflow.WorkflowEngineDispatchService;
import com.yss.valset.task.application.service.workflow.engine.WorkflowEngineAdapter;
import com.yss.valset.task.application.service.workflow.engine.WorkflowEngineType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 默认估值内部工作流分发服务。
 *
 * <p>
 * 当工作流引擎类型是内部实现时，直接走本地调度器或适配器；
 * 当没有显式引擎时，则回退到本地调度器触发任务。
 * </p>
 */
@Service
public class DefaultWorkflowEngineDispatchService implements WorkflowEngineDispatchService {

    private final SchedulerService schedulerService;
    private final List<WorkflowEngineAdapter> workflowEngineAdapters;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
            // 没有可用引擎适配器时，直接交给本地调度器继续执行。
            triggerInternal(taskId);
            return;
        }
        // 交给内部工作流引擎时，需要把上下文展开成统一参数集合。
        adapter.trigger(resolveWorkflowCode(context), stageCode, buildContext(taskId, stageCode, context));
    }

    @Override
    public void retry(Long taskId, String stageCode, WorkflowExecutionContextDTO context) {
        if (taskId == null) {
            throw new IllegalArgumentException("任务id不能为空");
        }
        WorkflowEngineAdapter adapter = resolveAdapter(context);
        if (adapter == null) {
            // 回退到本地调度器时，重试语义与触发语义保持一致。
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

    /**
     * 回退到本地调度器的触发方式。
     */
    private void triggerInternal(Long taskId) {
        if (schedulerService == null) {
            throw new IllegalStateException("调度器未启用，无法触发任务：" + taskId);
        }
        schedulerService.triggerNow(taskId);
    }

    /**
     * 解析工作流编码，供内部引擎调用。
     */
    private String resolveWorkflowCode(WorkflowExecutionContextDTO context) {
        if (context == null || !StringUtils.hasText(context.getWorkflowCode())) {
            return null;
        }
        return context.getWorkflowCode().trim();
    }

    /**
     * 将上下文展开为工作流引擎可直接消费的参数集合。
     */
    private Map<String, Object> buildContext(Long taskId, String stageCode, WorkflowExecutionContextDTO context) {
        Map<String, Object> values = new java.util.LinkedHashMap<>();
        put(values, WorkflowContextKeys.TASK_ID, taskId);
        put(values, WorkflowContextKeys.INSTANCE_ID, taskId == null ? null : String.valueOf(taskId));
        put(values, WorkflowContextKeys.STAGE_CODE, stageCode);
        put(values, WorkflowContextKeys.WORKFLOW_CODE, context == null ? null : context.getWorkflowCode());
        put(values, WorkflowContextKeys.WORKFLOW_ID, context == null ? null : context.getWorkflowId());
        put(values, WorkflowContextKeys.WORKFLOW_VERSION_NO, context == null ? null : context.getWorkflowVersionNo());
        put(values, WorkflowContextKeys.WORKFLOW_STAGE_CODE, context == null ? null : context.getWorkflowStageCode());
        put(values, WorkflowContextKeys.WORKFLOW_STAGE_NAME, context == null ? null : context.getWorkflowStageName());
        put(values, WorkflowContextKeys.WORKFLOW_STAGE_DESCRIPTION, context == null ? null : context.getWorkflowStageDescription());
        put(values, WorkflowContextKeys.WORKFLOW_ENGINE_TYPE, context == null ? null : context.getEngineType());
        put(values, WorkflowContextKeys.WORKFLOW_ENGINE_EXTERNAL_REF, context == null ? null : context.getExternalRef());
        put(values, WorkflowContextKeys.WORKFLOW_ENGINE_CONFIG_JSON, context == null ? null : context.getConfigJson());
        put(values, WorkflowContextKeys.WORKFLOW_BINDING_ID, context == null ? null : context.getBindingId());
        put(values, WorkflowContextKeys.WORKFLOW_BINDING_RESOLVED, context == null ? null : context.getBindingResolved());
        put(values, WorkflowContextKeys.WORKFLOW_BUSINESS_CONTEXT_JSON, resolveBusinessContextJson(context));
        if (context != null && context.getCommonContext() != null) {
            // 公共上下文先透传，保证创建人、强制重建等任务级参数不会丢失。
            values.putAll(context.getCommonContext());
        }
        Map<String, Object> businessContext = context == null ? null : context.getBusinessContext();
        String businessContextJson = resolveBusinessContextJson(context);
        Map<String, Object> flattenedBusinessContext = parseBusinessContext(businessContextJson);
        if ((businessContext == null || businessContext.isEmpty()) && !flattenedBusinessContext.isEmpty()) {
            // 如果对象里没有展开后的业务上下文，就从 JSON 中回填一份。
            businessContext = flattenedBusinessContext;
        }
        if (businessContext != null) {
            values.putAll(businessContext);
        }
        if (!flattenedBusinessContext.isEmpty()) {
            // JSON 里可能还有额外字段，需要一并补入。
            values.putAll(flattenedBusinessContext);
        }
        return values;
    }

    /**
     * 兼容把 JSON 格式的业务上下文展开成扁平键值对。
     */
    private Map<String, Object> parseBusinessContext(String configJson) {
        if (!StringUtils.hasText(configJson)) {
            return Map.of();
        }
        try {
            Map<?, ?> raw = objectMapper.readValue(configJson, Map.class);
            return WorkflowContextPayloadSupport.flattenEnvelope(raw);
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    /**
     * 优先使用业务上下文 JSON，其次回退到配置 JSON。
     */
    private String resolveBusinessContextJson(WorkflowExecutionContextDTO context) {
        if (context == null) {
            return null;
        }
        if (StringUtils.hasText(context.getBusinessContextJson())) {
            return context.getBusinessContextJson();
        }
        if (StringUtils.hasText(context.getConfigJson())) {
            return context.getConfigJson();
        }
        return null;
    }

    private void put(Map<String, Object> values, String key, Object value) {
        values.put(key, value);
    }
}
