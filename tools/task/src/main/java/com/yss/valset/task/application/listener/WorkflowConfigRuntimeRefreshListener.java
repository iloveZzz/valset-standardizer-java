package com.yss.valset.task.application.listener;

import com.yss.valset.task.application.service.workflow.WorkflowRuntimeCatalog;
import com.yss.valset.task.application.event.workflow.WorkflowConfigRuntimeRefreshEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 工作流配置运行态刷新监听器。
 */
@Component
@RequiredArgsConstructor
public class WorkflowConfigRuntimeRefreshListener {

    private final WorkflowRuntimeCatalog stageCatalog;

    @EventListener
    public void onWorkflowConfigRuntimeRefreshEvent(WorkflowConfigRuntimeRefreshEvent event) {
        stageCatalog.refreshActiveWorkflowDefinition();
    }
}
