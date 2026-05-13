package com.yss.valset.transfer.infrastructure.convertor;

import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.domain.model.TargetType;
import com.yss.valset.transfer.domain.model.TransferStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 文件收发分拣模块的 MapStruct 公共转换能力。
 */
public interface TransferMapstructSupport {

    default String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }

    default TransferStatus statusOf(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return TransferStatus.valueOf(value);
    }

    default TargetType targetTypeOf(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return TargetType.valueOf(value);
    }

    default SourceType sourceTypeOf(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return SourceType.valueOf(value);
    }

    default Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }

    default LocalDate toLocalDate(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.toLocalDate();
    }

    default LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    default LocalDateTime toLocalDateTime(LocalDate localDate) {
        return localDate == null ? null : localDate.atStartOfDay();
    }

    default String stringValue(Map<String, Object> source, String key) {
        if (source == null) {
            return null;
        }
        Object value = source.get(key);
        return value == null ? null : String.valueOf(value);
    }

    default String stringValue(String value) {
        return value;
    }

    default Long longValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.valueOf(value);
    }

    default String stringValue(Long value) {
        return value == null ? null : String.valueOf(value);
    }

    default Map<String, Object> safeMap(Map<String, Object> source) {
        return source == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source);
    }
}
