package com.yss.valset.task.application.listener;

import com.yss.valset.application.event.lifecycle.ParseLifecycleEvent;
import com.yss.valset.application.event.lifecycle.WorkflowTaskLifecycleEvent;
import com.yss.valset.task.application.port.OutsourcedDataTaskGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 委外数据任务生命周期监听器。
 */
@Component
@RequiredArgsConstructor
public class OutsourcedDataTaskLifecycleListener {

    private final OutsourcedDataTaskGateway outsourcedDataTaskGateway;

    @EventListener
    public void onParseLifecycleEvent(ParseLifecycleEvent event) {
        outsourcedDataTaskGateway.recordParseLifecycleEvent(event);
    }

    @EventListener
    public void onWorkflowTaskLifecycleEvent(WorkflowTaskLifecycleEvent event) {
        outsourcedDataTaskGateway.recordWorkflowTaskLifecycleEvent(event);
    }
}
