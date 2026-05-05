package com.yss.valset.task.application.event.workflow;

/**
 * 工作流配置运行态刷新事件。
 */
public class WorkflowConfigRuntimeRefreshEvent {

    private final Object source;

    public WorkflowConfigRuntimeRefreshEvent(Object source) {
        this.source = source;
    }

    public Object getSource() {
        return source;
    }
}
