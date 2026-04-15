package com.yss.subjectmatch.extract.extractor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.subjectmatch.domain.exception.FileAccessException;
import com.yss.subjectmatch.domain.extractor.RawDataExtractor;
import com.yss.subjectmatch.domain.model.DataSourceConfig;
import com.yss.subjectmatch.domain.model.DataSourceType;
import com.yss.subjectmatch.extract.repository.entity.ValuationFileDataPO;
import com.yss.subjectmatch.extract.repository.mapper.ValuationFileDataMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * 基于 Apache POI 的 Excel 原始数据提取器。
 * <p>
 * 该实现同时支持：
 * <ul>
 *   <li>使用 HSSFWorkbook 读取 .xls 文件</li>
 *   <li>使用 SAX 流式解析读取 .xlsx 文件，降低内存占用</li>
 * </ul>
 * <p>
 * 每一行会被序列化为 JSON 数组，并对以下内容做特殊处理：
 * <ul>
 *   <li>公式单元格优先求值</li>
 *   <li>日期单元格转换为 ISO 8601 字符串（yyyy-MM-dd）</li>
 *   <li>空单元格写入 JSON null</li>
 *   <li>数值型内容转为字符串，避免精度丢失</li>
 * </ul>
 * <p>
 * 数据将按批次写入 ODS 表，以提升落库效率。
 */
@Slf4j
@Component
public class PoiRawDataExtractor implements RawDataExtractor {

    private static final int BATCH_SIZE = 1000;
    private static final int MAX_COLUMNS = 10000;
    private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final ValuationFileDataMapper valuationFileDataMapper;
    private final ObjectMapper objectMapper;
    private final boolean evaluateFormulas;

    public PoiRawDataExtractor(ValuationFileDataMapper valuationFileDataMapper, ObjectMapper objectMapper) {
        this(valuationFileDataMapper, objectMapper, false);
    }

    @Autowired
    public PoiRawDataExtractor(ValuationFileDataMapper valuationFileDataMapper,
                               ObjectMapper objectMapper,
                               @Value("${subject.match.extract.evaluate-formulas:false}") boolean evaluateFormulas) {
        this.valuationFileDataMapper = valuationFileDataMapper;
        this.objectMapper = objectMapper;
        this.evaluateFormulas = evaluateFormulas;
    }

    @Override
    public int extract(DataSourceConfig config, Long taskId, Long fileId) {
        if (taskId == null || fileId == null) {
            throw new IllegalArgumentException("taskId and fileId must not be null");
        }

        Path filePath = Paths.get(config.getSourceUri());
        if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
            throw new FileAccessException(config.getSourceUri());
        }

        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(filePath.toFile()))) {
            long startedAt = System.currentTimeMillis();
            int rowCount = extractFromWorkbook(inputStream, filePath, taskId, fileId);
            log.info("Excel 原始数据提取完成，filePath={}, rowCount={}, durationMs={}, evaluateFormulas={}",
                    filePath, rowCount, System.currentTimeMillis() - startedAt, evaluateFormulas);
            return rowCount;
        } catch (IOException e) {
            throw new FileAccessException(config.getSourceUri(), e);
        }
    }

    @Override
    public DataSourceType supportedType() {
        return DataSourceType.EXCEL;
    }

    private int extractFromWorkbook(InputStream inputStream, Path filePath, Long taskId, Long fileId) throws IOException {
        // 优先根据文件魔数判断真实格式，而不是依赖扩展名
        boolean isXls = isXlsFormat(inputStream);
        
        if (isXls) {
            return extractFromHSSF(inputStream, taskId, fileId);
        } else {
            return extractFromSXSSF(inputStream, taskId, fileId);
        }
    }

    /**
     * 通过文件魔数判断是否为 XLS 格式。
     * XLS 文件以 D0CF11E0 开头（OLE2 复合文档格式），XLSX 文件以 504B0304 开头（ZIP 格式）。
     */
    private boolean isXlsFormat(InputStream inputStream) throws IOException {
        // 标记流，便于读取魔数后回退
        if (!inputStream.markSupported()) {
            throw new IOException("InputStream does not support mark/reset");
        }
        
        // 读取前 8 个字节判断文件类型
        inputStream.mark(8);
        byte[] magic = new byte[8];
        int read = inputStream.read(magic);
        inputStream.reset();
        
        if (read < 8) {
            // 文件过小时无法可靠判断，默认按非 XLS 处理
            return false;
        }
        
        // XLS (BIFF5/BIFF8/OLE2): D0 CF 11 E0 A1 B1 1A E1
        // XLSX (ZIP): 50 4B 03 04
        boolean isOle2 = (magic[0] == (byte) 0xD0 && 
                          magic[1] == (byte) 0xCF && 
                          magic[2] == (byte) 0x11 && 
                          magic[3] == (byte) 0xE0);
        
        return isOle2;
    }

    private int extractFromHSSF(InputStream inputStream, Long taskId, Long fileId) throws IOException {
        try (HSSFWorkbook workbook = new HSSFWorkbook(inputStream)) {
            FormulaEvaluator evaluator = evaluateFormulas ? workbook.getCreationHelper().createFormulaEvaluator() : null;
            return extractFromWorkbook(workbook, evaluator, taskId, fileId);
        }
    }

    private int extractFromSXSSF(InputStream inputStream, Long taskId, Long fileId) throws IOException {
        Path tempFile = Files.createTempFile("ods-extract-", ".xlsx");
        OPCPackage opcPackage = null;
        try {
            Files.copy(inputStream, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            opcPackage = OPCPackage.open(tempFile.toFile());
            ReadOnlySharedStringsTable sharedStrings = new ReadOnlySharedStringsTable(opcPackage);
            XSSFReader reader = new XSSFReader(opcPackage);
            StylesTable stylesTable = reader.getStylesTable();
            int[] result = new int[] {0, 0};

            StreamingSheetHandler sheetHandler = new StreamingSheetHandler(
                    sharedStrings,
                    stylesTable,
                    taskId,
                    fileId,
                    result
            );
            XMLReader xmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
            XSSFReader.SheetIterator sheetIterator = (XSSFReader.SheetIterator) reader.getSheetsData();
            while (sheetIterator.hasNext()) {
                try (InputStream sheetStream = sheetIterator.next()) {
                    xmlReader.setContentHandler(sheetHandler);
                    xmlReader.parse(new InputSource(sheetStream));
                }
            }
            sheetHandler.flushBatch();
            return result[1];
        } catch (Exception e) {
            if (e instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("流式解析 xlsx 工作簿失败", e);
        } finally {
            if (opcPackage != null) {
                try {
                    opcPackage.close();
                } catch (Exception closeException) {
                    log.warn("关闭 OPCPackage 失败，临时文件={}", tempFile, closeException);
                }
            }
            Files.deleteIfExists(tempFile);
        }
    }

    private int extractFromWorkbook(Workbook workbook, FormulaEvaluator evaluator, Long taskId, Long fileId) {
        int persistedRows = 0;
        int rowDataNumber = 0;
        List<ValuationFileDataPO> batch = new ArrayList<>(BATCH_SIZE);

        for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
            Sheet sheet = workbook.getSheetAt(sheetIndex);
            if (sheet == null) {
                continue;
            }

            for (Row row : sheet) {
                if (row == null) {
                    continue;
                }

                rowDataNumber++;
                String rowDataJson = serializeRow(row, evaluator, rowDataNumber);

                if (rowDataJson != null) {
                    ValuationFileDataPO po = new ValuationFileDataPO();
                    po.setTaskId(taskId);
                    po.setFileId(fileId);
                    po.setRowDataNumber(rowDataNumber);
                    po.setRowDataJson(rowDataJson);
                    batch.add(po);
                    persistedRows++;

                    if (batch.size() >= BATCH_SIZE) {
                        valuationFileDataMapper.insert(new ArrayList<>(batch));
                        batch.clear();
                    }
                }
            }
        }

        if (!batch.isEmpty()) {
            // 写入最后不足一个批次的剩余数据
            valuationFileDataMapper.insert(new ArrayList<>(batch));
        }

        return persistedRows;
    }

    private String serializeRow(Row row, FormulaEvaluator evaluator, int rowDataNumber) {
        List<String> cellValues = new ArrayList<>();
        int lastCellNum = row.getLastCellNum();

        if (lastCellNum > MAX_COLUMNS) {
            log.warn("第 {} 行列数超过上限 {}，已截断为 {} 列",
                    rowDataNumber, MAX_COLUMNS, MAX_COLUMNS);
            lastCellNum = MAX_COLUMNS;
        }

        for (int cellIndex = 0; cellIndex < lastCellNum; cellIndex++) {
            Cell cell = row.getCell(cellIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            String cellValue = extractCellValue(cell, evaluator);
            cellValues.add(cellValue);
        }

        try {
            return objectMapper.writeValueAsString(cellValues);
        } catch (JsonProcessingException e) {
            log.error("第 {} 行序列化为 JSON 失败，已跳过", rowDataNumber, e);
            return null;
        }
    }

    private String extractCellValue(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) {
            return null;
        }

        CellType cellType = cell.getCellType();

        // 公式单元格优先求值
        if (cellType == CellType.FORMULA) {
            if (!evaluateFormulas) {
                return extractDirectCellValue(cell, cell.getCachedFormulaResultType());
            }
            try {
                CellValue cellValue = evaluator.evaluate(cell);
                return extractEvaluatedCellValue(cellValue, cell);
            } catch (Exception e) {
                log.warn("单元格 {} 公式求值失败，返回空值", cell.getAddress(), e);
                return null;
            }
        }

        return extractDirectCellValue(cell, cellType);
    }

    private String extractEvaluatedCellValue(CellValue cellValue, Cell cell) {
        if (cellValue == null) {
            return null;
        }

        return switch (cellValue.getCellType()) {
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield formatDateCell(cell.getNumericCellValue());
                }
                yield formatNumericValue(cellValue.getNumberValue());
            }
            case STRING -> cellValue.getStringValue();
            case BOOLEAN -> String.valueOf(cellValue.getBooleanValue());
            case BLANK -> null;
            default -> null;
        };
    }

    private String extractDirectCellValue(Cell cell, CellType cellType) {
        return switch (cellType) {
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield formatDateCell(cell.getNumericCellValue());
                }
                yield formatNumericValue(cell.getNumericCellValue());
            }
            case STRING -> cell.getStringCellValue();
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case BLANK -> null;
            default -> null;
        };
    }

    private String formatDateCell(double numericValue) {
        try {
            Date date = DateUtil.getJavaDate(numericValue);
            LocalDate localDate = date.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            return localDate.format(ISO_DATE_FORMATTER);
        } catch (Exception e) {
            log.warn("日期单元格 {} 格式化失败，回退为数值字符串", numericValue, e);
            return formatNumericValue(numericValue);
        }
    }

    private String formatNumericValue(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private final class StreamingSheetHandler extends DefaultHandler {
        private final ReadOnlySharedStringsTable sharedStrings;
        private final StylesTable stylesTable;
        private final Long taskId;
        private final Long fileId;
        private final int[] result;
        private final List<ValuationFileDataPO> batch = new ArrayList<>(BATCH_SIZE);
        private final StringBuilder valueBuffer = new StringBuilder();
        private List<String> currentRow;
        private boolean inValue;
        private boolean inInlineStringText;
        private String currentCellType;
        private String currentCellRef;
        private int currentStyleIndex = -1;
        private int currentColumnIndex;

        private StreamingSheetHandler(ReadOnlySharedStringsTable sharedStrings,
                                      StylesTable stylesTable,
                                      Long taskId,
                                      Long fileId,
                                      int[] result) {
            this.sharedStrings = sharedStrings;
            this.stylesTable = stylesTable;
            this.taskId = taskId;
            this.fileId = fileId;
            this.result = result;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            switch (qName) {
                case "row" -> currentRow = new ArrayList<>();
                case "c" -> {
                    currentCellRef = attributes.getValue("r");
                    currentCellType = attributes.getValue("t");
                    currentStyleIndex = parseStyleIndex(attributes.getValue("s"));
                    currentColumnIndex = resolveColumnIndex(currentCellRef);
                    while (currentRow.size() < currentColumnIndex) {
                        currentRow.add(null);
                    }
                    valueBuffer.setLength(0);
                }
                case "v" -> {
                    inValue = true;
                    valueBuffer.setLength(0);
                }
                case "is" -> inInlineStringText = true;
                case "t" -> {
                    if (inInlineStringText) {
                        inValue = true;
                        valueBuffer.setLength(0);
                    }
                }
                default -> {
                }
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            if (inValue) {
                valueBuffer.append(ch, start, length);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            switch (qName) {
                case "v", "t" -> inValue = false;
                case "is" -> inInlineStringText = false;
                case "c" -> {
                    String rawValue = valueBuffer.toString();
                    String cellValue = resolveCellValue(rawValue, currentCellType, currentStyleIndex);
                    if (currentColumnIndex >= MAX_COLUMNS) {
                        log.warn("第 {} 行列数超过上限 {}，已截断为 {} 列",
                                result[0] + 1, MAX_COLUMNS, MAX_COLUMNS);
                        cellValue = null;
                    }
                    if (currentColumnIndex >= MAX_COLUMNS) {
                        return;
                    }
                    if (currentRow.size() == currentColumnIndex) {
                        currentRow.add(cellValue);
                    } else if (currentRow.size() < currentColumnIndex) {
                        while (currentRow.size() < currentColumnIndex) {
                            currentRow.add(null);
                        }
                        currentRow.add(cellValue);
                    } else {
                        currentRow.set(currentColumnIndex - 1, cellValue);
                    }
                }
                case "row" -> persistCurrentRow();
                default -> {
                }
            }
        }

        private void persistCurrentRow() {
            if (currentRow == null) {
                return;
            }
            int rowDataNumber = ++result[0];
            try {
                String rowDataJson = objectMapper.writeValueAsString(currentRow);
                ValuationFileDataPO po = new ValuationFileDataPO();
                po.setTaskId(taskId);
                po.setFileId(fileId);
                po.setRowDataNumber(rowDataNumber);
                po.setRowDataJson(rowDataJson);
                batch.add(po);
                if (batch.size() >= BATCH_SIZE) {
                    valuationFileDataMapper.insert(new ArrayList<>(batch));
                    batch.clear();
                }
                result[1]++;
            } catch (JsonProcessingException e) {
                log.error("第 {} 行序列化为 JSON 失败，已跳过", rowDataNumber, e);
            }
        }

        private void flushBatch() {
            if (!batch.isEmpty()) {
                valuationFileDataMapper.insert(new ArrayList<>(batch));
                batch.clear();
            }
        }

        private String resolveCellValue(String rawValue, String cellType, int styleIndex) {
            if (rawValue == null || rawValue.isEmpty()) {
                return null;
            }
            if ("s".equals(cellType)) {
                int index = Integer.parseInt(rawValue);
                return sharedStrings.getItemAt(index).getString();
            }
            if ("inlineStr".equals(cellType) || "str".equals(cellType)) {
                return rawValue;
            }
            if ("b".equals(cellType)) {
                return "1".equals(rawValue) ? "true" : "false";
            }
            if (isDateStyle(styleIndex)) {
                return formatDateCell(Double.parseDouble(rawValue));
            }
            if (isNumericLike(rawValue)) {
                return new BigDecimal(rawValue).stripTrailingZeros().toPlainString();
            }
            return rawValue;
        }

        private boolean isDateStyle(int styleIndex) {
            if (styleIndex < 0 || stylesTable == null) {
                return false;
            }
            try {
                var style = stylesTable.getStyleAt(styleIndex);
                return DateUtil.isADateFormat(style.getDataFormat(), style.getDataFormatString());
            } catch (Exception ignored) {
                return false;
            }
        }

        private int parseStyleIndex(String style) {
            if (style == null || style.isBlank()) {
                return -1;
            }
            try {
                return Integer.parseInt(style);
            } catch (NumberFormatException exception) {
                return -1;
            }
        }

        private int resolveColumnIndex(String cellReference) {
            if (cellReference == null || cellReference.isBlank()) {
                return currentRow.size();
            }
            return new CellReference(cellReference).getCol();
        }

        private boolean isNumericLike(String value) {
            try {
                new BigDecimal(value);
                return true;
            } catch (NumberFormatException exception) {
                return false;
            }
        }
    }
}
