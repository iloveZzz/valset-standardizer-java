package com.yss.valset.workflow.dolphinscheduler;

import com.yss.valset.workflow.service.AbstractWorkflowSchedulePlatformAdapter;
import org.springframework.stereotype.Component;

/**
 * DolphinScheduler 工作流调度适配器。
 */
@Component
public class DolphinSchedulerWorkflowSchedulePlatformAdapter extends AbstractWorkflowSchedulePlatformAdapter {

    public DolphinSchedulerWorkflowSchedulePlatformAdapter(DolphinSchedulerWorkflowSchedulePlatformClient client) {
        super(client);
    }
}
