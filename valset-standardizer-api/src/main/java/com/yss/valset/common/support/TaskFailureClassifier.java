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
        String message = resolveMostSpecificMessage(throwable);
        if (message != null && !message.trim().isEmpty()) {
            return message.trim();
        }
        return throwable == null ? null : throwable.getMessage();
    }

    public static String resolveMostSpecificMessage(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        String fallback = null;
        String matched = null;
        Throwable current = throwable;
        int depth = 0;
        while (current != null && depth++ < 32) {
            String message = normalizeMessage(current.getMessage());
            if (message != null) {
                if (fallback == null) {
                    fallback = message;
                }
                if (!isWrapperMessage(message)) {
                    matched = message;
                }
            }
            Throwable cause = current.getCause();
            if (cause == null || cause == current) {
                break;
            }
            current = cause;
        }
        return matched == null ? fallback : matched;
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
        if (throwable instanceof BizException) {
            BizException bizException = (BizException) throwable;
            String code = bizException.getCode();
            if (code == null || code.trim().isEmpty()) {
                return null;
            }
            String normalized = code.trim().toUpperCase(Locale.ROOT);
            switch (normalized) {
                case "FILE_ACCESS_ERROR":
                    return FILE_ACCESS_ERROR;
                case "UNSUPPORTED_DATA_SOURCE":
                    return UNSUPPORTED_DATA_SOURCE;
                case "INVALID_WORKBOOK":
                    return INVALID_WORKBOOK;
                case "MATCH_ENGINE_ERROR":
                    return MATCH_ENGINE_ERROR;
                default:
                    return normalized;
            }
        }
        return null;
    }

    private static String normalizeMessage(String message) {
        if (message == null) {
            return null;
        }
        String text = message.trim();
        return text.isEmpty() ? null : text;
    }

    private static boolean isWrapperMessage(String message) {
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.startsWith("批量任务 文件解析阶段失败")
                || normalized.startsWith("批量任务 结构标准化阶段失败")
                || normalized.startsWith("批量任务 标准表落地阶段失败")
                || normalized.startsWith("批量任务 作业执行失败")
                || normalized.startsWith("failed to execute parse task")
                || normalized.startsWith("failed to execute match task")
                || normalized.startsWith("failed to execute mapping evaluation task")
                || normalized.startsWith("failed to execute step")
                || normalized.startsWith("failed to execute job");
    }

    private static String firstMeaningfulMessage(Throwable rootCause, Throwable throwable) {
        if (rootCause != null && rootCause.getMessage() != null && !rootCause.getMessage().trim().isEmpty()) {
            return rootCause.getMessage();
        }
        if (throwable != null && throwable.getMessage() != null && !throwable.getMessage().trim().isEmpty()) {
            return throwable.getMessage();
        }
        return null;
    }
}
