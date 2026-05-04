package com.yss.valset.task.application.command.workflow;

import lombok.Data;

/**
 * 工作流配置查询命令。
 */
@Data
public class WorkflowConfigQueryCommand {

    private String workflowCode;
    private String workflowName;
    private String businessType;
    private String engineType;
    private String status;
    private Boolean enabled;
    private Integer pageIndex;
    private Integer pageSize;
}
