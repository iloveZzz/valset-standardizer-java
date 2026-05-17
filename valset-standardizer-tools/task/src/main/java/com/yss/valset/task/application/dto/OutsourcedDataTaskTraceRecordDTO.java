package com.yss.valset.task.application.dto;

import lombok.Data;

import java.util.Map;

/**
 * 估值解析链路节点记录。
 */
@Data
public class OutsourcedDataTaskTraceRecordDTO implements java.io.Serializable {

    private String id;

    private String type;

    private String name;

    private String status;

    private String statusName;

    private String upstreamId;

    private String downstreamId;

    private String startedAt;

    private String endedAt;

    private Long durationMs;

    private String errorCode;

    private String errorMessage;

    private String inputSummary;

    private String outputSummary;

    private String logRef;

    private Map<String, Object> attributes;
}
