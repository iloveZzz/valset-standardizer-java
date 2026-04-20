package com.yss.valset.transfer.infrastructure.plugin;

import com.yss.valset.transfer.domain.model.ProbeResult;
import com.yss.valset.transfer.domain.model.RecognitionContext;
import com.yss.valset.transfer.domain.plugin.FileProbePlugin;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 默认文件探测插件。
 */
@Component
public class DefaultFileProbePlugin implements FileProbePlugin {

    @Override
    public String type() {
        return "DEFAULT";
    }

    @Override
    public int priority() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean supports(RecognitionContext context) {
        return true;
    }

    @Override
    public ProbeResult probe(RecognitionContext context) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        if (context.attributes() != null) {
            attributes.putAll(context.attributes());
        }
        attributes.putIfAbsent("fileName", context.fileName());
        attributes.putIfAbsent("mimeType", context.mimeType());
        attributes.putIfAbsent("sourceType", context.sourceType() == null ? null : context.sourceType().name());
        attributes.putIfAbsent("sender", context.sender());
        attributes.putIfAbsent("subject", context.subject());
        attributes.putIfAbsent("mailId", context.mailId());
        attributes.putIfAbsent("mailProtocol", context.mailProtocol());
        attributes.putIfAbsent("mailFolder", context.mailFolder());
        return new ProbeResult(true, detectType(context), attributes);
    }

    private String detectType(RecognitionContext context) {
        if (context == null) {
            return "UNKNOWN";
        }
        if (context.mailId() != null && !context.mailId().isBlank()) {
            return "EMAIL_ATTACHMENT";
        }
        if (context.mimeType() != null && !context.mimeType().isBlank()) {
            return context.mimeType();
        }
        return context.fileName() == null ? "UNKNOWN" : "FILE";
    }
}
