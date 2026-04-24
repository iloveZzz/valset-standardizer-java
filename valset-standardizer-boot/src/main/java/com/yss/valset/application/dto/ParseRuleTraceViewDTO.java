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
    /**
     * 追踪主键。
     */
    private String id;
    /**
     * 追踪范围。
     */
    private String traceScope;
    /**
     * 追踪类型。
     */
    private String traceType;
    /**
     * 模板主键。
     */
    private String profileId;
    /**
     * 模板编码。
     */
    private String profileCode;
    /**
     * 模板版本。
     */
    private String version;
    /**
     * 文件主键。
     */
    private String fileId;
    /**
     * 任务主键。
     */
    private String taskId;
    /**
     * 规则步骤名称。
     */
    private String stepName;
    /**
     * 表达式内容。
     */
    private String expression;
    /**
     * 输入 JSON。
     */
    private String inputJson;
    /**
     * 输出 JSON。
     */
    private String outputJson;
    /**
     * 是否成功。
     */
    private Boolean success;
    /**
     * 耗时，单位毫秒。
     */
    private String costMs;
    /**
     * 错误信息。
     */
    private String errorMessage;
    /**
     * 追踪时间。
     */
    private LocalDateTime traceTime;
}
