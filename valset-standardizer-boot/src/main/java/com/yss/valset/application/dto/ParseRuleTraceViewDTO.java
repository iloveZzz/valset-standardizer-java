package com.yss.valset.application.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 解析规则追踪视图。
 */
@Data
@Builder
public class ParseRuleTraceViewDTO {
    private Long id;
    private String traceScope;
    private String traceType;
    private Long profileId;
    private String profileCode;
    private String version;
    private Long fileId;
    private Long taskId;
    private String stepName;
    private String expression;
    private String inputJson;
    private String outputJson;
    private Boolean success;
    private Long costMs;
    private String errorMessage;
    private LocalDateTime traceTime;
}
