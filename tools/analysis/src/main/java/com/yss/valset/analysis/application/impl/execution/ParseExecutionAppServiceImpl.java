package com.yss.valset.analysis.application.impl.execution;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.application.command.ParseTaskCommand;
import com.yss.valset.analysis.application.port.ParseExecutionUseCase;
import com.yss.valset.domain.gateway.DwdExternalValuationGateway;
import com.yss.valset.domain.gateway.DwdJjhzgzbGateway;
import com.yss.valset.domain.gateway.StandardizedExternalValuationGateway;
import com.yss.valset.domain.gateway.TaskGateway;
import com.yss.valset.domain.gateway.TrIndexGateway;
import com.yss.valset.domain.gateway.ValsetFileInfoGateway;
import com.yss.valset.domain.model.DataSourceConfig;
import com.yss.valset.domain.model.DataSourceType;
import com.yss.valset.domain.model.ParsedValuationData;
import com.yss.valset.domain.model.TaskInfo;
import com.yss.valset.domain.model.ValsetFileInfo;
import com.yss.valset.domain.parser.ValuationDataParser;
import com.yss.valset.domain.parser.ValuationDataParserProvider;
import com.yss.valset.domain.rule.ParseRuleTraceContext;
import com.yss.valset.domain.rule.ParseRuleTraceContextHolder;
import com.yss.valset.extract.standardization.ExternalValuationStandardizationService;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

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
    private final ValsetFileInfoGateway subjectMatchFileInfoGateway;
    private final ExternalValuationStandardizationService standardizationService;
    private final ObjectMapper objectMapper;
    private final Tracer tracer;

    public ParseExecutionAppServiceImpl(
            TaskGateway taskGateway,
            ValuationDataParserProvider parserProvider,
            DwdExternalValuationGateway dwdExternalValuationGateway,
            StandardizedExternalValuationGateway standardizedExternalValuationGateway,
            DwdJjhzgzbGateway dwdJjhzgzbGateway,
            TrIndexGateway trIndexGateway,
            ValsetFileInfoGateway subjectMatchFileInfoGateway,
            ExternalValuationStandardizationService standardizationService,
            ObjectMapper objectMapper,
            Tracer tracer
    ) {
        this.taskGateway = taskGateway;
        this.parserProvider = parserProvider;
        this.dwdExternalValuationGateway = dwdExternalValuationGateway;
        this.standardizedExternalValuationGateway = standardizedExternalValuationGateway;
        this.dwdJjhzgzbGateway = dwdJjhzgzbGateway;
        this.trIndexGateway = trIndexGateway;
        this.subjectMatchFileInfoGateway = subjectMatchFileInfoGateway;
        this.standardizationService = standardizationService;
        this.objectMapper = objectMapper;
        this.tracer = tracer;
    }

    /**
     * 执行任务 ID 的解析工作流程。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void execute(Long taskId) {
        Span rootSpan = tracer.nextSpan().name("workflow.parse.execute").tag("task.id", String.valueOf(taskId)).start();
        try (Tracer.SpanInScope ws = tracer.withSpan(rootSpan)) {
            TaskInfo taskInfo = taskGateway.findById(taskId);
            ParseRuleTraceContext traceContext = ParseRuleTraceContext.builder()
                    .profileId(null)
                    .profileCode("runtime")
                    .version("runtime")
                    .fileId(taskInfo == null ? null : taskInfo.getFileId())
                    .taskId(taskId)
                    .traceEnabled(Boolean.FALSE)
                    .traceScope("RUNTIME_PARSE")
                    .build();
            try (ParseRuleTraceContextHolder.TraceScope ignored = ParseRuleTraceContextHolder.withContext(traceContext)) {
                try {
                    long startedAt = System.currentTimeMillis();
                    log.info("开始执行估值数据解析任务，taskId={}", taskId);
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

                    long parseStartedAt = System.currentTimeMillis();
                    ParsedValuationData parsedValuationData = traceSpan("workflow.parse.raw_parse", () -> parser.parse(config));
                    long parseFinishedAt = System.currentTimeMillis();
                    String fileNameOriginal = resolveFileNameOriginal(taskInfo);
                    parsedValuationData = parsedValuationData.toBuilder()
                            .fileNameOriginal(fileNameOriginal)
                            .build();
                    final ParsedValuationData parsedValuationDataFinal = parsedValuationData;

                    traceSpan("workflow.parse.persist_raw_dwd", () ->
                            dwdExternalValuationGateway.saveDwdExternalValuation(taskId, taskInfo.getFileId(), parsedValuationDataFinal));

                    long standardizeStartedAt = System.currentTimeMillis();
                    ParsedValuationData standardizedValuationData = traceSpan("workflow.parse.standardize",
                            () -> standardizationService.standardize(parsedValuationDataFinal));
                    long standardizeFinishedAt = System.currentTimeMillis();
                    standardizedValuationData = standardizedValuationData == null ? null : standardizedValuationData.toBuilder()
                            .fileNameOriginal(fileNameOriginal)
                            .build();

                    String sourceSign = fileNameOriginal;
                    String sourceTypeName = type.name();
                    ParsedValuationData finalStandardizedValuationData = standardizedValuationData;
                    traceSpan("workflow.parse.persist_standardized", () -> {
                        standardizedExternalValuationGateway.saveStandardizedExternalValuation(taskId, taskInfo.getFileId(), finalStandardizedValuationData);
                        dwdJjhzgzbGateway.saveStandardizedJjhzgzb(taskId, taskInfo.getFileId(), sourceTypeName, sourceSign, finalStandardizedValuationData);
                        trIndexGateway.saveStandardizedIndex(taskId, taskInfo.getFileId(), sourceTypeName, sourceSign, finalStandardizedValuationData);
                    });
                    long persistFinishedAt = System.currentTimeMillis();

                    long standardizeDurationMs = standardizeFinishedAt - standardizeStartedAt;
                    taskGateway.updateTaskTimings(taskId, null, standardizeDurationMs, null);
                    String resultPayload = buildResultPayload(parsedValuationDataFinal);
                    taskGateway.markSuccess(taskId, resultPayload);
                    log.info("估值数据解析任务执行完成，taskId={}, subjectCount={}, metricCount={}",
                            taskId,
                            parsedValuationDataFinal.getSubjects() == null ? 0 : parsedValuationDataFinal.getSubjects().size(),
                            parsedValuationDataFinal.getMetrics() == null ? 0 : parsedValuationDataFinal.getMetrics().size());
                    log.info("解析流程耗时统计，taskId={}, totalMs={}, parseMs={}, standardizeMs={}, persistMs={}",
                            taskId,
                            System.currentTimeMillis() - startedAt,
                            parseFinishedAt - parseStartedAt,
                            standardizeFinishedAt - standardizeStartedAt,
                            persistFinishedAt - standardizeFinishedAt);
                } catch (Exception e) {
                    rootSpan.error(e);
                    log.error("执行估值数据解析任务失败，taskId={}", taskId, e);
                    throw new IllegalStateException("Failed to execute parse task " + taskId, e);
                }
            }
        } finally {
            rootSpan.end();
        }
    }

    private <T> T traceSpan(String spanName, Supplier<T> supplier) {
        Span span = tracer.nextSpan().name(spanName).start();
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            return supplier.get();
        } catch (RuntimeException exception) {
            span.error(exception);
            throw exception;
        } finally {
            span.end();
        }
    }

    private void traceSpan(String spanName, Runnable runnable) {
        Span span = tracer.nextSpan().name(spanName).start();
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            runnable.run();
        } catch (RuntimeException exception) {
            span.error(exception);
            throw exception;
        } finally {
            span.end();
        }
    }

    private String resolveFileNameOriginal(TaskInfo taskInfo) {
        ValsetFileInfo fileInfo = taskInfo == null || taskInfo.getFileId() == null
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
    private String buildResultPayload(ParsedValuationData parsedValuationData) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("workbookPath", parsedValuationData.getWorkbookPath());
            payload.put("sheetName", parsedValuationData.getSheetName());
            payload.put("fileNameOriginal", parsedValuationData.getFileNameOriginal());
            payload.put("subjectCount", parsedValuationData.getSubjects() == null ? 0 : parsedValuationData.getSubjects().size());
            payload.put("metricCount", parsedValuationData.getMetrics() == null ? 0 : parsedValuationData.getMetrics().size());
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            return "Parsed workbook: " + parsedValuationData.getWorkbookPath();
        }
    }
}
