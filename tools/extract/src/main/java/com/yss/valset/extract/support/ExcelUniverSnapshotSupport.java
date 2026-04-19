package com.yss.valset.extract.support;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Color;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Excel 到 Univer 数据结构的转换辅助器。
 * <p>
 * 该工具负责把 POI 中可见的结构性样式信息、合并单元格信息转换成 Univer 可直接消费的
 * JSON 结构。Fesod 继续负责按行读取原始值，POI 仅用于补充表头/标题所需的布局信息。
 * 字体族、字号、斜体、粗体、字体颜色等细粒度字体属性不再采集。
 * </p>
 */
@Slf4j
public class ExcelUniverSnapshotSupport implements Closeable {

    private final Workbook workbook;

    public ExcelUniverSnapshotSupport(Path filePath) {
        try {
            InputStream inputStream = Files.newInputStream(filePath);
            this.workbook = WorkbookFactory.create(inputStream);
        } catch (Exception exception) {
            throw new IllegalStateException("打开 Excel 工作簿失败，filePath=" + filePath, exception);
        }
    }

    /**
     * 构建单行 Univer 单元格快照。
     *
     * @param includeStyle 是否保留单元格样式
     */
    public RowSnapshot buildRowSnapshot(String sheetName, int rowIndex, List<String> rowValues, boolean includeStyle) {
        Sheet sheet = sheet(sheetName);
        Row row = sheet == null ? null : sheet.getRow(rowIndex);
        int columnCount = resolveColumnCount(row, rowValues);

        Map<Integer, Map<String, Object>> rowCellData = new LinkedHashMap<>();
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            String value = columnIndex < rowValues.size() ? rowValues.get(columnIndex) : null;
            Cell cell = includeStyle && row != null
                    ? row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)
                    : null;
            Map<String, Object> univerCellData = buildCellData(cell, value, includeStyle);
            if (!univerCellData.isEmpty()) {
                rowCellData.put(columnIndex, univerCellData);
            }
        }
        return new RowSnapshot(sheetName, rowIndex, rowCellData);
    }

    /**
     * 构建表头预览元数据。
     */
    public Map<String, Object> buildHeaderMeta(String sheetName, List<RowSnapshot> previewRows) {
        Sheet sheet = sheet(sheetName);
        Map<String, Object> headerMeta = new LinkedHashMap<>();
        headerMeta.put("sheetName", sheetName);
        headerMeta.put("previewRowCount", previewRows == null ? 0 : previewRows.size());
        headerMeta.put("rowCount", sheet == null ? 0 : sheet.getLastRowNum() + 1);
        headerMeta.put("columnCount", resolveColumnCount(sheet));
        headerMeta.put("defaultColumnWidth", resolveDefaultColumnWidth(sheet));
        headerMeta.put("defaultRowHeight", resolveDefaultRowHeight(sheet));
        headerMeta.put("mergeData", buildMergeData(sheet));
        headerMeta.put("headerRowNumbers", previewRows == null ? List.of() : previewRows.stream()
                .map(RowSnapshot::getRowIndex)
                .toList());
        Map<Integer, Map<Integer, Map<String, Object>>> cellData = new LinkedHashMap<>();
        if (previewRows != null) {
            for (RowSnapshot previewRow : previewRows) {
                if (previewRow.getRowCellData() != null && !previewRow.getRowCellData().isEmpty()) {
                    cellData.put(previewRow.getRowIndex(), previewRow.getRowCellData());
                }
            }
        }
        headerMeta.put("cellData", cellData);
        return headerMeta;
    }

    public String getSheetName(int sheetIndex) {
        if (sheetIndex < 0 || sheetIndex >= workbook.getNumberOfSheets()) {
            return null;
        }
        return workbook.getSheetName(sheetIndex);
    }

    public Sheet sheet(String sheetName) {
        if (sheetName == null) {
            return workbook.getSheetAt(0);
        }
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet != null) {
            return sheet;
        }
        return workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
    }

    @Override
    public void close() throws IOException {
        workbook.close();
    }

    private Map<String, Object> buildCellData(Cell cell, String rawValue, boolean includeStyle) {
        Map<String, Object> cellData = new LinkedHashMap<>();
        if (rawValue != null) {
            cellData.put("v", rawValue);
        }
        if (includeStyle && cell != null && cell.getCellStyle() != null) {
            Map<String, Object> styleData = convertStyle(cell.getCellStyle());
            if (!styleData.isEmpty()) {
                cellData.put("s", styleData);
            }
        }
        return cellData;
    }

    private Map<String, Object> convertStyle(CellStyle cellStyle) {
        Map<String, Object> styleData = new LinkedHashMap<>();
        if (cellStyle == null) {
            return styleData;
        }

        if (cellStyle.getFillPattern() != FillPatternType.NO_FILL) {
            String fillColor = resolveFillColor(cellStyle);
            if (fillColor != null) {
                styleData.put("bg", Map.of("rgb", fillColor));
            }
        }

        Map<String, Object> borderData = convertBorder(cellStyle);
        if (!borderData.isEmpty()) {
            styleData.put("bd", borderData);
        }

        styleData.put("ht", convertHorizontalAlignment(cellStyle.getAlignment()));
        styleData.put("vt", convertVerticalAlignment(cellStyle.getVerticalAlignment()));
        styleData.put("tb", cellStyle.getWrapText() ? 3 : 1);

        int rotation = cellStyle.getRotation();
        if (rotation != 0) {
            styleData.put("tr", Map.of("a", rotation == 255 ? 90 : rotation, "v", rotation == 255 ? 1 : 0));
        }

        String dataFormat = cellStyle.getDataFormatString();
        if (dataFormat != null && !dataFormat.isBlank() && !"General".equalsIgnoreCase(dataFormat)) {
            styleData.put("n", Map.of("pattern", dataFormat));
        }
        return styleData;
    }

    private Map<String, Object> convertBorder(CellStyle cellStyle) {
        Map<String, Object> borderData = new LinkedHashMap<>();
        putBorder(borderData, "t", cellStyle.getBorderTop(), cellStyle.getTopBorderColor());
        putBorder(borderData, "b", cellStyle.getBorderBottom(), cellStyle.getBottomBorderColor());
        putBorder(borderData, "l", cellStyle.getBorderLeft(), cellStyle.getLeftBorderColor());
        putBorder(borderData, "r", cellStyle.getBorderRight(), cellStyle.getRightBorderColor());
        return borderData;
    }

    private void putBorder(Map<String, Object> borderData, String key, BorderStyle borderStyle, short colorIndex) {
        if (borderStyle == null || borderStyle == BorderStyle.NONE) {
            return;
        }
        Map<String, Object> border = new LinkedHashMap<>();
        border.put("s", borderStyle.getCode());
        String color = resolveIndexedColor(colorIndex);
        if (color != null) {
            border.put("cl", Map.of("rgb", color));
        }
        borderData.put(key, border);
    }

    private Integer convertHorizontalAlignment(HorizontalAlignment alignment) {
        if (alignment == null) {
            return 1;
        }
        return switch (alignment) {
            case CENTER, CENTER_SELECTION, GENERAL -> 2;
            case RIGHT, FILL, JUSTIFY -> 3;
            default -> 1;
        };
    }

    private Integer convertVerticalAlignment(VerticalAlignment alignment) {
        if (alignment == null) {
            return 1;
        }
        return switch (alignment) {
            case CENTER -> 2;
            case BOTTOM -> 3;
            default -> 1;
        };
    }

    private String resolveFillColor(CellStyle cellStyle) {
        try {
            return resolveColor(cellStyle.getFillForegroundColorColor());
        } catch (Exception ignored) {
            // ignore
        }
        return resolveIndexedColor(cellStyle.getFillForegroundColor());
    }

    private String resolveColor(Color color) {
        if (color == null) {
            return null;
        }
        if (color instanceof XSSFColor xssfColor) {
            return normalizeHex(xssfColor.getARGBHex());
        }
        return null;
    }

    private String resolveIndexedColor(short colorIndex) {
        try {
            return indexedColorHex(colorIndex);
        } catch (Exception exception) {
            return null;
        }
    }

    private String indexedColorHex(short colorIndex) {
        return switch (colorIndex) {
            case 8 -> "#000000";
            case 10 -> "#FF0000";
            case 11 -> "#00FF00";
            case 12 -> "#0000FF";
            case 13 -> "#FFFF00";
            case 14 -> "#FF00FF";
            case 15 -> "#00FFFF";
            case 64 -> "#000000";
            default -> null;
        };
    }

    private String normalizeHex(String color) {
        if (color == null || color.isBlank()) {
            return null;
        }
        String value = color.replace("#", "");
        if (value.length() == 8) {
            value = value.substring(2);
        }
        if (value.length() == 6) {
            return "#" + value.toUpperCase();
        }
        return null;
    }

    private List<Map<String, Object>> buildMergeData(Sheet sheet) {
        if (sheet == null || sheet.getNumMergedRegions() <= 0) {
            return List.of();
        }
        List<Map<String, Object>> mergeData = new ArrayList<>(sheet.getNumMergedRegions());
        for (int index = 0; index < sheet.getNumMergedRegions(); index++) {
            CellRangeAddress range = sheet.getMergedRegion(index);
            Map<String, Object> merge = new LinkedHashMap<>();
            merge.put("startRow", range.getFirstRow());
            merge.put("startColumn", range.getFirstColumn());
            merge.put("endRow", range.getLastRow());
            merge.put("endColumn", range.getLastColumn());
            mergeData.add(merge);
        }
        return mergeData;
    }

    private int resolveColumnCount(Row row, List<String> rowValues) {
        int columnCount = rowValues == null ? 0 : rowValues.size();
        if (row != null && row.getLastCellNum() > columnCount) {
            columnCount = row.getLastCellNum();
        }
        return Math.max(columnCount, 0);
    }

    private int resolveColumnCount(Sheet sheet) {
        if (sheet == null) {
            return 0;
        }
        int maxColumnCount = 0;
        for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            maxColumnCount = Math.max(maxColumnCount, row.getLastCellNum());
        }
        return Math.max(maxColumnCount, 0);
    }

    private int resolveDefaultColumnWidth(Sheet sheet) {
        if (sheet == null) {
            return 0;
        }
        return Math.max(0, sheet.getDefaultColumnWidth() * 7);
    }

    private int resolveDefaultRowHeight(Sheet sheet) {
        if (sheet == null) {
            return 0;
        }
        return Math.max(0, Math.round(sheet.getDefaultRowHeightInPoints() * 96 / 72f));
    }

    /**
     * 表头预览行快照。
     */
    @Getter
    public static final class RowSnapshot {
        private final String sheetName;
        private final int rowIndex;
        private final Map<Integer, Map<String, Object>> rowCellData;

        public RowSnapshot(String sheetName, int rowIndex, Map<Integer, Map<String, Object>> rowCellData) {
            this.sheetName = sheetName;
            this.rowIndex = rowIndex;
            this.rowCellData = rowCellData == null ? Collections.emptyMap() : rowCellData;
        }
    }
}
