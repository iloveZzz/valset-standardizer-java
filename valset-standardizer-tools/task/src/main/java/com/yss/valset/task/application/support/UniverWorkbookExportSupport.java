package com.yss.valset.task.application.support;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.fesod.sheet.FesodSheet;
import org.apache.fesod.sheet.write.handler.CellWriteHandler;
import org.apache.fesod.sheet.write.handler.SheetWriteHandler;
import org.apache.fesod.sheet.write.handler.context.CellWriteHandlerContext;
import org.apache.fesod.sheet.write.handler.context.SheetWriteHandlerContext;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 将 Univer workbook 快照导出为 XLSX。
 */
@Component
public class UniverWorkbookExportSupport {

    private static final int EXCEL_COLUMN_WIDTH_UNIT = 256;
    private static final int DEFAULT_COLUMN_WIDTH = 80;
    private static final int DEFAULT_ROW_HEIGHT = 22;

    public byte[] export(JsonNode workbookData, String preferredSheetName) {
        UniverWorkbookSnapshot snapshot = parse(workbookData, preferredSheetName);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        FesodSheet.write(outputStream)
                .autoCloseStream(false)
                .needHead(false)
                .registerWriteHandler(new UniverSheetWriteHandler(snapshot))
                .registerWriteHandler(new UniverCellWriteHandler(snapshot))
                .sheet(snapshot.sheetName)
                .doWrite(snapshot.rows);
        return outputStream.toByteArray();
    }

    private UniverWorkbookSnapshot parse(JsonNode workbookData, String preferredSheetName) {
        if (workbookData == null || workbookData.isNull() || !workbookData.isObject()) {
            throw new IllegalArgumentException("Univer 工作簿快照不能为空");
        }
        JsonNode sheetsNode = workbookData.path("sheets");
        if (!sheetsNode.isObject() || sheetsNode.size() == 0) {
            throw new IllegalArgumentException("Univer 工作簿缺少 sheet 数据");
        }
        String sheetId = resolveSheetId(workbookData, sheetsNode, preferredSheetName);
        JsonNode sheetNode = sheetsNode.path(sheetId);
        if (!sheetNode.isObject()) {
            throw new IllegalArgumentException("Univer 工作簿缺少有效 sheet 数据");
        }

        Map<String, JsonNode> styles = parseStyles(workbookData.path("styles"));
        int rowCount = Math.max(intValue(sheetNode.path("rowCount"), 0), maxRowIndex(sheetNode.path("cellData")) + 1);
        int columnCount = Math.max(intValue(sheetNode.path("columnCount"), 0), maxColumnIndex(sheetNode.path("cellData")) + 1);
        rowCount = Math.max(rowCount, 1);
        columnCount = Math.max(columnCount, 1);

        List<List<Object>> rows = new ArrayList<>(rowCount);
        Map<String, UniverCell> cells = new HashMap<>();
        JsonNode cellData = sheetNode.path("cellData");
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            List<Object> row = new ArrayList<>(columnCount);
            JsonNode rowNode = cellData.path(String.valueOf(rowIndex));
            for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
                JsonNode cellNode = rowNode.path(String.valueOf(columnIndex));
                JsonNode valueNode = cellNode.path("v");
                Object value = cellValue(valueNode);
                row.add(value);
                if (cellNode.isObject()) {
                    cells.put(cellKey(rowIndex, columnIndex), new UniverCell(rowIndex, columnIndex, value, resolveStyle(cellNode.path("s"), styles)));
                }
            }
            rows.add(row);
        }

        return new UniverWorkbookSnapshot(
                safeSheetName(firstText(sheetNode.path("name"), preferredSheetName, "Sheet1")),
                rows,
                cells,
                parseColumnWidths(sheetNode.path("columnData"), columnCount),
                parseRowHeights(sheetNode.path("rowData"), rowCount),
                parseMergeData(sheetNode.path("mergeData")),
                parseFreeze(sheetNode.path("freeze")),
                parseAutoFilter(workbookData.path("resources"), sheetId)
        );
    }

    private String resolveSheetId(JsonNode workbookData, JsonNode sheetsNode, String preferredSheetName) {
        JsonNode sheetOrder = workbookData.path("sheetOrder");
        if (sheetOrder.isArray()) {
            for (JsonNode item : sheetOrder) {
                String id = item.asText("");
                JsonNode sheet = sheetsNode.path(id);
                if (!sheet.isObject()) {
                    continue;
                }
                if (!StringUtils.hasText(preferredSheetName) || preferredSheetName.equals(sheet.path("name").asText(""))) {
                    return id;
                }
            }
        }
        Iterator<Map.Entry<String, JsonNode>> iterator = sheetsNode.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            if (!StringUtils.hasText(preferredSheetName) || preferredSheetName.equals(entry.getValue().path("name").asText(""))) {
                return entry.getKey();
            }
        }
        return sheetsNode.fieldNames().next();
    }

    private Map<String, JsonNode> parseStyles(JsonNode stylesNode) {
        if (!stylesNode.isObject()) {
            return Collections.emptyMap();
        }
        Map<String, JsonNode> styles = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> iterator = stylesNode.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            styles.put(entry.getKey(), entry.getValue());
        }
        return styles;
    }

    private JsonNode resolveStyle(JsonNode styleNode, Map<String, JsonNode> styles) {
        if (styleNode == null || styleNode.isMissingNode() || styleNode.isNull()) {
            return null;
        }
        if (styleNode.isTextual()) {
            return styles.get(styleNode.asText());
        }
        return styleNode.isObject() ? styleNode : null;
    }

    private Object cellValue(JsonNode valueNode) {
        if (valueNode == null || valueNode.isMissingNode() || valueNode.isNull()) {
            return "";
        }
        if (valueNode.isNumber()) {
            return valueNode.numberValue();
        }
        if (valueNode.isBoolean()) {
            return valueNode.booleanValue();
        }
        return valueNode.asText("");
    }

    private int maxRowIndex(JsonNode cellData) {
        if (!cellData.isObject()) {
            return -1;
        }
        int max = -1;
        Iterator<String> iterator = cellData.fieldNames();
        while (iterator.hasNext()) {
            max = Math.max(max, parseIndex(iterator.next()));
        }
        return max;
    }

    private int maxColumnIndex(JsonNode cellData) {
        if (!cellData.isObject()) {
            return -1;
        }
        int max = -1;
        Iterator<JsonNode> rows = cellData.elements();
        while (rows.hasNext()) {
            JsonNode row = rows.next();
            if (!row.isObject()) {
                continue;
            }
            Iterator<String> columns = row.fieldNames();
            while (columns.hasNext()) {
                max = Math.max(max, parseIndex(columns.next()));
            }
        }
        return max;
    }

    private Map<Integer, Integer> parseColumnWidths(JsonNode columnData, int columnCount) {
        Map<Integer, Integer> widths = new HashMap<>();
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            widths.put(columnIndex, DEFAULT_COLUMN_WIDTH);
        }
        if (columnData.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> iterator = columnData.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                int columnIndex = parseIndex(entry.getKey());
                if (columnIndex >= 0) {
                    widths.put(columnIndex, Math.max(1, intValue(entry.getValue().path("w"), DEFAULT_COLUMN_WIDTH)));
                }
            }
        }
        return widths;
    }

    private Map<Integer, Integer> parseRowHeights(JsonNode rowData, int rowCount) {
        Map<Integer, Integer> heights = new HashMap<>();
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            heights.put(rowIndex, DEFAULT_ROW_HEIGHT);
        }
        if (rowData.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> iterator = rowData.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                int rowIndex = parseIndex(entry.getKey());
                if (rowIndex >= 0) {
                    heights.put(rowIndex, Math.max(1, intValue(entry.getValue().path("h"), DEFAULT_ROW_HEIGHT)));
                }
            }
        }
        return heights;
    }

    private List<CellRangeAddress> parseMergeData(JsonNode mergeData) {
        if (!mergeData.isArray()) {
            return Collections.emptyList();
        }
        List<CellRangeAddress> ranges = new ArrayList<>();
        for (JsonNode merge : mergeData) {
            int firstRow = intValue(merge.path("startRow"), -1);
            int firstColumn = intValue(merge.path("startColumn"), -1);
            int lastRow = intValue(merge.path("endRow"), -1);
            int lastColumn = intValue(merge.path("endColumn"), -1);
            if (firstRow >= 0 && firstColumn >= 0 && lastRow >= firstRow && lastColumn >= firstColumn) {
                ranges.add(new CellRangeAddress(firstRow, lastRow, firstColumn, lastColumn));
            }
        }
        return ranges;
    }

    private UniverFreeze parseFreeze(JsonNode freeze) {
        if (!freeze.isObject()) {
            return null;
        }
        int xSplit = intValue(freeze.path("xSplit"), intValue(freeze.path("startColumn"), 0));
        int ySplit = intValue(freeze.path("ySplit"), intValue(freeze.path("startRow"), 0));
        if (xSplit <= 0 && ySplit <= 0) {
            return null;
        }
        return new UniverFreeze(Math.max(0, xSplit), Math.max(0, ySplit));
    }

    private CellRangeAddress parseAutoFilter(JsonNode resources, String sheetId) {
        if (!resources.isArray()) {
            return null;
        }
        for (JsonNode resource : resources) {
            if (!"SHEET_FILTER_PLUGIN".equals(resource.path("name").asText(""))) {
                continue;
            }
            JsonNode data = resource.path("data");
            JsonNode filterData = data.isTextual() ? parseJson(data.asText("")) : data;
            JsonNode ref = filterData.path(sheetId).path("ref");
            int firstRow = intValue(ref.path("startRow"), -1);
            int firstColumn = intValue(ref.path("startColumn"), -1);
            int lastRow = intValue(ref.path("endRow"), -1);
            int lastColumn = intValue(ref.path("endColumn"), -1);
            if (firstRow >= 0 && firstColumn >= 0 && lastRow >= firstRow && lastColumn >= firstColumn) {
                return new CellRangeAddress(firstRow, lastRow, firstColumn, lastColumn);
            }
        }
        return null;
    }

    private JsonNode parseJson(String text) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readTree(text);
        } catch (Exception ignored) {
            return com.fasterxml.jackson.databind.node.MissingNode.getInstance();
        }
    }

    private static String cellKey(int rowIndex, int columnIndex) {
        return rowIndex + ":" + columnIndex;
    }

    private int intValue(JsonNode node, int defaultValue) {
        return node != null && node.isNumber() ? node.asInt(defaultValue) : defaultValue;
    }

    private int parseIndex(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return -1;
        }
    }

    private String firstText(JsonNode node, String fallback, String defaultValue) {
        String value = node == null ? null : node.asText(null);
        if (StringUtils.hasText(value)) {
            return value;
        }
        return StringUtils.hasText(fallback) ? fallback : defaultValue;
    }

    private String safeSheetName(String value) {
        String name = StringUtils.hasText(value) ? value.trim() : "Sheet1";
        name = name.replaceAll("[\\\\/?*\\[\\]:]", "_");
        return name.length() > 31 ? name.substring(0, 31) : name;
    }

    private static final class UniverWorkbookSnapshot {
        private final String sheetName;
        private final List<List<Object>> rows;
        private final Map<String, UniverCell> cells;
        private final Map<Integer, Integer> columnWidths;
        private final Map<Integer, Integer> rowHeights;
        private final List<CellRangeAddress> mergeRanges;
        private final UniverFreeze freeze;
        private final CellRangeAddress autoFilter;

        private UniverWorkbookSnapshot(String sheetName, List<List<Object>> rows, Map<String, UniverCell> cells,
                Map<Integer, Integer> columnWidths, Map<Integer, Integer> rowHeights,
                List<CellRangeAddress> mergeRanges, UniverFreeze freeze, CellRangeAddress autoFilter) {
            this.sheetName = sheetName;
            this.rows = rows;
            this.cells = cells;
            this.columnWidths = columnWidths;
            this.rowHeights = rowHeights;
            this.mergeRanges = mergeRanges;
            this.freeze = freeze;
            this.autoFilter = autoFilter;
        }
    }

    private static final class UniverCell {
        private final int rowIndex;
        private final int columnIndex;
        private final Object value;
        private final JsonNode style;

        private UniverCell(int rowIndex, int columnIndex, Object value, JsonNode style) {
            this.rowIndex = rowIndex;
            this.columnIndex = columnIndex;
            this.value = value;
            this.style = style;
        }
    }

    private static final class UniverFreeze {
        private final int xSplit;
        private final int ySplit;

        private UniverFreeze(int xSplit, int ySplit) {
            this.xSplit = xSplit;
            this.ySplit = ySplit;
        }
    }

    private static final class UniverSheetWriteHandler implements SheetWriteHandler {
        private final UniverWorkbookSnapshot snapshot;

        private UniverSheetWriteHandler(UniverWorkbookSnapshot snapshot) {
            this.snapshot = snapshot;
        }

        @Override
        public void afterSheetCreate(SheetWriteHandlerContext context) {
            Sheet sheet = context.getWriteSheetHolder().getSheet();
            snapshot.columnWidths.forEach((columnIndex, width) -> sheet.setColumnWidth(columnIndex,
                    Math.min(255 * EXCEL_COLUMN_WIDTH_UNIT, Math.max(EXCEL_COLUMN_WIDTH_UNIT, width * 36))));
            if (snapshot.freeze != null) {
                sheet.createFreezePane(snapshot.freeze.xSplit, snapshot.freeze.ySplit);
            }
        }

        @Override
        public void afterSheetDispose(SheetWriteHandlerContext context) {
            Sheet sheet = context.getWriteSheetHolder().getSheet();
            snapshot.rowHeights.forEach((rowIndex, height) -> {
                Row row = sheet.getRow(rowIndex);
                if (row != null) {
                    row.setHeightInPoints(Math.max(1, height) * 72f / 96f);
                }
            });
            for (CellRangeAddress range : snapshot.mergeRanges) {
                sheet.addMergedRegion(range);
            }
            if (snapshot.autoFilter != null) {
                sheet.setAutoFilter(snapshot.autoFilter);
            }
        }
    }

    private static final class UniverCellWriteHandler implements CellWriteHandler {
        private final UniverWorkbookSnapshot snapshot;
        private final Map<String, CellStyle> styleCache = new HashMap<>();

        private UniverCellWriteHandler(UniverWorkbookSnapshot snapshot) {
            this.snapshot = snapshot;
        }

        @Override
        public void afterCellDispose(CellWriteHandlerContext context) {
            Cell cell = context.getCell();
            if (cell == null) {
                return;
            }
            UniverCell univerCell = snapshot.cells.get(cellKey(cell.getRowIndex(), cell.getColumnIndex()));
            if (univerCell == null) {
                return;
            }
            applyValue(cell, univerCell.value);
            if (univerCell.style != null && univerCell.style.isObject()) {
                cell.setCellStyle(styleCache.computeIfAbsent(univerCell.style.toString(),
                        ignored -> buildCellStyle(cell.getSheet().getWorkbook(), univerCell.style)));
            }
        }

        private void applyValue(Cell cell, Object value) {
            if (value instanceof Number) {
                cell.setCellValue(((Number) value).doubleValue());
            } else if (value instanceof Boolean) {
                cell.setCellValue((Boolean) value);
            } else {
                cell.setCellValue(value == null ? "" : String.valueOf(value));
            }
        }

        private CellStyle buildCellStyle(Workbook workbook, JsonNode styleNode) {
            CellStyle style = workbook.createCellStyle();
            style.setAlignment(horizontalAlignment(styleNode.path("ht").asInt(1)));
            style.setVerticalAlignment(verticalAlignment(styleNode.path("vt").asInt(1)));
            int textWrap = styleNode.path("tb").asInt(1);
            style.setWrapText(textWrap == 2 || textWrap == 3);

            applyFill(workbook, style, styleNode.path("bg").path("rgb").asText(null));
            applyBorder(workbook, style, styleNode.path("bd"));
            applyFont(workbook, style, styleNode);
            String numberPattern = styleNode.path("n").path("pattern").asText(null);
            if (StringUtils.hasText(numberPattern)) {
                style.setDataFormat(workbook.createDataFormat().getFormat(numberPattern));
            }
            return style;
        }

        private void applyFill(Workbook workbook, CellStyle style, String rgb) {
            Color color = parseColor(rgb);
            if (color == null) {
                return;
            }
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            if (style instanceof XSSFCellStyle && workbook instanceof XSSFWorkbook) {
                ((XSSFCellStyle) style).setFillForegroundColor(new XSSFColor(color, ((XSSFWorkbook) workbook).getStylesSource().getIndexedColors()));
            } else {
                style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            }
        }

        private void applyBorder(Workbook workbook, CellStyle style, JsonNode borderNode) {
            applySingleBorder(workbook, style, borderNode.path("t"), "t");
            applySingleBorder(workbook, style, borderNode.path("r"), "r");
            applySingleBorder(workbook, style, borderNode.path("b"), "b");
            applySingleBorder(workbook, style, borderNode.path("l"), "l");
        }

        private void applySingleBorder(Workbook workbook, CellStyle style, JsonNode border, String side) {
            if (!border.isObject()) {
                return;
            }
            BorderStyle borderStyle = borderStyle(border.path("s").asInt(1));
            switch (side) {
                case "t":
                    style.setBorderTop(borderStyle);
                    setBorderColor(workbook, style, border.path("cl").path("rgb").asText(null), side);
                    break;
                case "r":
                    style.setBorderRight(borderStyle);
                    setBorderColor(workbook, style, border.path("cl").path("rgb").asText(null), side);
                    break;
                case "b":
                    style.setBorderBottom(borderStyle);
                    setBorderColor(workbook, style, border.path("cl").path("rgb").asText(null), side);
                    break;
                case "l":
                    style.setBorderLeft(borderStyle);
                    setBorderColor(workbook, style, border.path("cl").path("rgb").asText(null), side);
                    break;
                default:
                    break;
            }
        }

        private void setBorderColor(Workbook workbook, CellStyle style, String rgb, String side) {
            Color color = parseColor(rgb);
            if (color == null || !(style instanceof XSSFCellStyle) || !(workbook instanceof XSSFWorkbook)) {
                return;
            }
            XSSFColor xssfColor = new XSSFColor(color, ((XSSFWorkbook) workbook).getStylesSource().getIndexedColors());
            XSSFCellStyle xssfStyle = (XSSFCellStyle) style;
            if ("t".equals(side)) {
                xssfStyle.setTopBorderColor(xssfColor);
            } else if ("r".equals(side)) {
                xssfStyle.setRightBorderColor(xssfColor);
            } else if ("b".equals(side)) {
                xssfStyle.setBottomBorderColor(xssfColor);
            } else if ("l".equals(side)) {
                xssfStyle.setLeftBorderColor(xssfColor);
            }
        }

        private void applyFont(Workbook workbook, CellStyle style, JsonNode styleNode) {
            Font font = workbook.createFont();
            boolean changed = false;
            if (styleNode.path("bl").asInt(0) == 1) {
                font.setBold(true);
                changed = true;
            }
            Color color = parseColor(styleNode.path("cl").path("rgb").asText(null));
            if (color != null && font instanceof org.apache.poi.xssf.usermodel.XSSFFont && workbook instanceof XSSFWorkbook) {
                ((org.apache.poi.xssf.usermodel.XSSFFont) font).setColor(
                        new XSSFColor(color, ((XSSFWorkbook) workbook).getStylesSource().getIndexedColors()));
                changed = true;
            }
            if (changed) {
                style.setFont(font);
            }
        }

        private HorizontalAlignment horizontalAlignment(int value) {
            if (value == 2) {
                return HorizontalAlignment.CENTER;
            }
            if (value == 3) {
                return HorizontalAlignment.RIGHT;
            }
            return HorizontalAlignment.LEFT;
        }

        private VerticalAlignment verticalAlignment(int value) {
            if (value == 2) {
                return VerticalAlignment.CENTER;
            }
            if (value == 3) {
                return VerticalAlignment.BOTTOM;
            }
            return VerticalAlignment.TOP;
        }

        private BorderStyle borderStyle(int value) {
            if (value <= 0) {
                return BorderStyle.NONE;
            }
            if (value == 2) {
                return BorderStyle.MEDIUM;
            }
            if (value == 3) {
                return BorderStyle.DASHED;
            }
            return BorderStyle.THIN;
        }

        private Color parseColor(String rgb) {
            if (!StringUtils.hasText(rgb)) {
                return null;
            }
            String value = rgb.trim();
            if (value.startsWith("#")) {
                value = value.substring(1);
            }
            if (value.length() == 8) {
                value = value.substring(2);
            }
            if (value.length() != 6) {
                return null;
            }
            try {
                return new Color(Integer.parseInt(value, 16));
            } catch (Exception ignored) {
                return null;
            }
        }
    }
}
