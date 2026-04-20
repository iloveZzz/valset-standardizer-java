package com.yss.valset.analysis.parser.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.analysis.support.ExcelParsingSupport;
import com.yss.valset.analysis.support.SubjectHierarchySupport;
import com.yss.valset.domain.model.DataSourceConfig;
import com.yss.valset.domain.model.HeaderColumnMeta;
import com.yss.valset.domain.model.MetricRecord;
import com.yss.valset.domain.model.ParsedValuationData;
import com.yss.valset.domain.model.SubjectRecord;
import com.yss.valset.domain.parser.ValuationDataParser;
import com.yss.valset.extract.rule.ParseRuleSupport;
import com.yss.valset.extract.rule.ParseRuleTemplateResolver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * CSV 格式估值表数据解析器。
 */
@Slf4j
@Component
public class CsvValuationDataParser implements ValuationDataParser {

    private static final String DEFAULT_SHEET_NAME = "CSV_RAW_DATA";
    private static final List<String> FOOTER_KEYWORDS = List.of("制表", "复核", "打印", "备注");
    private static final Charset[] CANDIDATE_CHARSETS = new Charset[] {
            Charset.forName("UTF-8"),
            Charset.forName("GBK"),
            Charset.forName("GB2312")
    };

    private final ObjectMapper objectMapper;
    private final ParseRuleTemplateResolver parseRuleTemplateResolver;

    public CsvValuationDataParser(ObjectMapper objectMapper) {
        this(objectMapper, null);
    }

    @Autowired
    public CsvValuationDataParser(ObjectMapper objectMapper, ParseRuleTemplateResolver parseRuleTemplateResolver) {
        this.objectMapper = objectMapper;
        this.parseRuleTemplateResolver = parseRuleTemplateResolver;
    }

    @Override
    public ParsedValuationData parse(DataSourceConfig config) {
        Path csvPath = Paths.get(config.getSourceUri());
        try {
            long startedAt = System.currentTimeMillis();
            log.info("开始解析 CSV 估值文件，sourceUri={}", config.getSourceUri());
            List<List<Object>> rows = readRows(csvPath);
            if (rows.isEmpty()) {
                log.warn("CSV 估值文件没有可解析行，sourceUri={}", config.getSourceUri());
                return emptyResult(csvPath);
            }
            String fileScene = resolveFileScene(config);
            String fileTypeName = resolveFileTypeName(config);
            List<String> requiredHeaders = resolveRequiredHeaders(fileScene, fileTypeName);
            Pattern subjectCodePattern = resolveSubjectCodePattern(fileScene, fileTypeName);

            // Step 1: 识别表头和数据起始行
            int headerRowIndex = findHeaderRow(rows, requiredHeaders, subjectCodePattern);
            int dataStartRowIndex = findDataStartRow(rows, headerRowIndex, subjectCodePattern);

            // Step 2: 构建分层表头结构（header/headerDetails/headerColumns）
            List<List<String>> headerBlockRows = extractHeaderBlock(rows, headerRowIndex, dataStartRowIndex);
            HeaderLayout headerLayout = buildHeaderLayout(headerBlockRows);
            List<String> headers = headerLayout.headers();
            Map<String, Integer> headerIndex = buildHeaderIndex(headers);

            // Step 3: 解析标题与基础信息，拆分科目行和指标行
            TitleAndInfo titleAndInfo = extractTitleAndBasicInfo(rows, headerRowIndex);
            SplitResult splitResult = splitSubjectsAndMetrics(rows, dataStartRowIndex, headers, headerIndex, subjectCodePattern);
            log.info("CSV 估值文件解析完成，sourceUri={}, headerRow={}, dataStartRow={}, headerCount={}, subjectCount={}, metricCount={}, elapsedMs={}",
                    config.getSourceUri(),
                    headerRowIndex + 1,
                    dataStartRowIndex + 1,
                    headers.size(),
                    splitResult.subjects().size(),
                    splitResult.metrics().size(),
                    System.currentTimeMillis() - startedAt);

            return ParsedValuationData.builder()
                    .workbookPath(csvPath.toAbsolutePath().toString())
                    .sheetName(DEFAULT_SHEET_NAME)
                    .headerRowNumber(headerRowIndex + 1)
                    .dataStartRowNumber(dataStartRowIndex + 1)
                    .title(titleAndInfo.title())
                    .basicInfo(titleAndInfo.basicInfo())
                    .headers(headers)
                    .headerDetails(headerLayout.headerDetails())
                    .headerColumns(headerLayout.headerColumns())
                    .subjects(splitResult.subjects())
                    .metrics(splitResult.metrics())
                    .build();
        } catch (Exception e) {
            log.error("CSV 估值文件解析失败，sourceUri={}", config.getSourceUri(), e);
            throw new IllegalStateException("Failed to parse CSV source: " + config.getSourceUri(), e);
        }
    }

    private List<List<Object>> readRows(Path csvPath) throws Exception {
        Exception lastException = null;
        for (Charset charset : CANDIDATE_CHARSETS) {
            try (Reader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(csvPath), charset.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)));
                 CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.builder()
                         .setIgnoreEmptyLines(false)
                         .setTrim(false)
                         .build())) {
                List<List<Object>> rows = new ArrayList<>();
                for (CSVRecord record : csvParser) {
                    rows.add(extractValues(record));
                }
                if (!rows.isEmpty()) {
                    return rows;
                }
            } catch (CharacterCodingException e) {
                lastException = e;
            } catch (Exception e) {
                lastException = e;
                if (!isLikelyEncodingFailure(e)) {
                    throw e;
                }
            }
        }
        if (lastException != null) {
            throw lastException;
        }
        return List.of();
    }

    private ParsedValuationData emptyResult(Path csvPath) {
        return ParsedValuationData.builder()
                .workbookPath(csvPath.toAbsolutePath().toString())
                .sheetName(DEFAULT_SHEET_NAME)
                .title(csvPath.getFileName().toString())
                .basicInfo(Map.of())
                .headers(List.of())
                .headerDetails(List.of())
                .headerColumns(List.of())
                .subjects(List.of())
                .metrics(List.of())
                .build();
    }

    private int findHeaderRow(List<List<Object>> rows, List<String> requiredHeaders, Pattern subjectCodePattern) {
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            List<Object> rowValues = rows.get(rowIndex);
            if (!CollectionUtils.isEmpty(requiredHeaders) && !ParseRuleSupport.rowContainsAll(rowValues, requiredHeaders)) {
                continue;
            }
            List<String> texts = toRowTexts(rowValues);
            if (texts.containsAll(requiredHeaders)) {
                return rowIndex;
            }
        }
        throw new IllegalArgumentException("未识别到必选表头" + requiredHeaders + "，无法解析这张表。");
    }

    private int findPreviousMeaningfulRow(List<List<Object>> rows, int startIndex) {
        for (int rowIndex = startIndex; rowIndex >= 0; rowIndex--) {
            if (!isBlankRow(toRowTexts(rows.get(rowIndex)))) {
                return rowIndex;
            }
        }
        return -1;
    }

    private int findDataStartRow(List<List<Object>> rows, int headerRowIndex, Pattern subjectCodePattern) {
        int firstMetricCandidate = -1;
        for (int rowIndex = headerRowIndex + 1; rowIndex < rows.size(); rowIndex++) {
            List<Object> rowValues = rows.get(rowIndex);
            if (isFooterRow(rowValues)) {
                break;
            }
            if (ExcelParsingSupport.isSubjectDataRow(rowValues, subjectCodePattern)) {
                return rowIndex;
            }
            if (firstMetricCandidate < 0 && isMetricCandidate(rowValues, subjectCodePattern)) {
                firstMetricCandidate = rowIndex;
            }
        }
        if (firstMetricCandidate >= 0) {
            return firstMetricCandidate;
        }
        throw new IllegalArgumentException("在表头下方未找到科目数据。");
    }

    private List<List<String>> extractHeaderBlock(List<List<Object>> rows, int headerRowIndex, int dataStartRowIndex) {
        List<List<String>> headerBlockRows = new ArrayList<>();
        for (int rowIndex = headerRowIndex; rowIndex < dataStartRowIndex; rowIndex++) {
            List<String> rowTexts = toRowTexts(rows.get(rowIndex));
            if (isBlankRow(rowTexts)) {
                continue;
            }
            headerBlockRows.add(fillMergedHeaderRow(rowTexts));
        }
        return headerBlockRows;
    }

    private HeaderLayout buildHeaderLayout(List<List<String>> headerBlockRows) {
        if (headerBlockRows.isEmpty()) {
            return new HeaderLayout(List.of(), List.of(), List.of());
        }
        int columnCount = headerBlockRows.stream().mapToInt(List::size).max().orElse(0);
        List<String> headers = new ArrayList<>(columnCount);
        List<List<String>> headerDetails = new ArrayList<>(columnCount);
        List<HeaderColumnMeta> headerColumns = new ArrayList<>(columnCount);
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            List<String> pathSegments = new ArrayList<>();
            for (List<String> headerRow : headerBlockRows) {
                if (columnIndex >= headerRow.size()) {
                    continue;
                }
                String segment = headerRow.get(columnIndex);
                if (!segment.isBlank()) {
                    pathSegments.add(segment);
                }
            }
            String headerPath = String.join("|", pathSegments);
            headers.add(headerPath);
            headerDetails.add(pathSegments);
            headerColumns.add(HeaderColumnMeta.builder()
                    .columnIndex(columnIndex)
                    .headerName(headerPath)
                    .headerPath(headerPath)
                    .pathSegments(pathSegments)
                    .blankColumn(headerPath.isBlank())
                    .build());
        }
        return new HeaderLayout(headers, headerDetails, headerColumns);
    }

    private Map<String, Integer> buildHeaderIndex(List<String> headers) {
        Map<String, Integer> headerIndex = new LinkedHashMap<>();
        for (int index = 0; index < headers.size(); index++) {
            String header = headers.get(index);
            if (!header.isBlank()) {
                headerIndex.putIfAbsent(header, index);
            }
        }
        return headerIndex;
    }

    private List<String> fillMergedHeaderRow(List<String> rowTexts) {
        List<String> normalized = new ArrayList<>(rowTexts.size());
        String carry = "";
        for (String text : rowTexts) {
            if (text == null || text.isBlank()) {
                normalized.add(carry);
                continue;
            }
            carry = text;
            normalized.add(text);
        }
        return normalized;
    }

    private boolean isBlankRow(List<String> rowTexts) {
        return rowTexts == null || rowTexts.stream().allMatch(String::isBlank);
    }

    private TitleAndInfo extractTitleAndBasicInfo(List<List<Object>> rows, int headerRowIndex) {
        Map<String, String> basicInfo = new LinkedHashMap<>();
        List<String> titleCandidates = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < headerRowIndex; rowIndex++) {
            List<String> rowTexts = toRowTexts(rows.get(rowIndex));
            List<String> nonEmptyTexts = rowTexts.stream().filter(text -> !text.isBlank()).toList();
            if (nonEmptyTexts.isEmpty()) {
                continue;
            }
            if (nonEmptyTexts.size() == 1 && !nonEmptyTexts.get(0).contains("：") && !nonEmptyTexts.get(0).contains(":")) {
                titleCandidates.add(nonEmptyTexts.get(0));
            }
            for (int cellIndex = 0; cellIndex < rowTexts.size(); cellIndex++) {
                String text = rowTexts.get(cellIndex);
                String delimiter = text.contains("：") ? "：" : (text.contains(":") ? ":" : null);
                if (delimiter == null) {
                    continue;
                }
                String[] parts = text.split(delimiter, 2);
                String key = stripTrailingPunctuation(parts[0]);
                String value = parts.length > 1 ? parts[1].trim() : "";
                if (value.isBlank()) {
                    value = findNextMeaningfulText(rowTexts, cellIndex + 1);
                }
                if (!key.isBlank()) {
                    basicInfo.put(key, stripTrailingPunctuation(value));
                }
            }
        }
        String title = "";
        for (String candidate : titleCandidates) {
            if (candidate.length() > title.length()) {
                title = candidate;
            }
        }
        return new TitleAndInfo(title, basicInfo);
    }

    private SplitResult splitSubjectsAndMetrics(
            List<List<Object>> rows,
            int dataStartRowIndex,
            List<String> headers,
            Map<String, Integer> headerIndex,
            Pattern subjectCodePattern
    ) {
        List<SubjectRecord> subjects = new ArrayList<>();
        List<MetricRecord> metrics = new ArrayList<>();
        for (int rowIndex = dataStartRowIndex; rowIndex < rows.size(); rowIndex++) {
            List<Object> rowValues = rows.get(rowIndex);
            if (isBlankRow(toRowTexts(rowValues))) {
                continue;
            }
            if (isFooterRow(rowValues)) {
                break;
            }
            if (ExcelParsingSupport.isSubjectDataRow(rowValues, subjectCodePattern)) {
                subjects.add(extractSubjectRecord(rowIndex, rowValues, headers, headerIndex));
                continue;
            }
            if (ExcelParsingSupport.isMetricDataRow(rowValues, subjectCodePattern) || ExcelParsingSupport.isMetricRow(rowValues, subjectCodePattern)) {
                metrics.add(extractMetricRecord(rowIndex, rowValues, headers, headerIndex, subjectCodePattern));
            }
        }
        SubjectHierarchySupport.enrichSubjectHierarchy(subjects);
        return new SplitResult(subjects, metrics);
    }

    private List<String> resolveRequiredHeaders(String fileScene, String fileTypeName) {
        if (parseRuleTemplateResolver == null) {
            return List.of("科目代码", "科目名称", "币种");
        }
        List<String> requiredHeaders = parseRuleTemplateResolver.resolveRequiredHeaders(fileScene, fileTypeName);
        return requiredHeaders == null || requiredHeaders.isEmpty()
                ? List.of("科目代码", "科目名称", "币种")
                : requiredHeaders;
    }

    private Pattern resolveSubjectCodePattern(String fileScene, String fileTypeName) {
        if (parseRuleTemplateResolver == null) {
            return Pattern.compile("^\\d{4}[A-Za-z0-9]*$");
        }
        Pattern pattern = parseRuleTemplateResolver.resolveSubjectCodePattern(fileScene, fileTypeName);
        return pattern == null ? Pattern.compile("^\\d{4}[A-Za-z0-9]*$") : pattern;
    }

    private SubjectRecord extractSubjectRecord(
            int rowIndex,
            List<Object> rowValues,
            List<String> headers,
            Map<String, Integer> headerIndex
    ) {
        String rawSubjectCode = getText(rowValues, headers, headerIndex, "科目代码");
        String subjectCode = ExcelParsingSupport.normalizeSubjectCode(rawSubjectCode);
        String subjectName = getText(rowValues, headers, headerIndex, "科目名称");
        List<String> segments = SubjectHierarchySupport.splitSubjectCode(rawSubjectCode);
        List<String> pathCodes = SubjectHierarchySupport.buildSubjectPathCodes(rawSubjectCode, segments);
        return SubjectRecord.builder()
                .sheetName(DEFAULT_SHEET_NAME)
                .rowDataNumber(rowIndex + 1)
                .subjectCode(subjectCode)
                .subjectName(subjectName)
                .level(pathCodes.size())
                .parentCode(null)
                .rootCode(pathCodes.isEmpty() ? subjectCode : pathCodes.get(0))
                .segmentCount(segments.size())
                .pathCodes(pathCodes)
                .rawValues(new ArrayList<>(rowValues))
                .leaf(Boolean.TRUE)
                .build();
    }

    private MetricRecord extractMetricRecord(
            int rowIndex,
            List<Object> rowValues,
            List<String> headers,
            Map<String, Integer> headerIndex,
            Pattern subjectCodePattern
    ) {
        String metricName = normalizeMetricLabel(rowValues);
        if (ExcelParsingSupport.isMetricDataRow(rowValues, subjectCodePattern)) {
            int labelIndex = ExcelParsingSupport.findFirstMeaningfulCellIndex(rowValues);
            Object rawValue = findFirstValueAfterLabel(rowValues, labelIndex);
            return MetricRecord.builder()
                    .sheetName(DEFAULT_SHEET_NAME)
                    .rowDataNumber(rowIndex + 1)
                    .metricName(metricName)
                    .metricType("metric_data")
                    .value(toTextValue(rawValue))
                    .rawValues(Map.of("value", normalizeMetricValue(rawValue)))
                    .build();
        }

        Map<String, Object> rawValues = new LinkedHashMap<>();
        for (String header : headers) {
            if (header == null || header.isBlank()) {
                continue;
            }
            Integer columnIndex = headerIndex.get(header);
            if (columnIndex == null) {
                continue;
            }
            rawValues.put(header, normalizeMetricValue(ExcelParsingSupport.valueAt(rowValues, columnIndex)));
        }
        if (!rawValues.containsKey("科目名称") || rawValues.get("科目名称") == null || rawValues.get("科目名称").toString().isBlank()) {
            rawValues.put("科目名称", metricName);
        }

        Object value = firstNonBlank(rawValues.get("市值"), rawValues.get("成本"), rawValues.get("数量"));
        return MetricRecord.builder()
                .sheetName(DEFAULT_SHEET_NAME)
                .rowDataNumber(rowIndex + 1)
                .metricName(metricName)
                .metricType("metric_row")
                .value(value == null ? "" : String.valueOf(value))
                .rawValues(rawValues)
                .build();
    }

    private List<String> toRowTexts(List<Object> rowValues) {
        List<String> texts = new ArrayList<>(rowValues.size());
        for (Object value : rowValues) {
            texts.add(ExcelParsingSupport.normalizeText(value));
        }
        return texts;
    }

    private String getText(List<Object> rowValues, List<String> headers, Map<String, Integer> headerIndex, String headerName) {
        Integer columnIndex = resolveHeaderIndex(headers, headerIndex, headerName);
        return columnIndex == null ? "" : ExcelParsingSupport.textAt(rowValues, columnIndex);
    }

    private String normalizeMetricLabel(List<Object> rowValues) {
        int labelIndex = ExcelParsingSupport.findFirstMeaningfulCellIndex(rowValues);
        if (labelIndex < 0) {
            return "";
        }
        return stripTrailingPunctuation(ExcelParsingSupport.textAt(rowValues, labelIndex));
    }

    private Integer resolveHeaderIndex(List<String> headers, Map<String, Integer> headerIndex, String headerName) {
        Integer exactIndex = headerIndex.get(headerName);
        if (exactIndex != null) {
            return exactIndex;
        }
        for (int index = 0; index < headers.size(); index++) {
            String header = headers.get(index);
            if (headerMatches(header, headerName)) {
                return index;
            }
        }
        return null;
    }

    private boolean headerMatches(String header, String headerName) {
        if (header == null || header.isBlank() || headerName == null || headerName.isBlank()) {
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

    private boolean isMetricCandidate(List<Object> rowValues, Pattern subjectCodePattern) {
        return (ExcelParsingSupport.isMetricDataRow(rowValues, subjectCodePattern) || ExcelParsingSupport.isMetricRow(rowValues, subjectCodePattern))
                && !isFooterRow(rowValues);
    }

    private boolean isFooterRow(List<Object> rowValues) {
        int labelIndex = ExcelParsingSupport.findFirstMeaningfulCellIndex(rowValues);
        if (labelIndex < 0) {
            return false;
        }
        String labelText = ExcelParsingSupport.textAt(rowValues, labelIndex);
        for (String keyword : FOOTER_KEYWORDS) {
            if (labelText.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String findNextMeaningfulText(List<String> rowTexts, int startIndex) {
        if (rowTexts == null || startIndex < 0 || startIndex >= rowTexts.size()) {
            return "";
        }
        for (int index = startIndex; index < rowTexts.size(); index++) {
            String text = rowTexts.get(index);
            if (text != null && !text.isBlank()) {
                return text;
            }
        }
        return "";
    }

    private String stripTrailingPunctuation(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .replaceAll("[：:]+$", "")
                .trim();
    }

    private Object findFirstValueAfterLabel(List<Object> rowValues, int labelIndex) {
        if (rowValues == null || rowValues.isEmpty() || labelIndex < 0) {
            return "";
        }
        for (int index = labelIndex + 1; index < rowValues.size(); index++) {
            Object value = ExcelParsingSupport.valueAt(rowValues, index);
            String text = ExcelParsingSupport.textAt(rowValues, index);
            if (text.isBlank() || "-".equals(text)) {
                continue;
            }
            return value;
        }
        return "";
    }

    private Object normalizeMetricValue(Object rawValue) {
        BigDecimal number = ExcelParsingSupport.normalizeNumber(rawValue);
        return number != null ? number : ExcelParsingSupport.normalizeText(rawValue);
    }

    private String toTextValue(Object rawValue) {
        Object normalizedValue = normalizeMetricValue(rawValue);
        if (normalizedValue instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        return normalizedValue == null ? "" : String.valueOf(normalizedValue);
    }

    private Object firstNonBlank(Object... values) {
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            if (value instanceof String text && text.isBlank()) {
                continue;
            }
            return value;
        }
        return null;
    }

    private List<Object> extractValues(CSVRecord record) {
        List<Object> values = new ArrayList<>(record.size());
        for (int index = 0; index < record.size(); index++) {
            String value = record.get(index);
            values.add(value == null || value.isEmpty() ? null : value);
        }
        return values;
    }

    private boolean isLikelyEncodingFailure(Exception exception) {
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

    private String resolveFileScene(DataSourceConfig config) {
        return "VALSET";
    }

    private String resolveFileTypeName(DataSourceConfig config) {
        if (config == null || config.getSourceType() == null) {
            return null;
        }
        return config.getSourceType().name();
    }

    private record TitleAndInfo(String title, Map<String, String> basicInfo) {
    }

    private record HeaderLayout(List<String> headers, List<List<String>> headerDetails, List<HeaderColumnMeta> headerColumns) {
    }

    private record SplitResult(List<SubjectRecord> subjects, List<MetricRecord> metrics) {
    }
}
