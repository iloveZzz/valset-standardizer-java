package com.yss.valset.workflow.model;

/**
 * 通用 ETL 错误码。
 */
public enum WorkflowErrorCode {
    WORKFLOW_NOT_FOUND("WORKFLOW_NOT_FOUND", "未找到工作流定义"),
    WORKFLOW_VERSION_NOT_FOUND("WORKFLOW_VERSION_NOT_FOUND", "未找到工作流版本"),
    INSTANCE_NOT_FOUND("INSTANCE_NOT_FOUND", "未找到任务实例"),
    ADAPTER_NOT_FOUND("ADAPTER_NOT_FOUND", "未找到适配器"),
    ENGINE_TYPE_NOT_SUPPORTED("ENGINE_TYPE_NOT_SUPPORTED", "当前平台类型不支持"),
    INVALID_WORKFLOW_DEFINITION("INVALID_WORKFLOW_DEFINITION", "工作流定义不合法"),
    INVALID_ENGINE_BINDING("INVALID_ENGINE_BINDING", "引擎绑定不合法"),
    INVALID_TRIGGER_REQUEST("INVALID_TRIGGER_REQUEST", "触发请求不合法"),
    UNSUPPORTED_OPERATION("UNSUPPORTED_OPERATION", "当前平台不支持该操作");

    private final String code;
    private final String message;

    WorkflowErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
