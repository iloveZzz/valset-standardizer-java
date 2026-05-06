package com.yss.valset.workflow.springbatch;

import com.yss.valset.workflow.service.AbstractWorkflowPlatformAdapter;
import org.springframework.stereotype.Component;

/**
 * Spring Batch 工作流适配器。
 */
@Component
public class SpringBatchWorkflowPlatformAdapter extends AbstractWorkflowPlatformAdapter {

    public SpringBatchWorkflowPlatformAdapter(SpringBatchWorkflowPlatformClient client) {
        super(client);
    }
}
