package com.yss.valset.analysis.application.impl.execution;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.application.command.ParseTaskCommand;
import com.yss.valset.application.event.lifecycle.ParseLifecycleEvent;
import com.yss.valset.application.event.lifecycle.ParseLifecycleEventPublisher;
import com.yss.valset.application.event.lifecycle.ParseLifecycleStage;
import com.yss.valset.analysis.application.port.ParseExecutionUseCase;
import com.yss.valset.domain.gateway.DwdExternalValuationGateway;
import com.yss.valset.domain.gateway.DwdJjhzgzbGateway;
import com.yss.valset.domain.gateway.StandardizedExternalValuationGateway;
import com.yss.valset.domain.gateway.WorkflowTaskGateway;
import com.yss.valset.domain.gateway.TrIndexGateway;
import com.yss.valset.domain.gateway.ValsetFileInfoGateway;
import com.yss.valset.domain.model.DataSourceConfig;
import com.yss.valset.domain.model.DataSourceType;
import com.yss.valset.domain.model.ParsedValuationData;
import com.yss.valset.domain.model.WorkflowTask;
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
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

/**
 * 解析工作流程实现。
 */
@Slf4j
@Service
public class ParseExecutionAppServiceImpl implements ParseExecutionUseCase {

    private final WorkflowTaskGateway taskGateway;
    private final ValuationDataParserProvider parserProvider;
    private final DwdExternalValuationGateway dwdExternalValuationGateway;
    private final StandardizedExternalValuationGateway standardizedExternalValuationGateway;
    private final DwdJjhzgzbGateway dwdJjhzgzbGateway;
    private final TrIndexGateway trIndexGateway;
    private final ValsetFileInfoGateway subjectMatchFileInfoGateway;
    private final ExternalValuationStandardizationService standardizationService;
    private final ObjectMapper objectMapper;
    private final Tracer tracer;
    private final ParseLifecycleEventPublisher parseLifecycleEventPublisher;

    public ParseExecutionAppServiceImpl(
            WorkflowTaskGateway taskGateway,
            ValuationDataParserProvider parserProvider,
            DwdExternalValuationGateway dwdExternalValuationGateway,
            StandardizedExternalValuationGateway standardizedExternalValuationGateway,
            DwdJjhzgzbGateway dwdJjhzgzbGateway,
            TrIndexGateway trIndexGateway,
            ValsetFileInfoGateway subjectMatchFileInfoGateway,
            ExternalValuationStandardizationService standardizationService,
            ObjectMapper objectMapper,
            Tracer tracer,
            ParseLifecycleEventPublisher parseLifecycleEventPublisher
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
        this.parseLifecycleEventPublisher = parseLifecycleEventPublisher;
    }

    /**
     * 执行任务 ID 的解析工作流程。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void execute(Long taskId) {
        Span rootSpan = tracer.nextSpan().name("workflow.parse.execute").tag("task.id", String.valueOf(taskId)).start();
        try (Tracer.SpanInScope ws = tracer.withSpan(rootSpan)) {
            WorkflowTask workflowTask = taskGateway.findById(taskId);
            ParseRuleTraceContext traceContext = ParseRuleTraceContext.builder()
                    .profileId(null)
                    .profileCode("runtime")
                    .version("runtime")
                    .fileId(workflowTask == null ? null : workflowTask.getFileId())
                    .taskId(taskId)
                    .traceEnabled(Boolean.FALSE)
                    .traceScope("RUNTIME_PARSE")
                    .build();
            try (ParseRuleTraceContextHolder.TraceScope ignored = ParseRuleTraceContextHolder.withContext(traceContext)) {
                ParseTaskCommand command = null;
                try {
                    long startedAt = System.currentTimeMillis();
                    log.info("开始执行估值数据解析任务，taskId={}", taskId);
                    command = objectMapper.readValue(workflowTask.getInputPayload(), ParseTaskCommand.class);
                    publishLifecycleEvent(ParseLifecycleStage.TASK_EXECUTION_STARTED, taskId, command, "开始执行解析任务");

                    String sourceTypeStr = command.getDataSourceType();
                    DataSourceType type = DataSourceType.EXCEL;
                    if (sourceTypeStr != null && !sourceTypeStr.isBlank()) {
                        type = DataSourceType.valueOf(sourceTypeStr.toUpperCase());
                    }

                    DataSourceConfig config = buildAnalysisConfig(type, resolveAnalysisWorkbookPath(command), command.getFileId());

                    ValuationDataParser parser = parserProvider.getParser(type);
                    log.debug("解析任务 {} 使用分析器 {}，sourceType={}, sourceUri={}, fileId={}",
                            taskId,
                            parser.getClass().getSimpleName(),
                            type,
                            config.getSourceUri(),
                            command.getFileId());

                    long parseStartedAt = System.currentTimeMillis();
                    ParsedValuationData parsedValuationData = traceSpan("workflow.parse.raw_parse", () -> parser.parse(config));
                    long parseFinishedAt = System.currentTimeMillis();
                    publishLifecycleEvent(ParseLifecycleStage.TASK_RAW_PARSED, taskId, command, "原始数据解析完成");
                    validateParsedValuationData(parsedValuationData, command);
                    String fileNameOriginal = resolveFileNameOriginal(workflowTask);
                    parsedValuationData = parsedValuationData.toBuilder()
                            .fileNameOriginal(fileNameOriginal)
                            .build();
                    final ParsedValuationData parsedValuationDataFinal = parsedValuationData;

                    traceSpan("workflow.parse.persist_raw_dwd", () ->
                            dwdExternalValuationGateway.saveDwdExternalValuation(taskId, workflowTask.getFileId(), parsedValuationDataFinal));

                    long standardizeStartedAt = System.currentTimeMillis();
                    ParsedValuationData standardizedValuationData = traceSpan("workflow.parse.standardize",
                            () -> standardizationService.standardize(parsedValuationDataFinal));
                    long standardizeFinishedAt = System.currentTimeMillis();
                    publishLifecycleEvent(ParseLifecycleStage.TASK_STANDARDIZED, taskId, command, "标准化完成");
                    standardizedValuationData = standardizedValuationData == null ? null : standardizedValuationData.toBuilder()
                            .fileNameOriginal(fileNameOriginal)
                            .build();

                    String sourceSign = fileNameOriginal;
                    String sourceTypeName = type.name();
                    ParsedValuationData finalStandardizedValuationData = standardizedValuationData;
                    traceSpan("workflow.parse.persist_standardized", () -> {
                        standardizedExternalValuationGateway.saveStandardizedExternalValuation(taskId, workflowTask.getFileId(), finalStandardizedValuationData);
                        dwdJjhzgzbGateway.saveStandardizedJjhzgzb(taskId, workflowTask.getFileId(), sourceTypeName, sourceSign, finalStandardizedValuationData);
                        trIndexGateway.saveStandardizedIndex(taskId, workflowTask.getFileId(), sourceTypeName, sourceSign, finalStandardizedValuationData);
                    });
                    long persistFinishedAt = System.currentTimeMillis();
                    publishLifecycleEvent(ParseLifecycleStage.TASK_PERSISTED, taskId, command, "标准化结果已落库");

                    long standardizeDurationMs = standardizeFinishedAt - standardizeStartedAt;
                    taskGateway.updateTaskTimings(taskId, null, standardizeDurationMs, null);
                    String resultPayload = buildResultPayload(parsedValuationDataFinal);
                    taskGateway.markSuccess(taskId, resultPayload);
                    publishLifecycleEvent(ParseLifecycleStage.TASK_SUCCEEDED, taskId, command, "解析任务执行成功", Map.of(
                            "subjectCount", parsedValuationDataFinal.getSubjects() == null ? 0 : parsedValuationDataFinal.getSubjects().size(),
                            "metricCount", parsedValuationDataFinal.getMetrics() == null ? 0 : parsedValuationDataFinal.getMetrics().size()
                    ));
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
                    Map<String, Object> failedAttributes = new LinkedHashMap<>();
                    if (e.getMessage() != null) {
                        failedAttributes.put("errorMessage", e.getMessage());
                    }
                    publishLifecycleEvent(ParseLifecycleStage.TASK_FAILED, taskId, command, "解析任务执行失败", failedAttributes);
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

    private String resolveFileNameOriginal(WorkflowTask workflowTask) {
        ValsetFileInfo fileInfo = workflowTask == null || workflowTask.getFileId() == null
                ? null
                : subjectMatchFileInfoGateway.findById(workflowTask.getFileId());
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
        return DataSourceConfig.builder()
                .sourceType(type)
                .sourceUri(sourceUri)
                .additionalParams(fileId == null ? null : String.valueOf(fileId))
                .build();
    }

    private String resolveAnalysisWorkbookPath(ParseTaskCommand command) {
        String commandPath = command == null ? null : command.getWorkbookPath();
        Long fileId = command == null ? null : command.getFileId();
        ValsetFileInfo fileInfo = fileId == null ? null : subjectMatchFileInfoGateway.findById(fileId);
        String tempPath = fileInfo == null ? null : fileInfo.getLocalTempPath();
        String realPath = fileInfo == null ? null : fileInfo.getRealStoragePath();
        String selectedPath = firstReadablePath(commandPath, tempPath, realPath);
        if (selectedPath != null) {
            return selectedPath;
        }
        return commandPath;
    }

    private String firstReadablePath(String... candidates) {
        if (candidates == null) {
            return null;
        }
        for (String candidate : candidates) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            try {
                Path path = Paths.get(candidate.trim());
                if (Files.exists(path) && Files.isReadable(path)) {
                    return path.toString();
                }
            } catch (InvalidPathException ignored) {
                log.warn("解析任务文件路径无效，已跳过，path={}", candidate);
            }
        }
        return null;
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

    private void validateParsedValuationData(ParsedValuationData parsedValuationData, ParseTaskCommand command) {
        if (parsedValuationData == null) {
            throw new IllegalStateException("解析失败，未返回结构化数据，fileId=" + (command == null ? null : command.getFileId()));
        }
        if (parsedValuationData.getHeaderRowNumber() == null || parsedValuationData.getDataStartRowNumber() == null) {
            throw new IllegalStateException("解析失败，未识别表头行号或数据起始行号，fileId="
                    + (command == null ? null : command.getFileId())
                    + ", headerRowNumber=" + parsedValuationData.getHeaderRowNumber()
                    + ", dataStartRowNumber=" + parsedValuationData.getDataStartRowNumber());
        }
    }

    private void publishLifecycleEvent(ParseLifecycleStage stage, Long taskId, ParseTaskCommand command, String message) {
        publishLifecycleEvent(stage, taskId, command, message, Map.of());
    }

    private void publishLifecycleEvent(ParseLifecycleStage stage, Long taskId, ParseTaskCommand command, String message, Map<String, Object> attributes) {
        if (parseLifecycleEventPublisher == null || stage == null) {
            return;
        }
        ParseLifecycleEvent.ParseLifecycleEventBuilder builder = ParseLifecycleEvent.builder()
                .stage(stage)
                .source("parse-execution")
                .taskId(taskId)
                .message(message);
        if (command != null) {
            builder.fileId(command.getFileId())
                    .dataSourceType(command.getDataSourceType());
        }
        if (attributes != null && !attributes.isEmpty()) {
            LinkedHashMap<String, Object> mergedAttributes = new LinkedHashMap<>(attributes);
            if (command != null && command.getForceRebuild() != null) {
                mergedAttributes.put("forceRebuild", command.getForceRebuild());
            }
            builder.attributes(mergedAttributes);
        } else if (command != null && command.getForceRebuild() != null) {
            builder.attributes(Map.of("forceRebuild", command.getForceRebuild()));
        }
        parseLifecycleEventPublisher.publish(builder.build());
    }
}
