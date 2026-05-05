package com.yss.valset.task.application.impl.workflow;

import com.yss.valset.task.application.service.workflow.WorkflowRuntimeCatalog;
import com.yss.valset.application.dto.workflow.WorkflowExecutionContextDTO;
import com.yss.valset.application.service.workflow.WorkflowExecutionContextResolver;
import com.yss.valset.task.application.dto.workflow.WorkflowDefinitionDTO;
import com.yss.valset.task.application.dto.workflow.WorkflowExecutorBindingDTO;
import com.yss.valset.task.application.dto.workflow.WorkflowStageDTO;
import com.yss.valset.domain.model.TaskStage;
import com.yss.valset.domain.model.TaskType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * 默认工作流执行上下文解析器。
 */
@Service
public class DefaultWorkflowExecutionContextResolver implements WorkflowExecutionContextResolver {

    private final WorkflowRuntimeCatalog stageCatalog;

    public DefaultWorkflowExecutionContextResolver(WorkflowRuntimeCatalog stageCatalog) {
        this.stageCatalog = stageCatalog;
    }

    @Override
    public WorkflowExecutionContextDTO resolve(TaskType taskType, TaskStage taskStage) {
        WorkflowDefinitionDTO definition = stageCatalog.activeWorkflowDefinition().orElse(null);
        if (definition == null) {
            return null;
        }
        WorkflowStageDTO stageDefinition = resolveStageDefinition(definition, taskType, taskStage);
        String stageCode = stageDefinition == null ? stageCatalog.resolveWorkflowStage(taskType, taskStage).name()
                : normalizeStageCode(stageDefinition.getStageCode(), stageCatalog.resolveWorkflowStage(taskType, taskStage).name());
        WorkflowExecutorBindingDTO binding = resolveBinding(definition, stageCode);
        WorkflowExecutionContextDTO context = new WorkflowExecutionContextDTO();
        context.setWorkflowId(definition.getWorkflowId());
        context.setWorkflowCode(definition.getWorkflowCode());
        context.setWorkflowVersionNo(definition.getVersionNo());
        context.setWorkflowStageCode(stageCode);
        context.setWorkflowStageName(stageDefinition == null ? stageCode : stageDefinition.getStageName());
        context.setWorkflowStageDescription(stageDefinition == null ? null : stageDefinition.getStageDescription());
        if (binding != null) {
            context.setEngineType(binding.getEngineType());
            context.setExternalRef(binding.getExternalRef());
            context.setConfigJson(binding.getConfigJson());
            context.setBindingId(binding.getBindingId());
            context.setBindingResolved(Boolean.TRUE);
        } else {
            context.setEngineType(defaultText(definition.getEngineType(), "INTERNAL"));
            context.setBindingResolved(Boolean.FALSE);
        }
        return context;
    }

    private WorkflowStageDTO resolveStageDefinition(WorkflowDefinitionDTO definition, TaskType taskType, TaskStage taskStage) {
        if (definition == null || definition.getStages() == null || definition.getStages().isEmpty()) {
            return null;
        }
        String targetStageCode = stageCatalog.resolveWorkflowStage(taskType, taskStage).name();
        return definition.getStages().stream()
                .filter(stage -> stage != null && Boolean.TRUE.equals(stage.getEnabled()))
                .filter(stage -> matchesStage(stage.getStageCode(), targetStageCode) || matchesStage(stage.getStepCode(), targetStageCode))
                .max(Comparator.comparingInt(stage -> stage.getSortOrder() == null ? Integer.MAX_VALUE : stage.getSortOrder()))
                .orElseGet(() -> definition.getStages().stream()
                        .filter(stage -> stage != null && Boolean.TRUE.equals(stage.getEnabled()))
                        .findFirst()
                        .orElse(null));
    }

    private WorkflowExecutorBindingDTO resolveBinding(WorkflowDefinitionDTO definition, String stageCode) {
        if (definition == null || definition.getExecutorBindings() == null || definition.getExecutorBindings().isEmpty()) {
            return null;
        }
        List<WorkflowExecutorBindingDTO> bindings = definition.getExecutorBindings();
        WorkflowExecutorBindingDTO exact = bindings.stream()
                .filter(this::isEnabledBinding)
                .filter(binding -> matchesStage(binding.getStageCode(), stageCode))
                .findFirst()
                .orElse(null);
        if (exact != null) {
            return exact;
        }
        String defaultEngineType = defaultText(definition.getEngineType(), "INTERNAL");
        return bindings.stream()
                .filter(this::isEnabledBinding)
                .filter(binding -> !StringUtils.hasText(binding.getStageCode()))
                .filter(binding -> matchesEngineType(binding.getEngineType(), defaultEngineType))
                .findFirst()
                .orElseGet(() -> bindings.stream()
                        .filter(this::isEnabledBinding)
                        .filter(binding -> !StringUtils.hasText(binding.getStageCode()))
                        .findFirst()
                        .orElse(null));
    }

    private boolean isEnabledBinding(WorkflowExecutorBindingDTO binding) {
        return binding != null && !Boolean.FALSE.equals(binding.getEnabled());
    }

    private boolean matchesStage(String left, String right) {
        return StringUtils.hasText(left) && StringUtils.hasText(right) && left.trim().equalsIgnoreCase(right.trim());
    }

    private boolean matchesEngineType(String left, String right) {
        return StringUtils.hasText(left) && StringUtils.hasText(right) && left.trim().equalsIgnoreCase(right.trim());
    }

    private String normalizeStageCode(String stageCode, String fallback) {
        if (!StringUtils.hasText(stageCode)) {
            return fallback;
        }
        return stageCode.trim();
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }
}
