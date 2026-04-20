package com.yss.valset.transfer.domain.model;

import java.util.Map;

/**
 * 探测插件返回的文件特征结果。
 */
public record ProbeResult(
        boolean detected,
        String detectedType,
        Map<String, Object> attributes
) {
}
