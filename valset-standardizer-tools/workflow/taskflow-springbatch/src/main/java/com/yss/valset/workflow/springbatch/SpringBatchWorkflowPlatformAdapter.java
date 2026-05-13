package com.yss.valset.workflow.springbatch;

import com.yss.valset.workflow.service.AbstractWorkflowPlatformAdapter;
import org.springframework.stereotype.Component;

/**
 * Spring Batch 工作流适配器。
 *
 * <p>这是平台适配层的薄包装，真正的执行逻辑都在
 * {@link SpringBatchWorkflowPlatformClient} 中完成。
 */
@Component
public class SpringBatchWorkflowPlatformAdapter extends AbstractWorkflowPlatformAdapter {

    public SpringBatchWorkflowPlatformAdapter(SpringBatchWorkflowPlatformClient client) {
        super(client);
    }
}
