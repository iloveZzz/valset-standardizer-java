package com.yss.valset.extract.standardization.mapping;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 暴露给 QLExpress 表头映射脚本的受控函数对象。
 */
@Component
public class QlexpressHeaderFunctionFacade {

    public boolean hasCandidate(Object candidate) {
        return candidate != null;
    }

    public boolean headerContainsAnySegment(Object headerText, Object segments) {
        return HeaderMappingRuleSupport.headerContainsAnySegment(asString(headerText, ""), asStringList(segments));
    }

    public boolean headerContainsAllSegments(Object headerText, Object segments) {
        return HeaderMappingRuleSupport.headerContainsAllSegments(asString(headerText, ""), asStringList(segments));
    }

    private String asString(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return text.trim().isEmpty() ? defaultValue : text;
    }

    private List<String> asStringList(Object value) {
        if (value instanceof List<?>) {
            return ((List<?>) value).stream()
                    .map(item -> item == null ? "" : String.valueOf(item))
                    .collect(java.util.stream.Collectors.toList());
        }
        if (value == null) {
            return java.util.Arrays.asList();
        }
        return java.util.Arrays.asList(String.valueOf(value));
    }
}
