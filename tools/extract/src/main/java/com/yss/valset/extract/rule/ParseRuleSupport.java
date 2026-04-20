package com.yss.valset.extract.rule;

import com.yss.valset.extract.support.ExcelParsingSupport;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 解析规则共享助手。
 */
public final class ParseRuleSupport {

    public static final List<String> DEFAULT_FOOTER_KEYWORDS = List.of("制表", "复核", "打印", "备注");

    private ParseRuleSupport() {
    }

    /**
     * 读取行内的纯文本快照。
     */
    public static List<String> toTexts(List<Object> rowValues) {
        if (CollectionUtils.isEmpty(rowValues)) {
            return List.of();
        }
        List<String> texts = new ArrayList<>(rowValues.size());
        for (Object value : rowValues) {
            texts.add(ExcelParsingSupport.normalizeText(value));
        }
        return texts;
    }

    /**
     * 统计命中关键词的单元格数量。
     */
    public static int rowHitCount(List<Object> rowValues, List<String> keywords) {
        if (CollectionUtils.isEmpty(rowValues) || CollectionUtils.isEmpty(keywords)) {
            return 0;
        }
        List<String> texts = toTexts(rowValues);
        int hitCount = 0;
        for (String keyword : keywords) {
            if (keyword == null || keyword.isBlank()) {
                continue;
            }
            if (texts.stream().anyMatch(text -> matchesKeyword(text, keyword))) {
                hitCount++;
            }
        }
        return hitCount;
    }

    /**
     * 是否同时包含所有关键词。
     */
    public static boolean rowContainsAll(List<Object> rowValues, List<String> keywords) {
        if (CollectionUtils.isEmpty(keywords)) {
            return false;
        }
        return rowHitCount(rowValues, keywords) >= keywords.size();
    }

    /**
     * 判断行中是否命中任意关键词。
     */
    public static boolean rowContainsAny(List<Object> rowValues, List<String> keywords) {
        if (CollectionUtils.isEmpty(rowValues) || CollectionUtils.isEmpty(keywords)) {
            return false;
        }
        List<String> texts = toTexts(rowValues);
        for (String keyword : keywords) {
            if (keyword == null || keyword.isBlank()) {
                continue;
            }
            if (texts.stream().anyMatch(text -> matchesKeyword(text, keyword))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断文本是否命中任意关键词。
     */
    public static boolean containsAny(String text, List<String> keywords) {
        if (text == null || text.isBlank() || CollectionUtils.isEmpty(keywords)) {
            return false;
        }
        for (String keyword : keywords) {
            if (matchesKeyword(text, keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断文本是否命中全部关键词。
     */
    public static boolean containsAll(String text, List<String> keywords) {
        if (text == null || text.isBlank() || CollectionUtils.isEmpty(keywords)) {
            return false;
        }
        for (String keyword : keywords) {
            if (!matchesKeyword(text, keyword)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 计算第一条非空单元格的索引。
     */
    public static int firstMeaningfulCellIndex(List<Object> rowValues) {
        if (CollectionUtils.isEmpty(rowValues)) {
            return -1;
        }
        for (int index = 0; index < rowValues.size(); index++) {
            if (!ExcelParsingSupport.textAt(rowValues, index).isBlank()) {
                return index;
            }
        }
        return -1;
    }

    /**
     * 读取第一条非空文本。
     */
    public static String firstMeaningfulText(List<Object> rowValues) {
        int index = firstMeaningfulCellIndex(rowValues);
        return index < 0 ? "" : ExcelParsingSupport.textAt(rowValues, index);
    }

    /**
     * 判断是否是科目行。
     */
    public static boolean isSubjectRow(List<Object> rowValues) {
        return isSubjectRow(rowValues, null);
    }

    /**
     * 判断是否是科目行。
     */
    public static boolean isSubjectRow(List<Object> rowValues, Pattern subjectCodePattern) {
        int codeIndex = findSubjectCodeColumnIndex(rowValues, subjectCodePattern);
        return codeIndex >= 0 && hasSubjectNameAfterCode(rowValues, codeIndex);
    }

    /**
     * 判断是否是表头行。
     */
    public static boolean isHeaderRow(List<Object> rowValues, List<String> requiredHeaders) {
        return rowContainsAll(rowValues, requiredHeaders);
    }

    /**
     * 判断是否是数据起始行。
     */
    public static boolean isDataStartRow(List<Object> rowValues) {
        return isDataStartRow(rowValues, null);
    }

    /**
     * 判断是否是数据起始行。
     */
    public static boolean isDataStartRow(List<Object> rowValues, Pattern subjectCodePattern) {
        return isSubjectRow(rowValues, subjectCodePattern) || isMetricCandidate(rowValues, subjectCodePattern);
    }

    /**
     * 分类行类型。
     */
    public static String classifyRow(List<Object> rowValues, List<String> footerKeywords) {
        return classifyRow(rowValues, footerKeywords, null);
    }

    /**
     * 分类行类型。
     */
    public static String classifyRow(List<Object> rowValues, List<String> footerKeywords, Pattern subjectCodePattern) {
        if (isSubjectRow(rowValues, subjectCodePattern)) {
            return "SUBJECT";
        }
        if (ExcelParsingSupport.isMetricDataRow(rowValues, subjectCodePattern)) {
            return "METRIC_DATA";
        }
        if (ExcelParsingSupport.isMetricRow(rowValues, subjectCodePattern)) {
            return "METRIC_ROW";
        }
        if (isFooterRow(rowValues, footerKeywords)) {
            return "FOOTER";
        }
        return "IGNORE";
    }

    /**
     * 查找科目代码列。
     */
    public static int findSubjectCodeColumnIndex(List<Object> rowValues) {
        return findSubjectCodeColumnIndex(rowValues, null);
    }

    /**
     * 查找科目代码列。
     */
    public static int findSubjectCodeColumnIndex(List<Object> rowValues, Pattern subjectCodePattern) {
        if (CollectionUtils.isEmpty(rowValues)) {
            return -1;
        }
        for (int index = 0; index < rowValues.size(); index++) {
            String text = ExcelParsingSupport.normalizeText(ExcelParsingSupport.valueAt(rowValues, index));
            if (ExcelParsingSupport.isSubjectCode(text, subjectCodePattern)) {
                return index;
            }
        }
        return -1;
    }

    /**
     * 判断代码列后是否存在科目名称。
     */
    public static boolean hasSubjectNameAfterCode(List<Object> rowValues, int codeIndex) {
        if (CollectionUtils.isEmpty(rowValues) || codeIndex < 0 || codeIndex >= rowValues.size()) {
            return false;
        }
        for (int index = codeIndex + 1; index < rowValues.size(); index++) {
            String text = ExcelParsingSupport.textAt(rowValues, index);
            if (!text.isBlank()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否是指标候选行。
     */
    public static boolean isMetricCandidate(List<Object> rowValues) {
        return isMetricCandidate(rowValues, null);
    }

    /**
     * 判断是否是指标候选行。
     */
    public static boolean isMetricCandidate(List<Object> rowValues, Pattern subjectCodePattern) {
        return ExcelParsingSupport.isMetricDataRow(rowValues, subjectCodePattern) || ExcelParsingSupport.isMetricRow(rowValues, subjectCodePattern);
    }

    /**
     * 判断是否是页脚或备注行。
     */
    public static boolean isFooterRow(List<Object> rowValues, List<String> footerKeywords) {
        if (CollectionUtils.isEmpty(rowValues)) {
            return false;
        }
        List<String> keywords = CollectionUtils.isEmpty(footerKeywords) ? DEFAULT_FOOTER_KEYWORDS : footerKeywords;
        String firstText = firstMeaningfulText(rowValues);
        for (String keyword : keywords) {
            if (matchesKeyword(firstText, keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断首个有效文本是否命中任意关键词。
     */
    public static boolean firstMeaningfulTextContainsAny(List<Object> rowValues, List<String> keywords) {
        return containsAny(firstMeaningfulText(rowValues), keywords);
    }

    /**
     * 判断首个有效文本是否命中全部关键词。
     */
    public static boolean firstMeaningfulTextContainsAll(List<Object> rowValues, List<String> keywords) {
        return containsAll(firstMeaningfulText(rowValues), keywords);
    }

    /**
     * 判断有效单元格数量是否达到下限。
     */
    public static boolean hasAtLeastNonBlank(List<Object> rowValues, Integer minCount) {
        if (minCount == null || minCount <= 0) {
            return false;
        }
        return nonBlankCount(rowValues) >= minCount;
    }

    /**
     * 判断文本是否命中关键字。
     */
    public static boolean matchesKeyword(String text, String keyword) {
        if (text == null || text.isBlank() || keyword == null || keyword.isBlank()) {
            return false;
        }
        return text.equals(keyword) || text.contains(keyword) || keyword.contains(text);
    }

    /**
     * 计算行中的有效值数量。
     */
    public static int nonBlankCount(List<Object> rowValues) {
        if (CollectionUtils.isEmpty(rowValues)) {
            return 0;
        }
        int count = 0;
        for (Object value : rowValues) {
            if (!ExcelParsingSupport.normalizeText(value).isBlank()) {
                count++;
            }
        }
        return count;
    }

    /**
     * 规范化关键词列表。
     */
    public static List<String> normalizeKeywords(List<String> keywords) {
        if (CollectionUtils.isEmpty(keywords)) {
            return Collections.emptyList();
        }
        return keywords.stream()
                .filter(keyword -> keyword != null && !keyword.isBlank())
                .map(String::trim)
                .toList();
    }
}
