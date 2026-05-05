package com.yss.valset.task.application.port.workflow;

import com.yss.valset.application.command.workflow.WorkflowRuntimeParamSaveCommand;
import com.yss.valset.application.dto.workflow.WorkflowRuntimeParamDTO;

import java.util.Optional;

/**
 * 工作流运行参数网关。
 */
public interface WorkflowRuntimeParamGateway {

    /**
     * 根据命名空间查询运行参数。
     *
     * @param paramNamespace 命名空间
     * @return 运行参数
     */
    Optional<WorkflowRuntimeParamDTO> findByNamespace(String paramNamespace);

    /**
     * 保存运行参数。
     *
     * @param command 保存命令
     * @return 保存结果
     */
    WorkflowRuntimeParamDTO save(WorkflowRuntimeParamSaveCommand command);
}
