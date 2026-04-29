package com.yss.valset.transfer.domain.rule;

import com.alibaba.qlexpress4.annotation.QLFunction;
import com.yss.valset.common.support.SpreadsheetXmlSupport;
import com.yss.valset.domain.exception.FileAccessException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ooxml.util.SAXHelper;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.nio.charset.Charset;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.stream.Stream;
import java.util.regex.Pattern;
import java.util.Locale;
import java.util.ArrayList;

/**
 * 脚本规则可用的基础函数。
 */
public class TransferRuleFunctions {

    private static final Charset[] CSV_CHARSETS = new Charset[] {
            Charset.forName("UTF-8"),
            Charset.forName("GBK"),
            Charset.forName("GB2312")
    };
    private static final int DEFAULT_HEADER_SCAN_LIMIT = 100;

    @QLFunction({"containsIgnoreCase"})
    public boolean containsIgnoreCase(Object source, Object keyword) {
        String sourceText = normalizeKeyword(source);
        String keywordText = normalizeKeyword(keyword);
        if (sourceText.isBlank() || keywordText.isBlank()) {
            return false;
        }
        return sourceText.toLowerCase(Locale.ROOT).contains(keywordText.toLowerCase(Locale.ROOT));
    }

    /**
     * 判断文本是否有实际内容，供 QLExpress 直接调用。
     */
    @QLFunction({"hasText"})
    public boolean hasText(Object value) {
        return value != null && StringUtils.hasText(String.valueOf(value));
    }

    @QLFunction({"matchesRegex"})
    public boolean matchesRegex(String source, Object regex) {
        if (source == null || regex == null) {
            return false;
        }
        String sourceText = normalizeKeyword(source);
        String regexText = normalizeKeyword(regex);
        if (sourceText.isBlank() || regexText.isBlank()) {
            return false;
        }
        return Pattern.compile(regexText).matcher(sourceText).matches();
    }

    /**
     * 判断文本是否命中任意一个正则表达式。
     */
    @QLFunction({"matchesAnyRegex"})
    public boolean matchesAnyRegex(Object source, Object regexList) {
        String sourceText = normalizeKeyword(source);
        if (sourceText.isBlank() || regexList == null) {
            return false;
        }
        if (regexList instanceof Collection<?> collection) {
            for (Object regex : collection) {
                if (matchesRegex(sourceText, String.valueOf(regex))) {
                    return true;
                }
            }
            return false;
        }
        if (regexList.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(regexList);
            for (int index = 0; index < length; index++) {
                if (matchesRegex(sourceText, String.valueOf(java.lang.reflect.Array.get(regexList, index)))) {
                    return true;
                }
            }
            return false;
        }
        String regexText = normalizeKeyword(regexList);
        if (regexText.isBlank()) {
            return false;
        }
        for (String item : regexText.split("[,;|\\n]")) {
            if (matchesRegex(sourceText, item)) {
                return true;
            }
        }
        return false;
    }

    @QLFunction({"isExcel"})
    public boolean isExcel(Object fileName) {
        return hasExtension(fileName, ".xlsx", ".xls");
    }

    @QLFunction({"isExcelFile"})
    public boolean isExcelFile(Object fileName) {
        return isExcel(fileName);
    }

    @QLFunction({"isCsv"})
    public boolean isCsv(Object fileName) {
        return hasExtension(fileName, ".csv");
    }

    @QLFunction({"isCsvFile"})
    public boolean isCsvFile(Object fileName) {
        return isCsv(fileName);
    }

    @QLFunction({"readExcelData"})
    public List<List<String>> readExcelData(Object source) {
        Path filePath = resolvePath(source);
        validateReadableFile(filePath);
        try {
            return readExcelData(filePath);
        } catch (IOException e) {
            throw new IllegalStateException("读取 Excel 数据失败，filePath=" + filePath, e);
        }
    }

    @QLFunction({"readCsvData"})
    public List<List<String>> readCsvData(Object source) {
        Path filePath = resolvePath(source);
        validateReadableFile(filePath);
        IOException lastError = null;
        for (Charset charset : CSV_CHARSETS) {
            try {
                return readCsvData(filePath, charset);
            } catch (CharacterCodingException e) {
                lastError = e;
            } catch (IOException e) {
                lastError = e;
                if (isLikelyEncodingFailure(e)) {
                    continue;
                }
                throw new IllegalStateException("读取 CSV 数据失败，filePath=" + filePath, e);
            }
        }
        throw new IllegalStateException("读取 CSV 数据失败，filePath=" + filePath, lastError);
    }

    /**
     * 只读取 Excel 前 N 行，适合大文件的表头预检。
     */
    @QLFunction({"readExcelDataWithin"})
    public List<List<String>> readExcelDataWithin(Object source, Integer maxRows) {
        Path filePath = resolvePath(source);
        validateReadableFile(filePath);
        try {
            return readExcelDataWithin(filePath, normalizeScanLimit(maxRows));
        } catch (IOException e) {
            throw new IllegalStateException("读取 Excel 预览数据失败，filePath=" + filePath, e);
        }
    }

    /**
     * 只读取 CSV 前 N 行，适合大文件的表头预检。
     */
    @QLFunction({"readCsvDataWithin"})
    public List<List<String>> readCsvDataWithin(Object source, Integer maxRows) {
        Path filePath = resolvePath(source);
        validateReadableFile(filePath);
        IOException lastError = null;
        int limit = normalizeScanLimit(maxRows);
        for (Charset charset : CSV_CHARSETS) {
            try {
                return readCsvDataWithin(filePath, charset, limit);
            } catch (CharacterCodingException e) {
                lastError = e;
            } catch (IOException e) {
                lastError = e;
                if (isLikelyEncodingFailure(e)) {
                    continue;
                }
                throw new IllegalStateException("读取 CSV 预览数据失败，filePath=" + filePath, e);
            }
        }
        throw new IllegalStateException("读取 CSV 预览数据失败，filePath=" + filePath, lastError);
    }

    /**
     * 在前 N 行中查找同时包含两个关键字的行号。
     */
    @QLFunction({"findHeaderRowIndexWithin"})
    public int findHeaderRowIndexWithin(Object source, Integer maxRows, String keyword1, String keyword2) {
        List<List<String>> rows = readPreviewRows(source, maxRows);
        return findHeaderRowIndex(rows, keyword1, keyword2);
    }

    /**
     * 在前 N 行中判断是否存在同时包含两个关键字的表头行。
     */
    @QLFunction({"hasHeaderKeywordsWithinFirstRows"})
    public boolean hasHeaderKeywordsWithinFirstRows(Object source, Integer maxRows, String keyword1, String keyword2) {
        return findHeaderRowIndexWithin(source, maxRows, keyword1, keyword2) >= 0;
    }

    /**
     * 在前 N 行中判断是否存在同时包含多个关键字的表头行。
     */
    @QLFunction({"hasHeaderKeywordsWithinFirstRowsByList"})
    public boolean hasHeaderKeywordsWithinFirstRowsByList(Object source, Integer maxRows, Collection<?> keywords) {
        return findHeaderRowIndexWithin(source, maxRows, keywords) >= 0;
    }

    /**
     * 识别估值表：在前 N 行内同时找到“科目代码”和“科目名称”即可认为是估值表。
     */
    @QLFunction({"isValuationTable"})
    public boolean isValuationTable(Object source, Integer maxRows) {
        return hasHeaderKeywordsWithinFirstRows(source, maxRows, "科目代码", "科目名称");
    }

    /**
     * 按关键词集合识别估值表。
     */
    @QLFunction({"isValuationTableByKeywords"})
    public boolean isValuationTableByKeywords(Object source, Integer maxRows, Collection<?> keywords) {
        return hasHeaderKeywordsWithinFirstRowsByList(source, maxRows, keywords);
    }

    /**
     * 按标签扩展配置识别估值表。
     */
    @QLFunction({"isValuationTableByMeta"})
    public boolean isValuationTableByMeta(Object source, Object meta) {
        Integer scanLimit = normalizeScanLimit(extractScanLimit(meta));
        Collection<?> keywords = extractHeaderKeywords(meta);
        if (keywords == null || keywords.isEmpty()) {
            keywords = List.of("科目代码", "科目名称");
        }
        return isValuationTableByKeywords(source, scanLimit, keywords);
    }

    public boolean isCompressed(String fileName) {
        if (fileName == null) {
            return false;
        }
        String lower = fileName.toLowerCase();
        return lower.endsWith(".zip") || lower.endsWith(".rar") || lower.endsWith(".7z");
    }

    public boolean senderInWhitelist(String sender, Collection<String> whitelist) {
        if (sender == null || whitelist == null) {
            return false;
        }
        return whitelist.stream().filter(Objects::nonNull).anyMatch(item -> sender.equalsIgnoreCase(item));
    }

    /**
     * 判断文本是否命中任意一个关键词。
     */
    public boolean containsAny(String source, String keywords) {
        return containsAny(source, parseKeywords(keywords));
    }

    /**
     * 判断文本是否命中任意一个关键词。
     */
    public boolean containsAny(Object source, Object keywords) {
        String sourceText = normalizeKeyword(source);
        if (sourceText.isBlank()) {
            return false;
        }
        if (keywords == null) {
            return false;
        }
        if (keywords instanceof Collection<?> collection) {
            return containsAny(sourceText, collection);
        }
        if (keywords.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(keywords);
            List<Object> values = new java.util.ArrayList<>(length);
            for (int index = 0; index < length; index++) {
                values.add(java.lang.reflect.Array.get(keywords, index));
            }
            return containsAny(sourceText, values);
        }
        return containsAny(sourceText, String.valueOf(keywords));
    }

    /**
     * 判断文本是否命中任意一个关键词。
     */
    public boolean containsAny(String source, Collection<?> keywords) {
        if (source == null || source.isBlank() || keywords == null || keywords.isEmpty()) {
            return false;
        }
        String normalizedSource = source.trim().toLowerCase(Locale.ROOT);
        for (Object keyword : keywords) {
            String candidate = normalizeKeyword(keyword);
            if (candidate.isBlank()) {
                continue;
            }
            String normalizedCandidate = candidate.toLowerCase(Locale.ROOT);
            if (normalizedSource.equals(normalizedCandidate)
                    || normalizedSource.contains(normalizedCandidate)
                    || normalizedCandidate.contains(normalizedSource)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断文本是否同时命中所有关键词。
     */
    public boolean containsAll(String source, String keywords) {
        return containsAll(source, parseKeywords(keywords));
    }

    /**
     * 判断文本是否同时命中所有关键词。
     */
    public boolean containsAll(Object source, Object keywords) {
        String sourceText = normalizeKeyword(source);
        if (sourceText.isBlank()) {
            return false;
        }
        if (keywords == null) {
            return false;
        }
        if (keywords instanceof Collection<?> collection) {
            return containsAll(sourceText, collection);
        }
        if (keywords.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(keywords);
            List<Object> values = new java.util.ArrayList<>(length);
            for (int index = 0; index < length; index++) {
                values.add(java.lang.reflect.Array.get(keywords, index));
            }
            return containsAll(sourceText, values);
        }
        return containsAll(sourceText, String.valueOf(keywords));
    }

    /**
     * 判断文本是否同时命中所有关键词。
     */
    public boolean containsAll(String source, Collection<?> keywords) {
        if (source == null || source.isBlank() || keywords == null || keywords.isEmpty()) {
            return false;
        }
        String normalizedSource = source.trim().toLowerCase(Locale.ROOT);
        for (Object keyword : keywords) {
            String candidate = normalizeKeyword(keyword);
            if (candidate.isBlank()) {
                continue;
            }
            String normalizedCandidate = candidate.toLowerCase(Locale.ROOT);
            if (!(normalizedSource.equals(normalizedCandidate)
                    || normalizedSource.contains(normalizedCandidate)
                    || normalizedCandidate.contains(normalizedSource))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断文本是否命中任意一个关键词，供 QLExpress 直接调用。
     */
    @QLFunction({"containsAnyText"})
    public boolean containsAnyText(Object source, Object keywords) {
        return containsAny(source, keywords);
    }

    /**
     * 判断文本是否同时命中所有关键词，供 QLExpress 直接调用。
     */
    @QLFunction({"containsAllText"})
    public boolean containsAllText(Object source, Object keywords) {
        return containsAll(source, keywords);
    }

    private List<String> parseKeywords(String keywords) {
        if (keywords == null || keywords.isBlank()) {
            return List.of();
        }
        return Stream.of(keywords.split("[,;|\\n]"))
                .map(item -> item == null ? "" : item.trim())
                .filter(item -> !item.isBlank())
                .toList();
    }

    private String normalizeKeyword(Object keyword) {
        return keyword == null ? "" : String.valueOf(keyword).trim();
    }

    private boolean hasExtension(Object fileName, String... extensions) {
        if (fileName == null) {
            return false;
        }
        String candidate = normalizeKeyword(fileName);
        if (candidate.isBlank()) {
            return false;
        }
        String lower = candidate.toLowerCase(Locale.ROOT);
        for (String extension : extensions) {
            if (lower.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    private Path resolvePath(Object source) {
        if (source == null) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        if (source instanceof Path path) {
            return path;
        }
        if (source instanceof File file) {
            return file.toPath();
        }
        String pathText = String.valueOf(source).trim();
        if (pathText.isBlank()) {
            throw new IllegalArgumentException("文件路径不能为空");
        }
        return Paths.get(pathText);
    }

    private void validateReadableFile(Path filePath) {
        if (filePath == null || !Files.exists(filePath) || !Files.isReadable(filePath)) {
            throw new FileAccessException(filePath == null ? null : filePath.toString());
        }
    }

    private List<List<String>> readExcelData(Path filePath) throws IOException {
        return readExcelDataWithin(filePath, Integer.MAX_VALUE);
    }

    private List<List<String>> readExcelDataWithin(Path filePath, int maxRows) throws IOException {
        if (SpreadsheetXmlSupport.isSpreadsheetXml(filePath)) {
            return readSpreadsheetXmlData(filePath, maxRows);
        }
        if (isXlsx(filePath)) {
            return readExcelDataWithinXlsx(filePath, maxRows);
        }
        try (InputStream inputStream = Files.newInputStream(filePath);
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            if (workbook.getNumberOfSheets() <= 0) {
                return List.of();
            }

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return List.of();
            }

            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            List<List<String>> data = new ArrayList<>();
            int limit = normalizeScanLimit(maxRows);
            for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum() && rowIndex < limit; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                data.add(readExcelRow(row, formatter, evaluator));
            }
            return data;
        }
    }

    private List<List<String>> readSpreadsheetXmlData(Path filePath, int maxRows) throws IOException {
        List<List<String>> rows = SpreadsheetXmlSupport.readFirstSheetRows(filePath);
        int limit = normalizeScanLimit(maxRows);
        List<List<String>> data = new ArrayList<>();
        for (List<String> row : rows) {
            if (data.size() >= limit) {
                break;
            }
            data.add(normalizeSpreadsheetXmlRow(row));
        }
        return data;
    }

    private List<String> readExcelRow(Row row, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (row == null) {
            return List.of();
        }
        short lastCellNum = row.getLastCellNum();
        if (lastCellNum <= 0) {
            return List.of();
        }

        List<String> values = new ArrayList<>(lastCellNum);
        for (int cellIndex = 0; cellIndex < lastCellNum; cellIndex++) {
            var cell = row.getCell(cellIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell == null) {
                values.add(null);
                continue;
            }
            String value = formatter.formatCellValue(cell, evaluator);
            values.add(value == null || value.isBlank() ? null : value);
        }
        return values;
    }

    private List<String> normalizeSpreadsheetXmlRow(List<String> row) {
        if (row == null || row.isEmpty()) {
            return List.of();
        }
        List<String> normalized = new ArrayList<>(row.size());
        for (String value : row) {
            if (value == null) {
                normalized.add(null);
                continue;
            }
            String trimmed = value.trim();
            normalized.add(trimmed.isEmpty() ? null : trimmed);
        }
        return normalized;
    }

    private List<List<String>> readCsvData(Path filePath, Charset charset) throws IOException {
        return readCsvDataWithin(filePath, charset, Integer.MAX_VALUE);
    }

    private List<List<String>> readCsvDataWithin(Path filePath, Charset charset, int maxRows) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setIgnoreEmptyLines(false)
                .setTrim(false)
                .build();

        try (InputStream inputStream = Files.newInputStream(filePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charset.newDecoder()
                     .onMalformedInput(CodingErrorAction.REPORT)
                     .onUnmappableCharacter(CodingErrorAction.REPORT)));
             CSVParser csvParser = new CSVParser(reader, format)) {
            List<List<String>> data = new ArrayList<>();
            int limit = normalizeScanLimit(maxRows);
            for (CSVRecord record : csvParser) {
                if (data.size() >= limit) {
                    break;
                }
                data.add(readCsvRecord(record));
            }
            return data;
        } catch (UncheckedIOException e) {
            throw new IOException(e.getCause());
        }
    }

    private List<List<String>> readPreviewRows(Object source, Integer maxRows) {
        Path filePath = resolvePath(source);
        validateReadableFile(filePath);
        try {
            if (isCsv(filePath.getFileName())) {
                IOException lastError = null;
                for (Charset charset : CSV_CHARSETS) {
                    try {
                        return readCsvDataWithin(filePath, charset, normalizeScanLimit(maxRows));
                    } catch (CharacterCodingException e) {
                        lastError = e;
                    } catch (IOException e) {
                        lastError = e;
                        if (isLikelyEncodingFailure(e)) {
                            continue;
                        }
                        throw e;
                    }
                }
                throw new IOException(lastError);
            }
            return readExcelDataWithin(filePath, normalizeScanLimit(maxRows));
        } catch (IOException e) {
            throw new IllegalStateException("读取预览数据失败，filePath=" + filePath, e);
        }
    }

    private int findHeaderRowIndex(List<List<String>> rows, String keyword1, String keyword2) {
        if (rows == null || rows.isEmpty()) {
            return -1;
        }
        String expected1 = normalizeKeyword(keyword1);
        String expected2 = normalizeKeyword(keyword2);
        if (expected1.isBlank() || expected2.isBlank()) {
            return -1;
        }
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            String rowText = joinRowText(rows.get(rowIndex));
            if (containsIgnoreCase(rowText, expected1) && containsIgnoreCase(rowText, expected2)) {
                return rowIndex;
            }
        }
        return -1;
    }

    private int findHeaderRowIndexWithin(Object source, Integer maxRows, Collection<?> keywords) {
        List<List<String>> rows = readPreviewRows(source, maxRows);
        return findHeaderRowIndex(rows, keywords);
    }

    private int findHeaderRowIndex(List<List<String>> rows, Collection<?> keywords) {
        if (rows == null || rows.isEmpty() || keywords == null || keywords.isEmpty()) {
            return -1;
        }
        List<String> normalizedKeywords = keywords.stream()
                .map(this::normalizeKeyword)
                .filter(item -> !item.isBlank())
                .toList();
        if (normalizedKeywords.isEmpty()) {
            return -1;
        }
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            String rowText = joinRowText(rows.get(rowIndex));
            boolean matched = true;
            for (String keyword : normalizedKeywords) {
                if (!containsIgnoreCase(rowText, keyword)) {
                    matched = false;
                    break;
                }
            }
            if (matched) {
                return rowIndex;
            }
        }
        return -1;
    }

    private String joinRowText(List<String> row) {
        if (row == null || row.isEmpty()) {
            return "";
        }
        return row.stream()
                .filter(item -> item != null && !item.isBlank())
                .map(String::trim)
                .reduce((left, right) -> left + " " + right)
                .orElse("");
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        if (source == null || keyword == null) {
            return false;
        }
        String normalizedSource = source.trim().toLowerCase(Locale.ROOT);
        String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        if (normalizedSource.isBlank() || normalizedKeyword.isBlank()) {
            return false;
        }
        return normalizedSource.contains(normalizedKeyword);
    }

    private int normalizeScanLimit(Integer maxRows) {
        if (maxRows == null || maxRows <= 0) {
            return DEFAULT_HEADER_SCAN_LIMIT;
        }
        return maxRows;
    }

    private Integer extractScanLimit(Object meta) {
        if (!(meta instanceof Map<?, ?> map)) {
            return DEFAULT_HEADER_SCAN_LIMIT;
        }
        Object scanLimit = map.get("scanLimit");
        if (scanLimit instanceof Number number) {
            return number.intValue();
        }
        if (scanLimit instanceof String text && !text.isBlank()) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return DEFAULT_HEADER_SCAN_LIMIT;
            }
        }
        return DEFAULT_HEADER_SCAN_LIMIT;
    }

    private Collection<?> extractHeaderKeywords(Object meta) {
        if (!(meta instanceof Map<?, ?> map)) {
            return List.of();
        }
        Object keywords = map.get("headerKeywords");
        if (keywords instanceof Collection<?> collection) {
            return collection;
        }
        if (keywords != null && keywords.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(keywords);
            List<Object> values = new ArrayList<>(length);
            for (int index = 0; index < length; index++) {
                values.add(java.lang.reflect.Array.get(keywords, index));
            }
            return values;
        }
        if (keywords instanceof String text && !text.isBlank()) {
            return parseKeywords(text);
        }
        return List.of();
    }

    private boolean isXlsx(Path filePath) {
        return filePath != null && hasExtension(filePath.getFileName(), ".xlsx");
    }

    private List<List<String>> readExcelDataWithinXlsx(Path filePath, int maxRows) throws IOException {
        try (OPCPackage packageFile = OPCPackage.open(filePath.toFile())) {
            XSSFReader reader = new XSSFReader(packageFile);
            StylesTable stylesTable = reader.getStylesTable();
            SharedStrings sharedStrings = reader.getSharedStringsTable();
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            ExcelPreviewSheetHandler sheetHandler = new ExcelPreviewSheetHandler(maxRows);
            XMLReader xmlReader = SAXHelper.newXMLReader();
            xmlReader.setContentHandler(new XSSFSheetXMLHandler(
                    stylesTable,
                    null,
                    sharedStrings,
                    sheetHandler,
                    formatter,
                    false
            ));
            try (InputStream sheetData = reader.getSheetsData().next()) {
                xmlReader.parse(new InputSource(sheetData));
            }
            return sheetHandler.rows();
        } catch (ExcelPreviewStopException ignore) {
            // 已达到最大扫描行数，直接返回已收集内容。
            return ignore.rows();
        } catch (Exception e) {
            throw new IOException("读取 xlsx 预览数据失败，filePath=" + filePath, e);
        }
    }

    private static final class ExcelPreviewStopException extends RuntimeException {
        private final List<List<String>> rows;

        private ExcelPreviewStopException(List<List<String>> rows) {
            super(null, null, false, false);
            this.rows = rows;
        }

        private List<List<String>> rows() {
            return rows;
        }
    }

    private static final class ExcelPreviewSheetHandler implements XSSFSheetXMLHandler.SheetContentsHandler {
        private final int maxRows;
        private final List<List<String>> rows = new ArrayList<>();
        private List<String> currentRow;
        private int currentRowIndex = -1;

        private ExcelPreviewSheetHandler(int maxRows) {
            this.maxRows = maxRows <= 0 ? DEFAULT_HEADER_SCAN_LIMIT : maxRows;
        }

        private List<List<String>> rows() {
            return rows;
        }

        @Override
        public void startRow(int rowNum) {
            if (rowNum >= maxRows) {
                throw new ExcelPreviewStopException(rows);
            }
            currentRowIndex = rowNum;
            currentRow = new ArrayList<>();
        }

        @Override
        public void endRow(int rowNum) {
            if (currentRow == null) {
                return;
            }
            while (rows.size() < rowNum) {
                rows.add(List.of());
            }
            rows.add(currentRow);
        }

        @Override
        public void cell(String cellReference, String formattedValue, XSSFComment comment) {
            if (currentRow == null) {
                return;
            }
            int columnIndex = columnIndexFromCellReference(cellReference);
            while (currentRow.size() < columnIndex) {
                currentRow.add(null);
            }
            currentRow.add(formattedValue == null || formattedValue.isBlank() ? null : formattedValue);
        }

        @Override
        public void headerFooter(String text, boolean isHeader, String tagName) {
            // 不处理页眉页脚。
        }
    }

    private static int columnIndexFromCellReference(String cellReference) {
        if (cellReference == null || cellReference.isBlank()) {
            return 0;
        }
        int columnIndex = 0;
        for (int index = 0; index < cellReference.length(); index++) {
            char current = cellReference.charAt(index);
            if (!Character.isLetter(current)) {
                break;
            }
            columnIndex = columnIndex * 26 + (Character.toUpperCase(current) - 'A' + 1);
        }
        return Math.max(columnIndex - 1, 0);
    }

    private List<String> readCsvRecord(CSVRecord record) {
        List<String> values = new ArrayList<>(record.size());
        for (int index = 0; index < record.size(); index++) {
            String value = record.get(index);
            values.add(value == null || value.isEmpty() ? null : value);
        }
        return values;
    }

    private boolean isLikelyEncodingFailure(IOException exception) {
        Throwable cause = exception;
        while (cause != null) {
            String message = cause.getMessage();
            if (message != null && (message.contains("MalformedInput") || message.contains("UnmappableCharacter"))) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
