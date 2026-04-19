package com.yss.valset.analysis.parser.file;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.domain.model.DataSourceConfig;
import com.yss.valset.domain.model.HeaderColumnMeta;
import com.yss.valset.domain.model.MetricRecord;
import com.yss.valset.domain.model.ParsedValuationData;
import com.yss.valset.domain.model.SubjectRecord;
import com.yss.valset.domain.parser.ValuationDataParser;
import com.yss.valset.extract.repository.entity.ValuationFileDataPO;
import com.yss.valset.extract.repository.mapper.ValuationFileDataMapper;
import com.yss.valset.extract.rule.ParseRuleExpressions;
import com.yss.valset.analysis.support.ExcelParsingSupport;
import com.yss.valset.analysis.support.SubjectHierarchySupport;
import com.yss.valset.extract.rule.ParseRuleTemplateResolver;
import com.yss.valset.extract.rule.QlexpressParseRuleEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 基于 ODS 原始行数据的估值分析器。
 */
@Slf4j
@Component
public class OdsValuationDataParser implements ValuationDataParser {

    private static final String DEFAULT_SHEET_NAME = "ODS_RAW_DATA";

    private final ValuationFileDataMapper valuationFileDataMapper;
    private final ObjectMapper objectMapper;
    private final QlexpressParseRuleEngine parseRuleEngine;
    private final ParseRuleTemplateResolver parseRuleTemplateResolver;

    public OdsValuationDataParser(ValuationFileDataMapper valuationFileDataMapper, ObjectMapper objectMapper) {
        this(valuationFileDataMapper, objectMapper, new QlexpressParseRuleEngine(), null);
    }

    @Autowired
    public OdsValuationDataParser(
            ValuationFileDataMapper valuationFileDataMapper,
            ObjectMapper objectMapper,
            QlexpressParseRuleEngine parseRuleEngine,
            ParseRuleTemplateResolver parseRuleTemplateResolver
    ) {
        this.valuationFileDataMapper = valuationFileDataMapper;
        this.objectMapper = objectMapper;
        this.parseRuleEngine = parseRuleEngine;
        this.parseRuleTemplateResolver = parseRuleTemplateResolver;
    }

    @Override
    public ParsedValuationData parse(DataSourceConfig config) {
        Long fileId = resolveFileId(config.getAdditionalParams());
        if (fileId == null) {
            throw new IllegalStateException("ODS 原始数据分析需要提供 fileId");
        }

        log.info("开始基于原始数据表进行估值分析，sourceUri={}, fileId={}", config.getSourceUri(), fileId);
        List<ValuationFileDataPO> rawRows = valuationFileDataMapper.findByFileId(fileId);
        if (rawRows == null || rawRows.isEmpty()) {
            log.warn("未找到 fileId={} 对应的原始数据行", fileId);
            return ParsedValuationData.builder()
                    .workbookPath(config.getSourceUri())
                    .sheetName(DEFAULT_SHEET_NAME)
                    .title("")
                    .basicInfo(Map.of())
                    .headers(List.of())
                    .headerDetails(List.of())
                    .headerColumns(List.of())
                    .subjects(List.of())
                    .metrics(List.of())
                    .build();
        }

        List<List<Object>> rows = rawRows.stream()
                .map(this::readRowValues)
                .toList();
        String fileTypeName = resolveFileTypeName(config);
        String fileScene = resolveFileScene(config);
        String headerExpr = parseRuleTemplateResolver == null
                ? null
                : parseRuleTemplateResolver.resolveHeaderExpr(fileScene, fileTypeName);
        String rowClassifyExpr = parseRuleTemplateResolver == null
                ? null
                : parseRuleTemplateResolver.resolveRowClassifyExpr(fileScene, fileTypeName);
        List<String> requiredHeaders = resolveRequiredHeaders(fileScene, fileTypeName);
        Pattern subjectCodePattern = resolveSubjectCodePattern(fileScene, fileTypeName);

        int headerRowIndex = findHeaderRow(rows, headerExpr, requiredHeaders);
        int dataStartRowIndex = findDataStartRow(rows, headerRowIndex, ParseRuleExpressions.DATA_START_EXPR, subjectCodePattern);
        List<List<String>> headerBlockRows = extractHeaderBlock(rows, headerRowIndex, dataStartRowIndex);
        HeaderLayout headerLayout = buildHeaderLayout(headerBlockRows);
        List<String> headers = headerLayout.headers();
        Map<String, Integer> headerIndex = buildHeaderIndex(headers);
        List<List<String>> headerDetails = headerLayout.headerDetails();
        List<HeaderColumnMeta> headerColumns = headerLayout.headerColumns();
        TitleAndInfo titleAndInfo = extractTitleAndBasicInfo(rows, headerRowIndex);
        SplitResult splitResult = splitSubjectsAndMetrics(rows, dataStartRowIndex, headers, headerIndex, rowClassifyExpr, subjectCodePattern);

        log.info("基于原始数据表的估值分析完成，fileId={}, headerRow={}, dataStartRow={}, subjectCount={}, metricCount={}",
                fileId,
                headerRowIndex + 1,
                dataStartRowIndex + 1,
                splitResult.subjects().size(),
                splitResult.metrics().size());

        return ParsedValuationData.builder()
                .workbookPath(config.getSourceUri())
                .sheetName(DEFAULT_SHEET_NAME)
                .headerRowNumber(headerRowIndex + 1)
                .dataStartRowNumber(dataStartRowIndex + 1)
                .title(titleAndInfo.title())
                .basicInfo(titleAndInfo.basicInfo())
                .headers(headers)
                .headerDetails(headerDetails)
                .headerColumns(headerColumns)
                .subjects(splitResult.subjects())
                .metrics(splitResult.metrics())
                .build();
    }

    private Long resolveFileId(String additionalParams) {
        if (additionalParams == null || additionalParams.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(additionalParams.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private List<Object> readRowValues(ValuationFileDataPO po) {
        try {
            return objectMapper.readValue(po.getRowDataJson(), new TypeReference<List<Object>>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException("无法解析原始行数据 JSON，fileId=" + po.getFileId() + ", rowDataNumber=" + po.getRowDataNumber(), e);
        }
    }

    private int findHeaderRow(List<List<Object>> rows, String headerExpr, List<String> requiredHeaders) {
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            if (parseRuleEngine.matchesHeaderRow(rows.get(rowIndex), requiredHeaders, headerExpr)) {
                return rowIndex;
            }
        }
        throw new IllegalArgumentException("未找到包含“科目代码/科目名称”的表头。");
    }

    private int findDataStartRow(List<List<Object>> rows, int headerRowIndex, String dataStartExpr, Pattern subjectCodePattern) {
        int firstMetricCandidate = -1;
        for (int rowIndex = headerRowIndex + 1; rowIndex < rows.size(); rowIndex++) {
            List<Object> rowValues = rows.get(rowIndex);
            if (isFooterRow(rowValues)) {
                break;
            }
            if (parseRuleEngine.matchesDataStartRow(rowValues, dataStartExpr) && ExcelParsingSupport.isSubjectDataRow(rowValues, subjectCodePattern)) {
                return rowIndex;
            }
            if (firstMetricCandidate < 0 && parseRuleEngine.matchesDataStartRow(rowValues, dataStartExpr) && isMetricCandidate(rowValues, subjectCodePattern)) {
                firstMetricCandidate = rowIndex;
            }
        }
        if (firstMetricCandidate >= 0) {
            return firstMetricCandidate;
        }
        throw new IllegalArgumentException("在表头下方未找到科目数据。");
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
            String rowClassifyExpr,
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
            String rowType = parseRuleEngine.classifyRow(rowValues, List.of("制表", "复核", "打印", "备注"), subjectCodePattern, rowClassifyExpr);
            if ("SUBJECT".equalsIgnoreCase(rowType) || ExcelParsingSupport.isSubjectDataRow(rowValues, subjectCodePattern)) {
                subjects.add(extractSubjectRecord(rowIndex, rowValues, headers, headerIndex));
                continue;
            }
            if ("METRIC_DATA".equalsIgnoreCase(rowType)
                    || "METRIC_ROW".equalsIgnoreCase(rowType)
                    || ExcelParsingSupport.isMetricDataRow(rowValues, subjectCodePattern)
                    || ExcelParsingSupport.isMetricRow(rowValues, subjectCodePattern)) {
                metrics.add(extractMetricRecord(rowIndex, rowValues, headers, headerIndex, subjectCodePattern));
            }
        }
        SubjectHierarchySupport.enrichSubjectHierarchy(subjects);
        return new SplitResult(subjects, metrics);
    }

    private List<String> resolveRequiredHeaders(String fileScene, String fileTypeName) {
        if (parseRuleTemplateResolver == null) {
            return List.of("科目代码", "科目名称");
        }
        List<String> requiredHeaders = parseRuleTemplateResolver.resolveRequiredHeaders(fileScene, fileTypeName);
        return requiredHeaders == null || requiredHeaders.isEmpty()
                ? List.of("科目代码", "科目名称")
                : requiredHeaders;
    }

    private Pattern resolveSubjectCodePattern(String fileScene, String fileTypeName) {
        if (parseRuleTemplateResolver == null) {
            return Pattern.compile("^\\d{4}[A-Za-z0-9]*$");
        }
        Pattern pattern = parseRuleTemplateResolver.resolveSubjectCodePattern(fileScene, fileTypeName);
        return pattern == null ? Pattern.compile("^\\d{4}[A-Za-z0-9]*$") : pattern;
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

    private BigDecimal getNumber(List<Object> rowValues, List<String> headers, Map<String, Integer> headerIndex, String headerName, String... excludedTokens) {
        Integer columnIndex = resolveHeaderIndex(headers, headerIndex, headerName, excludedTokens);
        return columnIndex == null ? null : ExcelParsingSupport.normalizeNumber(ExcelParsingSupport.valueAt(rowValues, columnIndex));
    }

    private Integer resolveHeaderIndex(List<String> headers, Map<String, Integer> headerIndex, String headerName, String... excludedTokens) {
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
        return parseRuleEngine.matchesFooterRow(rowValues, List.of("制表", "复核", "打印", "备注"));
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

    private boolean containsAnySegment(String header, String... excludedTokens) {
        if (header == null || header.isBlank() || excludedTokens == null || excludedTokens.length == 0) {
            return false;
        }
        String[] segments = header.split("\\|");
        for (String excludedToken : excludedTokens) {
            if (excludedToken == null || excludedToken.isBlank()) {
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

    private record TitleAndInfo(String title, Map<String, String> basicInfo) {
    }

    private record HeaderLayout(List<String> headers, List<List<String>> headerDetails, List<HeaderColumnMeta> headerColumns) {
    }

    private record SplitResult(List<SubjectRecord> subjects, List<MetricRecord> metrics) {
    }
}
