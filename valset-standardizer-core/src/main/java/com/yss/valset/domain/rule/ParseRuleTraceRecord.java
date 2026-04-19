package com.yss.valset.domain.rule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 解析规则追踪记录。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParseRuleTraceRecord {

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
    private Long profileId;

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
    private Long fileId;

    /**
     * 任务主键。
     */
    private Long taskId;

    /**
     * 步骤名称。
     */
    private String stepName;

    /**
     * 表达式文本。
     */
    private String expression;

    /**
     * 输入内容。
     */
    private String inputJson;

    /**
     * 输出内容。
     */
    private String outputJson;

    /**
     * 是否成功。
     */
    private Boolean success;

    /**
     * 耗时毫秒。
     */
    private Long costMs;

    /**
     * 错误信息。
     */
    private String errorMessage;

    /**
     * 追踪时间。
     */
    private LocalDateTime traceTime;
}
