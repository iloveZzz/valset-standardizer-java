package com.yss.valset.workflow.infrastructure.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工作流 JSON 编解码工具。
 */
@Component
public class WorkflowJsonCodec {

    private final ObjectMapper objectMapper;

    public WorkflowJsonCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy().registerModule(new JavaTimeModule());
    }

    public String toJson(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("工作流 JSON 序列化失败", ex);
        }
    }

    public Map<String, Object> toMap(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Object> value = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            return value == null ? new LinkedHashMap<>(): new LinkedHashMap<>(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("工作流 JSON 反序列化失败", ex);
        }
    }
}
