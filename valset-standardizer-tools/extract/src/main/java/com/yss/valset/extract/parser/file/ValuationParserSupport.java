package com.yss.valset.extract.parser.file;

import com.yss.valset.common.support.ExcelParsingSupport;
import com.yss.valset.domain.rule.ParseRuleType;
import com.yss.valset.extract.rule.ParseRuleStepDescriptor;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 估值文件解析过程中的 Java 辅助逻辑。
 *
 * <p>CSV 与工作簿解析器只负责文件读取和流程编排，文件读取后的文本清洗、表头定位、
 * 默认抽取值归一化和规则错误策略统一收口到这里。业务行判断函数由 QLExpress 函数管理维护，
 * 这里不再作为脚本函数门面。</p>
 */
final class ValuationParserSupport {

    private static final String ERROR_POLICY_FAIL_FAST = "FAIL_FAST";
    private static final String ERROR_POLICY_SKIP_ROW = "SKIP_ROW";
    private static final String ERROR_POLICY_FALLBACK_DEFAULT = "FALLBACK_DEFAULT";

    private ValuationParserSupport() {
    }

    static List<String> toRowTexts(List<Object> rowValues) {
        if (rowValues == null) {
            return java.util.Arrays.asList();
        }
        List<String> texts = new ArrayList<>(rowValues.size());
        for (Object value : rowValues) {
            texts.add(ExcelParsingSupport.normalizeText(value));
        }
        return texts;
    }

    static boolean isBlankRow(List<?> rowValues) {
        return rowValues == null || rowValues.stream()
                .allMatch(value -> value == null || ExcelParsingSupport.normalizeText(value).trim().isEmpty());
    }

    static boolean rowContainsAll(List<Object> rowValues, List<String> keywords) {
        if (CollectionUtils.isEmpty(rowValues) || CollectionUtils.isEmpty(keywords)) {
            return false;
        }
        for (String keyword : keywords) {
            if (keyword == null || keyword.trim().isEmpty()) {
                continue;
            }
            boolean matched = false;
            for (Object value : rowValues) {
                String text = ExcelParsingSupport.normalizeText(value).trim();
                if (text.isEmpty()) {
                    continue;
                }
                String normalizedKeyword = keyword.trim();
                if (text.equals(normalizedKeyword) || text.contains(normalizedKeyword)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return false;
            }
        }
        return true;
    }

    static int resolveSubjectCodeColumnIndex(List<Object> headerRowValues) {
        if (headerRowValues == null || headerRowValues.isEmpty()) {
            throw new IllegalArgumentException("未识别到科目代码列表头，无法定位数据区。");
        }
        for (int columnIndex = 0; columnIndex < headerRowValues.size(); columnIndex++) {
            String headerText = ExcelParsingSupport.textAt(headerRowValues, columnIndex);
            if (headerMatches(headerText, "科目代码") || headerText.contains("科目代码")) {
                return columnIndex;
            }
        }
        throw new IllegalArgumentException("未识别到科目代码列表头，无法定位数据区。");
    }

    static Map<String, Object> buildDataStartRuleContext(List<Object> rowValues, int rowIndex, String subjectCodePattern) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("row", rowValues);
        context.put("rowIndex", rowIndex);
        context.put("subjectCodePattern", subjectCodePattern);
        return context;
    }

    static Integer resolveHeaderIndex(List<String> headers, Map<String, Integer> headerIndex, String headerName, String... excludedTokens) {
        Integer exactIndex = headerIndex.get(headerName);
        if (exactIndex != null) {
            return exactIndex;
        }
        for (int index = 0; index < headers.size(); index++) {
            String header = headers.get(index);
            if (headerMatches(header, headerName) && !containsAnySegment(header, excludedTokens)) {
                return index;
            }
        }
        return null;
    }

    static boolean headerMatches(String header, String headerName) {
        if (header == null || header.trim().isEmpty() || headerName == null || headerName.trim().isEmpty()) {
            return false;
        }
        if (header.equals(headerName)) {
            return true;
        }
        String[] segments = header.split("\\|");
        for (String segment : segments) {
            if (headerName.equals(segment.trim())) {
                return true;
            }
        }
        return header.startsWith(headerName + "|")
                || header.endsWith("|" + headerName)
                || header.contains("|" + headerName + "|");
    }

    static boolean containsAnySegment(String header, String... excludedTokens) {
        if (header == null || header.trim().isEmpty() || excludedTokens == null || excludedTokens.length == 0) {
            return false;
        }
        String[] segments = header.split("\\|");
        for (String excludedToken : excludedTokens) {
            if (excludedToken == null || excludedToken.trim().isEmpty()) {
                continue;
            }
            for (String segment : segments) {
                if (excludedToken.equals(segment.trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    static String findNextMeaningfulText(List<String> rowTexts, int startIndex) {
        if (rowTexts == null || startIndex < 0 || startIndex >= rowTexts.size()) {
            return "";
        }
        for (int index = startIndex; index < rowTexts.size(); index++) {
            String text = rowTexts.get(index);
            if (text != null && !text.trim().isEmpty()) {
                return text;
            }
        }
        return "";
    }

    static String stripTrailingPunctuation(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .replaceAll("[：:]+$", "")
                .trim();
    }

    static Object findFirstValueAfterLabel(List<Object> rowValues, int labelIndex) {
        if (rowValues == null || rowValues.isEmpty() || labelIndex < 0) {
            return "";
        }
        for (int index = labelIndex + 1; index < rowValues.size(); index++) {
            Object value = ExcelParsingSupport.valueAt(rowValues, index);
            String text = ExcelParsingSupport.textAt(rowValues, index);
            if (text.trim().isEmpty() || "-".equals(text)) {
                continue;
            }
            return value;
        }
        return "";
    }

    static Object findFirstNumericValueAfterLabel(List<Object> rowValues, int labelIndex) {
        if (rowValues == null || rowValues.isEmpty() || labelIndex < 0) {
            return "";
        }
        for (int index = labelIndex + 1; index < rowValues.size(); index++) {
            Object value = ExcelParsingSupport.valueAt(rowValues, index);
            if (ExcelParsingSupport.normalizeNumber(value) != null) {
                return value;
            }
        }
        return "";
    }

    static Object normalizeMetricValue(Object rawValue) {
        BigDecimal number = ExcelParsingSupport.normalizeNumber(rawValue);
        return number != null ? number : ExcelParsingSupport.normalizeText(rawValue);
    }

    static String toTextValue(Object rawValue) {
        Object normalizedValue = normalizeMetricValue(rawValue);
        if (normalizedValue instanceof BigDecimal) {
            BigDecimal decimal = (BigDecimal) normalizedValue;
            return decimal.stripTrailingZeros().toPlainString();
        }
        return normalizedValue == null ? "" : String.valueOf(normalizedValue);
    }

    static Object firstNonBlank(Object... values) {
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            if (value instanceof String && ((String) value).trim().isEmpty()) {
                continue;
            }
            return value;
        }
        return null;
    }

    static boolean isFailFast(ParseRuleStepDescriptor rule) {
        return ERROR_POLICY_FAIL_FAST.equalsIgnoreCase(errorPolicy(rule));
    }

    static boolean isSkipRow(ParseRuleStepDescriptor rule) {
        return ERROR_POLICY_SKIP_ROW.equalsIgnoreCase(errorPolicy(rule));
    }

    static String errorPolicy(ParseRuleStepDescriptor rule) {
        String policy = rule == null ? null : rule.getErrorPolicy();
        return policy == null || policy.trim().isEmpty() ? ERROR_POLICY_FALLBACK_DEFAULT : policy.trim();
    }

    static String profileCode(ParseRuleStepDescriptor rule) {
        return rule == null ? null : rule.getProfileCode();
    }

    static String version(ParseRuleStepDescriptor rule) {
        return rule == null ? null : rule.getVersion();
    }

    static String ruleTypeName(ParseRuleStepDescriptor rule, ParseRuleType defaultType) {
        ParseRuleType ruleType = rule == null ? null : rule.getRuleType();
        return ruleType == null ? defaultType.name() : ruleType.name();
    }
}
