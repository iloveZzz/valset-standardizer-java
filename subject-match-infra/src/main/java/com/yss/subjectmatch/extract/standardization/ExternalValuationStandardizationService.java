package com.yss.subjectmatch.extract.standardization;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.subjectmatch.domain.model.HeaderColumnMeta;
import com.yss.subjectmatch.domain.model.MetricRecord;
import com.yss.subjectmatch.domain.model.ParsedValuationData;
import com.yss.subjectmatch.domain.model.SubjectRecord;
import com.yss.subjectmatch.extract.repository.entity.FileParseRulePO;
import com.yss.subjectmatch.extract.repository.entity.FileParseSourcePO;
import com.yss.subjectmatch.extract.repository.mapper.FileParseRuleRepository;
import com.yss.subjectmatch.extract.repository.mapper.FileParseSourceRepository;
import com.yss.subjectmatch.extract.support.MatchTextSupport;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 外部估值标准化服务。
 */
@Slf4j
@Service
public class ExternalValuationStandardizationService {

    private final ObjectMapper objectMapper;
    private final FileParseRuleRepository parseRuleRepository;
    private final FileParseSourceRepository parseSourceRepository;
    private volatile Dictionary dictionary;

    public ExternalValuationStandardizationService(
            ObjectMapper objectMapper,
            FileParseRuleRepository parseRuleRepository,
            FileParseSourceRepository parseSourceRepository
    ) {
        this.objectMapper = objectMapper;
        this.parseRuleRepository = parseRuleRepository;
        this.parseSourceRepository = parseSourceRepository;
    }

    public ParsedValuationData standardize(ParsedValuationData parsedValuationData) {
        if (parsedValuationData == null) {
            return null;
        }
        Dictionary dictionary = dictionary();
        List<String> headers = parsedValuationData.getHeaders() == null ? List.of() : parsedValuationData.getHeaders();
        List<HeaderColumnMeta> headerColumns = parsedValuationData.getHeaderColumns() == null ? List.of() : parsedValuationData.getHeaderColumns();
        Map<Integer, HeaderMappingDecision> standardColumnByIndex = resolveStandardColumnByIndex(headers, headerColumns, dictionary);
        logHeaderMappingSummary(headers, standardColumnByIndex);

        List<SubjectRecord> standardizedSubjects = parsedValuationData.getSubjects() == null
                ? List.of()
                : parsedValuationData.getSubjects().stream()
                .map(subject -> standardizeSubject(subject, headers, standardColumnByIndex, dictionary))
                .toList();
        List<MetricRecord> standardizedMetrics = parsedValuationData.getMetrics() == null
                ? List.of()
                : parsedValuationData.getMetrics().stream()
                .map(metric -> standardizeMetric(metric, dictionary))
                .toList();
        logSubjectMetricMappingSummary(standardizedSubjects, standardizedMetrics);

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
                .subjects(standardizedSubjects)
                .metrics(standardizedMetrics)
                .build();
    }

    private SubjectRecord standardizeSubject(
            SubjectRecord subject,
            List<String> headers,
            Map<Integer, HeaderMappingDecision> standardColumnByIndex,
            Dictionary dictionary
    ) {
        Map<String, Object> standardValues = new LinkedHashMap<>();
        List<HeaderMappingDecision> matchedDecisions = new java.util.ArrayList<>();
        List<Object> rawValues = subject.getRawValues() == null ? List.of() : subject.getRawValues();
        for (int index = 0; index < Math.min(headers.size(), rawValues.size()); index++) {
            HeaderMappingDecision decision = standardColumnByIndex.get(index);
            if (decision == null || !decision.matched()) {
                continue;
            }
            String standardCode = decision.standardCode();
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
        String standardCode = ruleEntry == null ? metric.getMetricName() : ruleEntry.getColumnMap();
        String standardName = ruleEntry == null ? metric.getMetricName() : ruleEntry.getColumnMapName();
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
        metric.setMappingStatus(sourceEntry == null ? "UNMAPPED" : "MAPPED");
        metric.setMappingReason(sourceEntry == null ? "No source mapping matched" : "Matched source mapping for " + metric.getMetricName());
        metric.setMappingConfidence(sourceEntry == null ? 0D : 0.92D);
        return metric;
    }

    private Map<Integer, HeaderMappingDecision> resolveStandardColumnByIndex(
            List<String> headers,
            List<HeaderColumnMeta> headerColumns,
            Dictionary dictionary
    ) {
        Map<Integer, HeaderMappingDecision> result = new LinkedHashMap<>();
        for (int index = 0; index < headers.size(); index++) {
            HeaderMappingDecision decision = resolveHeaderDecision(headers.get(index), dictionary);
            if (decision != null && decision.matched()) {
                result.put(index, decision);
            }
        }
        for (HeaderColumnMeta meta : headerColumns) {
            if (meta == null || meta.getColumnIndex() == null) {
                continue;
            }
            HeaderMappingDecision decision = resolveHeaderDecision(meta, dictionary);
            if (decision == null || !decision.matched()) {
                continue;
            }
            HeaderMappingDecision current = result.get(meta.getColumnIndex());
            if (current == null || decision.confidence() > current.confidence()) {
                result.put(meta.getColumnIndex(), decision);
            }
        }
        return result;
    }

    private HeaderMappingDecision resolveHeaderDecision(HeaderColumnMeta meta, Dictionary dictionary) {
        if (meta == null) {
            return HeaderMappingDecision.unmatched();
        }
        HeaderMappingDecision byHeaderName = resolveHeaderDecision(meta.getHeaderName(), dictionary);
        if (byHeaderName.matched()) {
            return byHeaderName;
        }
        if (meta.getPathSegments() == null || meta.getPathSegments().isEmpty()) {
            return HeaderMappingDecision.unmatched();
        }
        for (String segment : meta.getPathSegments()) {
            HeaderMappingDecision bySegment = resolveHeaderDecision(segment, dictionary);
            if (bySegment.matched()) {
                return bySegment;
            }
        }
        return HeaderMappingDecision.unmatched();
    }

    private HeaderMappingDecision resolveHeaderDecision(String text, Dictionary dictionary) {
        if (text == null || text.isBlank()) {
            return HeaderMappingDecision.unmatched();
        }
        String trimmedText = text.trim();

        ParseSourceEntry exact = resolveSourceExact(trimmedText, dictionary);
        if (exact != null) {
            return HeaderMappingDecision.of(exact, "exact_header", 0.98D, trimmedText);
        }

        for (String segment : trimmedText.split("\\|")) {
            String segmentText = segment.trim();
            if (segmentText.isBlank()) {
                continue;
            }
            ParseSourceEntry segmentEntry = resolveSourceExact(segmentText, dictionary);
            if (segmentEntry != null) {
                return HeaderMappingDecision.of(segmentEntry, "header_segment", 0.92D, segmentText);
            }
        }

        ParseSourceEntry containsEntry = resolveAliasContains(trimmedText, dictionary);
        if (containsEntry != null) {
            return HeaderMappingDecision.of(containsEntry, "alias_contains", 0.80D, trimmedText);
        }

        return HeaderMappingDecision.unmatched();
    }

    private ParseSourceEntry resolveSource(String text, Dictionary dictionary) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String trimmedText = text.trim();
        ParseSourceEntry entry = resolveSourceExact(trimmedText, dictionary);
        if (entry != null) {
            return entry;
        }
        for (String segment : trimmedText.split("\\|")) {
            String segmentText = segment.trim();
            if (segmentText.isBlank()) {
                continue;
            }
            entry = resolveSourceExact(segmentText, dictionary);
            if (entry != null) {
                return entry;
            }
        }
        return resolveAliasContains(trimmedText, dictionary);
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

    private void logHeaderMappingSummary(List<String> headers, Map<Integer, HeaderMappingDecision> standardColumnByIndex) {
        if (headers == null || headers.isEmpty()) {
            return;
        }
        int mappedCount = 0;
        List<String> unmappedHeaders = new java.util.ArrayList<>();
        for (int index = 0; index < headers.size(); index++) {
            String header = headers.get(index);
            if (header == null || header.isBlank()) {
                continue;
            }
            HeaderMappingDecision decision = standardColumnByIndex.get(index);
            if (decision != null && decision.matched()) {
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
            return;
        }
        log.info(
                "外部估值表头映射完成，mapped={}, total={}, ratio={}, unmappedCount={}",
                mappedCount,
                total,
                String.format(java.util.Locale.ROOT, "%.2f", ratio),
                unmappedHeaders.size()
        );
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

    private String buildSubjectMappingReason(Map<String, Object> standardValues, List<HeaderMappingDecision> matchedDecisions) {
        if (standardValues == null || standardValues.isEmpty()) {
            return "No standard columns matched for subject row";
        }
        Map<String, Integer> strategyCount = new HashMap<>();
        for (HeaderMappingDecision decision : matchedDecisions) {
            if (decision == null || !decision.matched() || decision.strategy() == null) {
                continue;
            }
            strategyCount.merge(decision.strategy(), 1, Integer::sum);
        }
        return "Matched " + standardValues.size() + " standard columns, strategies=" + strategyCount;
    }

    private Double calculateSubjectMappingConfidence(Map<String, Object> standardValues, List<HeaderMappingDecision> matchedDecisions) {
        if (standardValues == null || standardValues.isEmpty() || matchedDecisions == null || matchedDecisions.isEmpty()) {
            return 0D;
        }
        double total = 0D;
        int count = 0;
        for (HeaderMappingDecision decision : matchedDecisions) {
            if (decision == null || !decision.matched()) {
                continue;
            }
            total += decision.confidence();
            count++;
        }
        if (count == 0) {
            return 0D;
        }
        return Math.min(1D, total / count);
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

    private record HeaderMappingDecision(
            String standardCode,
            ParseSourceEntry source,
            String strategy,
            Double confidence,
            String matchedText
    ) {
        static HeaderMappingDecision unmatched() {
            return new HeaderMappingDecision(null, null, null, 0D, null);
        }

        static HeaderMappingDecision of(ParseSourceEntry source, String strategy, Double confidence, String matchedText) {
            if (source == null || source.getRule() == null || source.getRule().getColumnMap() == null || source.getRule().getColumnMap().isBlank()) {
                return unmatched();
            }
            return new HeaderMappingDecision(source.getRule().getColumnMap(), source, strategy, confidence, matchedText);
        }

        boolean matched() {
            return standardCode != null && !standardCode.isBlank();
        }
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
}
