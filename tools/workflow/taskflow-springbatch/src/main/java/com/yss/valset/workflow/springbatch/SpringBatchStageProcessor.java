package com.yss.valset.workflow.springbatch;

import com.yss.valset.workflow.model.WorkflowDefinitionDTO;
import com.yss.valset.workflow.model.WorkflowInstanceDTO;
import com.yss.valset.workflow.model.WorkflowPlatformCommand;
import com.yss.valset.workflow.model.WorkflowStageDTO;

/**
 * Spring Batch 阶段处理器。
 *
 * <p>该接口用于把“工作流阶段定义”转换成“可执行阶段结果”。
 * 实现类只关心单个阶段的业务语义，不负责作业提交、查询或日志存储。
 */
public interface SpringBatchStageProcessor {

    /**
     * 处理单个工作流阶段并返回结构化执行结果。
     *
     * @param definition 工作流定义，包含名称、版本和阶段列表
     * @param instance 当前实例，包含业务键和运行时标识
     * @param stage 当前阶段定义
     * @param command 本次执行命令，包含操作类型、上下文和透传参数
     * @return 阶段执行结果，供日志和回放使用
     */
    SpringBatchStageExecutionResult process(WorkflowDefinitionDTO definition,
                                            WorkflowInstanceDTO instance,
                                            WorkflowStageDTO stage,
                                            WorkflowPlatformCommand command);
}
