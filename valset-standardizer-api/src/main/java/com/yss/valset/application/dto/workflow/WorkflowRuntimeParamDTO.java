package com.yss.valset.application.dto.workflow;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工作流运行参数视图。
 */
@Data
public class WorkflowRuntimeParamDTO implements java.io.Serializable {

    private String runtimeParamId;
    private String paramNamespace;
    private Boolean skipExcelStyleParsing;
    private Boolean enableMatchProcess;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
