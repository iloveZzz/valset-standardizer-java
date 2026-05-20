package com.yss.valset.application.command.workflow;

import lombok.Data;

/**
 * 工作流运行参数保存命令。
 */
@Data
public class WorkflowRuntimeParamSaveCommand {

    private String paramNamespace;

    private Boolean skipExcelStyleParsing;

    private Boolean enableMatchProcess;

    private String description;
}
