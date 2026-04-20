package com.yss.valset.extract.standardization;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.domain.model.HeaderColumnMeta;
import com.yss.valset.domain.model.MappingDecision;
import com.yss.valset.domain.model.MappingQualityReport;
import com.yss.valset.domain.model.MetricRecord;
import com.yss.valset.domain.model.ParsedValuationData;
import com.yss.valset.domain.model.SubjectRecord;
import com.yss.valset.extract.repository.entity.FileParseRulePO;
import com.yss.valset.extract.repository.entity.FileParseSourcePO;
import com.yss.valset.extract.repository.mapper.FileParseRuleRepository;
import com.yss.valset.extract.repository.mapper.FileParseSourceRepository;
import com.yss.valset.extract.rule.ParseRuleTemplateResolver;
import com.yss.valset.extract.rule.QlexpressParseRuleEngine;
import com.yss.valset.extract.standardization.mapping.BuiltinHeaderAliasCatalog;
import com.yss.valset.extract.standardization.mapping.BuiltinMetricAliasCatalog;
import com.yss.valset.extract.standardization.mapping.HeaderMappingCandidate;
import com.yss.valset.extract.standardization.mapping.HeaderMappingEngine;
import com.yss.valset.extract.standardization.mapping.HeaderMappingInput;
import com.yss.valset.extract.standardization.mapping.HeaderMappingLookup;
import com.yss.valset.extract.standardization.mapping.QlexpressHeaderMappingEngine;
import com.yss.valset.extract.support.MatchTextSupport;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 外部估值标准化服务。
 */
@Slf4j
@Service
public class ExternalValuationStandardizationService {

    private final ObjectMapper objectMapper;
    private final FileParseRuleRepository parseRuleRepository;
    private final FileParseSourceRepository parseSourceRepository;
    private final QlexpressParseRuleEngine qlexpressRuleEngine;
    private final HeaderMappingEngine headerMappingEngine;
    private final ParseRuleTemplateResolver parseRuleTemplateResolver;
    private volatile Dictionary dictionary;

    @Autowired
    public ExternalValuationStandardizationService(
            ObjectMapper objectMapper,
            FileParseRuleRepository parseRuleRepository,
            FileParseSourceRepository parseSourceRepository,
            QlexpressParseRuleEngine qlexpressRuleEngine,
            HeaderMappingEngine headerMappingEngine,
            ParseRuleTemplateResolver parseRuleTemplateResolver
    ) {
        this.objectMapper = objectMapper;
        this.parseRuleRepository = parseRuleRepository;
        this.parseSourceRepository = parseSourceRepository;
        this.qlexpressRuleEngine = qlexpressRuleEngine;
        this.headerMappingEngine = headerMappingEngine;
        this.parseRuleTemplateResolver = parseRuleTemplateResolver;
    }

    public ExternalValuationStandardizationService(
            ObjectMapper objectMapper,
            FileParseRuleRepository parseRuleRepository,
            FileParseSourceRepository parseSourceRepository
    ) {
        this(
                objectMapper,
                parseRuleRepository,
                parseSourceRepository,
                new QlexpressParseRuleEngine(),
                new QlexpressHeaderMappingEngine(),
                null
        );
    }

    public ExternalValuationStandardizationService(
            ObjectMapper objectMapper,
            FileParseRuleRepository parseRuleRepository,
            FileParseSourceRepository parseSourceRepository,
            QlexpressParseRuleEngine qlexpressRuleEngine
    ) {
        this(objectMapper, parseRuleRepository, parseSourceRepository, qlexpressRuleEngine, new QlexpressHeaderMappingEngine(), null);
    }

    public ParsedValuationData standardize(ParsedValuationData parsedValuationData) {
        if (parsedValuationData == null) {
            return null;
        }
        long startedAt = System.currentTimeMillis();
        Dictionary dictionary = dictionary();
        List<String> headers = parsedValuationData.getHeaders() == null ? List.of() : parsedValuationData.getHeaders();
        List<HeaderColumnMeta> headerColumns = parsedValuationData.getHeaderColumns() == null ? List.of() : parsedValuationData.getHeaderColumns();

        // Step 1: 对外部表头进行标准字段映射，输出映射决策明细
        String fileScene = resolveFileScene(parsedValuationData);
        String fileTypeName = resolveFileTypeName(parsedValuationData);
        String fieldMapExpr = parseRuleTemplateResolver == null
                ? null
                : parseRuleTemplateResolver.resolveFieldMapExpr(fileScene, fileTypeName);
        Map<Integer, MappingDecision> mappingDecisionByIndex = resolveHeaderMappingDecisionByIndex(headers, headerColumns, dictionary, fieldMapExpr);
        Map<Integer, String> standardColumnByIndex = mappingDecisionByIndex.entrySet().stream()
                .filter(entry -> Boolean.TRUE.equals(entry.getValue().getMatched()))
                .filter(entry -> entry.getValue().getStandardCode() != null && !entry.getValue().getStandardCode().isBlank())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().getStandardCode(),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
        HeaderQualitySummary headerQualitySummary = logHeaderMappingSummary(headers, mappingDecisionByIndex);
        logHeaderStrategySummary(mappingDecisionByIndex);

        // Step 2: 标准化科目与指标，补齐标准字段和映射元信息
        List<SubjectRecord> standardizedSubjects = parsedValuationData.getSubjects() == null
                ? List.of()
                : parsedValuationData.getSubjects().stream()
                .map(subject -> standardizeSubject(subject, headers, standardColumnByIndex, mappingDecisionByIndex, dictionary))
                .toList();
        List<MetricRecord> standardizedMetrics = parsedValuationData.getMetrics() == null
                ? List.of()
                : parsedValuationData.getMetrics().stream()
                .map(metric -> standardizeMetric(metric, dictionary))
                .toList();
        logSubjectMetricMappingSummary(standardizedSubjects, standardizedMetrics);

        // Step 3: 汇总质量报告，便于后续监控与回放补规则
        MappingQualityReport mappingQualityReport = buildMappingQualityReport(
                headerQualitySummary,
                standardizedSubjects,
                standardizedMetrics
        );
        List<MappingDecision> headerMappingDecisions = mappingDecisionByIndex.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .toList();

        log.info(
                "外部估值标准化完成，headerMapped={}/{}, subjectMapped={}/{}, metricMapped={}/{}, elapsedMs={}",
                mappingQualityReport.getHeaderMapped(),
                mappingQualityReport.getHeaderTotal(),
                mappingQualityReport.getSubjectMapped(),
                mappingQualityReport.getSubjectTotal(),
                mappingQualityReport.getMetricMapped(),
                mappingQualityReport.getMetricTotal(),
                System.currentTimeMillis() - startedAt
        );

        return ParsedValuationData.builder()
                .workbookPath(parsedValuationData.getWorkbookPath())
                .sheetName(parsedValuationData.getSheetName())
                .headerRowNumber(parsedValuationData.getHeaderRowNumber())
                .dataStartRowNumber(parsedValuationData.getDataStartRowNumber())
                .title(parsedValuationData.getTitle())
                .fileNameOriginal(parsedValuationData.getFileNameOriginal())
                .basicInfo(parsedValuationData.getBasicInfo())
                .headers(parsedValuationData.getHeaders())
                .headerDetails(parsedValuationData.getHeaderDetails())
                .headerColumns(parsedValuationData.getHeaderColumns())
                .headerMappingDecisions(headerMappingDecisions)
                .mappingQualityReport(mappingQualityReport)
                .subjects(standardizedSubjects)
                .metrics(standardizedMetrics)
                .build();
    }

    /**
     * 刷新解析字典缓存。
     */
    public void refreshDictionaryCache() {
        synchronized (this) {
            dictionary = null;
        }
    }

    private void logHeaderStrategySummary(Map<Integer, MappingDecision> mappingDecisionByIndex) {
        if (mappingDecisionByIndex == null || mappingDecisionByIndex.isEmpty()) {
            return;
        }
        Map<String, Long> strategyCounter = mappingDecisionByIndex.values().stream()
                .filter(Objects::nonNull)
                .filter(decision -> Boolean.TRUE.equals(decision.getMatched()))
                .map(MappingDecision::getStrategy)
                .filter(strategy -> strategy != null && !strategy.isBlank())
                .collect(Collectors.groupingBy(strategy -> strategy, LinkedHashMap::new, Collectors.counting()));
        if (!strategyCounter.isEmpty()) {
            log.info("外部估值表头映射策略分布，strategies={}", strategyCounter);
        }
    }

    private SubjectRecord standardizeSubject(
            SubjectRecord subject,
            List<String> headers,
            Map<Integer, String> standardColumnByIndex,
            Map<Integer, MappingDecision> mappingDecisionByIndex,
            Dictionary dictionary
    ) {
        Map<String, Object> standardValues = new LinkedHashMap<>();
        List<MappingDecision> matchedDecisions = new java.util.ArrayList<>();
        List<Object> rawValues = subject.getRawValues() == null ? List.of() : subject.getRawValues();
        for (int index = 0; index < Math.min(headers.size(), rawValues.size()); index++) {
            MappingDecision decision = mappingDecisionByIndex.get(index);
            if (decision == null || !Boolean.TRUE.equals(decision.getMatched())) {
                continue;
            }
            String standardCode = standardColumnByIndex.get(index);
            Object value = rawValues.get(index);
            if (isBlankValue(value)) {
                continue;
            }
            if (!standardValues.containsKey(standardCode)) {
                standardValues.put(standardCode, normalizeValue(value));
                matchedDecisions.add(decision);
            }
        }

        ParseSourceEntry subjectCodeEntry = resolveSource(subject.getSubjectCode(), dictionary);
        ParseSourceEntry subjectNameEntry = resolveSource(subject.getSubjectName(), dictionary);
        Long mappingRuleId = firstNonNull(subjectCodeEntry, subjectNameEntry, entry -> entry.getRule().getId());
        Long mappingSourceId = firstNonNull(subjectCodeEntry, subjectNameEntry, ParseSourceEntry::getId);

        String mappingStatus = standardValues.isEmpty() ? "UNMAPPED" : "MAPPED";
        String mappingReason = buildSubjectMappingReason(standardValues, matchedDecisions);
        Double mappingConfidence = calculateSubjectMappingConfidence(standardValues, matchedDecisions);

        subject.setStandardCode(subject.getSubjectCode());
        subject.setStandardName(subject.getSubjectName());
        subject.setStandardValues(standardValues);
        subject.setMappingRuleId(mappingRuleId);
        subject.setMappingSourceId(mappingSourceId);
        subject.setMappingStatus(mappingStatus);
        subject.setMappingReason(mappingReason);
        subject.setMappingConfidence(mappingConfidence);
        return subject;
    }

    private MetricRecord standardizeMetric(MetricRecord metric, Dictionary dictionary) {
        ParseSourceEntry sourceEntry = resolveSource(metric.getMetricName(), dictionary);
        ParseRuleEntry ruleEntry = sourceEntry == null ? null : sourceEntry.getRule();
        BuiltinMetricAliasCatalog.BuiltinMetricMapping builtinMetric = sourceEntry == null
                ? BuiltinMetricAliasCatalog.match(metric.getMetricName())
                : null;
        Map<String, Object> strategyContext = new HashMap<>();
        strategyContext.put("sourceEntry", sourceEntry);
        strategyContext.put("builtinMetric", builtinMetric);
        strategyContext.put("metricName", metric.getMetricName());
        String metricStrategy = "fallback";
        try {
            metricStrategy = qlexpressRuleEngine.evaluateString(METRIC_STRATEGY_EXPR, strategyContext);
        } catch (Exception exception) {
            log.warn("指标映射策略表达式执行失败，将使用回退策略，metricName={}", metric.getMetricName(), exception);
        }
        String standardCode = ruleEntry != null
                ? ruleEntry.getColumnMap()
                : (builtinMetric != null ? builtinMetric.standardCode() : metric.getMetricName());
        String standardName = ruleEntry != null
                ? ruleEntry.getColumnMapName()
                : (builtinMetric != null ? builtinMetric.standardName() : metric.getMetricName());
        Object valueCandidate = metric.getValue();
        BigDecimal numericValue = toBigDecimal(valueCandidate);

        Map<String, Object> standardValues = new LinkedHashMap<>();
        if (metric.getRawValues() != null && !metric.getRawValues().isEmpty()) {
            standardValues.putAll(metric.getRawValues());
        }
        standardValues.put("metric_code", standardCode);
        standardValues.put("metric_name", standardName);
        if (valueCandidate != null) {
            standardValues.put("metric_value", normalizeValue(valueCandidate));
        }

        metric.setStandardCode(standardCode);
        metric.setStandardName(standardName);
        metric.setStandardValueText(valueCandidate == null ? null : String.valueOf(valueCandidate));
        metric.setStandardValueNumber(numericValue);
        metric.setStandardValueUnit(extractUnit(metric.getMetricName()));
        metric.setStandardValues(standardValues);
        metric.setMappingRuleId(ruleEntry == null ? null : ruleEntry.getId());
        metric.setMappingSourceId(sourceEntry == null ? null : sourceEntry.getId());
        if ("source_mapping".equals(metricStrategy) && sourceEntry != null) {
            metric.setMappingStatus("MAPPED");
            metric.setMappingReason("Matched source mapping for " + metric.getMetricName());
            metric.setMappingConfidence(0.92D);
        } else if ("builtin_alias".equals(metricStrategy) && builtinMetric != null) {
            metric.setMappingStatus("MAPPED");
            metric.setMappingReason("Matched builtin metric alias for " + metric.getMetricName());
            metric.setMappingConfidence(0.75D);
        } else if ("passthrough".equals(metricStrategy) && metric.getMetricName() != null && !metric.getMetricName().isBlank()) {
            metric.setMappingStatus("MAPPED");
            metric.setMappingReason("Fallback passthrough metric name");
            metric.setMappingConfidence(0.55D);
        } else {
            metric.setMappingStatus("UNMAPPED");
            metric.setMappingReason("No source mapping matched");
            metric.setMappingConfidence(0D);
        }
        return metric;
    }

    private Map<Integer, MappingDecision> resolveHeaderMappingDecisionByIndex(
            List<String> headers,
            List<HeaderColumnMeta> headerColumns,
            Dictionary dictionary,
            String strategyExpr
    ) {
        Map<Integer, List<String>> segmentsByIndex = new LinkedHashMap<>();
        for (HeaderColumnMeta meta : headerColumns) {
            if (meta == null || meta.getColumnIndex() == null) {
                continue;
            }
            segmentsByIndex.put(meta.getColumnIndex(), meta.getPathSegments() == null ? List.of() : meta.getPathSegments());
        }
        List<HeaderMappingInput> inputs = new java.util.ArrayList<>();
        for (int index = 0; index < headers.size(); index++) {
            String header = headers.get(index);
            if (header == null || header.isBlank()) {
                continue;
            }
            inputs.add(new HeaderMappingInput(index, header, segmentsByIndex.getOrDefault(index, List.of())));
        }

        HeaderMappingLookup lookup = new HeaderMappingLookup() {
            @Override
            public HeaderMappingCandidate findExact(String text) {
                ParseSourceEntry source = resolveSourceExact(text, dictionary);
                if (source == null || source.getRule() == null || source.getRule().getColumnMap() == null) {
                    return null;
                }
                return new HeaderMappingCandidate(source.getRule().getId(), source.getId(), source.getRule().getColumnMap());
            }

            @Override
            public HeaderMappingCandidate findAliasContains(String text) {
                ParseSourceEntry source = resolveAliasContains(text, dictionary);
                if (source != null && source.getRule() != null && source.getRule().getColumnMap() != null) {
                    return new HeaderMappingCandidate(source.getRule().getId(), source.getId(), source.getRule().getColumnMap());
                }
                return BuiltinHeaderAliasCatalog.matchByContains(text);
            }
        };
        return headerMappingEngine.map(inputs, lookup, strategyExpr);
    }

    private ParseSourceEntry resolveSource(String text, Dictionary dictionary) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String trimmedText = text.trim();
        ParseSourceEntry entry = resolveSourceExact(trimmedText, dictionary);
        ParseSourceEntry segmentEntry = resolveSourceBySegments(trimmedText, dictionary);
        if (entry != null) {
            return chooseSourceByRule(trimmedText, entry, segmentEntry, resolveAliasContains(trimmedText, dictionary));
        }
        ParseSourceEntry aliasEntry = resolveAliasContains(trimmedText, dictionary);
        return chooseSourceByRule(trimmedText, entry, segmentEntry, aliasEntry);
    }

    private ParseSourceEntry resolveSourceExact(String text, Dictionary dictionary) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Map<String, ParseSourceEntry> sourceByCode = dictionary == null ? Map.of() : dictionary.sourceByCode();
        Map<String, ParseSourceEntry> sourceByAlias = dictionary == null ? Map.of() : dictionary.sourceByAlias();
        ParseSourceEntry entry = sourceByCode.get(text);
        if (entry != null) {
            return entry;
        }
        return sourceByAlias.get(text);
    }

    private ParseSourceEntry resolveSourceBySegments(String text, Dictionary dictionary) {
        if (text == null || text.isBlank()) {
            return null;
        }
        for (String segment : text.split("\\|")) {
            String segmentText = segment == null ? "" : segment.trim();
            if (segmentText.isBlank()) {
                continue;
            }
            ParseSourceEntry entry = resolveSourceExact(segmentText, dictionary);
            if (entry != null) {
                return entry;
            }
        }
        return null;
    }

    private ParseSourceEntry resolveAliasContains(String text, Dictionary dictionary) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String trimmedText = text.trim();
        Map<String, ParseSourceEntry> sourceByAlias = dictionary == null ? Map.of() : dictionary.sourceByAlias();
        String bestAlias = null;
        ParseSourceEntry bestEntry = null;
        for (Map.Entry<String, ParseSourceEntry> entry : sourceByAlias.entrySet()) {
            String alias = entry.getKey();
            if (alias == null || alias.isBlank() || alias.length() < 2) {
                continue;
            }
            if (trimmedText.contains(alias)) {
                if (bestAlias == null || alias.length() > bestAlias.length()) {
                    bestAlias = alias;
                    bestEntry = entry.getValue();
                }
            }
        }
        return bestEntry;
    }

    private ParseSourceEntry chooseSourceByRule(
            String metricText,
            ParseSourceEntry exactEntry,
            ParseSourceEntry segmentEntry,
            ParseSourceEntry aliasEntry
    ) {
        Map<String, Object> context = new HashMap<>();
        context.put("metricText", metricText);
        context.put("exactEntry", exactEntry);
        context.put("segmentEntry", segmentEntry);
        context.put("aliasEntry", aliasEntry);
        String strategy = "fallback";
        try {
            strategy = qlexpressRuleEngine.evaluateString(SOURCE_STRATEGY_EXPR, context);
        } catch (Exception exception) {
            log.warn("来源映射策略表达式执行失败，将使用回退策略，metricText={}", metricText, exception);
        }
        if ("exact_source".equals(strategy) && exactEntry != null) {
            return exactEntry;
        }
        if ("segment_source".equals(strategy) && segmentEntry != null) {
            return segmentEntry;
        }
        if ("alias_source".equals(strategy) && aliasEntry != null) {
            return aliasEntry;
        }
        if (exactEntry != null) {
            return exactEntry;
        }
        if (segmentEntry != null) {
            return segmentEntry;
        }
        return aliasEntry;
    }

    private HeaderQualitySummary logHeaderMappingSummary(List<String> headers, Map<Integer, MappingDecision> mappingDecisionByIndex) {
        if (headers == null || headers.isEmpty()) {
            return new HeaderQualitySummary(0, 0, List.of());
        }
        int mappedCount = 0;
        List<String> unmappedHeaders = new java.util.ArrayList<>();
        for (int index = 0; index < headers.size(); index++) {
            String header = headers.get(index);
            if (header == null || header.isBlank()) {
                continue;
            }
            MappingDecision decision = mappingDecisionByIndex.get(index);
            if (decision != null && Boolean.TRUE.equals(decision.getMatched())) {
                mappedCount++;
                continue;
            }
            unmappedHeaders.add(header);
        }
        int total = mappedCount + unmappedHeaders.size();
        double ratio = total == 0 ? 1D : ((double) mappedCount / total);
        if (ratio < 0.70D) {
            log.warn(
                    "外部估值表头映射率偏低，mapped={}, total={}, ratio={}, unmappedTop5={}",
                    mappedCount,
                    total,
                    String.format(java.util.Locale.ROOT, "%.2f", ratio),
                    unmappedHeaders.stream().limit(5).toList()
            );
            return new HeaderQualitySummary(total, mappedCount, unmappedHeaders);
        }
        log.info(
                "外部估值表头映射完成，mapped={}, total={}, ratio={}, unmappedCount={}",
                mappedCount,
                total,
                String.format(java.util.Locale.ROOT, "%.2f", ratio),
                unmappedHeaders.size()
        );
        return new HeaderQualitySummary(total, mappedCount, unmappedHeaders);
    }

    private void logSubjectMetricMappingSummary(List<SubjectRecord> subjects, List<MetricRecord> metrics) {
        long mappedSubjects = subjects == null ? 0 : subjects.stream().filter(s -> "MAPPED".equalsIgnoreCase(s.getMappingStatus())).count();
        long totalSubjects = subjects == null ? 0 : subjects.size();
        long mappedMetrics = metrics == null ? 0 : metrics.stream().filter(m -> "MAPPED".equalsIgnoreCase(m.getMappingStatus())).count();
        long totalMetrics = metrics == null ? 0 : metrics.size();
        log.info(
                "外部估值标准化映射统计，subjectMapped={}/{}, metricMapped={}/{}",
                mappedSubjects,
                totalSubjects,
                mappedMetrics,
                totalMetrics
        );
    }

    private String buildSubjectMappingReason(Map<String, Object> standardValues, List<MappingDecision> matchedDecisions) {
        if (standardValues == null || standardValues.isEmpty()) {
            return "No standard columns matched for subject row";
        }
        Map<String, Integer> strategyCount = new HashMap<>();
        for (MappingDecision decision : matchedDecisions) {
            if (decision == null || !Boolean.TRUE.equals(decision.getMatched()) || decision.getStrategy() == null) {
                continue;
            }
            strategyCount.merge(decision.getStrategy(), 1, Integer::sum);
        }
        return "Matched " + standardValues.size() + " standard columns, strategies=" + strategyCount;
    }

    private String resolveFileScene(ParsedValuationData parsedValuationData) {
        return "VALSET";
    }

    private String resolveFileTypeName(ParsedValuationData parsedValuationData) {
        if (parsedValuationData == null || parsedValuationData.getFileNameOriginal() == null) {
            return null;
        }
        String fileName = parsedValuationData.getFileNameOriginal().trim().toLowerCase(java.util.Locale.ROOT);
        if (fileName.endsWith(".csv")) {
            return "CSV";
        }
        return "EXCEL";
    }

    private Double calculateSubjectMappingConfidence(Map<String, Object> standardValues, List<MappingDecision> matchedDecisions) {
        if (standardValues == null || standardValues.isEmpty() || matchedDecisions == null || matchedDecisions.isEmpty()) {
            return 0D;
        }
        double total = 0D;
        int count = 0;
        for (MappingDecision decision : matchedDecisions) {
            if (decision == null || !Boolean.TRUE.equals(decision.getMatched()) || decision.getConfidence() == null) {
                continue;
            }
            total += decision.getConfidence();
            count++;
        }
        if (count == 0) {
            return 0D;
        }
        return Math.min(1D, total / count);
    }

    private MappingQualityReport buildMappingQualityReport(
            HeaderQualitySummary headerSummary,
            List<SubjectRecord> subjects,
            List<MetricRecord> metrics
    ) {
        int subjectTotal = subjects == null ? 0 : subjects.size();
        int subjectMapped = subjects == null ? 0 : (int) subjects.stream().filter(s -> "MAPPED".equalsIgnoreCase(s.getMappingStatus())).count();
        int metricTotal = metrics == null ? 0 : metrics.size();
        int metricMapped = metrics == null ? 0 : (int) metrics.stream().filter(m -> "MAPPED".equalsIgnoreCase(m.getMappingStatus())).count();
        return MappingQualityReport.builder()
                .headerTotal(headerSummary.total())
                .headerMapped(headerSummary.mapped())
                .headerUnmapped(Math.max(0, headerSummary.total() - headerSummary.mapped()))
                .headerUnmappedTop(headerSummary.unmappedHeaders().stream().limit(5).toList())
                .subjectTotal(subjectTotal)
                .subjectMapped(subjectMapped)
                .subjectUnmapped(Math.max(0, subjectTotal - subjectMapped))
                .metricTotal(metricTotal)
                .metricMapped(metricMapped)
                .metricUnmapped(Math.max(0, metricTotal - metricMapped))
                .build();
    }

    private String extractUnit(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String normalized = MatchTextSupport.normalizeMatchText(text);
        if (normalized.contains("%")) {
            return "%";
        }
        if (normalized.contains("原币")) {
            return "原币";
        }
        if (normalized.contains("本币")) {
            return "本币";
        }
        return null;
    }

    private boolean isBlankValue(Object value) {
        return value == null || (value instanceof String text && text.isBlank());
    }

    private Object normalizeValue(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros();
        }
        return value;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros();
        }
        String text = String.valueOf(value).trim();
        if (text.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(text.replace(",", "").replace("%", ""));
        } catch (Exception ignored) {
            return null;
        }
    }

    private <T> T firstNonNull(ParseSourceEntry first, ParseSourceEntry second, java.util.function.Function<ParseSourceEntry, T> mapper) {
        if (first != null) {
            T value = mapper.apply(first);
            if (value != null) {
                return value;
            }
        }
        if (second != null) {
            return mapper.apply(second);
        }
        return null;
    }

    private Dictionary loadDictionary(
            FileParseRuleRepository parseRuleRepository,
            FileParseSourceRepository parseSourceRepository
    ) {
        try {
            List<ParseRuleEntry> rules = loadRules(parseRuleRepository);
            Map<String, ParseRuleEntry> ruleByCode = new LinkedHashMap<>();
            for (ParseRuleEntry rule : rules) {
                if (rule == null || rule.getId() == null || rule.getColumnMap() == null || rule.getColumnMap().isBlank()) {
                    continue;
                }
                ruleByCode.putIfAbsent(rule.getColumnMap().trim(), rule);
            }

            List<ParseSourceEntry> sources = loadSources(parseSourceRepository, ruleByCode);
            Map<String, ParseSourceEntry> sourceByCode = new LinkedHashMap<>();
            Map<String, ParseSourceEntry> sourceByAlias = new LinkedHashMap<>();
            for (ParseSourceEntry source : sources) {
                registerSourceAlias(sourceByCode, sourceByAlias, source.getColumnName(), source);
                registerSourceAlias(sourceByCode, sourceByAlias, source.getColumnMap(), source);
                registerSourceAlias(sourceByCode, sourceByAlias, source.getRule().getColumnMapName(), source);
            }
            log.info("外部估值标准字典加载完成，ruleCount={}, sourceCount={}, aliasCount={}",
                    rules.size(), sources.size(), sourceByAlias.size());
            return new Dictionary(sourceByCode, sourceByAlias);
        } catch (Exception exception) {
            log.warn("加载外部估值标准字典失败，将使用空字典", exception);
            return new Dictionary(Map.of(), Map.of());
        }
    }

    private Dictionary dictionary() {
        Dictionary current = dictionary;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            current = dictionary;
            if (current == null) {
                current = loadDictionary(parseRuleRepository, parseSourceRepository);
                dictionary = current;
            }
            return current;
        }
    }

    private List<ParseRuleEntry> loadRules(FileParseRuleRepository parseRuleRepository) {
        return parseRuleRepository.selectList(
                        Wrappers.lambdaQuery(FileParseRulePO.class)
                                .orderByAsc(FileParseRulePO::getId)
                ).stream()
                .filter(this::isEnabled)
                .map(ParseRuleEntry::from)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<ParseSourceEntry> loadSources(
            FileParseSourceRepository parseSourceRepository,
            Map<String, ParseRuleEntry> ruleByCode
    ) {
        return parseSourceRepository.selectList(
                        Wrappers.lambdaQuery(FileParseSourcePO.class)
                                .orderByAsc(FileParseSourcePO::getId)
                ).stream()
                .filter(this::isEnabled)
                .map(source -> ParseSourceEntry.from(source, ruleByCode))
                .filter(Objects::nonNull)
                .toList();
    }

    private void registerSourceAlias(
            Map<String, ParseSourceEntry> sourceByCode,
            Map<String, ParseSourceEntry> sourceByAlias,
            String alias,
            ParseSourceEntry source
    ) {
        if (alias == null || alias.isBlank() || source == null) {
            return;
        }
        String trimmedAlias = alias.trim();
        sourceByCode.putIfAbsent(trimmedAlias, source);
        sourceByAlias.putIfAbsent(trimmedAlias, source);
    }

    private record Dictionary(
            Map<String, ParseSourceEntry> sourceByCode,
            Map<String, ParseSourceEntry> sourceByAlias
    ) {
    }
    private record HeaderQualitySummary(
            int total,
            int mapped,
            List<String> unmappedHeaders
    ) {
    }

    @Getter
    private static class ParseRuleEntry {
        private final Long id;
        private final String regionName;
        private final String columnMap;
        private final String columnMapName;
        private final String fileTypeName;
        private final Boolean required;
        private final Boolean multiIndex;

        private ParseRuleEntry(Long id, String regionName, String columnMap, String columnMapName, String fileTypeName, Boolean required, Boolean multiIndex) {
            this.id = id;
            this.regionName = regionName;
            this.columnMap = columnMap;
            this.columnMapName = columnMapName;
            this.fileTypeName = fileTypeName;
            this.required = required;
            this.multiIndex = multiIndex;
        }

        static ParseRuleEntry from(FileParseRulePO po) {
            if (po == null) {
                return null;
            }
            return new ParseRuleEntry(
                    po.getId(),
                    po.getRegionName(),
                    po.getColumnMap(),
                    po.getColumnMapName(),
                    po.getFileTypeName(),
                    po.getRequired(),
                    po.getMultiIndex()
            );
        }
    }

    @Getter
    private static class ParseSourceEntry {
        private final Long id;
        private final String fileType;
        private final String columnMap;
        private final String columnName;
        private final String fileExtInfo;
        private final Boolean status;
        private final ParseRuleEntry rule;

        private ParseSourceEntry(Long id, String fileType, String columnMap, String columnName, String fileExtInfo, Boolean status, ParseRuleEntry rule) {
            this.id = id;
            this.fileType = fileType;
            this.columnMap = columnMap;
            this.columnName = columnName;
            this.fileExtInfo = fileExtInfo;
            this.status = status;
            this.rule = rule;
        }

        static ParseSourceEntry from(
                FileParseSourcePO po,
                Map<String, ParseRuleEntry> ruleByCode
        ) {
            if (po == null) {
                return null;
            }
            ParseRuleEntry rule = null;
            if (rule == null && po.getColumnMap() != null && !po.getColumnMap().isBlank()) {
                rule = ruleByCode.get(po.getColumnMap().trim());
            }
            if (rule == null) {
                return null;
            }
            return new ParseSourceEntry(
                    po.getId(),
                    po.getFileType(),
                    po.getColumnMap(),
                    po.getColumnName(),
                    po.getFileExtInfo(),
                    po.getStatus(),
                    rule
            );
        }
    }

    private boolean isEnabled(FileParseRulePO po) {
        return po != null && !Boolean.FALSE.equals(po.getStatus());
    }

    private boolean isEnabled(FileParseSourcePO po) {
        return po != null && !Boolean.FALSE.equals(po.getStatus());
    }

    /**
     * 来源映射策略表达式。
     */
    private static final String SOURCE_STRATEGY_EXPR = "exactEntry != null ? 'exact_source' : "
            + "(segmentEntry != null ? 'segment_source' : "
            + "(aliasEntry != null ? 'alias_source' : 'fallback'))";

    /**
     * 指标映射策略表达式。
     */
    private static final String METRIC_STRATEGY_EXPR = "sourceEntry != null ? 'source_mapping' : "
            + "(builtinMetric != null ? 'builtin_alias' : "
            + "(hasText(metricName) ? 'passthrough' : 'unmapped'))";
}
