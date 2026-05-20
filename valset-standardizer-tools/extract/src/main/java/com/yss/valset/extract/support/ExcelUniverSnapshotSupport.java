package com.yss.valset.extract.support;

import com.yss.valset.common.support.SpreadsheetXmlSupport;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Color;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    private static final int XML_DEFAULT_COLUMN_WIDTH = 56;
    private static final int XML_DEFAULT_ROW_HEIGHT = 20;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Workbook workbook;
    private final SpreadsheetXmlSupport.SpreadsheetXmlWorkbook spreadsheetXmlWorkbook;

    public ExcelUniverSnapshotSupport(Path filePath) {
        try {
            if (SpreadsheetXmlSupport.isSpreadsheetXml(filePath)) {
                this.workbook = null;
                this.spreadsheetXmlWorkbook = SpreadsheetXmlSupport.read(filePath);
                return;
            }
            try (InputStream inputStream = Files.newInputStream(filePath)) {
                this.workbook = WorkbookFactory.create(inputStream);
            }
            this.spreadsheetXmlWorkbook = null;
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
        if (workbook != null) {
            Sheet sheet = sheet(sheetName);
            Map<String, Object> headerMeta = new LinkedHashMap<>();
            headerMeta.put("sheetName", sheetName);
            headerMeta.put("previewRowCount", previewRows == null ? 0 : previewRows.size());
            headerMeta.put("rowCount", sheet == null ? 0 : sheet.getLastRowNum() + 1);
            headerMeta.put("columnCount", resolveColumnCount(sheet));
            headerMeta.put("defaultColumnWidth", resolveDefaultColumnWidth(sheet));
            headerMeta.put("defaultRowHeight", resolveDefaultRowHeight(sheet));
            headerMeta.put("mergeData", buildMergeData(sheet));
            headerMeta.put("headerRowNumbers", previewRows == null ? java.util.Arrays.asList() : previewRows.stream()
                    .map(RowSnapshot::getRowIndex)
                    .collect(java.util.stream.Collectors.toList()));
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
        return buildSpreadsheetXmlHeaderMeta(sheetName, previewRows);
    }

    public String getSheetName(int sheetIndex) {
        if (workbook != null) {
            if (sheetIndex < 0 || sheetIndex >= workbook.getNumberOfSheets()) {
                return null;
            }
            return workbook.getSheetName(sheetIndex);
        }
        if (sheetIndex < 0 || spreadsheetXmlWorkbook == null || sheetIndex >= spreadsheetXmlWorkbook.sheets().size()) {
            return null;
        }
        return spreadsheetXmlWorkbook.sheets().get(sheetIndex).sheetName();
    }

    public Sheet sheet(String sheetName) {
        if (workbook == null) {
            return null;
        }
        if (sheetName == null) {
            return workbook.getSheetAt(0);
        }
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet != null) {
            return sheet;
        }
        return workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
    }

    /**
     * 构建完整 Univer workbook snapshot，覆盖工作簿内所有 Sheet。
     */
    public WorkbookSnapshot buildWorkbookSnapshot(String workbookName) {
        if (workbook != null) {
            return buildPoiWorkbookSnapshot(workbookName);
        }
        return buildSpreadsheetXmlWorkbookSnapshot(workbookName);
    }

    @Override
    public void close() throws IOException {
        if (workbook != null) {
            workbook.close();
        }
    }

    private Map<String, Object> buildCellData(Cell cell, Object rawValue, boolean includeStyle) {
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

    private WorkbookSnapshot buildPoiWorkbookSnapshot(String workbookName) {
        Map<String, Object> workbookData = baseWorkbookData(workbookName);
        List<String> sheetOrder = new ArrayList<>();
        Map<String, Object> sheets = new LinkedHashMap<>();
        int totalRows = 0;
        int sheetCount = workbook == null ? 0 : workbook.getNumberOfSheets();
        for (int sheetIndex = 0; sheetIndex < sheetCount; sheetIndex++) {
            Sheet sheet = workbook.getSheetAt(sheetIndex);
            String sheetId = buildSheetId(sheetIndex);
            sheetOrder.add(sheetId);
            sheets.put(sheetId, buildPoiSheetData(sheetId, sheet));
            totalRows += Math.max(0, sheet == null ? 0 : sheet.getLastRowNum() + 1);
        }
        workbookData.put("sheetOrder", sheetOrder);
        workbookData.put("sheets", sheets);
        return new WorkbookSnapshot(workbookData, sheetCount, totalRows);
    }

    private Map<String, Object> buildPoiSheetData(String sheetId, Sheet sheet) {
        Map<String, Object> sheetData = new LinkedHashMap<>();
        int rowCount = Math.max(1, sheet == null ? 0 : sheet.getLastRowNum() + 1);
        int columnCount = Math.max(1, resolveColumnCount(sheet));
        sheetData.put("id", sheetId);
        sheetData.put("name", sheet == null ? "Sheet" : sheet.getSheetName());
        sheetData.put("rowCount", Math.max(rowCount, 30));
        sheetData.put("columnCount", Math.max(columnCount, 8));
        sheetData.put("defaultColumnWidth", resolveDefaultColumnWidth(sheet));
        sheetData.put("defaultRowHeight", resolveDefaultRowHeight(sheet));
        sheetData.put("cellData", buildPoiCellData(sheet, rowCount, columnCount));
        sheetData.put("mergeData", buildMergeData(sheet));
        Map<Integer, Map<String, Object>> columnData = buildPoiColumnData(sheet, columnCount);
        if (!columnData.isEmpty()) {
            sheetData.put("columnData", columnData);
        }
        Map<Integer, Map<String, Object>> rowData = buildPoiRowData(sheet, rowCount);
        if (!rowData.isEmpty()) {
            sheetData.put("rowData", rowData);
        }
        return sheetData;
    }

    private Map<Integer, Map<Integer, Map<String, Object>>> buildPoiCellData(Sheet sheet, int rowCount, int columnCount) {
        Map<Integer, Map<Integer, Map<String, Object>>> cellData = new LinkedHashMap<>();
        if (sheet == null) {
            return cellData;
        }
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            Map<Integer, Map<String, Object>> rowCellData = new LinkedHashMap<>();
            int rowColumnCount = Math.max(columnCount, row.getLastCellNum());
            for (int columnIndex = 0; columnIndex < rowColumnCount; columnIndex++) {
                Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                if (cell == null) {
                    continue;
                }
                Map<String, Object> cellSnapshot = buildCellData(cell, cellValue(cell), true);
                Integer valueType = cellType(cell);
                if (valueType != null) {
                    cellSnapshot.put("t", valueType);
                }
                if (!cellSnapshot.isEmpty()) {
                    rowCellData.put(columnIndex, cellSnapshot);
                }
            }
            if (!rowCellData.isEmpty()) {
                cellData.put(rowIndex, rowCellData);
            }
        }
        return cellData;
    }

    private Map<Integer, Map<String, Object>> buildPoiColumnData(Sheet sheet, int columnCount) {
        Map<Integer, Map<String, Object>> columnData = new LinkedHashMap<>();
        if (sheet == null) {
            return columnData;
        }
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            int width = Math.max(0, Math.round(sheet.getColumnWidth(columnIndex) / 256f * 7f));
            if (width > 0) {
                columnData.put(columnIndex, com.yss.valset.common.support.Java8Maps.of("w", width));
            }
        }
        return columnData;
    }

    private Map<Integer, Map<String, Object>> buildPoiRowData(Sheet sheet, int rowCount) {
        Map<Integer, Map<String, Object>> rowData = new LinkedHashMap<>();
        if (sheet == null) {
            return rowData;
        }
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }
            int height = Math.max(0, Math.round(row.getHeightInPoints() * 96 / 72f));
            if (height > 0) {
                rowData.put(rowIndex, com.yss.valset.common.support.Java8Maps.of("h", height));
            }
        }
        return rowData;
    }

    private WorkbookSnapshot buildSpreadsheetXmlWorkbookSnapshot(String workbookName) {
        Map<String, Object> workbookData = baseWorkbookData(workbookName);
        List<String> sheetOrder = new ArrayList<>();
        Map<String, Object> sheets = new LinkedHashMap<>();
        int totalRows = 0;
        List<SpreadsheetXmlSupport.SpreadsheetXmlSheet> xmlSheets = spreadsheetXmlWorkbook == null
                ? Collections.emptyList()
                : spreadsheetXmlWorkbook.sheets();
        for (int sheetIndex = 0; sheetIndex < xmlSheets.size(); sheetIndex++) {
            SpreadsheetXmlSupport.SpreadsheetXmlSheet sheet = xmlSheets.get(sheetIndex);
            String sheetId = buildSheetId(sheetIndex);
            sheetOrder.add(sheetId);
            sheets.put(sheetId, buildSpreadsheetXmlSheetData(sheetId, sheet));
            totalRows += sheet == null || sheet.rows() == null ? 0 : sheet.rows().size();
        }
        workbookData.put("sheetOrder", sheetOrder);
        workbookData.put("sheets", sheets);
        return new WorkbookSnapshot(workbookData, xmlSheets.size(), totalRows);
    }

    private Map<String, Object> buildSpreadsheetXmlSheetData(String sheetId, SpreadsheetXmlSupport.SpreadsheetXmlSheet sheet) {
        int rowCount = Math.max(1, sheet == null || sheet.rows() == null ? 0 : sheet.rows().size());
        int columnCount = Math.max(1, resolveColumnCount(sheet));
        Map<String, Object> sheetData = new LinkedHashMap<>();
        sheetData.put("id", sheetId);
        sheetData.put("name", sheet == null ? "Sheet" : sheet.sheetName());
        sheetData.put("rowCount", Math.max(rowCount, 30));
        sheetData.put("columnCount", Math.max(columnCount, 8));
        sheetData.put("defaultColumnWidth", XML_DEFAULT_COLUMN_WIDTH);
        sheetData.put("defaultRowHeight", XML_DEFAULT_ROW_HEIGHT);
        sheetData.put("cellData", buildSpreadsheetXmlCellData(sheet));
        sheetData.put("mergeData", buildMergeData(sheet));
        return sheetData;
    }

    private Map<Integer, Map<Integer, Map<String, Object>>> buildSpreadsheetXmlCellData(SpreadsheetXmlSupport.SpreadsheetXmlSheet sheet) {
        Map<Integer, Map<Integer, Map<String, Object>>> cellData = new LinkedHashMap<>();
        if (sheet == null || sheet.rows() == null) {
            return cellData;
        }
        List<List<String>> rows = sheet.rows();
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            List<String> row = rows.get(rowIndex);
            if (row == null || row.isEmpty()) {
                continue;
            }
            Map<Integer, Map<String, Object>> rowCellData = new LinkedHashMap<>();
            for (int columnIndex = 0; columnIndex < row.size(); columnIndex++) {
                String value = row.get(columnIndex);
                if (value != null) {
                    rowCellData.put(columnIndex, com.yss.valset.common.support.Java8Maps.of("v", value));
                }
            }
            if (!rowCellData.isEmpty()) {
                cellData.put(rowIndex, rowCellData);
            }
        }
        return cellData;
    }

    private Map<String, Object> baseWorkbookData(String workbookName) {
        Map<String, Object> workbookData = new LinkedHashMap<>();
        workbookData.put("id", "raw_workbook_" + UUID.randomUUID().toString().replace("-", ""));
        workbookData.put("name", workbookName == null || workbookName.trim().isEmpty() ? "原始估值表" : workbookName.trim());
        workbookData.put("appVersion", "3.0.0");
        workbookData.put("locale", "zhCN");
        workbookData.put("styles", Collections.emptyMap());
        return workbookData;
    }

    private String buildSheetId(int sheetIndex) {
        return "sheet_" + (sheetIndex + 1);
    }

    private Object cellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        try {
            CellType cellType = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();
            switch (cellType) {
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell) && cell.getDateCellValue() != null) {
                        return DATE_TIME_FORMATTER.format(cell.getDateCellValue().toInstant()
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDateTime());
                    }
                    return cell.getNumericCellValue();
                case BOOLEAN:
                    return cell.getBooleanCellValue();
                case STRING:
                    return cell.getStringCellValue();
                case BLANK:
                    return null;
                default:
                    return cell.toString();
            }
        } catch (Exception exception) {
            return cell.toString();
        }
    }

    private Integer cellType(Cell cell) {
        if (cell == null) {
            return null;
        }
        CellType cellType = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();
        switch (cellType) {
            case NUMERIC:
                return 2;
            case BOOLEAN:
                return 4;
            case STRING:
                return 1;
            default:
                return null;
        }
    }

    private Map<String, Object> convertStyle(CellStyle cellStyle) {
        Map<String, Object> styleData = new LinkedHashMap<>();
        if (cellStyle == null) {
            return styleData;
        }

        if (cellStyle.getFillPattern() != FillPatternType.NO_FILL) {
            String fillColor = resolveFillColor(cellStyle);
            if (fillColor != null) {
                styleData.put("bg", com.yss.valset.common.support.Java8Maps.of("rgb", fillColor));
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
            styleData.put("tr", com.yss.valset.common.support.Java8Maps.of("a", rotation == 255 ? 90 : rotation, "v", rotation == 255 ? 1 : 0));
        }

        String dataFormat = cellStyle.getDataFormatString();
        if (dataFormat != null && !dataFormat.trim().isEmpty() && !"General".equalsIgnoreCase(dataFormat)) {
            styleData.put("n", com.yss.valset.common.support.Java8Maps.of("pattern", dataFormat));
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
            border.put("cl", com.yss.valset.common.support.Java8Maps.of("rgb", color));
        }
        borderData.put(key, border);
    }

    private Integer convertHorizontalAlignment(HorizontalAlignment alignment) {
        if (alignment == null) {
            return 1;
        }
        switch (alignment) {
            case CENTER:
            case CENTER_SELECTION:
            case GENERAL:
                return 2;
            case RIGHT:
            case FILL:
            case JUSTIFY:
                return 3;
            default:
                return 1;
        }
    }

    private Integer convertVerticalAlignment(VerticalAlignment alignment) {
        if (alignment == null) {
            return 1;
        }
        switch (alignment) {
            case CENTER:
                return 2;
            case BOTTOM:
                return 3;
            default:
                return 1;
        }
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
        if (color instanceof XSSFColor) {
            return normalizeHex(((XSSFColor) color).getARGBHex());
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
        switch (colorIndex) {
            case 8:
                return "#000000";
            case 10:
                return "#FF0000";
            case 11:
                return "#00FF00";
            case 12:
                return "#0000FF";
            case 13:
                return "#FFFF00";
            case 14:
                return "#FF00FF";
            case 15:
                return "#00FFFF";
            case 64:
                return "#000000";
            default:
                return null;
        }
    }

    private String normalizeHex(String color) {
        if (color == null || color.trim().isEmpty()) {
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
            return java.util.Arrays.asList();
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

    private Map<String, Object> buildSpreadsheetXmlHeaderMeta(String sheetName, List<RowSnapshot> previewRows) {
        SpreadsheetXmlSupport.SpreadsheetXmlSheet sheet = spreadsheetXmlSheet(sheetName);
        Map<String, Object> headerMeta = new LinkedHashMap<>();
        headerMeta.put("sheetName", sheet == null ? sheetName : sheet.sheetName());
        headerMeta.put("previewRowCount", previewRows == null ? 0 : previewRows.size());
        headerMeta.put("rowCount", sheet == null ? 0 : sheet.rows().size());
        headerMeta.put("columnCount", resolveColumnCount(sheet));
        headerMeta.put("defaultColumnWidth", XML_DEFAULT_COLUMN_WIDTH);
        headerMeta.put("defaultRowHeight", XML_DEFAULT_ROW_HEIGHT);
        headerMeta.put("mergeData", buildMergeData(sheet));
        headerMeta.put("headerRowNumbers", previewRows == null ? java.util.Arrays.asList() : previewRows.stream()
                .map(RowSnapshot::getRowIndex)
                .collect(java.util.stream.Collectors.toList()));
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

    private SpreadsheetXmlSupport.SpreadsheetXmlSheet spreadsheetXmlSheet(String sheetName) {
        if (spreadsheetXmlWorkbook == null || spreadsheetXmlWorkbook.sheets().isEmpty()) {
            return null;
        }
        if (sheetName == null) {
            return spreadsheetXmlWorkbook.sheets().get(0);
        }
        for (SpreadsheetXmlSupport.SpreadsheetXmlSheet sheet : spreadsheetXmlWorkbook.sheets()) {
            if (sheetName.equals(sheet.sheetName())) {
                return sheet;
            }
        }
        return spreadsheetXmlWorkbook.sheets().get(0);
    }

    private int resolveColumnCount(SpreadsheetXmlSupport.SpreadsheetXmlSheet sheet) {
        if (sheet == null || sheet.rows() == null) {
            return 0;
        }
        int maxColumnCount = 0;
        for (List<String> row : sheet.rows()) {
            if (row == null) {
                continue;
            }
            maxColumnCount = Math.max(maxColumnCount, row.size());
        }
        return Math.max(maxColumnCount, 0);
    }

    private List<Map<String, Object>> buildMergeData(SpreadsheetXmlSupport.SpreadsheetXmlSheet sheet) {
        if (sheet == null || sheet.mergeRegions() == null || sheet.mergeRegions().isEmpty()) {
            return java.util.Arrays.asList();
        }
        List<Map<String, Object>> mergeData = new ArrayList<>(sheet.mergeRegions().size());
        for (SpreadsheetXmlSupport.SpreadsheetXmlMergeRegion range : sheet.mergeRegions()) {
            Map<String, Object> merge = new LinkedHashMap<>();
            merge.put("startRow", range.startRow());
            merge.put("startColumn", range.startColumn());
            merge.put("endRow", range.endRow());
            merge.put("endColumn", range.endColumn());
            mergeData.add(merge);
        }
        return mergeData;
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

    /**
     * 完整工作簿快照。
     */
    @Getter
    public static final class WorkbookSnapshot {
        private final Map<String, Object> workbookData;
        private final int sheetCount;
        private final int rowCount;

        public WorkbookSnapshot(Map<String, Object> workbookData, int sheetCount, int rowCount) {
            this.workbookData = workbookData == null ? Collections.emptyMap() : workbookData;
            this.sheetCount = sheetCount;
            this.rowCount = rowCount;
        }
    }
}
