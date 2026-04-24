package com.yss.valset.transfer.infrastructure.convertor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 文件收发分拣模块的 JSON 映射工具。
 */
@Component
@RequiredArgsConstructor
public class TransferJsonMapper {

    private final ObjectMapper objectMapper;
    private final TransferSecretCodec transferSecretCodec;

    public String toJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> map) {
            value = transferSecretCodec.encryptMap(castMap(map));
        }
        if (value instanceof CharSequence charSequence) {
            return charSequence.toString();
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON 序列化失败", e);
        }
    }

    public Map<String, Object> toMap(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> map = objectMapper.readValue(value, new TypeReference<Map<String, Object>>() {});
            return transferSecretCodec.decryptMap(map);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON 反序列化失败", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Map<?, ?> source) {
        Map<String, Object> target = new java.util.LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            target.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return target;
    }
}
