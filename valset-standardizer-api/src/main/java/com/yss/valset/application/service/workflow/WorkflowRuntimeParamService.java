package com.yss.valset.application.service.workflow;

import com.yss.valset.application.command.workflow.WorkflowRuntimeParamSaveCommand;
import com.yss.valset.application.dto.workflow.WorkflowRuntimeParamDTO;

/**
 * 工作流运行参数服务。
 */
public interface WorkflowRuntimeParamService {

    /**
     * 获取当前命名空间的运行参数。
     *
     * @return 运行参数
     */
    WorkflowRuntimeParamDTO getRuntimeParam();

    /**
     * 保存当前命名空间的运行参数。
     *
     * @param command 保存命令
     * @return 保存后的运行参数
     */
    WorkflowRuntimeParamDTO saveRuntimeParam(WorkflowRuntimeParamSaveCommand command);

    /**
     * 刷新本地缓存。
     */
    void refresh();

    /**
     * 是否启用匹配流程。
     *
     * @return 是否启用
     */
    boolean enableMatchProcess();

    /**
     * 是否跳过 Excel 样式解析。
     *
     * @return 是否跳过
     */
    boolean skipExcelStyleParsing();

    /**
     * 是否持久化标准化后的 DWD 明细。
     *
     * @return 是否持久化
     */
    boolean persistStandardizedDwdDetails();
}
