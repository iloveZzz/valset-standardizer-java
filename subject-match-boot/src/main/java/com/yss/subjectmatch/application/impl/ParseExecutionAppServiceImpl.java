package com.yss.subjectmatch.application.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.subjectmatch.application.command.ParseTaskCommand;
import com.yss.subjectmatch.application.port.ParseExecutionUseCase;
import com.yss.subjectmatch.domain.exporter.ResultExporter;
import com.yss.subjectmatch.domain.gateway.DwdExternalValuationGateway;
import com.yss.subjectmatch.domain.gateway.DwdJjhzgzbGateway;
import com.yss.subjectmatch.domain.gateway.TrIndexGateway;
import com.yss.subjectmatch.domain.gateway.StandardizedExternalValuationGateway;
import com.yss.subjectmatch.domain.gateway.SubjectMatchFileInfoGateway;
import com.yss.subjectmatch.domain.gateway.TaskGateway;
import com.yss.subjectmatch.domain.model.ParsedValuationData;
import com.yss.subjectmatch.domain.model.SubjectMatchFileInfo;
import com.yss.subjectmatch.domain.model.TaskInfo;
import com.yss.subjectmatch.domain.parser.ValuationDataParser;
import com.yss.subjectmatch.domain.parser.ValuationDataParserProvider;
import com.yss.subjectmatch.domain.model.DataSourceConfig;
import com.yss.subjectmatch.domain.model.DataSourceType;
import com.yss.subjectmatch.extract.standardization.ExternalValuationStandardizationService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 解析工作流程实现。
 */
@Slf4j
@Service
public class ParseExecutionAppServiceImpl implements ParseExecutionUseCase {

    private final TaskGateway taskGateway;
    private final ValuationDataParserProvider parserProvider;
    private final DwdExternalValuationGateway dwdExternalValuationGateway;
    private final StandardizedExternalValuationGateway standardizedExternalValuationGateway;
    private final DwdJjhzgzbGateway dwdJjhzgzbGateway;
    private final TrIndexGateway trIndexGateway;
    private final SubjectMatchFileInfoGateway subjectMatchFileInfoGateway;
    private final ExternalValuationStandardizationService standardizationService;
    private final ResultExporter resultExporter;
    private final ObjectMapper objectMapper;
    private final String outputRoot;

    public ParseExecutionAppServiceImpl(
            TaskGateway taskGateway,
            ValuationDataParserProvider parserProvider,
            DwdExternalValuationGateway dwdExternalValuationGateway,
            StandardizedExternalValuationGateway standardizedExternalValuationGateway,
            DwdJjhzgzbGateway dwdJjhzgzbGateway,
            TrIndexGateway trIndexGateway,
            SubjectMatchFileInfoGateway subjectMatchFileInfoGateway,
            ExternalValuationStandardizationService standardizationService,
            ResultExporter resultExporter,
            ObjectMapper objectMapper,
            @Value("${subject.match.output-dir:output}") String outputRoot
    ) {
        this.taskGateway = taskGateway;
        this.parserProvider = parserProvider;
        this.dwdExternalValuationGateway = dwdExternalValuationGateway;
        this.standardizedExternalValuationGateway = standardizedExternalValuationGateway;
        this.dwdJjhzgzbGateway = dwdJjhzgzbGateway;
        this.trIndexGateway = trIndexGateway;
        this.subjectMatchFileInfoGateway = subjectMatchFileInfoGateway;
        this.standardizationService = standardizationService;
        this.resultExporter = resultExporter;
        this.objectMapper = objectMapper;
        this.outputRoot = outputRoot;
    }

    /**
     * 执行任务 ID 的解析工作流程。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void execute(Long taskId) {
        try {
            log.info("开始执行估值数据解析任务，taskId={}", taskId);
            TaskInfo taskInfo = taskGateway.findById(taskId);
            ParseTaskCommand command = objectMapper.readValue(taskInfo.getInputPayload(), ParseTaskCommand.class);

            String sourceTypeStr = command.getDataSourceType();
            DataSourceType type = DataSourceType.EXCEL;
            if (sourceTypeStr != null && !sourceTypeStr.isBlank()) {
                type = DataSourceType.valueOf(sourceTypeStr.toUpperCase());
            }

            DataSourceConfig config = buildAnalysisConfig(type, command.getWorkbookPath(), command.getFileId());

            ValuationDataParser parser = parserProvider.getParser(type);
            log.debug("解析任务 {} 使用分析器 {}，sourceType={}, sourceUri={}, fileId={}",
                    taskId,
                    parser.getClass().getSimpleName(),
                    type,
                    command.getWorkbookPath(),
                    command.getFileId());

            long standardizeStartMs = System.currentTimeMillis();
            ParsedValuationData parsedValuationData = parser.parse(config);
            String fileNameOriginal = resolveFileNameOriginal(taskInfo);
            parsedValuationData = parsedValuationData.toBuilder()
                    .fileNameOriginal(fileNameOriginal)
                    .build();
            dwdExternalValuationGateway.saveDwdExternalValuation(taskId, taskInfo.getFileId(), parsedValuationData);
            ParsedValuationData standardizedValuationData = standardizationService.standardize(parsedValuationData);
            standardizedValuationData = standardizedValuationData == null ? null : standardizedValuationData.toBuilder()
                    .fileNameOriginal(fileNameOriginal)
                    .build();
            standardizedExternalValuationGateway.saveStandardizedExternalValuation(taskId, taskInfo.getFileId(), standardizedValuationData);
            String sourceSign = fileNameOriginal;
            dwdJjhzgzbGateway.saveStandardizedJjhzgzb(taskId, taskInfo.getFileId(), type.name(), sourceSign, standardizedValuationData);
            trIndexGateway.saveStandardizedIndex(taskId, taskInfo.getFileId(), type.name(), sourceSign, standardizedValuationData);
            long standardizeDurationMs = System.currentTimeMillis() - standardizeStartMs;

            resultExporter.exportParsedValuationData(taskId, parsedValuationData);
            taskGateway.updateTaskTimings(taskId, null, standardizeDurationMs, null);
            String resultPayload = buildResultPayload(taskId, parsedValuationData);
            taskGateway.markSuccess(taskId, resultPayload);
            log.info("估值数据解析任务执行完成，taskId={}, subjectCount={}, metricCount={}",
                    taskId,
                    parsedValuationData.getSubjects() == null ? 0 : parsedValuationData.getSubjects().size(),
                    parsedValuationData.getMetrics() == null ? 0 : parsedValuationData.getMetrics().size());
        } catch (Exception e) {
            log.error("执行估值数据解析任务失败，taskId={}", taskId, e);
            throw new IllegalStateException("Failed to execute parse task " + taskId, e);
        }
    }

    private String resolveFileNameOriginal(TaskInfo taskInfo) {
        SubjectMatchFileInfo fileInfo = taskInfo == null || taskInfo.getFileId() == null
                ? null
                : subjectMatchFileInfoGateway.findById(taskInfo.getFileId());
        return fileInfo == null ? null : fileInfo.getFileNameOriginal();
    }

    private String firstNonBlank(String... candidates) {
        if (candidates == null) {
            return null;
        }
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate.trim();
            }
        }
        return null;
    }

    private DataSourceConfig buildAnalysisConfig(DataSourceType type, String sourceUri, Long fileId) {
        if ((type == DataSourceType.EXCEL || type == DataSourceType.CSV) && fileId == null) {
            throw new IllegalStateException("Excel/CSV 解析需要先完成原始数据提取，并传入 fileId");
        }
        return DataSourceConfig.builder()
                .sourceType(type)
                .sourceUri(sourceUri)
                .additionalParams(fileId == null ? null : String.valueOf(fileId))
                .build();
    }

    /**
     * 为解析任务构建结构化结果有效负载。
     */
    private String buildResultPayload(Long taskId, ParsedValuationData parsedValuationData) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("workbookPath", parsedValuationData.getWorkbookPath());
            payload.put("sheetName", parsedValuationData.getSheetName());
            payload.put("fileNameOriginal", parsedValuationData.getFileNameOriginal());
            payload.put("subjectCount", parsedValuationData.getSubjects() == null ? 0 : parsedValuationData.getSubjects().size());
            payload.put("metricCount", parsedValuationData.getMetrics() == null ? 0 : parsedValuationData.getMetrics().size());
            payload.put("outputDir", resolveTaskOutputDirectory(taskId).toString());
            payload.put("artifacts", List.of(
                    "parsed.json",
                    "subjects.csv",
                    "subject_relations.csv",
                    "subject_tree.json",
                    "metrics.csv",
                    "summary.json",
                    "parsed.duckdb"
            ));
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            return "Parsed workbook: " + parsedValuationData.getWorkbookPath();
        }
    }

    /**
     * 解析任务输出目录。
     */
    private Path resolveTaskOutputDirectory(Long taskId) {
        return Path.of(outputRoot).toAbsolutePath().resolve("task-" + taskId);
    }
}
