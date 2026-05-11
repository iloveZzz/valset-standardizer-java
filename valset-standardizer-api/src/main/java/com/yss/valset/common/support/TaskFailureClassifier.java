package com.yss.valset.common.support;

import com.yss.valset.common.exception.BizException;

import java.util.Locale;

/**
 * 任务失败原因分类器。
 */
public final class TaskFailureClassifier {

    public static final String UNKNOWN = "UNKNOWN_ERROR";
    public static final String FILE_ACCESS_ERROR = "FILE_ACCESS_ERROR";
    public static final String UNSUPPORTED_DATA_SOURCE = "UNSUPPORTED_DATA_SOURCE";
    public static final String INVALID_WORKBOOK = "INVALID_WORKBOOK";
    public static final String HEADER_MISSING = "HEADER_MISSING";
    public static final String TEMPLATE_NOT_MATCH = "TEMPLATE_NOT_MATCH";
    public static final String DATA_ROWS_NOT_FOUND = "DATA_ROWS_NOT_FOUND";
    public static final String RAW_ROW_JSON_ERROR = "RAW_ROW_JSON_ERROR";
    public static final String RULE_EXECUTION_ERROR = "RULE_EXECUTION_ERROR";
    public static final String MATCH_ENGINE_ERROR = "MATCH_ENGINE_ERROR";

    private TaskFailureClassifier() {
    }

    public static String classify(Throwable throwable) {
        if (throwable == null) {
            return UNKNOWN;
        }
        Throwable rootCause = rootCause(throwable);
        String code = codeFromThrowable(rootCause);
        if (code != null) {
            return code;
        }
        code = codeFromThrowable(throwable);
        if (code != null) {
            return code;
        }
        String message = firstMeaningfulMessage(rootCause, throwable);
        if (message == null) {
            return UNKNOWN;
        }
        String normalized = message.toLowerCase(Locale.ROOT);
        if (normalized.contains("未识别到必选表头") || normalized.contains("无法解析这张表")) {
            return HEADER_MISSING;
        }
        if (normalized.contains("在表头下方未找到科目数据") || normalized.contains("未找到科目数据")) {
            return DATA_ROWS_NOT_FOUND;
        }
        if (normalized.contains("无法解析原始行数据 json")) {
            return RAW_ROW_JSON_ERROR;
        }
        if (normalized.contains("qlexpress 规则执行失败")
                || normalized.contains("qlexpress 表头映射执行失败")
                || normalized.contains("表头映射执行失败")) {
            return RULE_EXECUTION_ERROR;
        }
        if (normalized.contains("failed to execute parse task")
                || normalized.contains("failed to execute match task")
                || normalized.contains("failed to execute mapping evaluation task")) {
            return TEMPLATE_NOT_MATCH;
        }
        return UNKNOWN;
    }

    public static String resolveReadableMessage(Throwable throwable) {
        String rootMessage = rootCauseMessage(throwable);
        if (rootMessage != null && !rootMessage.isBlank()) {
            return rootMessage.trim();
        }
        return throwable == null ? null : throwable.getMessage();
    }

    public static String rootCauseMessage(Throwable throwable) {
        Throwable root = rootCause(throwable);
        return root == null ? null : root.getMessage();
    }

    private static Throwable rootCause(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private static String codeFromThrowable(Throwable throwable) {
        if (throwable instanceof BizException bizException) {
            String code = bizException.getCode();
            if (code == null || code.isBlank()) {
                return null;
            }
            String normalized = code.trim().toUpperCase(Locale.ROOT);
            return switch (normalized) {
                case "FILE_ACCESS_ERROR" -> FILE_ACCESS_ERROR;
                case "UNSUPPORTED_DATA_SOURCE" -> UNSUPPORTED_DATA_SOURCE;
                case "INVALID_WORKBOOK" -> INVALID_WORKBOOK;
                case "MATCH_ENGINE_ERROR" -> MATCH_ENGINE_ERROR;
                default -> normalized;
            };
        }
        return null;
    }

    private static String firstMeaningfulMessage(Throwable rootCause, Throwable throwable) {
        if (rootCause != null && rootCause.getMessage() != null && !rootCause.getMessage().isBlank()) {
            return rootCause.getMessage();
        }
        if (throwable != null && throwable.getMessage() != null && !throwable.getMessage().isBlank()) {
            return throwable.getMessage();
        }
        return null;
    }
}
