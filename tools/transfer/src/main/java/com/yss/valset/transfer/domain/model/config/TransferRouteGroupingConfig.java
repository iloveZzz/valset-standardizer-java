package com.yss.valset.transfer.domain.model.config;

import com.yss.valset.transfer.domain.model.ProbeResult;
import com.yss.valset.transfer.domain.model.RecognitionContext;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 文件路由分组配置。
 */
public record TransferRouteGroupingConfig(
        TransferRouteGroupStrategy groupStrategy,
        String groupField,
        String groupExpression,
        String defaultTargetCode,
        Map<String, String> groupTargetMapping
) {

    public static TransferRouteGroupingConfig from(Map<String, Object> ruleMeta) {
        Map<String, Object> config = ruleMeta == null ? Map.of() : ruleMeta;
        return new TransferRouteGroupingConfig(
                enumValue(config, TransferConfigKeys.GROUP_STRATEGY, TransferRouteGroupStrategy.NONE),
                stringValue(config, TransferConfigKeys.GROUP_FIELD, null),
                stringValue(config, TransferConfigKeys.GROUP_EXPRESSION, null),
                stringValue(config, TransferConfigKeys.DEFAULT_TARGET_CODE, null),
                mappingValue(config.get(TransferConfigKeys.GROUP_TARGET_MAPPING))
        );
    }

    public Map<String, Object> toMetaMap() {
        Map<String, Object> meta = new LinkedHashMap<>();
        if (groupStrategy != null && groupStrategy != TransferRouteGroupStrategy.NONE) {
            meta.put(TransferConfigKeys.GROUP_STRATEGY, groupStrategy.name());
        }
        if (groupField != null && !groupField.isBlank()) {
            meta.put(TransferConfigKeys.GROUP_FIELD, groupField);
        }
        if (groupExpression != null && !groupExpression.isBlank()) {
            meta.put(TransferConfigKeys.GROUP_EXPRESSION, groupExpression);
        }
        if (defaultTargetCode != null && !defaultTargetCode.isBlank()) {
            meta.put(TransferConfigKeys.DEFAULT_TARGET_CODE, defaultTargetCode);
        }
        if (groupTargetMapping != null && !groupTargetMapping.isEmpty()) {
            meta.put(TransferConfigKeys.GROUP_TARGET_MAPPING, groupTargetMapping);
        }
        return meta;
    }

    public String resolveGroupKey(RecognitionContext context, ProbeResult probeResult) {
        if (groupStrategy == null || groupStrategy == TransferRouteGroupStrategy.NONE) {
            return null;
        }
        return switch (groupStrategy) {
            case FILE_TYPE -> firstNonBlank(
                    probeResult == null ? null : probeResult.detectedType(),
                    context == null ? null : context.mimeType()
            );
            case FILE_NAME -> context == null ? null : context.fileName();
            case MAIL_FROM -> context == null ? null : context.sender();
            case MAIL_TO -> context == null ? null : context.recipientsTo();
            case CUSTOM -> resolveCustomField(context, probeResult);
            case NONE -> null;
            default -> null;
        };
    }

    public String resolveTargetCode(String groupKey, String fallbackTargetCode) {
        if (groupKey != null && groupTargetMapping != null) {
            String mapped = groupTargetMapping.get(groupKey);
            if (mapped != null && !mapped.isBlank()) {
                return mapped;
            }
        }
        if (defaultTargetCode != null && !defaultTargetCode.isBlank()) {
            return defaultTargetCode;
        }
        return fallbackTargetCode;
    }

    private String resolveCustomField(RecognitionContext context, ProbeResult probeResult) {
        if (groupField == null || groupField.isBlank()) {
            return null;
        }
        if (context != null) {
            Object direct = switch (groupField) {
                case "sourceType" -> context.sourceType();
                case "sourceCode" -> context.sourceCode();
                case "fileName" -> context.fileName();
                case "mimeType" -> context.mimeType();
                case "sender" -> context.sender();
                case "recipientsTo" -> context.recipientsTo();
                case "recipientsCc" -> context.recipientsCc();
                case "recipientsBcc" -> context.recipientsBcc();
                case "subject" -> context.subject();
                case "path" -> context.path();
                default -> context.attributes() == null ? null : context.attributes().get(groupField);
            };
            if (direct != null) {
                return String.valueOf(direct);
            }
        }
        if (probeResult != null && probeResult.attributes() != null) {
            Object raw = probeResult.attributes().get(groupField);
            if (raw != null) {
                return String.valueOf(raw);
            }
        }
        return null;
    }

    private static Map<String, String> mappingValue(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                result.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
        }
        return result;
    }

    private static <E extends Enum<E>> E enumValue(Map<String, Object> config, String key, E defaultValue) {
        Object raw = config.get(key);
        if (raw == null || String.valueOf(raw).isBlank()) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(defaultValue.getDeclaringClass(), String.valueOf(raw));
        } catch (IllegalArgumentException ex) {
            return defaultValue;
        }
    }

    private static String stringValue(Map<String, Object> config, String key, String defaultValue) {
        Object raw = config.get(key);
        return raw == null ? defaultValue : String.valueOf(raw);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
