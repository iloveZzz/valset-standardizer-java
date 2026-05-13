package com.yss.valset.transfer.domain.model;

import java.util.Map;

/**
 * 规则执行上下文。
 */
public record RuleContext(
        RecognitionContext recognitionContext,
        ProbeResult probeResult,
        Map<String, Object> variables
) {
}
