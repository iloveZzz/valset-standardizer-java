package com.yss.valset.application.service.workflow;

import com.yss.valset.application.dto.workflow.WorkflowExecutionContextDTO;
import com.yss.valset.domain.model.TaskStage;
import com.yss.valset.domain.model.TaskType;

/**
 * 工作流执行上下文解析器。
 */
public interface WorkflowExecutionContextResolver {

    WorkflowExecutionContextDTO resolve(TaskType taskType, TaskStage taskStage);
}
