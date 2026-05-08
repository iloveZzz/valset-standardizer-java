package com.yss.valset.application.dto.workflow;

import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工作流上下文载荷工具。
 *
 * <p>
 * 负责在“扁平参数”与“分组信封”两种结构之间互相转换，避免上层服务重复拼装 JSON。
 * </p>
 */
public final class WorkflowContextPayloadSupport {

    private WorkflowContextPayloadSupport() {
    }

    public static Map<String, Object> buildEnvelope(Map<String, Object> commonContext, Map<String, Object> businessContext) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        if (commonContext != null && !commonContext.isEmpty()) {
            envelope.put(WorkflowContextKeys.COMMON_CONTEXT, new LinkedHashMap<>(commonContext));
        }
        if (businessContext != null && !businessContext.isEmpty()) {
            envelope.put(WorkflowContextKeys.BUSINESS_CONTEXT, new LinkedHashMap<>(businessContext));
        }
        return envelope;
    }

    /**
     * 将信封结构展开成执行器可直接读取的键值对。
     *
     * <p>
     * 先读取公共上下文和业务上下文分组，再把顶层其它字段补充进去。
     * </p>
     */
    public static Map<String, Object> flattenEnvelope(Map<?, ?> payload) {
        Map<String, Object> flattened = new LinkedHashMap<>();
        if (payload == null || payload.isEmpty()) {
            return flattened;
        }
        Object commonContext = payload.get(WorkflowContextKeys.COMMON_CONTEXT);
        if (commonContext instanceof Map<?, ?> commonMap) {
            commonMap.forEach((key, value) -> flattened.put(String.valueOf(key), value));
        }
        Object businessContext = payload.get(WorkflowContextKeys.BUSINESS_CONTEXT);
        if (businessContext instanceof Map<?, ?> businessMap) {
            businessMap.forEach((key, value) -> flattened.put(String.valueOf(key), value));
        }
        payload.forEach((key, value) -> {
            String textKey = String.valueOf(key);
            if (!WorkflowContextKeys.COMMON_CONTEXT.equals(textKey)
                    && !WorkflowContextKeys.BUSINESS_CONTEXT.equals(textKey)) {
                flattened.putIfAbsent(textKey, value);
            }
        });
        return flattened;
    }

    /**
     * 提取指定分组的参数。
     */
    public static Map<String, Object> extractSection(Map<?, ?> payload, String sectionKey) {
        Map<String, Object> section = new LinkedHashMap<>();
        if (payload == null || !StringUtils.hasText(sectionKey)) {
            return section;
        }
        Object value = payload.get(sectionKey);
        if (value instanceof Map<?, ?> map) {
            map.forEach((key, nestedValue) -> section.put(String.valueOf(key), nestedValue));
        }
        return section;
    }
}
