package com.yss.valset.task.application.impl.workflow;

import com.yss.valset.task.application.service.workflow.WorkflowRuntimeCatalog;
import com.yss.valset.application.dto.workflow.WorkflowExecutionContextDTO;
import com.yss.valset.application.service.workflow.WorkflowExecutionContextResolver;
import com.yss.valset.domain.model.TaskStage;
import com.yss.valset.domain.model.TaskType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
        WorkflowRuntimeCatalog.ActiveWorkflowDefinition definition = stageCatalog.activeWorkflowDefinition()
                .orElse(null);
        if (definition == null) {
            return null;
        }
        String stageCode = stageCatalog.resolveWorkflowStage(taskType, taskStage).name();
        WorkflowExecutionContextDTO context = new WorkflowExecutionContextDTO();
        context.setWorkflowId(definition.getWorkflowId());
        context.setWorkflowCode(definition.getWorkflowCode());
        context.setWorkflowVersionNo(definition.getVersionNo());
        context.setWorkflowStageCode(stageCode);
        context.setWorkflowStageName(stageCatalog.stageLabel(stageCode));
        context.setWorkflowStageDescription(stageCatalog.stageDescription(stageCode));
        context.setEngineType(defaultText(definition.getEngineType(), "INTERNAL"));
        context.setBindingResolved(Boolean.FALSE);
        return context;
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }
}
