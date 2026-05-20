package com.yss.valset.extract.parser.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.common.support.ExcelParsingSupport;
import com.yss.valset.common.support.SpreadsheetXmlSupport;
import com.yss.valset.common.support.SubjectHierarchySupport;
import com.yss.valset.domain.exception.FileAccessException;
import com.yss.valset.domain.model.DataSourceConfig;
import com.yss.valset.domain.model.DataSourceType;
import com.yss.valset.domain.model.HeaderColumnMeta;
import com.yss.valset.domain.model.MetricRecord;
import com.yss.valset.domain.model.ParsedValuationData;
import com.yss.valset.domain.model.SubjectRecord;
import com.yss.valset.domain.parser.ValuationDataParser;
import com.yss.valset.domain.rule.ParseRuleType;
import com.yss.valset.extract.rule.ParseRuleStepDescriptor;
import com.yss.valset.extract.rule.ParseRuleTemplateResolver;
import com.yss.valset.extract.rule.QlexpressParseRuleEngine;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于工作簿文件的估值分析器。
 *
 * <p>核心流程：校验文件路径后把 Excel/SpreadsheetML 工作簿摊平成二维原始行，
 * 再基于解析模板完成表头识别、数据起始行定位、多级表头构建、标题基础信息抽取和科目/指标拆分。</p>
 */
@Slf4j
@Component
public class OdsValuationDataParser implements ValuationDataParser {

    private static final String DEFAULT_SHEET_NAME = "ODS_RAW_DATA";
    private static final String DEFAULT_CSV_SHEET_NAME = "CSV_RAW_DATA";

    private final ObjectMapper objectMapper;
    private final QlexpressParseRuleEngine parseRuleEngine;
    private final ParseRuleTemplateResolver parseRuleTemplateResolver;

    public OdsValuationDataParser(ObjectMapper objectMapper) {
        this(objectMapper, null);
    }

    public OdsValuationDataParser(ObjectMapper objectMapper, ParseRuleTemplateResolver parseRuleTemplateResolver) {
        this(objectMapper, parseRuleTemplateResolver, new QlexpressParseRuleEngine());
    }

    @Autowired
    public OdsValuationDataParser(ObjectMapper objectMapper,
                                  ParseRuleTemplateResolver parseRuleTemplateResolver,
                                  QlexpressParseRuleEngine parseRuleEngine) {
        this.objectMapper = objectMapper;
        this.parseRuleEngine = parseRuleEngine == null ? new QlexpressParseRuleEngine() : parseRuleEngine;
        this.parseRuleTemplateResolver = parseRuleTemplateResolver;
    }

    @Override
    public ParsedValuationData parse(DataSourceConfig config) {
        Path sourcePath = resolveSourcePath(config);
        log.info("开始基于文件路径进行估值分析，sourceUri={}, resolvedPath={}", config.getSourceUri(), sourcePath);

        ValuationRawTable rawTable = ValuationRawTable.of(sourcePath, defaultSheetName(config), readRows(sourcePath, config));
        List<List<Object>> rows = rawTable.rows();
        if (rows.isEmpty()) {
            log.warn("文件没有可解析的原始数据行，sourceUri={}", config.getSourceUri());
            return ParsedValuationData.builder()
                    .workbookPath(config.getSourceUri())
                    .sheetName(rawTable.sheetName())
                    .title("")
                    .basicInfo(java.util.Collections.emptyMap())
                    .headers(java.util.Arrays.asList())
                    .headerDetails(java.util.Arrays.asList())
                    .headerColumns(java.util.Arrays.asList())
                    .subjects(java.util.Arrays.asList())
                    .metrics(java.util.Arrays.asList())
                    .build();
        }

        ParseRuntimeContext runtimeContext = ParseRuntimeContext.resolve(
                resolveFileScene(config),
                resolveFileTypeName(config),
                parseRuleTemplateResolver);

        // Step 1: 定位业务表头和数据区起点，这是后续表头构建、行过滤和字段抽取的共同锚点。
        int headerRowIndex = findHeaderRow(rows, runtimeContext.headerExpr(), runtimeContext.requiredHeaders(), runtimeContext.subjectCodePattern());
        int subjectCodeColumnIndex = ValuationParserSupport.resolveSubjectCodeColumnIndex(rows.get(headerRowIndex));
        int dataStartRowIndex = findDataStartRow(rows, headerRowIndex, subjectCodeColumnIndex, runtimeContext.dataStartRule(), runtimeContext.subjectCodePattern());

        // Step 2: 将多行表头压缩成稳定列路径，便于按“科目代码/科目名称/市值”等业务列取值。
        List<List<String>> headerBlockRows = extractHeaderBlock(rows, headerRowIndex, dataStartRowIndex);
        HeaderLayout headerLayout = buildHeaderLayout(headerBlockRows);
        List<String> headers = headerLayout.headers();
        Map<String, Integer> headerIndex = buildHeaderIndex(headers);
        List<List<String>> headerDetails = headerLayout.headerDetails();
        List<HeaderColumnMeta> headerColumns = headerLayout.headerColumns();

        // Step 3: 表头上方沉淀为标题和 basicInfo，数据区按“科目代码”列过滤出科目记录与指标记录。
        TitleAndInfo titleAndInfo = extractTitleAndBasicInfo(rows, headerRowIndex);
        SplitResult splitResult = splitSubjectsAndMetrics(
                rows,
                dataStartRowIndex,
                headers,
                headerIndex,
                headerColumns,
                titleAndInfo.basicInfo(),
                runtimeContext.subjectCodePattern(),
                runtimeContext.subjectExtractRule(),
                runtimeContext.metricExtractRule());

        log.info("基于文件路径的估值分析完成，sourceUri={}, headerRow={}, dataStartRow={}, subjectCount={}, metricCount={}",
                config.getSourceUri(),
                headerRowIndex + 1,
                dataStartRowIndex + 1,
                splitResult.subjects().size(),
                splitResult.metrics().size());

        return ParsedValuationData.builder()
                .workbookPath(config.getSourceUri())
                .sheetName(rawTable.sheetName())
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

    /**
     * 文件路径是本解析器的唯一读取入口，提前做存在性和可读性校验，避免下游 POI 抛出不清晰的 IO 异常。
     */
    private Path resolveSourcePath(DataSourceConfig config) {
        if (config == null || config.getSourceUri() == null || config.getSourceUri().trim().isEmpty()) {
            throw new IllegalStateException("估值表解析需要提供 sourceUri");
        }
        Path sourcePath = Paths.get(config.getSourceUri()).toAbsolutePath().normalize();
        if (!Files.exists(sourcePath) || !Files.isReadable(sourcePath)) {
            throw new FileAccessException(config.getSourceUri());
        }
        return sourcePath;
    }

    /**
     * 估值表解析只接受 EXCEL 来源；同一入口同时兼容标准工作簿和 Excel 2003 SpreadsheetML XML。
     */
    private List<List<Object>> readRows(Path sourcePath, DataSourceConfig config) {
        DataSourceType sourceType = config == null ? null : config.getSourceType();
        if (sourceType != null && sourceType != DataSourceType.EXCEL) {
            throw new IllegalArgumentException("OdsValuationDataParser 仅支持 EXCEL 来源，当前类型=" + sourceType);
        }
        try {
            if (SpreadsheetXmlSupport.isSpreadsheetXml(sourcePath)) {
                return readSpreadsheetXmlRows(sourcePath);
            }
            return readWorkbookRows(sourcePath);
        } catch (FileAccessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("读取 Excel 工作簿失败，sourceUri=" + sourcePath, exception);
        }
    }

    /**
     * 使用 POI 读取普通 xls/xlsx，公式单元格通过 FormulaEvaluator 转成显示值，
     * 多个 sheet 会按工作簿顺序合并为一张原始行表。
     */
    private List<List<Object>> readWorkbookRows(Path sourcePath) throws Exception {
        List<List<Object>> rows = new ArrayList<>();
        try (InputStream inputStream = Files.newInputStream(sourcePath);
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                if (sheet == null) {
                    continue;
                }
                for (Row row : sheet) {
                    List<Object> rowValues = ExcelParsingSupport.readRowValues(row, evaluator, formatter);
                    if (ValuationParserSupport.isBlankRow(rowValues)) {
                        continue;
                    }
                    rows.add(rowValues);
                }
            }
        }
        return rows;
    }

    /**
     * 兼容以 XML 形式保存的 SpreadsheetML 文件，读取后仍转换为统一的二维行结构。
     */
    private List<List<Object>> readSpreadsheetXmlRows(Path sourcePath) {
        try {
            SpreadsheetXmlSupport.SpreadsheetXmlWorkbook workbook = SpreadsheetXmlSupport.read(sourcePath);
            List<List<Object>> rows = new ArrayList<>();
            if (workbook == null || workbook.sheets() == null) {
                return rows;
            }
            for (SpreadsheetXmlSupport.SpreadsheetXmlSheet sheet : workbook.sheets()) {
                if (sheet == null || sheet.rows() == null) {
                    continue;
                }
                for (List<String> row : sheet.rows()) {
                    List<Object> rowValues = new ArrayList<>();
                    if (row != null) {
                        rowValues.addAll(row);
                    }
                    if (ValuationParserSupport.isBlankRow(rowValues)) {
                        continue;
                    }
                    rows.add(rowValues);
                }
            }
            return rows;
        } catch (Exception exception) {
            throw new IllegalStateException("读取 SpreadsheetML 工作簿失败，sourceUri=" + sourcePath, exception);
        }
    }

    /**
     * 表头识别先用必选字段做快速过滤，再执行模板中的 header 表达式；
     * 两层判断可以避免把标题行、基础信息行误识别为业务表头。
     */
    private int findHeaderRow(List<List<Object>> rows, String headerExpr, List<String> requiredHeaders, String subjectCodePattern) {
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            List<Object> rowValues = rows.get(rowIndex);
            if (requiredHeaders != null && !requiredHeaders.isEmpty() && !ValuationParserSupport.rowContainsAll(rowValues, requiredHeaders)) {
                continue;
            }
            if (parseRuleEngine.matchesHeaderRow(rowValues, requiredHeaders, headerExpr)) {
                return rowIndex;
            }
        }
        throw new IllegalArgumentException("未识别到必选表头" + requiredHeaders + "，无法解析这张表。");
    }

    /**
     * 数据起始行以表头“科目代码”列为锚点，选择第一条可过滤为科目明细或指标的数据行。
     */
    private int findDataStartRow(List<List<Object>> rows,
                                 int headerRowIndex,
                                 int subjectCodeColumnIndex,
                                 ParseRuleStepDescriptor dataStartRule,
                                 String subjectCodePattern) {
        String dataStartExpr = dataStartRule == null || dataStartRule.getExpression() == null || dataStartRule.getExpression().trim().isEmpty()
                ? null
                : dataStartRule.getExpression();
        for (int rowIndex = headerRowIndex + 1; rowIndex < rows.size(); rowIndex++) {
            List<Object> rowValues = rows.get(rowIndex);
            if (parseRuleEngine.matchesFooterRow(rowValues, null)) {
                break;
            }
            boolean dataStartMatched = dataStartExpr == null || matchesDataStartRow(rowValues, rowIndex, dataStartRule, dataStartExpr, subjectCodePattern);
            if (dataStartMatched && parseRuleEngine.matchesValuationDataRowByColumn(rowValues, subjectCodeColumnIndex, subjectCodePattern)) {
                return rowIndex;
            }
        }
        throw new IllegalArgumentException("在表头下方未找到科目数据。");
    }

    /**
     * 执行数据起始行表达式。模板配置为 FAIL_FAST 时直接暴露脚本错误；
     * 其他策略下回退到内置默认规则，优先保证历史模板兼容。
     */
    private boolean matchesDataStartRow(List<Object> rowValues,
                                        int rowIndex,
                                        ParseRuleStepDescriptor rule,
                                        String expression,
                                        String subjectCodePattern) {
        Map<String, Object> context = ValuationParserSupport.buildDataStartRuleContext(rowValues, rowIndex, subjectCodePattern);
        try {
            return parseRuleEngine.evaluateBoolean(expression, context);
        } catch (Exception exception) {
            if (ValuationParserSupport.isFailFast(rule)) {
                throw exception;
            }
            log.warn("数据起始行规则执行失败，profileCode={}, version={}, ruleType={}, rowIndex={}, errorPolicy={}，回退默认逻辑",
                    ValuationParserSupport.profileCode(rule),
                    ValuationParserSupport.version(rule),
                    ValuationParserSupport.ruleTypeName(rule, ParseRuleType.DATA_START),
                    rowIndex,
                    ValuationParserSupport.errorPolicy(rule),
                    exception);
            return true;
        }
    }

    /**
     * 建立表头路径到列下标的首个命中映射；同名列保留第一次出现的位置，避免后续取值漂移。
     */
    private Map<String, Integer> buildHeaderIndex(List<String> headers) {
        Map<String, Integer> headerIndex = new LinkedHashMap<>();
        for (int index = 0; index < headers.size(); index++) {
            String header = headers.get(index);
            if (!header.trim().isEmpty()) {
                headerIndex.putIfAbsent(header, index);
            }
        }
        return headerIndex;
    }

    /**
     * 取表头行到数据起始行之间的所有非空行，作为多级表头的原始区域。
     */
    private List<List<String>> extractHeaderBlock(List<List<Object>> rows, int headerRowIndex, int dataStartRowIndex) {
        List<List<String>> headerBlockRows = new ArrayList<>();
        for (int rowIndex = headerRowIndex; rowIndex < dataStartRowIndex; rowIndex++) {
            List<String> rowTexts = ValuationParserSupport.toRowTexts(rows.get(rowIndex));
            if (ValuationParserSupport.isBlankRow(rowTexts)) {
                continue;
            }
            headerBlockRows.add(fillMergedHeaderRow(rowTexts));
        }
        return headerBlockRows;
    }

    /**
     * 将多行表头压平成列路径，例如“持仓|市值”，同时保留 pathSegments 供前端和后续标准化使用。
     */
    private HeaderLayout buildHeaderLayout(List<List<String>> headerBlockRows) {
        if (headerBlockRows.isEmpty()) {
            return new HeaderLayout(java.util.Arrays.asList(), java.util.Arrays.asList(), java.util.Arrays.asList());
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
                if (!segment.trim().isEmpty()) {
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
                    .blankColumn(headerPath.trim().isEmpty())
                    .build());
        }
        return new HeaderLayout(headers, headerDetails, headerColumns);
    }

    /**
     * 工作簿读取后的空表头格继承左侧最近的非空文本，用于还原横向合并表头的业务含义。
     */
    private List<String> fillMergedHeaderRow(List<String> rowTexts) {
        List<String> normalized = new ArrayList<>(rowTexts.size());
        String carry = "";
        for (String text : rowTexts) {
            if (text == null || text.trim().isEmpty()) {
                normalized.add(carry);
                continue;
            }
            carry = text;
            normalized.add(text);
        }
        return normalized;
    }

    /**
     * 表头上方通常包含报表标题、产品代码、估值日期等信息；
     * 单独成行且没有冒号的最长文本作为标题，键值对行写入 basicInfo。
     */
    private TitleAndInfo extractTitleAndBasicInfo(List<List<Object>> rows, int headerRowIndex) {
        Map<String, String> basicInfo = new LinkedHashMap<>();
        List<String> titleCandidates = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < headerRowIndex; rowIndex++) {
            List<String> rowTexts = ValuationParserSupport.toRowTexts(rows.get(rowIndex));
            List<String> nonEmptyTexts = rowTexts.stream().filter(text -> !text.trim().isEmpty()).collect(java.util.stream.Collectors.toList());
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
                String key = ValuationParserSupport.stripTrailingPunctuation(parts[0]);
                String value = parts.length > 1 ? parts[1].trim() : "";
                if (value.trim().isEmpty()) {
                    value = ValuationParserSupport.findNextMeaningfulText(rowTexts, cellIndex + 1);
                }
                if (!key.trim().isEmpty()) {
                    basicInfo.put(key, ValuationParserSupport.stripTrailingPunctuation(value));
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

    private List<DataRow> collectDataRows(List<List<Object>> rows, int dataStartRowIndex) {
        List<DataRow> dataRows = new ArrayList<>();
        for (int rowIndex = dataStartRowIndex; rowIndex < rows.size(); rowIndex++) {
            List<Object> rowValues = rows.get(rowIndex);
            if (ValuationParserSupport.isBlankRow(ValuationParserSupport.toRowTexts(rowValues))) {
                continue;
            }
            if (parseRuleEngine.matchesFooterRow(rowValues, null)) {
                break;
            }
            dataRows.add(new DataRow(rowIndex, rowValues));
        }
        return dataRows;
    }

    /**
     * 从数据起始行开始迭代到页脚前，再按“科目代码”列过滤：
     * 非中文且符合代码口径的行进入 SubjectRecord，其余非空代码列行进入 MetricRecord。
     */
    private SplitResult splitSubjectsAndMetrics(
            List<List<Object>> rows,
            int dataStartRowIndex,
            List<String> headers,
            Map<String, Integer> headerIndex,
            List<HeaderColumnMeta> headerColumns,
            Map<String, String> basicInfo,
            String subjectCodePattern,
            ParseRuleStepDescriptor subjectExtractRule,
            ParseRuleStepDescriptor metricExtractRule
    ) {
        List<SubjectRecord> subjects = new ArrayList<>();
        List<MetricRecord> metrics = new ArrayList<>();
        Integer subjectCodeColumnIndex = ValuationParserSupport.resolveHeaderIndex(headers, headerIndex, "科目代码");
        if (subjectCodeColumnIndex == null) {
            throw new IllegalArgumentException("未识别到科目代码列，无法解析科目明细和指标数据。");
        }
        List<DataRow> dataRows = collectDataRows(rows, dataStartRowIndex);
        for (DataRow dataRow : dataRows) {
            if (parseRuleEngine.matchesSubjectDetailRowByColumn(dataRow.rowValues(), subjectCodeColumnIndex, subjectCodePattern)) {
                SubjectRecord subjectRecord = extractSubjectRecord(dataRow.rowIndex(), dataRow.rowValues(), headers, headerIndex, headerColumns, basicInfo, subjectCodePattern, subjectExtractRule);
                if (subjectRecord != null) {
                    subjects.add(subjectRecord);
                }
            }
        }
        for (DataRow dataRow : dataRows) {
            if (parseRuleEngine.matchesMetricDetailRowByColumn(dataRow.rowValues(), subjectCodeColumnIndex, subjectCodePattern)) {
                MetricRecord metricRecord = extractMetricRecord(dataRow.rowIndex(), dataRow.rowValues(), headers, headerIndex, headerColumns, basicInfo, subjectCodePattern, subjectCodeColumnIndex, metricExtractRule);
                if (metricRecord != null) {
                    metrics.add(metricRecord);
                }
            }
        }
        SubjectHierarchySupport.enrichSubjectHierarchy(subjects);
        return new SplitResult(subjects, metrics);
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

    /**
     * 科目抽取先构造内置默认结果，再用模板脚本返回值覆盖；
     * 返回 null 表示当前行按策略跳过，空 Map 表示沿用默认抽取。
     */
    private SubjectRecord extractSubjectRecord(
            int rowIndex,
            List<Object> rowValues,
            List<String> headers,
            Map<String, Integer> headerIndex,
            List<HeaderColumnMeta> headerColumns,
            Map<String, String> basicInfo,
            String subjectCodePattern,
            ParseRuleStepDescriptor subjectExtractRule
    ) {
        SubjectRecord defaultRecord = buildDefaultSubjectRecord(rowIndex, rowValues, headers, headerIndex);
        Map<String, Object> overrideValues = evaluateExtractRule(
                subjectExtractRule,
                ParseRuleType.SUBJECT_EXTRACT,
                rowIndex,
                rowValues,
                headers,
                headerIndex,
                headerColumns,
                basicInfo,
                subjectCodePattern);
        if (overrideValues == null) {
            return null;
        }
        if (overrideValues.isEmpty()) {
            return defaultRecord;
        }
        return mergeSubjectRecord(defaultRecord, overrideValues);
    }

    /**
     * 默认科目抽取依赖“科目代码/科目名称”列，并根据科目代码推导层级路径。
     */
    private SubjectRecord buildDefaultSubjectRecord(
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

    /**
     * 指标抽取与科目抽取策略一致：默认识别一行指标，再允许模板脚本覆盖指标名称、类型和值。
     */
    private MetricRecord extractMetricRecord(
            int rowIndex,
            List<Object> rowValues,
            List<String> headers,
            Map<String, Integer> headerIndex,
            List<HeaderColumnMeta> headerColumns,
            Map<String, String> basicInfo,
            String subjectCodePattern,
            int subjectCodeColumnIndex,
            ParseRuleStepDescriptor metricExtractRule
    ) {
        MetricRecord defaultRecord = buildDefaultMetricRecord(rowIndex, rowValues, headers, headerIndex, subjectCodePattern, subjectCodeColumnIndex);
        Map<String, Object> overrideValues = evaluateExtractRule(
                metricExtractRule,
                ParseRuleType.METRIC_EXTRACT,
                rowIndex,
                rowValues,
                headers,
                headerIndex,
                headerColumns,
                basicInfo,
                subjectCodePattern);
        if (overrideValues == null) {
            return null;
        }
        if (overrideValues.isEmpty()) {
            return defaultRecord;
        }
        return mergeMetricRecord(defaultRecord, overrideValues);
    }

    /**
     * 默认指标抽取兼容两类形态：
     * 1. “指标名 + 指标值”的横向键值行；
     * 2. 带完整业务表头的明细指标行。
     */
    private MetricRecord buildDefaultMetricRecord(
            int rowIndex,
            List<Object> rowValues,
            List<String> headers,
            Map<String, Integer> headerIndex,
            String subjectCodePattern,
            int subjectCodeColumnIndex
    ) {
        String metricName = normalizeMetricLabel(rowValues, subjectCodeColumnIndex);
        if (parseRuleEngine.matchesMetricDataRowByColumn(rowValues, subjectCodeColumnIndex, subjectCodePattern)) {
            Object rawValue = ValuationParserSupport.findFirstNumericValueAfterLabel(rowValues, subjectCodeColumnIndex);
            return MetricRecord.builder()
                    .sheetName(DEFAULT_SHEET_NAME)
                    .rowDataNumber(rowIndex + 1)
                    .metricName(metricName)
                    .metricType("metric_data")
                    .value(ValuationParserSupport.toTextValue(rawValue))
                    .rawValues(com.yss.valset.common.support.Java8Maps.of("value", ValuationParserSupport.normalizeMetricValue(rawValue)))
                    .build();
        }

        Map<String, Object> rawValues = new LinkedHashMap<>();
        for (String header : headers) {
            if (header == null || header.trim().isEmpty()) {
                continue;
            }
            Integer columnIndex = headerIndex.get(header);
            if (columnIndex == null) {
                continue;
            }
            rawValues.put(header, ValuationParserSupport.normalizeMetricValue(ExcelParsingSupport.valueAt(rowValues, columnIndex)));
        }
        if (!rawValues.containsKey("科目名称") || rawValues.get("科目名称") == null || rawValues.get("科目名称").toString().trim().isEmpty()) {
            rawValues.put("科目名称", metricName);
        }

        Object value = ValuationParserSupport.firstNonBlank(rawValues.get("市值"), rawValues.get("成本"), rawValues.get("数量"));
        return MetricRecord.builder()
                .sheetName(DEFAULT_SHEET_NAME)
                .rowDataNumber(rowIndex + 1)
                .metricName(metricName)
                .metricType("metric_row")
                .value(value == null ? "" : String.valueOf(value))
                .rawValues(rawValues)
                .build();
    }

    /**
     * 执行科目/指标抽取脚本，并统一处理错误策略：
     * FAIL_FAST 抛错，SKIP_ROW 返回 null，其他策略回退默认抽取。
     */
    private Map<String, Object> evaluateExtractRule(ParseRuleStepDescriptor rule,
                                                    ParseRuleType ruleType,
                                                    int rowIndex,
                                                    List<Object> rowValues,
                                                    List<String> headers,
                                                    Map<String, Integer> headerIndex,
                                                    List<HeaderColumnMeta> headerColumns,
                                                    Map<String, String> basicInfo,
                                                    String subjectCodePattern) {
        if (rule == null || rule.getExpression() == null || rule.getExpression().trim().isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        Map<String, Object> context = buildExtractRuleContext(rowIndex, rowValues, headers, headerIndex, headerColumns, basicInfo, subjectCodePattern);
        try {
            return parseRuleEngine.evaluateMap(rule.getExpression(), context);
        } catch (Exception exception) {
            if (ValuationParserSupport.isFailFast(rule)) {
                throw exception;
            }
            log.warn("解析抽取规则执行失败，profileCode={}, version={}, ruleType={}, rowIndex={}, errorPolicy={}",
                    ValuationParserSupport.profileCode(rule),
                    ValuationParserSupport.version(rule),
                    ValuationParserSupport.ruleTypeName(rule, ruleType),
                    rowIndex,
                    ValuationParserSupport.errorPolicy(rule),
                    exception);
            return ValuationParserSupport.isSkipRow(rule) ? null : java.util.Collections.emptyMap();
        }
    }

    /**
     * 暴露给 QLExpress 的上下文变量，脚本可通过 row/headerIndex/basicInfo 等对象读取原始行和表头信息。
     */
    private Map<String, Object> buildExtractRuleContext(int rowIndex,
                                                        List<Object> rowValues,
                                                        List<String> headers,
                                                        Map<String, Integer> headerIndex,
                                                        List<HeaderColumnMeta> headerColumns,
                                                        Map<String, String> basicInfo,
                                                        String subjectCodePattern) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("row", rowValues);
        context.put("rowIndex", rowIndex);
        context.put("headers", headers);
        context.put("headerIndex", headerIndex);
        context.put("headerColumns", headerColumns);
        context.put("basicInfo", basicInfo == null ? java.util.Collections.emptyMap() : basicInfo);
        context.put("subjectCodePattern", subjectCodePattern);
        return context;
    }

    /**
     * 将脚本抽取结果合并到默认科目记录，未返回的字段继续沿用默认值。
     */
    @SuppressWarnings("unchecked")
    private SubjectRecord mergeSubjectRecord(SubjectRecord defaults, Map<String, Object> values) {
        String subjectCode = firstString(values.get("subjectCode"), defaults.getSubjectCode());
        String subjectName = firstString(values.get("subjectName"), defaults.getSubjectName());
        List<String> pathCodes = asStringList(values.get("pathCodes"), defaults.getPathCodes());
        List<String> segments = SubjectHierarchySupport.splitSubjectCode(subjectCode);
        if (pathCodes == null || pathCodes.isEmpty()) {
            pathCodes = SubjectHierarchySupport.buildSubjectPathCodes(subjectCode, segments);
        }
        Object rawValues = values.containsKey("rawValues") ? values.get("rawValues") : defaults.getRawValues();
        return SubjectRecord.builder()
                .sheetName(defaults.getSheetName())
                .rowDataNumber(defaults.getRowDataNumber())
                .subjectCode(subjectCode)
                .subjectName(subjectName)
                .level(firstInteger(values.get("level"), pathCodes.size()))
                .parentCode(firstString(values.get("parentCode"), defaults.getParentCode()))
                .rootCode(firstString(values.get("rootCode"), pathCodes.isEmpty() ? subjectCode : pathCodes.get(0)))
                .segmentCount(firstInteger(values.get("segmentCount"), segments.size()))
                .pathCodes(pathCodes)
                .rawValues(rawValues instanceof List<?> ? new ArrayList<Object>((List<Object>) rawValues) : defaults.getRawValues())
                .leaf(firstBoolean(values.get("leaf"), defaults.getLeaf()))
                .build();
    }

    /**
     * 将脚本抽取结果合并到默认指标记录，rawValues 只有在脚本返回 Map 时才整体替换。
     */
    @SuppressWarnings("unchecked")
    private MetricRecord mergeMetricRecord(MetricRecord defaults, Map<String, Object> values) {
        Object rawValues = values.containsKey("rawValues") ? values.get("rawValues") : defaults.getRawValues();
        Map<String, Object> normalizedRawValues = defaults.getRawValues();
        if (rawValues instanceof Map<?, ?>) {
            normalizedRawValues = new LinkedHashMap<>((Map<String, Object>) rawValues);
        }
        return MetricRecord.builder()
                .sheetName(defaults.getSheetName())
                .rowDataNumber(defaults.getRowDataNumber())
                .metricName(firstString(values.get("metricName"), defaults.getMetricName()))
                .metricType(firstString(values.get("metricType"), defaults.getMetricType()))
                .value(firstString(values.get("value"), defaults.getValue()))
                .rawValues(normalizedRawValues)
                .build();
    }

    private String getText(List<Object> rowValues, List<String> headers, Map<String, Integer> headerIndex, String headerName) {
        Integer columnIndex = ValuationParserSupport.resolveHeaderIndex(headers, headerIndex, headerName);
        return columnIndex == null ? "" : ExcelParsingSupport.textAt(rowValues, columnIndex);
    }

    private String normalizeMetricLabel(List<Object> rowValues) {
        int labelIndex = ExcelParsingSupport.findFirstMeaningfulCellIndex(rowValues);
        if (labelIndex < 0) {
            return "";
        }
        return ValuationParserSupport.stripTrailingPunctuation(ExcelParsingSupport.textAt(rowValues, labelIndex));
    }

    private String normalizeMetricLabel(List<Object> rowValues, int subjectCodeColumnIndex) {
        if (subjectCodeColumnIndex >= 0 && subjectCodeColumnIndex < rowValues.size()) {
            String metricLabel = ValuationParserSupport.stripTrailingPunctuation(ExcelParsingSupport.textAt(rowValues, subjectCodeColumnIndex));
            if (!metricLabel.isEmpty()) {
                return metricLabel;
            }
        }
        return normalizeMetricLabel(rowValues);
    }

    private String firstString(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value);
        return text.trim().isEmpty() ? defaultValue : text;
    }

    private Integer firstInteger(Object value, Integer defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private Boolean firstBoolean(Object value, Boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.valueOf(String.valueOf(value));
    }

    @SuppressWarnings("unchecked")
    private List<String> asStringList(Object value, List<String> defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof List<?>) {
            List<?> source = (List<?>) value;
            List<String> result = new ArrayList<>(source.size());
            for (Object item : source) {
                if (item != null) {
                    result.add(String.valueOf(item));
                }
            }
            return result;
        }
        return java.util.Arrays.asList(String.valueOf(value));
    }

    @Value
    private static class TitleAndInfo {
        String title;
        Map<String, String> basicInfo;

        public String title() { return title; }
        public Map<String, String> basicInfo() { return basicInfo; }
    }

    @Value
    private static class HeaderLayout {
        List<String> headers;
        List<List<String>> headerDetails;
        List<HeaderColumnMeta> headerColumns;

        public List<String> headers() { return headers; }
        public List<List<String>> headerDetails() { return headerDetails; }
        public List<HeaderColumnMeta> headerColumns() { return headerColumns; }
    }

    @Value
    private static class DataRow {
        int rowIndex;
        List<Object> rowValues;

        public int rowIndex() { return rowIndex; }
        public List<Object> rowValues() { return rowValues; }
    }

    @Value
    private static class SplitResult {
        List<SubjectRecord> subjects;
        List<MetricRecord> metrics;

        public List<SubjectRecord> subjects() { return subjects; }
        public List<MetricRecord> metrics() { return metrics; }
    }

    private String defaultSheetName(DataSourceConfig config) {
        DataSourceType sourceType = config == null ? null : config.getSourceType();
        if (sourceType == DataSourceType.CSV) {
            return DEFAULT_CSV_SHEET_NAME;
        }
        return DEFAULT_SHEET_NAME;
    }
}
