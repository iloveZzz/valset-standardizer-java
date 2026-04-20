package com.yss.valset.transfer.domain.model;

import java.util.Map;

/**
 * 识别上下文，用于向插件和脚本规则暴露受控字段。
 */
public record RecognitionContext(
        SourceType sourceType,
        String sourceCode,
        String fileName,
        String mimeType,
        Long fileSize,
        String sender,
        String subject,
        String path,
        Map<String, Object> attributes
) {
}
