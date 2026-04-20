package com.yss.valset.analysis.support;

import org.apache.poi.ss.usermodel.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 用于读取工作簿行和标准化单元格值的共享助手。
 */
public final class ExcelParsingSupport {

    private static final Pattern DEFAULT_SUBJECT_CODE_PATTERN = Pattern.compile("^\\d{4}[A-Za-z0-9]*$");

    private ExcelParsingSupport() {
    }

    public static List<Object> readRowValues(Row row, FormulaEvaluator evaluator, DataFormatter formatter) {
        if (row == null) {
            return List.of();
        }
        int lastCellNum = Math.max(row.getLastCellNum(), (short) 0);
        List<Object> values = new ArrayList<>(lastCellNum);
        for (int cellIndex = 0; cellIndex < lastCellNum; cellIndex++) {
            values.add(readCellValue(row.getCell(cellIndex), evaluator, formatter));
        }
        return values;
    }

    public static List<String> readRowTexts(Row row, FormulaEvaluator evaluator, DataFormatter formatter) {
        List<Object> rawValues = readRowValues(row, evaluator, formatter);
        List<String> texts = new ArrayList<>(rawValues.size());
        for (Object rawValue : rawValues) {
            texts.add(normalizeText(rawValue));
        }
        return texts;
    }

    public static Object readCellValue(Cell cell, FormulaEvaluator evaluator, DataFormatter formatter) {
        if (cell == null) {
            return "";
        }
        CellType cellType = cell.getCellType();
        if (cellType == CellType.FORMULA) {
            cellType = evaluator.evaluateFormulaCell(cell);
        }
        switch (cellType) {
            case STRING:
                return cell.getRichStringCellValue().getString().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return formatter.formatCellValue(cell, evaluator).trim();
                }
                return BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros();
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case BLANK:
            case _NONE:
            case ERROR:
            default:
                return formatter.formatCellValue(cell, evaluator).trim();
        }
    }

    public static String normalizeText(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal decimal) {
            BigDecimal normalized = decimal.stripTrailingZeros();
            return normalized.scale() <= 0 ? normalized.toBigInteger().toString() : normalized.toPlainString();
        }
        return String.valueOf(value).trim();
    }

    public static BigDecimal normalizeNumber(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros();
        }
        String text = normalizeText(value);
        if (text.isEmpty()) {
            return null;
        }
        try {
            String normalized = text.replace(",", "");
            if (normalized.endsWith("%")) {
                normalized = normalized.substring(0, normalized.length() - 1);
                return new BigDecimal(normalized).divide(BigDecimal.valueOf(100));
            }
            return new BigDecimal(normalized);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public static boolean isSubjectCode(String value) {
        return isSubjectCode(value, DEFAULT_SUBJECT_CODE_PATTERN);
    }

    public static boolean isSubjectCode(String value, Pattern subjectCodePattern) {
        String normalized = normalizeSubjectCode(value);
        if (normalized.isEmpty()) {
            return false;
        }
        if (containsChineseCharacter(normalized)) {
            return false;
        }
        Pattern pattern = subjectCodePattern == null ? DEFAULT_SUBJECT_CODE_PATTERN : subjectCodePattern;
        if (!pattern.matcher(normalized).matches()) {
            return false;
        }
        return true;
    }

    /**
     * 将科目代码归一为便于识别和比较的紧凑形式。
     * <p>
     * 会统一全角点、空格和标点，并移除所有分隔符，只保留字母数字。
     * </p>
     */
    public static String normalizeSubjectCode(Object value) {
        String normalized = normalizeText(value);
        if (normalized.isEmpty()) {
            return "";
        }
        normalized = normalized
                .replace('．', '.')
                .replace('。', '.')
                .replace('　', ' ')
                .replace('\u00A0', ' ')
                .replaceAll("[,，]", "")
                .trim();
        if (normalized.isEmpty()) {
            return "";
        }

        List<String> segments = new ArrayList<>();
        for (String segment : normalized.split("[\\.\\s]+")) {
            if (!segment.isBlank()) {
                segments.add(segment.trim());
            }
        }
        if (segments.size() > 1) {
            return String.join("", segments);
        }

        return normalized.replaceAll("[\\s\\.\\p{Punct}]+", "");
    }

    /**
     * 读取一行中第一个有效单元格的索引。
     * <p>
     * 有效单元格指非空且不是常见占位符 "-"。
     * </p>
     */
    public static int findFirstMeaningfulCellIndex(List<Object> rowValues) {
        if (rowValues == null || rowValues.isEmpty()) {
            return -1;
        }
        for (int index = 0; index < rowValues.size(); index++) {
            String text = textAt(rowValues, index);
            if (!text.isBlank() && !"-".equals(text)) {
                return index;
            }
        }
        return -1;
    }

    /**
     * 统计一行中的有效单元格数量。
     */
    public static int countMeaningfulCells(List<Object> rowValues) {
        if (rowValues == null || rowValues.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (Object value : rowValues) {
            String text = normalizeText(value);
            if (!text.isBlank() && !"-".equals(text)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 判断文本是否看起来像数字、百分比或日期时间值。
     */
    public static boolean looksNumericLike(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return text.trim().matches(".*\\d.*");
    }

    public static boolean isSubjectDataRow(List<Object> rowValues) {
        return isSubjectDataRow(rowValues, DEFAULT_SUBJECT_CODE_PATTERN);
    }

    public static boolean isSubjectDataRow(List<Object> rowValues, Pattern subjectCodePattern) {
        return findSubjectCodeColumnIndex(rowValues, subjectCodePattern) >= 0;
    }

    public static int findSubjectCodeColumnIndex(List<Object> rowValues) {
        return findSubjectCodeColumnIndex(rowValues, DEFAULT_SUBJECT_CODE_PATTERN);
    }

    public static int findSubjectCodeColumnIndex(List<Object> rowValues, Pattern subjectCodePattern) {
        if (rowValues == null || rowValues.isEmpty()) {
            return -1;
        }
        Pattern pattern = subjectCodePattern == null ? DEFAULT_SUBJECT_CODE_PATTERN : subjectCodePattern;
        boolean seenMeaningfulText = false;
        for (int columnIndex = 0; columnIndex < rowValues.size(); columnIndex++) {
            String candidateCode = textAt(rowValues, columnIndex);
            if (candidateCode.isBlank() || "-".equals(candidateCode)) {
                continue;
            }
            if (!isSubjectCode(candidateCode, pattern)) {
                seenMeaningfulText = true;
                continue;
            }
            if (seenMeaningfulText) {
                continue;
            }
            if (hasSubjectNameAfterCode(rowValues, columnIndex)) {
                return columnIndex;
            }
        }
        return -1;
    }

    public static boolean hasSubjectNameAfterCode(List<Object> rowValues, int codeColumnIndex) {
        if (rowValues == null || rowValues.isEmpty() || codeColumnIndex < 0) {
            return false;
        }
        for (int columnIndex = codeColumnIndex + 1; columnIndex < rowValues.size(); columnIndex++) {
            String candidateText = textAt(rowValues, columnIndex);
            if (candidateText.isBlank() || "-".equals(candidateText)) {
                continue;
            }
            return true;
        }
        return false;
    }

    public static boolean isMetricDataRow(List<Object> rowValues) {
        return isMetricDataRow(rowValues, DEFAULT_SUBJECT_CODE_PATTERN);
    }

    public static boolean isMetricDataRow(List<Object> rowValues, Pattern subjectCodePattern) {
        int labelIndex = findFirstMeaningfulCellIndex(rowValues);
        if (labelIndex < 0) {
            return false;
        }
        String labelText = textAt(rowValues, labelIndex);
        if (isSubjectCode(labelText, subjectCodePattern)) {
            return false;
        }
        int valueCount = 0;
        boolean hasNumericLikeValue = false;
        for (int index = labelIndex + 1; index < rowValues.size(); index++) {
            String valueText = textAt(rowValues, index);
            if (valueText.isBlank() || "-".equals(valueText)) {
                continue;
            }
            valueCount++;
            hasNumericLikeValue = hasNumericLikeValue || looksNumericLike(valueText);
        }
        return valueCount == 1 && hasNumericLikeValue;
    }

    public static boolean isMetricRow(List<Object> rowValues) {
        return isMetricRow(rowValues, DEFAULT_SUBJECT_CODE_PATTERN);
    }

    public static boolean isMetricRow(List<Object> rowValues, Pattern subjectCodePattern) {
        int labelIndex = findFirstMeaningfulCellIndex(rowValues);
        if (labelIndex < 0) {
            return false;
        }
        String labelText = textAt(rowValues, labelIndex);
        if (isSubjectCode(labelText, subjectCodePattern)) {
            return false;
        }
        int valueCount = 0;
        boolean hasNumericLikeValue = false;
        for (int index = labelIndex + 1; index < rowValues.size(); index++) {
            String valueText = textAt(rowValues, index);
            if (valueText.isBlank() || "-".equals(valueText)) {
                continue;
            }
            valueCount++;
            hasNumericLikeValue = hasNumericLikeValue || looksNumericLike(valueText);
        }
        return valueCount >= 2 && hasNumericLikeValue;
    }

    public static String textAt(List<Object> values, int index) {
        if (values == null || index < 0 || index >= values.size()) {
            return "";
        }
        return normalizeText(values.get(index));
    }

    private static boolean isChineseCharacter(char ch) {
        return Character.UnicodeScript.of(ch) == Character.UnicodeScript.HAN;
    }

    private static String splitCompactNumericCode(String compact) {
        if (compact.length() <= 4) {
            return compact;
        }
        List<String> segments = new ArrayList<>();
        segments.add(compact.substring(0, 4));
        String remainder = compact.substring(4);
        while (!remainder.isEmpty()) {
            if (remainder.length() <= 2) {
                segments.add(remainder);
                break;
            }
            segments.add(remainder.substring(0, 2));
            remainder = remainder.substring(2);
        }
        return String.join(".", segments);
    }

    private static boolean containsChineseCharacter(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            if (isChineseCharacter(value.charAt(index))) {
                return true;
            }
        }
        return false;
    }

    public static Object valueAt(List<Object> values, int index) {
        if (values == null || index < 0 || index >= values.size()) {
            return null;
        }
        return values.get(index);
    }
}
