package com.yss.valset.extract.repository.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.domain.exporter.ResultExporter;
import com.yss.valset.domain.model.*;
import com.yss.valset.extract.support.ParsedValuationDataProjectionSupport;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 文件系统导出器，为每个任务写入 JSON 和 CSV 工件。
 */
@Component
public class FileSystemResultExporter implements ResultExporter {

    private final ObjectMapper objectMapper;
    private final Path outputRoot;

    public FileSystemResultExporter(
            ObjectMapper objectMapper,
            @Value("${subject.match.output-dir:output}") String outputRoot
    ) {
        this.objectMapper = objectMapper;
        this.outputRoot = Paths.get(outputRoot).toAbsolutePath();
    }

    /**
     * 将解析工件导出到任务输出目录。
     */
    @Override
    public void exportParsedValuationData(Long taskId, ParsedValuationData parsedValuationData) {
        Path taskDirectory = resolveTaskDirectory(taskId);
        List<SubjectRelation> subjectRelations = ParsedValuationDataProjectionSupport.buildSubjectRelations(parsedValuationData.getSubjects());
        List<SubjectTreeNode> subjectTree = ParsedValuationDataProjectionSupport.buildSubjectTree(parsedValuationData.getSubjects());
        WorkbookSummary summary = ParsedValuationDataProjectionSupport.buildSummary(parsedValuationData);

        writeJson(taskDirectory.resolve("parsed.json"), buildParsedPayload(parsedValuationData, subjectRelations, subjectTree));
        writeCsv(taskDirectory.resolve("subjects.csv"), buildSubjectRows(parsedValuationData.getSubjects()));
        writeCsv(taskDirectory.resolve("subject_relations.csv"), buildSubjectRelationRows(subjectRelations));
        writeJson(taskDirectory.resolve("subject_tree.json"), com.yss.valset.common.support.Java8Maps.of("roots", subjectTree));
        writeCsv(taskDirectory.resolve("metrics.csv"), buildMetricRows(parsedValuationData));
        writeJson(taskDirectory.resolve("summary.json"), summary);
    }

    /**
     * 将匹配工件导出到任务输出目录。
     */
    @Override
    public void exportMatchResults(Long taskId, ParsedValuationData parsedValuationData, List<ValsetMatchResult> results) {
        Path taskDirectory = resolveTaskDirectory(taskId);
        Map<String, Object> summary = buildMatchSummary(parsedValuationData, results);
        List<Map<String, Object>> top1Rows = buildTop1Rows(results);
        List<Map<String, Object>> candidateRows = buildCandidateRows(results);
        List<Map<String, Object>> reviewQueueRows = buildReviewQueueRows(taskId, results);
        writeJson(taskDirectory.resolve("match_results.json"), results);
        writeJson(taskDirectory.resolve("match_summary.json"), summary);
        writeCsv(taskDirectory.resolve("match_top1.csv"), top1Rows);
        writeCsv(taskDirectory.resolve("match_candidates.csv"), candidateRows);
        writeCsv(taskDirectory.resolve("review_queue.csv"), reviewQueueRows);
    }

    /**
     * 将映射评估工件导出到任务输出目录。
     */
    @Override
    public void exportMappingEvaluation(Long taskId, Map<String, Object> evaluationResult) {
        Path taskDirectory = resolveTaskDirectory(taskId);
        writeJson(taskDirectory.resolve("mapping_evaluation.json"), evaluationResult);
        Object failureAnalysis = evaluationResult == null ? null : evaluationResult.get("failure_analysis");
        if (failureAnalysis != null) {
            writeJson(taskDirectory.resolve("failure_cluster.json"), failureAnalysis);
        }
        Map<String, Object> weightSearchReport = new LinkedHashMap<>();
        if (evaluationResult != null) {
            weightSearchReport.put("baseline_weights", evaluationResult.get("baseline_weights"));
            weightSearchReport.put("baseline_metrics", evaluationResult.get("baseline_metrics"));
            weightSearchReport.put("recommended_weights", evaluationResult.get("recommended_weights"));
            weightSearchReport.put("recommended_metrics", evaluationResult.get("recommended_metrics"));
            weightSearchReport.put("weight_search", evaluationResult.get("weight_search"));
        }
        writeJson(taskDirectory.resolve("weight_search_report.json"), weightSearchReport);
    }

    private Path resolveTaskDirectory(Long taskId) {
        try {
            Path taskDirectory = outputRoot.resolve("task-" + taskId);
            Files.createDirectories(taskDirectory);
            return taskDirectory;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to prepare task output directory", exception);
        }
    }

    private List<Map<String, Object>> buildSubjectRows(List<SubjectRecord> subjects) {
        return subjects == null ? java.util.Arrays.asList() : subjects.stream()
                                              .map(subject -> {
                                                  Map<String, Object> row = new LinkedHashMap<>();
                                                  row.put("sheet_name", subject.getSheetName());
                                                  row.put("row_data_number", subject.getRowDataNumber());
                                                  row.put("subject_code", subject.getSubjectCode());
                                                  row.put("subject_name", subject.getSubjectName());
                                                  row.put("level", subject.getLevel());
                                                  row.put("parent_code", subject.getParentCode());
                                                  row.put("root_code", subject.getRootCode());
                                                  row.put("segment_count", subject.getSegmentCount());
                                                  row.put("path_codes", subject.getPathCodes());
                                                  row.put("is_leaf", subject.getLeaf());
                                                  return row;
                                              })
                                              .collect(java.util.stream.Collectors.toList());
    }

    private List<Map<String, Object>> buildSubjectRelationRows(List<SubjectRelation> relations) {
        return relations == null ? java.util.Arrays.asList() : relations.stream()
                                               .map(relation -> {
                                                   Map<String, Object> row = new LinkedHashMap<>();
                                                   row.put("subject_code", relation.getSubjectCode());
                                                   row.put("subject_name", relation.getSubjectName());
                                                   row.put("parent_code", relation.getParentCode());
                                                   row.put("parent_name", relation.getParentName());
                                                   row.put("level", relation.getLevel());
                                                   row.put("root_code", relation.getRootCode());
                                                   row.put("segment_count", relation.getSegmentCount());
                                                   row.put("is_leaf", relation.getLeaf());
                                                   row.put("path_codes", relation.getPathCodes());
                                                   return row;
                                               })
                                               .collect(java.util.stream.Collectors.toList());
    }

    private List<Map<String, Object>> buildMetricRows(ParsedValuationData parsedValuationData) {
        if (parsedValuationData == null || parsedValuationData.getMetrics() == null) {
            return java.util.Arrays.asList();
        }
        return parsedValuationData.getMetrics().stream()
                .map(metric -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("sheet_name", metric.getSheetName());
                    row.put("row_data_number", metric.getRowDataNumber());
                    row.put("metric_name", metric.getMetricName());
                    row.put("metric_type", metric.getMetricType());
                    row.put("value", metric.getValue());
                    if (metric.getRawValues() != null) {
                        for (Map.Entry<String, Object> entry : metric.getRawValues().entrySet()) {
                            row.put("raw_" + entry.getKey(), entry.getValue());
                        }
                    }
                    return row;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    private List<HeaderColumnMeta> resolveHeaderColumns(ParsedValuationData parsedValuationData) {
        if (parsedValuationData == null) {
            return java.util.Arrays.asList();
        }
        if (parsedValuationData.getHeaderColumns() != null && !parsedValuationData.getHeaderColumns().isEmpty()) {
            return parsedValuationData.getHeaderColumns();
        }
        if (parsedValuationData.getHeaders() == null || parsedValuationData.getHeaders().isEmpty()) {
            return java.util.Arrays.asList();
        }
        List<HeaderColumnMeta> result = new ArrayList<>(parsedValuationData.getHeaders().size());
        for (int index = 0; index < parsedValuationData.getHeaders().size(); index++) {
            String header = parsedValuationData.getHeaders().get(index);
            List<String> detail = parsedValuationData.getHeaderDetails() != null && index < parsedValuationData.getHeaderDetails().size()
                    ? parsedValuationData.getHeaderDetails().get(index)
                    : java.util.Arrays.asList();
            result.add(HeaderColumnMeta.builder()
                    .columnIndex(index)
                    .headerName(header)
                    .headerPath(header)
                    .pathSegments(detail)
                    .blankColumn(header == null || header.trim().isEmpty())
                    .build());
        }
        return result;
    }

    private List<Map<String, Object>> buildTop1Rows(List<ValsetMatchResult> results) {
        if (results == null) {
            return java.util.Arrays.asList();
        }
        return results.stream()
                .map(result -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("external_subject_code", result.getExternalSubjectCode());
                    row.put("external_subject_name", result.getExternalSubjectName());
                    row.put("external_level", result.getExternalLevel());
                    row.put("external_is_leaf", result.getExternalIsLeaf());
                    row.put("anchor_subject_code", result.getAnchorSubjectCode());
                    row.put("anchor_subject_name", result.getAnchorSubjectName());
                    row.put("anchor_level", result.getAnchorLevel());
                    row.put("anchor_path_text", result.getAnchorPathText());
                    row.put("anchor_reason", result.getAnchorReason());
                    row.put("matched_standard_code", result.getMatchedStandardCode());
                    row.put("matched_standard_name", result.getMatchedStandardName());
                    row.put("score", result.getScore());
                    row.put("score_name", result.getScoreName());
                    row.put("score_path", result.getScorePath());
                    row.put("score_keyword", result.getScoreKeyword());
                    row.put("score_code", result.getScoreCode());
                    row.put("score_history", result.getScoreHistory());
                    row.put("score_embedding", result.getScoreEmbedding());
                    row.put("confidence_level", result.getConfidenceLevel());
                    row.put("needs_review", result.getNeedsReview());
                    row.put("match_reason", result.getMatchReason());
                    row.put("candidate_count", result.getCandidateCount());
                    return row;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    private List<Map<String, Object>> buildCandidateRows(List<ValsetMatchResult> results) {
        if (results == null) {
            return java.util.Arrays.asList();
        }
        List<Map<String, Object>> rows = new java.util.ArrayList<>();
        for (ValsetMatchResult result : results) {
            if (result.getTopCandidates() == null) {
                continue;
            }
            int rank = 1;
            for (MatchCandidate candidate : result.getTopCandidates()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("external_subject_code", result.getExternalSubjectCode());
                row.put("external_subject_name", result.getExternalSubjectName());
                row.put("anchor_subject_code", result.getAnchorSubjectCode());
                row.put("anchor_subject_name", result.getAnchorSubjectName());
                row.put("rank", rank++);
                row.put("standard_code", candidate.getStandardCode());
                row.put("standard_name", candidate.getStandardName());
                row.put("score", candidate.getScore());
                row.put("score_name", candidate.getScoreName());
                row.put("score_path", candidate.getScorePath());
                row.put("score_keyword", candidate.getScoreKeyword());
                row.put("score_code", candidate.getScoreCode());
                row.put("score_history", candidate.getScoreHistory());
                row.put("score_embedding", candidate.getScoreEmbedding());
                row.put("matched_by_history", candidate.getMatchedByHistory());
                row.put("candidate_sources", candidate.getCandidateSources());
                row.put("reasons", candidate.getReasons());
                rows.add(row);
            }
        }
        return rows;
    }

    private List<Map<String, Object>> buildReviewQueueRows(Long taskId, List<ValsetMatchResult> results) {
        if (results == null) {
            return java.util.Arrays.asList();
        }
        List<ValsetMatchResult> reviewResults = results.stream()
                .filter(result -> Boolean.TRUE.equals(result.getNeedsReview()))
                .sorted(Comparator
                        .comparingInt(this::reviewPriority)
                        .thenComparing(this::top2Gap, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ValsetMatchResult::getScore, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(result -> stringify(result.getExternalSubjectCode())))
                .collect(java.util.stream.Collectors.toList());
        List<Map<String, Object>> rows = new java.util.ArrayList<>(reviewResults.size());
        String batchId = "task-" + taskId;
        for (int index = 0; index < reviewResults.size(); index++) {
            ValsetMatchResult result = reviewResults.get(index);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("review_id", String.format("TASK-%d-%04d", taskId, index + 1));
            row.put("batch_id", batchId);
            row.put("external_subject_code", result.getExternalSubjectCode());
            row.put("external_subject_name", result.getExternalSubjectName());
            row.put("anchor_subject_name", result.getAnchorSubjectName());
            row.put("matched_standard_code", result.getMatchedStandardCode());
            row.put("matched_standard_name", result.getMatchedStandardName());
            row.put("confidence", result.getConfidenceLevel());
            row.put("score", result.getScore());
            row.put("top2_gap", top2Gap(result));
            row.put("match_reason", result.getMatchReason());
            row.put("review_status", "PENDING_REVIEW");
            row.put("review_comment", "");
            row.put("final_standard_code", "");
            rows.add(row);
        }
        return rows;
    }

    private Map<String, Object> buildMatchSummary(ParsedValuationData parsedValuationData, List<ValsetMatchResult> results) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("workbook_path", parsedValuationData == null ? "" : parsedValuationData.getWorkbookPath());
        summary.put("sheet_name", parsedValuationData == null ? "" : parsedValuationData.getSheetName());
        summary.put("subject_count", results == null ? 0 : results.size());
        Map<String, Integer> distribution = new LinkedHashMap<>();
        distribution.put("HIGH", 0);
        distribution.put("MEDIUM", 0);
        distribution.put("LOW", 0);
        int needsReviewCount = 0;
        BigDecimal totalScore = BigDecimal.ZERO;
        if (results != null) {
            for (ValsetMatchResult result : results) {
                ConfidenceLevel confidenceLevel = result.getConfidenceLevel() == null ? ConfidenceLevel.LOW : result.getConfidenceLevel();
                distribution.compute(confidenceLevel.name(), (key, value) -> value == null ? 1 : value + 1);
                if (Boolean.TRUE.equals(result.getNeedsReview())) {
                    needsReviewCount++;
                }
                totalScore = totalScore.add(result.getScore() == null ? BigDecimal.ZERO : result.getScore());
            }
        }
        summary.put("confidence_distribution", distribution);
        summary.put("needs_review_count", needsReviewCount);
        summary.put("review_queue_count", needsReviewCount);
        summary.put("average_score", results == null || results.isEmpty()
                ? BigDecimal.ZERO
                : totalScore.divide(BigDecimal.valueOf(results.size()), 6, java.math.RoundingMode.HALF_UP));
        return summary;
    }

    private Map<String, Object> buildParsedPayload(
            ParsedValuationData parsedValuationData,
            List<SubjectRelation> subjectRelations,
            List<SubjectTreeNode> subjectTree
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("workbook_path", parsedValuationData.getWorkbookPath());
        payload.put("sheet_name", parsedValuationData.getSheetName());
        payload.put("header_row_number", parsedValuationData.getHeaderRowNumber());
        payload.put("data_start_row_number", parsedValuationData.getDataStartRowNumber());
        payload.put("file_name_original", parsedValuationData.getFileNameOriginal());
        payload.put("title", parsedValuationData.getTitle());
        payload.put("basic_info", parsedValuationData.getBasicInfo());
        payload.put("headers", parsedValuationData.getHeaders());
        payload.put("header_details", parsedValuationData.getHeaderDetails());
        payload.put("header_columns", parsedValuationData.getHeaderColumns());
        payload.put("subjects", parsedValuationData.getSubjects());
        payload.put("subject_relations", subjectRelations);
        payload.put("subject_tree", subjectTree);
        payload.put("metrics", parsedValuationData.getMetrics());
        return payload;
    }

    private void writeJson(Path outputPath, Object data) {
        try {
            Files.createDirectories(outputPath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputPath.toFile(), data);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write json file " + outputPath, exception);
        }
    }

    private void writeCsv(Path outputPath, List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        LinkedHashSet<String> headers = new LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            headers.addAll(row.keySet());
        }
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(headers.toArray(new String[0]))
                .build();
        try {
            Files.createDirectories(outputPath.getParent());
            try (Writer writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8);
                 CSVPrinter printer = new CSVPrinter(writer, csvFormat)) {
                for (Map<String, Object> row : rows) {
                    List<String> record = headers.stream()
                            .map(header -> stringify(row.get(header)))
                            .collect(java.util.stream.Collectors.toList());
                    printer.printRecord(record);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write csv file " + outputPath, exception);
        }
    }

    private String stringify(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).stripTrailingZeros().toPlainString();
        }
        if (value instanceof Number || value instanceof Boolean || value instanceof Enum<?>) {
            return String.valueOf(value);
        }
        if (value instanceof Collection<?> || value instanceof Map<?, ?>) {
            try {
                return objectMapper.writeValueAsString(value);
            } catch (JsonProcessingException exception) {
                return String.valueOf(value);
            }
        }
        return String.valueOf(value);
    }

    private int defaultInteger(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal jdbcDecimal(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.scale() < 0 ? value.setScale(0) : value;
    }

    private int integerValue(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? 0 : Integer.parseInt(text);
    }

    private boolean booleanValue(Object value) {
        if (value instanceof Boolean) {
            return ((Boolean) value).booleanValue();
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private BigDecimal decimalValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return jdbcDecimal((BigDecimal) value);
        }
        if (value instanceof Number) {
            return jdbcDecimal(BigDecimal.valueOf(((Number) value).doubleValue()));
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        return jdbcDecimal(new BigDecimal(text));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(Object value) {
        if (value instanceof Map<?, ?>) {
            return (Map<String, Object>) value;
        }
        return java.util.Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    private List<Object> listValue(Object value) {
        if (value instanceof List<?>) {
            return (List<Object>) value;
        }
        return java.util.Arrays.asList();
    }

    private int reviewPriority(ValsetMatchResult result) {
        if (result == null || result.getConfidenceLevel() == null) {
            return 99;
        }
        switch (result.getConfidenceLevel()) {
            case LOW:
                return 0;
            case MEDIUM:
                return 1;
            case HIGH:
                return 2;
            default:
                return 99;
        }
    }

    private BigDecimal top2Gap(ValsetMatchResult result) {
        if (result == null || result.getTopCandidates() == null || result.getTopCandidates().isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal top1 = result.getTopCandidates().get(0).getScore();
        BigDecimal top2 = result.getTopCandidates().size() > 1
                ? result.getTopCandidates().get(1).getScore()
                : BigDecimal.ZERO;
        BigDecimal gap = (top1 == null ? BigDecimal.ZERO : top1).subtract(top2 == null ? BigDecimal.ZERO : top2);
        return gap.setScale(6, java.math.RoundingMode.HALF_UP);
    }
}
