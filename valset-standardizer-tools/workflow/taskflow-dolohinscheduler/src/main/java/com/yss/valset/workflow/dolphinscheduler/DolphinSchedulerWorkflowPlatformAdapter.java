package com.yss.valset.workflow.dolphinscheduler;

import com.yss.valset.workflow.service.AbstractWorkflowPlatformAdapter;
import org.springframework.stereotype.Component;

/**
 * DolphinScheduler 工作流适配器。
 */
@Component
public class DolphinSchedulerWorkflowPlatformAdapter extends AbstractWorkflowPlatformAdapter {

    public DolphinSchedulerWorkflowPlatformAdapter(DolphinSchedulerWorkflowPlatformClient client) {
        super(client);
    }
}
