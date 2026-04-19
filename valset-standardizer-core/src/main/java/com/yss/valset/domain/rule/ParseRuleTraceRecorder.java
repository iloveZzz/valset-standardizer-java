package com.yss.valset.domain.rule;

/**
 * 解析规则追踪记录器。
 */
public interface ParseRuleTraceRecorder {

    /**
     * 记录一条追踪。
     */
    void record(ParseRuleTraceRecord record);
}
