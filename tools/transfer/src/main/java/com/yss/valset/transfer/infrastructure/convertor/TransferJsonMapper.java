package com.yss.valset.transfer.infrastructure.convertor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * 文件收发分拣模块的 JSON 映射工具。
 */
@Component
@RequiredArgsConstructor
public class TransferJsonMapper {

    private final ObjectMapper objectMapper;
    private final TransferSecretCodec transferSecretCodec;

    @Named("transferToJson")
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

    @Named("transferToCompactJson")
    public String toCompactJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> compactMap = compactMap(castMap(map));
            if (compactMap.isEmpty()) {
                return null;
            }
            return toJson(compactMap);
        }
        JsonNode compactNode = compactNode(objectMapper.valueToTree(value));
        if (compactNode == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(compactNode);
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

    private Map<String, Object> compactMap(Map<String, Object> source) {
        Map<String, Object> compact = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            Object value = compactValue(entry.getValue());
            if (value != null) {
                compact.put(entry.getKey(), value);
            }
        }
        return compact;
    }

    private Object compactValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof CharSequence charSequence) {
            String text = charSequence.toString().trim();
            return text.isEmpty() ? null : text;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> compactMap = compactMap(castMap(map));
            return compactMap.isEmpty() ? null : compactMap;
        }
        if (value instanceof Collection<?> collection) {
            ArrayList<Object> compactItems = new ArrayList<>();
            for (Object item : collection) {
                Object compactItem = compactValue(item);
                if (compactItem != null) {
                    compactItems.add(compactItem);
                }
            }
            return compactItems.isEmpty() ? null : compactItems;
        }
        if (value.getClass().isArray()) {
            ArrayList<Object> compactItems = new ArrayList<>();
            int length = Array.getLength(value);
            for (int index = 0; index < length; index++) {
                Object compactItem = compactValue(Array.get(value, index));
                if (compactItem != null) {
                    compactItems.add(compactItem);
                }
            }
            return compactItems.isEmpty() ? null : compactItems;
        }
        return value;
    }

    private JsonNode compactNode(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (node.isTextual()) {
            return node.asText().trim().isEmpty() ? null : node;
        }
        if (node.isArray()) {
            ArrayNode compactArray = objectMapper.createArrayNode();
            for (JsonNode item : node) {
                JsonNode compactItem = compactNode(item);
                if (compactItem != null) {
                    compactArray.add(compactItem);
                }
            }
            return compactArray.isEmpty() ? null : compactArray;
        }
        if (node.isObject()) {
            ObjectNode compactObject = objectMapper.createObjectNode();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                JsonNode compactField = compactNode(field.getValue());
                if (compactField != null) {
                    compactObject.set(field.getKey(), compactField);
                }
            }
            return compactObject.isEmpty() ? null : compactObject;
        }
        return node;
    }
}
