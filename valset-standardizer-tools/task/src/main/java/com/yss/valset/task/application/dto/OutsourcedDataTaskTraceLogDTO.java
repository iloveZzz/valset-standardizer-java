package com.yss.valset.task.application.dto;

import lombok.Data;

/**
 * 估值解析链路日志摘要。
 */
@Data
public class OutsourcedDataTaskTraceLogDTO implements java.io.Serializable {

    private String logId;

    private String nodeType;

    private String nodeId;

    private String level;

    private String message;

    private String loggedAt;

    private String logRef;
}
