package com.yss.subjectmatch.extract.repository.convertor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.subjectmatch.domain.model.MatchCandidate;
import com.yss.subjectmatch.domain.model.HeaderColumnMeta;
import com.yss.subjectmatch.domain.model.MetricRecord;
import com.yss.subjectmatch.domain.model.SubjectRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 通用 JSON 序列化映射器，用于 MapStruct 进行复杂对象在持久化时的转换。
 */
@Component
@RequiredArgsConstructor
public class JsonStringMapper {

    private final ObjectMapper objectMapper;

    /**
     * 将对象格式化为 JSON 字符串。
     * @param value 任意内部对象
     * @return 序列化后的 JSON 字符串
     */
    public String toJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof CharSequence charSequence) {
            return normalizeString(charSequence.toString());
        }
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize object payload", e);
        }
    }

    /**
     * JSON 反序列化：Map<String, String>
     */
    public Map<String, String> toMapStringString(String value) {
        return read(value, new TypeReference<Map<String, String>>() {});
    }

    /**
     * JSON 反序列化：List<String>
     */
    public List<String> toListString(String value) {
        return read(value, new TypeReference<List<String>>() {});
    }

    /**
     * JSON 反序列化：List<List<String>>
     */
    public List<List<String>> toListListString(String value) {
        return read(value, new TypeReference<List<List<String>>>() {});
    }

    /**
     * JSON 反序列化：List<HeaderColumnMeta>
     */
    public List<HeaderColumnMeta> toListHeaderColumnMeta(String value) {
        return read(value, new TypeReference<List<HeaderColumnMeta>>() {});
    }

    /**
     * JSON 反序列化：List<SubjectRecord>
     */
    public List<SubjectRecord> toListSubjectRecord(String value) {
        return read(value, new TypeReference<List<SubjectRecord>>() {});
    }

    /**
     * JSON 反序列化：List<MetricRecord>
     */
    public List<MetricRecord> toListMetricRecord(String value) {
        return read(value, new TypeReference<List<MetricRecord>>() {});
    }

    /**
     * JSON 反序列化：List<MatchCandidate>
     */
    public List<MatchCandidate> toListMatchCandidate(String value) {
        return read(value, new TypeReference<List<MatchCandidate>>() {});
    }

    private <T> T read(String value, TypeReference<T> typeReference) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(value, typeReference);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize generic payload", e);
        }
    }

    private String normalizeString(String value) {
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }
}
