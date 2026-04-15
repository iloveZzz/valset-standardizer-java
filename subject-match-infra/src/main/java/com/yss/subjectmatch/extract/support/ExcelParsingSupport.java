package com.yss.subjectmatch.extract.support;

import org.apache.poi.ss.usermodel.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 用于读取工作簿行和标准化单元格值的共享助手。
 */
public final class ExcelParsingSupport {

    public static final List<String> REQUIRED_HEADERS = List.of("科目代码", "科目名称");
    private static final Pattern SUBJECT_CODE_PATTERN = Pattern.compile("^[A-Za-z0-9]+(?:\\.[A-Za-z0-9_ ]+)*$");

    private ExcelParsingSupport() {
    }

    /**
     * 将一行读取为原始单元格值。
     */
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

    /**
     * 将一行读取为标准化文本值。
     */
    public static List<String> readRowTexts(Row row, FormulaEvaluator evaluator, DataFormatter formatter) {
        List<Object> rawValues = readRowValues(row, evaluator, formatter);
        List<String> texts = new ArrayList<>(rawValues.size());
        for (Object rawValue : rawValues) {
            texts.add(normalizeText(rawValue));
        }
        return texts;
    }

    /**
     * 读取单个单元格值并计算公式。
     */
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

    /**
     * 将单元格值标准化为纯文本。
     */
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

    /**
     * 如果可能，将单元格值标准化为十进制数。
     */
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

    /**
     * 检查某个值是否看起来像科目代码。
     */
    public static boolean isSubjectCode(String value) {
        return value != null && !value.isBlank() && SUBJECT_CODE_PATTERN.matcher(value.trim()).matches();
    }

    /**
     * 检查某行是否包含指标数据而不是科目行。
     */
    public static boolean isMetricDataRow(List<Object> rowValues) {
        String firstCell = textAt(rowValues, 0);
        if (firstCell.isEmpty() || isSubjectCode(firstCell)) {
            return false;
        }
        if (textAt(rowValues, 1).isEmpty()) {
            return false;
        }
        for (int index = 2; index < rowValues.size(); index++) {
            if (!textAt(rowValues, index).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查某行是指标行还是指标数据行。
     */
    public static boolean isMetricRow(List<Object> rowValues) {
        String firstCell = textAt(rowValues, 0);
        if (firstCell.isEmpty() || isSubjectCode(firstCell)) {
            return false;
        }
        int filledCount = 0;
        for (int index = 1; index < rowValues.size(); index++) {
            if (!textAt(rowValues, index).isEmpty()) {
                filledCount++;
            }
        }
        return filledCount >= 2;
    }

    /**
     * 从行快照读取文本单元格值。
     */
    public static String textAt(List<Object> values, int index) {
        if (values == null || index < 0 || index >= values.size()) {
            return "";
        }
        return normalizeText(values.get(index));
    }

    /**
     * 从行快照中读取原始单元格值。
     */
    public static Object valueAt(List<Object> values, int index) {
        if (values == null || index < 0 || index >= values.size()) {
            return null;
        }
        return values.get(index);
    }
}
