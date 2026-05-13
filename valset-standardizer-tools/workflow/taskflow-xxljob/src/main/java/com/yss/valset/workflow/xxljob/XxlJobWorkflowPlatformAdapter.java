package com.yss.valset.workflow.xxljob;

import com.yss.valset.workflow.service.AbstractWorkflowPlatformAdapter;
import org.springframework.stereotype.Component;

/**
 * XXL-JOB 工作流适配器。
 */
@Component
public class XxlJobWorkflowPlatformAdapter extends AbstractWorkflowPlatformAdapter {

    public XxlJobWorkflowPlatformAdapter(XxlJobWorkflowPlatformClient client) {
        super(client);
    }
}
