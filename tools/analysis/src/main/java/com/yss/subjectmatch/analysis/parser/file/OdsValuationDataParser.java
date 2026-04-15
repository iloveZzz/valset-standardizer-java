package com.yss.subjectmatch.analysis.parser.file;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.subjectmatch.domain.model.DataSourceConfig;
import com.yss.subjectmatch.domain.model.HeaderColumnMeta;
import com.yss.subjectmatch.domain.model.MetricRecord;
import com.yss.subjectmatch.domain.model.ParsedValuationData;
import com.yss.subjectmatch.domain.model.SubjectRecord;
import com.yss.subjectmatch.domain.parser.ValuationDataParser;
import com.yss.subjectmatch.extract.repository.entity.ValuationFileDataPO;
import com.yss.subjectmatch.extract.repository.mapper.ValuationFileDataMapper;
import com.yss.subjectmatch.analysis.support.ExcelParsingSupport;
import com.yss.subjectmatch.analysis.support.SubjectHierarchySupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 ODS 原始行数据的估值分析器。
 */
@Slf4j
@Component
public class OdsValuationDataParser implements ValuationDataParser {

    private static final String DEFAULT_SHEET_NAME = "ODS_RAW_DATA";
    private static final List<String> REQUIRED_HEADERS = List.of("科目代码", "科目名称");

    private final ValuationFileDataMapper valuationFileDataMapper;
    private final ObjectMapper objectMapper;

    public OdsValuationDataParser(ValuationFileDataMapper valuationFileDataMapper, ObjectMapper objectMapper) {
        this.valuationFileDataMapper = valuationFileDataMapper;
        this.objectMapper = objectMapper;
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

        int headerRowIndex = findHeaderRow(rows);
        int dataStartRowIndex = findDataStartRow(rows, headerRowIndex);
        int headerBlockStartIndex = findHeaderBlockStart(rows, headerRowIndex);
        List<List<String>> headerBlockRows = extractHeaderBlock(rows, headerBlockStartIndex, dataStartRowIndex);
        HeaderLayout headerLayout = buildHeaderLayout(headerBlockRows);
        List<String> headers = headerLayout.headers();
        Map<String, Integer> headerIndex = buildHeaderIndex(headers);
        List<List<String>> headerDetails = headerLayout.headerDetails();
        List<HeaderColumnMeta> headerColumns = headerLayout.headerColumns();
        TitleAndInfo titleAndInfo = extractTitleAndBasicInfo(rows, headerBlockStartIndex);
        SplitResult splitResult = splitSubjectsAndMetrics(rows, dataStartRowIndex, headers, headerIndex);

        log.info("基于原始数据表的估值分析完成，fileId={}, headerRow={}, dataStartRow={}, subjectCount={}, metricCount={}",
                fileId,
                headerRowIndex + 1,
                dataStartRowIndex + 1,
                splitResult.subjects().size(),
                splitResult.metrics().size());

        return ParsedValuationData.builder()
                .workbookPath(config.getSourceUri())
                .sheetName(DEFAULT_SHEET_NAME)
                .headerRowNumber(headerBlockStartIndex + 1)
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

    private int findHeaderRow(List<List<Object>> rows) {
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            List<String> texts = toRowTexts(rows.get(rowIndex));
            if (texts.containsAll(REQUIRED_HEADERS)) {
                return rowIndex;
            }
        }
        throw new IllegalArgumentException("未找到包含“科目代码/科目名称”的表头。");
    }

    private int findDataStartRow(List<List<Object>> rows, int headerRowIndex) {
        for (int rowIndex = headerRowIndex + 1; rowIndex < rows.size(); rowIndex++) {
            List<Object> rowValues = rows.get(rowIndex);
            String firstCell = ExcelParsingSupport.textAt(rowValues, 0);
            String secondCell = ExcelParsingSupport.textAt(rowValues, 1);
            if (ExcelParsingSupport.isSubjectCode(firstCell) && !secondCell.isBlank()) {
                return rowIndex;
            }
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

    private int findHeaderBlockStart(List<List<Object>> rows, int headerRowIndex) {
        int blockStart = headerRowIndex;
        while (blockStart > 0) {
            List<String> previousRow = toRowTexts(rows.get(blockStart - 1));
            if (isBlankRow(previousRow) || isBasicInfoRow(previousRow)) {
                break;
            }
            blockStart--;
        }
        return blockStart;
    }

    private List<List<String>> extractHeaderBlock(List<List<Object>> rows, int headerBlockStartIndex, int dataStartRowIndex) {
        List<List<String>> headerBlockRows = new ArrayList<>();
        for (int rowIndex = headerBlockStartIndex; rowIndex < dataStartRowIndex; rowIndex++) {
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

    private boolean isBasicInfoRow(List<String> rowTexts) {
        if (rowTexts == null || rowTexts.isEmpty()) {
            return false;
        }
        long nonBlankCount = rowTexts.stream().filter(text -> text != null && !text.isBlank()).count();
        if (nonBlankCount == 0) {
            return false;
        }
        boolean containsKeyValueText = rowTexts.stream()
                .filter(text -> text != null && !text.isBlank())
                .anyMatch(text -> text.contains("：") || text.contains(":"));
        return containsKeyValueText && nonBlankCount <= 3;
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
            for (String text : nonEmptyTexts) {
                String delimiter = text.contains("：") ? "：" : (text.contains(":") ? ":" : null);
                if (delimiter == null) {
                    continue;
                }
                String[] parts = text.split(delimiter, 2);
                String key = parts[0].trim();
                String value = parts.length > 1 ? parts[1].trim() : "";
                if (!key.isBlank()) {
                    basicInfo.put(key, value);
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
            Map<String, Integer> headerIndex
    ) {
        List<SubjectRecord> subjects = new ArrayList<>();
        List<MetricRecord> metrics = new ArrayList<>();
        for (int rowIndex = dataStartRowIndex; rowIndex < rows.size(); rowIndex++) {
            List<Object> rowValues = rows.get(rowIndex);
            String firstCell = ExcelParsingSupport.textAt(rowValues, 0);
            String secondCell = ExcelParsingSupport.textAt(rowValues, 1);
            if (firstCell.isBlank() && secondCell.isBlank()) {
                continue;
            }
            if (ExcelParsingSupport.isSubjectCode(firstCell) && !secondCell.isBlank()) {
                subjects.add(extractSubjectRecord(rowIndex, rowValues, headers, headerIndex));
                continue;
            }
            if (ExcelParsingSupport.isMetricDataRow(rowValues) || ExcelParsingSupport.isMetricRow(rowValues)) {
                metrics.add(extractMetricRecord(rowIndex, rowValues, headers, headerIndex));
            }
        }
        SubjectHierarchySupport.enrichSubjectHierarchy(subjects);
        return new SplitResult(subjects, metrics);
    }

    private SubjectRecord extractSubjectRecord(
            int rowIndex,
            List<Object> rowValues,
            List<String> headers,
            Map<String, Integer> headerIndex
    ) {
        String subjectCode = getText(rowValues, headers, headerIndex, "科目代码");
        String subjectName = getText(rowValues, headers, headerIndex, "科目名称");
        List<String> segments = SubjectHierarchySupport.splitSubjectCode(subjectCode);
        List<String> pathCodes = SubjectHierarchySupport.buildSubjectPathCodes(subjectCode, segments);
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
            Map<String, Integer> headerIndex
    ) {
        String metricName = ExcelParsingSupport.textAt(rowValues, 0);
        if (ExcelParsingSupport.isMetricDataRow(rowValues)) {
            Object rawValue = ExcelParsingSupport.valueAt(rowValues, 1);
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
