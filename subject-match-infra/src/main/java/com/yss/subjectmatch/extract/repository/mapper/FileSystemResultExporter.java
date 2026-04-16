package com.yss.subjectmatch.extract.repository.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.subjectmatch.domain.exporter.ResultExporter;
import com.yss.subjectmatch.domain.model.*;
import com.yss.subjectmatch.extract.support.ParsedValuationDataProjectionSupport;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.duckdb.DuckDBConnection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * 文件系统导出器，为每个任务写入 JSON、CSV 和 DuckDB 工件。
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
        this.outputRoot = Path.of(outputRoot).toAbsolutePath();
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
        writeJson(taskDirectory.resolve("subject_tree.json"), Map.of("roots", subjectTree));
        writeCsv(taskDirectory.resolve("metrics.csv"), buildMetricRows(parsedValuationData));
        writeJson(taskDirectory.resolve("summary.json"), summary);
        writeDuckDb(taskDirectory.resolve("parsed.duckdb"), parsedValuationData, summary, subjectRelations, subjectTree);
    }

    /**
     * 将匹配工件导出到任务输出目录。
     */
    @Override
    public void exportMatchResults(Long taskId, ParsedValuationData parsedValuationData, List<SubjectMatchResult> results) {
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
        writeMatchDuckDb(taskDirectory.resolve("match.duckdb"), summary, top1Rows, candidateRows, reviewQueueRows);
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
        writeEvaluationDuckDb(taskDirectory.resolve("evaluation.duckdb"), evaluationResult, weightSearchReport);
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
        return subjects == null ? List.of() : subjects.stream()
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
                                              .toList();
    }

    private List<Map<String, Object>> buildSubjectRelationRows(List<SubjectRelation> relations) {
        return relations == null ? List.of() : relations.stream()
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
                                               .toList();
    }

    private List<Map<String, Object>> buildMetricRows(ParsedValuationData parsedValuationData) {
        if (parsedValuationData == null || parsedValuationData.getMetrics() == null) {
            return List.of();
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
                .toList();
    }

    private List<HeaderColumnMeta> resolveHeaderColumns(ParsedValuationData parsedValuationData) {
        if (parsedValuationData == null) {
            return List.of();
        }
        if (parsedValuationData.getHeaderColumns() != null && !parsedValuationData.getHeaderColumns().isEmpty()) {
            return parsedValuationData.getHeaderColumns();
        }
        if (parsedValuationData.getHeaders() == null || parsedValuationData.getHeaders().isEmpty()) {
            return List.of();
        }
        List<HeaderColumnMeta> result = new ArrayList<>(parsedValuationData.getHeaders().size());
        for (int index = 0; index < parsedValuationData.getHeaders().size(); index++) {
            String header = parsedValuationData.getHeaders().get(index);
            List<String> detail = parsedValuationData.getHeaderDetails() != null && index < parsedValuationData.getHeaderDetails().size()
                    ? parsedValuationData.getHeaderDetails().get(index)
                    : List.of();
            result.add(HeaderColumnMeta.builder()
                    .columnIndex(index)
                    .headerName(header)
                    .headerPath(header)
                    .pathSegments(detail)
                    .blankColumn(header == null || header.isBlank())
                    .build());
        }
        return result;
    }

    private List<Map<String, Object>> buildTop1Rows(List<SubjectMatchResult> results) {
        if (results == null) {
            return List.of();
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
                .toList();
    }

    private List<Map<String, Object>> buildCandidateRows(List<SubjectMatchResult> results) {
        if (results == null) {
            return List.of();
        }
        List<Map<String, Object>> rows = new java.util.ArrayList<>();
        for (SubjectMatchResult result : results) {
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

    private List<Map<String, Object>> buildReviewQueueRows(Long taskId, List<SubjectMatchResult> results) {
        if (results == null) {
            return List.of();
        }
        List<SubjectMatchResult> reviewResults = results.stream()
                .filter(result -> Boolean.TRUE.equals(result.getNeedsReview()))
                .sorted(Comparator
                        .comparingInt(this::reviewPriority)
                        .thenComparing(this::top2Gap, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(SubjectMatchResult::getScore, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(result -> stringify(result.getExternalSubjectCode())))
                .toList();
        List<Map<String, Object>> rows = new java.util.ArrayList<>(reviewResults.size());
        String batchId = "task-" + taskId;
        for (int index = 0; index < reviewResults.size(); index++) {
            SubjectMatchResult result = reviewResults.get(index);
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

    private Map<String, Object> buildMatchSummary(ParsedValuationData parsedValuationData, List<SubjectMatchResult> results) {
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
            for (SubjectMatchResult result : results) {
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
                .setHeader(headers.toArray(String[]::new))
                .build();
        try {
            Files.createDirectories(outputPath.getParent());
            try (Writer writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8);
                 CSVPrinter printer = new CSVPrinter(writer, csvFormat)) {
                for (Map<String, Object> row : rows) {
                    List<String> record = headers.stream()
                            .map(header -> stringify(row.get(header)))
                            .toList();
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
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
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

    private void writeDuckDb(
            Path outputPath,
            ParsedValuationData parsedValuationData,
            WorkbookSummary summary,
            List<SubjectRelation> subjectRelations,
            List<SubjectTreeNode> subjectTree
    ) {
        try {
            Files.createDirectories(outputPath.getParent());
            Files.deleteIfExists(outputPath);
            try (DuckDBConnection connection = (DuckDBConnection) DriverManager.getConnection("jdbc:duckdb:" + outputPath)) {
                connection.setAutoCommit(false);
                createDuckDbTables(connection);
                insertWorkbookInfo(connection, parsedValuationData);
                insertBasicInfo(connection, parsedValuationData);
                insertHeaders(connection, parsedValuationData);
                insertHeaderDetails(connection, parsedValuationData);
                insertHeaderColumns(connection, parsedValuationData);
                insertSubjects(connection, parsedValuationData);
                insertSubjectRelations(connection, subjectRelations);
                insertMetrics(connection, parsedValuationData);
                insertSummary(connection, summary);
                insertSubjectTree(connection, subjectTree);
                connection.commit();
            }
        } catch (IOException | SQLException exception) {
            throw new IllegalStateException("Failed to write DuckDB file " + outputPath, exception);
        }
    }

    private void createDuckDbTables(DuckDBConnection connection) throws SQLException {
        execute(connection, "create table workbook_info(workbook_path varchar, sheet_name varchar, file_name_original varchar, title varchar, header_row_number integer, data_start_row_number integer)");
        execute(connection, "create table basic_info(info_key varchar, info_value varchar)");
        execute(connection, "create table headers(column_index integer, header_name varchar)");
        execute(connection, "create table header_details(detail_row_index integer, column_index integer, cell_value varchar)");
        execute(connection, "create table header_columns(column_index integer, header_name varchar, header_path varchar, path_segments varchar, is_blank boolean)");
        execute(connection, "create table subjects(sheet_name varchar, row_data_number integer, subject_code varchar, subject_name varchar, level integer, parent_code varchar, root_code varchar, segment_count integer, path_codes varchar, is_leaf boolean)");
        execute(connection, "create table subject_relations(subject_code varchar, subject_name varchar, parent_code varchar, parent_name varchar, level integer, root_code varchar, segment_count integer, is_leaf boolean, path_codes varchar)");
        execute(connection, "create table metrics(sheet_name varchar, row_data_number integer, metric_name varchar, metric_type varchar, value varchar, raw_values varchar)");
        execute(connection, "create table summary(file_name_original varchar, title varchar, sheet_name varchar, header_row_number integer, data_start_row_number integer, subject_count integer, root_subject_count integer, leaf_subject_count integer, non_leaf_subject_count integer, metric_count integer, metric_row_count integer, metric_data_count integer, max_level integer, duplicate_subject_codes varchar, level_distribution varchar, basic_info varchar)");
        execute(connection, "create table subject_tree_json(tree_json varchar)");
    }

    private void execute(DuckDBConnection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private void insertWorkbookInfo(DuckDBConnection connection, ParsedValuationData parsedValuationData) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("insert into workbook_info values (?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, parsedValuationData.getWorkbookPath());
            statement.setString(2, parsedValuationData.getSheetName());
            statement.setString(3, parsedValuationData.getFileNameOriginal());
            statement.setString(4, parsedValuationData.getTitle());
            statement.setInt(5, defaultInteger(parsedValuationData.getHeaderRowNumber()));
            statement.setInt(6, defaultInteger(parsedValuationData.getDataStartRowNumber()));
            statement.executeUpdate();
        }
    }

    private void insertBasicInfo(DuckDBConnection connection, ParsedValuationData parsedValuationData) throws SQLException {
        if (parsedValuationData.getBasicInfo() == null || parsedValuationData.getBasicInfo().isEmpty()) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement("insert into basic_info values (?, ?)")) {
            for (Map.Entry<String, String> entry : parsedValuationData.getBasicInfo().entrySet()) {
                statement.setString(1, entry.getKey());
                statement.setString(2, entry.getValue());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void insertHeaders(DuckDBConnection connection, ParsedValuationData parsedValuationData) throws SQLException {
        if (parsedValuationData.getHeaders() == null || parsedValuationData.getHeaders().isEmpty()) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement("insert into headers values (?, ?)")) {
            for (int index = 0; index < parsedValuationData.getHeaders().size(); index++) {
                statement.setInt(1, index + 1);
                statement.setString(2, parsedValuationData.getHeaders().get(index));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void insertHeaderDetails(DuckDBConnection connection, ParsedValuationData parsedValuationData) throws SQLException {
        if (parsedValuationData.getHeaderDetails() == null || parsedValuationData.getHeaderDetails().isEmpty()) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement("insert into header_details values (?, ?, ?)")) {
            for (int rowIndex = 0; rowIndex < parsedValuationData.getHeaderDetails().size(); rowIndex++) {
                List<String> detailRow = parsedValuationData.getHeaderDetails().get(rowIndex);
                if (detailRow == null) {
                    continue;
                }
                for (int columnIndex = 0; columnIndex < detailRow.size(); columnIndex++) {
                    String value = detailRow.get(columnIndex);
                    if (value == null || value.isBlank()) {
                        continue;
                    }
                    statement.setInt(1, rowIndex + 1);
                    statement.setInt(2, columnIndex + 1);
                    statement.setString(3, value);
                    statement.addBatch();
                }
            }
            statement.executeBatch();
        }
    }

    private void insertHeaderColumns(DuckDBConnection connection, ParsedValuationData parsedValuationData) throws SQLException {
        List<HeaderColumnMeta> headerColumns = resolveHeaderColumns(parsedValuationData);
        if (headerColumns == null || headerColumns.isEmpty()) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement("insert into header_columns values (?, ?, ?, ?, ?)")) {
            for (HeaderColumnMeta headerColumn : headerColumns) {
                statement.setInt(1, defaultInteger(headerColumn.getColumnIndex()) + 1);
                statement.setString(2, headerColumn.getHeaderName());
                statement.setString(3, headerColumn.getHeaderPath());
                statement.setString(4, stringify(headerColumn.getPathSegments()));
                statement.setBoolean(5, Boolean.TRUE.equals(headerColumn.getBlankColumn()));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void insertSubjects(DuckDBConnection connection, ParsedValuationData parsedValuationData) throws SQLException {
        if (parsedValuationData.getSubjects() == null || parsedValuationData.getSubjects().isEmpty()) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement("insert into subjects values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            for (SubjectRecord subject : parsedValuationData.getSubjects()) {
                statement.setString(1, subject.getSheetName());
                statement.setInt(2, defaultInteger(subject.getRowDataNumber()));
                statement.setString(3, subject.getSubjectCode());
                statement.setString(4, subject.getSubjectName());
                statement.setInt(5, defaultInteger(subject.getLevel()));
                statement.setString(6, subject.getParentCode());
                statement.setString(7, subject.getRootCode());
                statement.setInt(8, defaultInteger(subject.getSegmentCount()));
                statement.setString(9, stringify(subject.getPathCodes()));
                statement.setBoolean(10, Boolean.TRUE.equals(subject.getLeaf()));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void insertSubjectRelations(DuckDBConnection connection, List<SubjectRelation> subjectRelations) throws SQLException {
        if (subjectRelations == null || subjectRelations.isEmpty()) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement("insert into subject_relations values (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            for (SubjectRelation relation : subjectRelations) {
                statement.setString(1, relation.getSubjectCode());
                statement.setString(2, relation.getSubjectName());
                statement.setString(3, relation.getParentCode());
                statement.setString(4, relation.getParentName());
                statement.setInt(5, defaultInteger(relation.getLevel()));
                statement.setString(6, relation.getRootCode());
                statement.setInt(7, defaultInteger(relation.getSegmentCount()));
                statement.setBoolean(8, Boolean.TRUE.equals(relation.getLeaf()));
                statement.setString(9, stringify(relation.getPathCodes()));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void insertMetrics(DuckDBConnection connection, ParsedValuationData parsedValuationData) throws SQLException {
        if (parsedValuationData.getMetrics() == null || parsedValuationData.getMetrics().isEmpty()) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement("insert into metrics values (?, ?, ?, ?, ?, ?)")) {
            for (var metric : parsedValuationData.getMetrics()) {
                statement.setString(1, metric.getSheetName());
                statement.setInt(2, defaultInteger(metric.getRowDataNumber()));
                statement.setString(3, metric.getMetricName());
                statement.setString(4, metric.getMetricType());
                statement.setString(5, metric.getValue());
                statement.setString(6, stringify(metric.getRawValues()));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void insertSummary(DuckDBConnection connection, WorkbookSummary summary) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("insert into summary values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, summary.getFileNameOriginal());
            statement.setString(2, summary.getTitle());
            statement.setString(3, summary.getSheetName());
            statement.setInt(4, defaultInteger(summary.getHeaderRowNumber()));
            statement.setInt(5, defaultInteger(summary.getDataStartRowNumber()));
            statement.setInt(6, defaultInteger(summary.getSubjectCount()));
            statement.setInt(7, defaultInteger(summary.getRootSubjectCount()));
            statement.setInt(8, defaultInteger(summary.getLeafSubjectCount()));
            statement.setInt(9, defaultInteger(summary.getNonLeafSubjectCount()));
            statement.setInt(10, defaultInteger(summary.getMetricCount()));
            statement.setInt(11, defaultInteger(summary.getMetricRowCount()));
            statement.setInt(12, defaultInteger(summary.getMetricDataCount()));
            statement.setInt(13, defaultInteger(summary.getMaxLevel()));
            statement.setString(14, stringify(summary.getDuplicateSubjectCodes()));
            statement.setString(15, stringify(summary.getLevelDistribution()));
            statement.setString(16, stringify(summary.getBasicInfo()));
            statement.executeUpdate();
        }
    }

    private void insertSubjectTree(DuckDBConnection connection, List<SubjectTreeNode> subjectTree) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("insert into subject_tree_json values (?)")) {
            statement.setString(1, stringify(subjectTree));
            statement.executeUpdate();
        }
    }

    private void writeMatchDuckDb(
            Path outputPath,
            Map<String, Object> summary,
            List<Map<String, Object>> top1Rows,
            List<Map<String, Object>> candidateRows,
            List<Map<String, Object>> reviewQueueRows
    ) {
        try {
            Files.createDirectories(outputPath.getParent());
            Files.deleteIfExists(outputPath);
            try (DuckDBConnection connection = (DuckDBConnection) DriverManager.getConnection("jdbc:duckdb:" + outputPath)) {
                connection.setAutoCommit(false);
                createMatchDuckDbTables(connection);
                insertMatchSummary(connection, summary);
                insertMatchTop1(connection, top1Rows);
                insertMatchCandidates(connection, candidateRows);
                insertReviewQueue(connection, reviewQueueRows);
                connection.commit();
            }
        } catch (IOException | SQLException exception) {
            throw new IllegalStateException("Failed to write DuckDB file " + outputPath, exception);
        }
    }

    private void writeEvaluationDuckDb(
            Path outputPath,
            Map<String, Object> evaluationResult,
            Map<String, Object> weightSearchReport
    ) {
        try {
            Files.createDirectories(outputPath.getParent());
            Files.deleteIfExists(outputPath);
            try (DuckDBConnection connection = (DuckDBConnection) DriverManager.getConnection("jdbc:duckdb:" + outputPath)) {
                connection.setAutoCommit(false);
                createEvaluationDuckDbTables(connection);
                insertEvaluationSummary(connection, evaluationResult);
                insertWeightVersions(connection, evaluationResult);
                insertFailureClusters(connection, evaluationResult);
                insertFailureSamples(connection, evaluationResult);
                insertWeightSearchReport(connection, weightSearchReport);
                connection.commit();
            }
        } catch (IOException | SQLException exception) {
            throw new IllegalStateException("Failed to write DuckDB file " + outputPath, exception);
        }
    }

    private void createMatchDuckDbTables(DuckDBConnection connection) throws SQLException {
        execute(connection, "create table match_summary(workbook_path varchar, sheet_name varchar, subject_count integer, high_count integer, medium_count integer, low_count integer, needs_review_count integer, average_score decimal(38,12), summary_json varchar)");
        execute(connection, "create table match_top1(external_subject_code varchar, external_subject_name varchar, external_level integer, external_is_leaf boolean, anchor_subject_code varchar, anchor_subject_name varchar, anchor_level integer, anchor_path_text varchar, anchor_reason varchar, matched_standard_code varchar, matched_standard_name varchar, score decimal(38,12), score_name decimal(38,12), score_path decimal(38,12), score_keyword decimal(38,12), score_code decimal(38,12), score_history decimal(38,12), score_embedding decimal(38,12), confidence_level varchar, needs_review boolean, match_reason varchar, candidate_count integer)");
        execute(connection, "create table match_candidates(external_subject_code varchar, external_subject_name varchar, anchor_subject_code varchar, anchor_subject_name varchar, rank integer, standard_code varchar, standard_name varchar, score decimal(38,12), score_name decimal(38,12), score_path decimal(38,12), score_keyword decimal(38,12), score_code decimal(38,12), score_history decimal(38,12), score_embedding decimal(38,12), matched_by_history boolean, candidate_sources varchar, reasons varchar)");
        execute(connection, "create table review_queue(review_id varchar, batch_id varchar, external_subject_code varchar, external_subject_name varchar, anchor_subject_name varchar, matched_standard_code varchar, matched_standard_name varchar, confidence varchar, score decimal(38,12), top2_gap decimal(38,12), match_reason varchar, review_status varchar, review_comment varchar, final_standard_code varchar)");
    }

    private void createEvaluationDuckDbTables(DuckDBConnection connection) throws SQLException {
        execute(connection, "create table evaluation_summary(mapping_workbook varchar, standard_workbook varchar, split_mode varchar, train_sample_count integer, test_sample_count integer, tuning_sample_count integer, max_tuning_samples integer, max_test_samples integer, embedding_model varchar, embedding_weight decimal(38,12), embedding_strategy varchar, summary_json varchar)");
        execute(connection, "create table weight_versions(version_name varchar, selected boolean, name_weight decimal(38,12), path_weight decimal(38,12), keyword_weight decimal(38,12), code_weight decimal(38,12), history_weight decimal(38,12), embedding_weight decimal(38,12), top1_accuracy decimal(38,12), top3_recall decimal(38,12), top5_recall decimal(38,12), high_confidence_count integer, high_confidence_top1_accuracy decimal(38,12), low_confidence_ratio decimal(38,12), average_score decimal(38,12), metrics_json varchar)");
        execute(connection, "create table failure_clusters(cluster_type varchar, cluster_key varchar, rank_no integer, sample_count integer)");
        execute(connection, "create table failure_samples(org_name varchar, external_code varchar, external_name varchar, expected_standard_code varchar, expected_standard_name varchar, predicted_standard_code varchar, predicted_standard_name varchar, confidence varchar, score decimal(38,12))");
        execute(connection, "create table weight_search_report(evaluated_weight_count integer, selection_rule varchar, best_metrics_json varchar, report_json varchar)");
    }

    private void insertMatchSummary(DuckDBConnection connection, Map<String, Object> summary) throws SQLException {
        if (summary == null || summary.isEmpty()) {
            return;
        }
        Map<String, Object> distribution = summary.get("confidence_distribution") instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();
        try (PreparedStatement statement = connection.prepareStatement("insert into match_summary values (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, stringify(summary.get("workbook_path")));
            statement.setString(2, stringify(summary.get("sheet_name")));
            statement.setInt(3, integerValue(summary.get("subject_count")));
            statement.setInt(4, integerValue(distribution.get("HIGH")));
            statement.setInt(5, integerValue(distribution.get("MEDIUM")));
            statement.setInt(6, integerValue(distribution.get("LOW")));
            statement.setInt(7, integerValue(summary.get("needs_review_count")));
            statement.setBigDecimal(8, decimalValue(summary.get("average_score")));
            statement.setString(9, stringify(summary));
            statement.executeUpdate();
        }
    }

    private void insertMatchTop1(DuckDBConnection connection, List<Map<String, Object>> rows) throws SQLException {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement("insert into match_top1 values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            for (Map<String, Object> row : rows) {
                statement.setString(1, stringify(row.get("external_subject_code")));
                statement.setString(2, stringify(row.get("external_subject_name")));
                statement.setInt(3, integerValue(row.get("external_level")));
                statement.setBoolean(4, booleanValue(row.get("external_is_leaf")));
                statement.setString(5, stringify(row.get("anchor_subject_code")));
                statement.setString(6, stringify(row.get("anchor_subject_name")));
                statement.setInt(7, integerValue(row.get("anchor_level")));
                statement.setString(8, stringify(row.get("anchor_path_text")));
                statement.setString(9, stringify(row.get("anchor_reason")));
                statement.setString(10, stringify(row.get("matched_standard_code")));
                statement.setString(11, stringify(row.get("matched_standard_name")));
                statement.setBigDecimal(12, decimalValue(row.get("score")));
                statement.setBigDecimal(13, decimalValue(row.get("score_name")));
                statement.setBigDecimal(14, decimalValue(row.get("score_path")));
                statement.setBigDecimal(15, decimalValue(row.get("score_keyword")));
                statement.setBigDecimal(16, decimalValue(row.get("score_code")));
                statement.setBigDecimal(17, decimalValue(row.get("score_history")));
                statement.setBigDecimal(18, decimalValue(row.get("score_embedding")));
                statement.setString(19, stringify(row.get("confidence_level")));
                statement.setBoolean(20, booleanValue(row.get("needs_review")));
                statement.setString(21, stringify(row.get("match_reason")));
                statement.setInt(22, integerValue(row.get("candidate_count")));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void insertMatchCandidates(DuckDBConnection connection, List<Map<String, Object>> rows) throws SQLException {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement("insert into match_candidates values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            for (Map<String, Object> row : rows) {
                statement.setString(1, stringify(row.get("external_subject_code")));
                statement.setString(2, stringify(row.get("external_subject_name")));
                statement.setString(3, stringify(row.get("anchor_subject_code")));
                statement.setString(4, stringify(row.get("anchor_subject_name")));
                statement.setInt(5, integerValue(row.get("rank")));
                statement.setString(6, stringify(row.get("standard_code")));
                statement.setString(7, stringify(row.get("standard_name")));
                statement.setBigDecimal(8, decimalValue(row.get("score")));
                statement.setBigDecimal(9, decimalValue(row.get("score_name")));
                statement.setBigDecimal(10, decimalValue(row.get("score_path")));
                statement.setBigDecimal(11, decimalValue(row.get("score_keyword")));
                statement.setBigDecimal(12, decimalValue(row.get("score_code")));
                statement.setBigDecimal(13, decimalValue(row.get("score_history")));
                statement.setBigDecimal(14, decimalValue(row.get("score_embedding")));
                statement.setBoolean(15, booleanValue(row.get("matched_by_history")));
                statement.setString(16, stringify(row.get("candidate_sources")));
                statement.setString(17, stringify(row.get("reasons")));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void insertReviewQueue(DuckDBConnection connection, List<Map<String, Object>> rows) throws SQLException {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement("insert into review_queue values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            for (Map<String, Object> row : rows) {
                statement.setString(1, stringify(row.get("review_id")));
                statement.setString(2, stringify(row.get("batch_id")));
                statement.setString(3, stringify(row.get("external_subject_code")));
                statement.setString(4, stringify(row.get("external_subject_name")));
                statement.setString(5, stringify(row.get("anchor_subject_name")));
                statement.setString(6, stringify(row.get("matched_standard_code")));
                statement.setString(7, stringify(row.get("matched_standard_name")));
                statement.setString(8, stringify(row.get("confidence")));
                statement.setBigDecimal(9, decimalValue(row.get("score")));
                statement.setBigDecimal(10, decimalValue(row.get("top2_gap")));
                statement.setString(11, stringify(row.get("match_reason")));
                statement.setString(12, stringify(row.get("review_status")));
                statement.setString(13, stringify(row.get("review_comment")));
                statement.setString(14, stringify(row.get("final_standard_code")));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void insertEvaluationSummary(DuckDBConnection connection, Map<String, Object> evaluationResult) throws SQLException {
        if (evaluationResult == null || evaluationResult.isEmpty()) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement("insert into evaluation_summary values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, stringify(evaluationResult.get("mapping_workbook")));
            statement.setString(2, stringify(evaluationResult.get("standard_workbook")));
            statement.setString(3, stringify(evaluationResult.get("split_mode")));
            statement.setInt(4, integerValue(evaluationResult.get("train_sample_count")));
            statement.setInt(5, integerValue(evaluationResult.get("test_sample_count")));
            statement.setInt(6, integerValue(evaluationResult.get("tuning_sample_count")));
            statement.setInt(7, integerValue(evaluationResult.get("max_tuning_samples")));
            statement.setInt(8, integerValue(evaluationResult.get("max_test_samples")));
            statement.setString(9, stringify(evaluationResult.get("embedding_model")));
            statement.setBigDecimal(10, decimalValue(evaluationResult.get("embedding_weight")));
            statement.setString(11, stringify(evaluationResult.get("embedding_strategy")));
            statement.setString(12, stringify(evaluationResult));
            statement.executeUpdate();
        }
    }

    private void insertWeightVersions(DuckDBConnection connection, Map<String, Object> evaluationResult) throws SQLException {
        if (evaluationResult == null || evaluationResult.isEmpty()) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement("insert into weight_versions values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            insertWeightVersion(statement,
                    "baseline",
                    false,
                    mapValue(evaluationResult.get("baseline_weights")),
                    mapValue(evaluationResult.get("baseline_metrics")));
            insertWeightVersion(statement,
                    "recommended",
                    true,
                    mapValue(evaluationResult.get("recommended_weights")),
                    mapValue(evaluationResult.get("recommended_metrics")));
            statement.executeBatch();
        }
    }

    private void insertWeightVersion(
            PreparedStatement statement,
            String versionName,
            boolean selected,
            Map<String, Object> weights,
            Map<String, Object> metrics
    ) throws SQLException {
        statement.setString(1, versionName);
        statement.setBoolean(2, selected);
        statement.setBigDecimal(3, decimalValue(weights.get("nameWeight")));
        statement.setBigDecimal(4, decimalValue(weights.get("pathWeight")));
        statement.setBigDecimal(5, decimalValue(weights.get("keywordWeight")));
        statement.setBigDecimal(6, decimalValue(weights.get("codeWeight")));
        statement.setBigDecimal(7, decimalValue(weights.get("historyWeight")));
        statement.setBigDecimal(8, decimalValue(weights.get("embeddingWeight")));
        statement.setBigDecimal(9, decimalValue(metrics.get("top1_accuracy")));
        statement.setBigDecimal(10, decimalValue(metrics.get("top3_recall")));
        statement.setBigDecimal(11, decimalValue(metrics.get("top5_recall")));
        statement.setInt(12, integerValue(metrics.get("high_confidence_count")));
        statement.setBigDecimal(13, decimalValue(metrics.get("high_confidence_top1_accuracy")));
        statement.setBigDecimal(14, decimalValue(metrics.get("low_confidence_ratio")));
        statement.setBigDecimal(15, decimalValue(metrics.get("average_score")));
        statement.setString(16, stringify(metrics));
        statement.addBatch();
    }

    private void insertFailureClusters(DuckDBConnection connection, Map<String, Object> evaluationResult) throws SQLException {
        Map<String, Object> failureAnalysis = mapValue(evaluationResult == null ? null : evaluationResult.get("failure_analysis"));
        if (failureAnalysis.isEmpty()) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement("insert into failure_clusters values (?, ?, ?, ?)")) {
            insertClusterRows(statement, "expected_root", listValue(failureAnalysis.get("top_expected_roots")));
            insertClusterRows(statement, "predicted_root", listValue(failureAnalysis.get("top_predicted_roots")));
            insertClusterRows(statement, "root_confusion", listValue(failureAnalysis.get("top_root_confusions")));
            insertClusterRows(statement, "org", listValue(failureAnalysis.get("top_orgs")));
            insertClusterRows(statement, "external_keyword", listValue(failureAnalysis.get("top_external_keywords")));
            Map<String, Object> confidenceDistribution = mapValue(failureAnalysis.get("confidence_distribution"));
            int rank = 1;
            for (Map.Entry<String, Object> entry : confidenceDistribution.entrySet()) {
                statement.setString(1, "confidence");
                statement.setString(2, entry.getKey());
                statement.setInt(3, rank++);
                statement.setInt(4, integerValue(entry.getValue()));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void insertClusterRows(PreparedStatement statement, String clusterType, List<Object> rows) throws SQLException {
        int rank = 1;
        for (Object rowObject : rows) {
            if (!(rowObject instanceof List<?> row) || row.isEmpty()) {
                continue;
            }
            statement.setString(1, clusterType);
            statement.setString(2, stringify(row.get(0)));
            statement.setInt(3, rank++);
            statement.setInt(4, row.size() > 1 ? integerValue(row.get(1)) : 0);
            statement.addBatch();
        }
    }

    private void insertFailureSamples(DuckDBConnection connection, Map<String, Object> evaluationResult) throws SQLException {
        Map<String, Object> metrics = mapValue(evaluationResult == null ? null : evaluationResult.get("recommended_metrics"));
        List<Object> failureSamples = listValue(metrics.get("failure_samples"));
        if (failureSamples.isEmpty()) {
            return;
        }
        try (PreparedStatement statement = connection.prepareStatement("insert into failure_samples values (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            for (Object sampleObject : failureSamples) {
                Map<String, Object> sample = mapValue(sampleObject);
                statement.setString(1, stringify(sample.get("org_name")));
                statement.setString(2, stringify(sample.get("external_code")));
                statement.setString(3, stringify(sample.get("external_name")));
                statement.setString(4, stringify(sample.get("expected_standard_code")));
                statement.setString(5, stringify(sample.get("expected_standard_name")));
                statement.setString(6, stringify(sample.get("predicted_standard_code")));
                statement.setString(7, stringify(sample.get("predicted_standard_name")));
                statement.setString(8, stringify(sample.get("confidence")));
                statement.setBigDecimal(9, decimalValue(sample.get("score")));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void insertWeightSearchReport(DuckDBConnection connection, Map<String, Object> weightSearchReport) throws SQLException {
        if (weightSearchReport == null || weightSearchReport.isEmpty()) {
            return;
        }
        Map<String, Object> search = mapValue(weightSearchReport.get("weight_search"));
        try (PreparedStatement statement = connection.prepareStatement("insert into weight_search_report values (?, ?, ?, ?)")) {
            statement.setInt(1, integerValue(search.get("evaluated_weight_count")));
            statement.setString(2, stringify(search.get("selection_rule")));
            statement.setString(3, stringify(search.get("best_metrics")));
            statement.setString(4, stringify(weightSearchReport));
            statement.executeUpdate();
        }
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
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? 0 : Integer.parseInt(text);
    }

    private boolean booleanValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private BigDecimal decimalValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return jdbcDecimal(decimal);
        }
        if (value instanceof Number number) {
            return jdbcDecimal(BigDecimal.valueOf(number.doubleValue()));
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        return jdbcDecimal(new BigDecimal(text));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private List<Object> listValue(Object value) {
        if (value instanceof List<?> list) {
            return (List<Object>) list;
        }
        return List.of();
    }

    private int reviewPriority(SubjectMatchResult result) {
        if (result == null || result.getConfidenceLevel() == null) {
            return 99;
        }
        return switch (result.getConfidenceLevel()) {
            case LOW -> 0;
            case MEDIUM -> 1;
            case HIGH -> 2;
        };
    }

    private BigDecimal top2Gap(SubjectMatchResult result) {
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
