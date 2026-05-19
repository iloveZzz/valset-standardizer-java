package com.yss.valset.qlexpress.application.command;

import lombok.Data;

import java.util.Map;

/**
 * QLExpress 自定义函数在线调试命令。
 */
@Data
public class QlexpressFunctionDebugCommand {

    private String functionId;

    private String functionName;

    private String scriptBody;

    private String debugExpression;

    private String runnerScope;

    private Map<String, Object> context;
}
