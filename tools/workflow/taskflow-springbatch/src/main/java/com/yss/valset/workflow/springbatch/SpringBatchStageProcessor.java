package com.yss.valset.workflow.springbatch;

import com.yss.valset.workflow.model.WorkflowDefinitionDTO;
import com.yss.valset.workflow.model.WorkflowInstanceDTO;
import com.yss.valset.workflow.model.WorkflowPlatformCommand;
import com.yss.valset.workflow.model.WorkflowStageDTO;

/**
 * Spring Batch 阶段处理器。
 */
public interface SpringBatchStageProcessor {

    SpringBatchStageExecutionResult process(WorkflowDefinitionDTO definition,
                                            WorkflowInstanceDTO instance,
                                            WorkflowStageDTO stage,
                                            WorkflowPlatformCommand command);
}
