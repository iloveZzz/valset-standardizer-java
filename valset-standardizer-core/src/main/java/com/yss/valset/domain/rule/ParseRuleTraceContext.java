package com.yss.valset.domain.rule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 解析规则追踪上下文。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParseRuleTraceContext {

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
     * 是否开启追踪。
     */
    private Boolean traceEnabled;

    /**
     * 追踪范围。
     */
    private String traceScope;
}
