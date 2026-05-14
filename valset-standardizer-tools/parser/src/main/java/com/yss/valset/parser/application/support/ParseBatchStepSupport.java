package com.yss.valset.parser.application.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.application.command.ParseTaskCommand;
import com.yss.valset.application.event.lifecycle.ParseLifecycleEvent;
import com.yss.valset.application.event.lifecycle.ParseLifecycleEventPublisher;
import com.yss.valset.application.event.lifecycle.ParseLifecycleStage;
import com.yss.valset.common.support.Java8Maps;
import com.yss.valset.domain.gateway.DwdExternalValuationGateway;
import com.yss.valset.domain.gateway.DwdJjhzgzbGateway;
import com.yss.valset.domain.gateway.StandardizedExternalValuationGateway;
import com.yss.valset.domain.gateway.TrIndexGateway;
import com.yss.valset.domain.gateway.ValsetFileInfoGateway;
import com.yss.valset.domain.gateway.WorkflowTaskGateway;
import com.yss.valset.domain.model.DataSourceConfig;
import com.yss.valset.domain.model.DataSourceType;
import com.yss.valset.domain.model.ParsedValuationData;
import com.yss.valset.domain.model.ValsetFileInfo;
import com.yss.valset.domain.model.WorkflowTask;
import com.yss.valset.domain.parser.ValuationDataParser;
import com.yss.valset.domain.parser.ValuationDataParserProvider;
import com.yss.valset.domain.rule.ParseRuleTraceContext;
import com.yss.valset.domain.rule.ParseRuleTraceContextHolder;
import com.yss.valset.extract.standardization.ExternalValuationStandardizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 估值表解析 Spring Batch 阶段支持组件。
 *
 * <p>
 * 这个组件承载 Spring Batch 三个步骤的真实业务逻辑：
 * 第一步负责文件解析，第二步负责结构标准化，第三步负责标准化结果落地。
 * 各步骤之间通过 {@link ExecutionContext} 传递中间结果，避免重复读取和重复解析。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ParseBatchStepSupport {

    public static final String JOB_CONTEXT_PARSED_DATA_JSON = "parse.parsedDataJson";
    public static final String JOB_CONTEXT_STANDARDIZED_DATA_JSON = "parse.standardizedDataJson";
    public static final String JOB_CONTEXT_RESULT_PAYLOAD = "parse.resultPayload";
    public static final String JOB_CONTEXT_FILE_PARSE_MS = "parse.fileParseMs";
    public static final String JOB_CONTEXT_STANDARDIZE_MS = "parse.standardizeMs";

    private final WorkflowTaskGateway taskGateway;
    private final ValuationDataParserProvider parserProvider;
    private final DwdExternalValuationGateway dwdExternalValuationGateway;
    private final StandardizedExternalValuationGateway standardizedExternalValuationGateway;
    private final DwdJjhzgzbGateway dwdJjhzgzbGateway;
    private final TrIndexGateway trIndexGateway;
    private final ValsetFileInfoGateway subjectMatchFileInfoGateway;
    private final ExternalValuationStandardizationService standardizationService;
    private final ObjectMapper objectMapper;
    private final ParseLifecycleEventPublisher parseLifecycleEventPublisher;

    /**
     * 文件解析步骤。
     *
     * <p>
     * 负责把原始估值文件解析成统一的中间模型，并写入 DWD 级结果和作业上下文。
     * 这个步骤是整条流水线的入口，后续步骤都依赖它产出的解析结果。
     * </p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void executeFileParse(Long taskId, ExecutionContext jobExecutionContext) {
        WorkflowTask workflowTask = requireTask(taskId);
        ParseTaskCommand command = readCommand(workflowTask);
        ParseLifecycleStage currentStage = ParseLifecycleStage.FILE_PARSE;
        ParseRuleTraceContext traceContext = buildTraceContext(workflowTask, taskId);
        long startedAt = System.currentTimeMillis();
        try (ParseRuleTraceContextHolder.TraceScope ignored = ParseRuleTraceContextHolder.withContext(traceContext)) {
            try {
                DataSourceType type = resolveDataSourceType(command);
                DataSourceConfig config = buildAnalysisConfig(type, resolveAnalysisWorkbookPath(command), command.getFileId());
                ValuationDataParser parser = parserProvider.getParser(type);
                log.info("Spring Batch 解析文件阶段开始，taskId={}, parser={}, sourceType={}, sourceUri={}",
                        taskId, parser.getClass().getSimpleName(), type, config.getSourceUri());
                ParsedValuationData parsedValuationData = parser.parse(config);
                validateParsedValuationData(parsedValuationData, command);
                String fileNameOriginal = resolveFileNameOriginal(workflowTask);
                ParsedValuationData normalizedParsedData = parsedValuationData.toBuilder()
                        .fileNameOriginal(fileNameOriginal)
                        .build();
                dwdExternalValuationGateway.saveDwdExternalValuation(taskId, workflowTask.getFileId(), normalizedParsedData);
                publishLifecycleEvent(ParseLifecycleStage.FILE_PARSE, taskId, command, "文件解析完成");
                putJson(jobExecutionContext, JOB_CONTEXT_PARSED_DATA_JSON, normalizedParsedData);
                jobExecutionContext.putLong(JOB_CONTEXT_FILE_PARSE_MS, System.currentTimeMillis() - startedAt);
            } catch (Exception exception) {
                publishFailureEvent(currentStage, taskId, command, exception);
                throw new IllegalStateException("Spring Batch 文件解析阶段失败，taskId=" + taskId, exception);
            }
        }
    }

    /**
     * 结构标准化步骤。
     *
     * <p>
     * 在文件解析结果的基础上，继续完成字段标准化、指标标准化和结构规整，
     * 处理后的结果仍然保存在作业上下文中，供最后一步复用。
     * </p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void executeStructureStandardize(Long taskId, ExecutionContext jobExecutionContext) {
        WorkflowTask workflowTask = requireTask(taskId);
        ParseTaskCommand command = readCommand(workflowTask);
        ParseLifecycleStage currentStage = ParseLifecycleStage.STRUCTURE_STANDARDIZE;
        ParseRuleTraceContext traceContext = buildTraceContext(workflowTask, taskId);
        long startedAt = System.currentTimeMillis();
        try (ParseRuleTraceContextHolder.TraceScope ignored = ParseRuleTraceContextHolder.withContext(traceContext)) {
            try {
                ParsedValuationData parsedValuationData = readJson(jobExecutionContext, JOB_CONTEXT_PARSED_DATA_JSON, ParsedValuationData.class);
                if (parsedValuationData == null) {
                    throw new IllegalStateException("Spring Batch 结构标准化阶段缺少文件解析结果，taskId=" + taskId);
                }
                ParsedValuationData standardizedValuationData = standardizationService.standardize(parsedValuationData);
                String fileNameOriginal = resolveFileNameOriginal(workflowTask);
                ParsedValuationData normalizedStandardizedData = standardizedValuationData == null ? null
                        : standardizedValuationData.toBuilder().fileNameOriginal(fileNameOriginal).build();
                putJson(jobExecutionContext, JOB_CONTEXT_STANDARDIZED_DATA_JSON, normalizedStandardizedData);
                publishLifecycleEvent(ParseLifecycleStage.STRUCTURE_STANDARDIZE, taskId, command, "结构标准化完成");
                jobExecutionContext.putLong(JOB_CONTEXT_STANDARDIZE_MS, System.currentTimeMillis() - startedAt);
            } catch (Exception exception) {
                publishFailureEvent(currentStage, taskId, command, exception);
                throw new IllegalStateException("Spring Batch 结构标准化阶段失败，taskId=" + taskId, exception);
            }
        }
    }

    /**
     * 标准表落地步骤。
     *
     * <p>
     * 这是整条 Spring Batch 流水线的最后一步，负责把标准化结果写入业务目标表，
     * 同时回写任务耗时、执行结果和生命周期事件。
     * </p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void executeStandardLanding(Long taskId, ExecutionContext jobExecutionContext) {
        WorkflowTask workflowTask = requireTask(taskId);
        ParseTaskCommand command = readCommand(workflowTask);
        ParseLifecycleStage currentStage = ParseLifecycleStage.STANDARD_LANDING;
        ParseRuleTraceContext traceContext = buildTraceContext(workflowTask, taskId);
        try (ParseRuleTraceContextHolder.TraceScope ignored = ParseRuleTraceContextHolder.withContext(traceContext)) {
            try {
                ParsedValuationData standardizedValuationData = readJson(jobExecutionContext, JOB_CONTEXT_STANDARDIZED_DATA_JSON, ParsedValuationData.class);
                if (standardizedValuationData == null) {
                    throw new IllegalStateException("Spring Batch 标准表落地阶段缺少标准化结果，taskId=" + taskId);
                }
                DataSourceType type = resolveDataSourceType(command);
                String fileNameOriginal = resolveFileNameOriginal(workflowTask);
                ParsedValuationData finalStandardizedValuationData = standardizedValuationData.toBuilder()
                        .fileNameOriginal(fileNameOriginal)
                        .build();
                String sourceTypeName = type.name();
                String sourceSign = fileNameOriginal;
                standardizedExternalValuationGateway.saveStandardizedExternalValuation(taskId, workflowTask.getFileId(), finalStandardizedValuationData);
                dwdJjhzgzbGateway.saveStandardizedJjhzgzb(taskId, workflowTask.getFileId(), sourceTypeName, sourceSign, finalStandardizedValuationData);
                trIndexGateway.saveStandardizedIndex(taskId, workflowTask.getFileId(), sourceTypeName, sourceSign, finalStandardizedValuationData);
                String resultPayload = buildResultPayload(finalStandardizedValuationData);
                taskGateway.updateTaskTimings(taskId,
                        jobExecutionContext.getLong(JOB_CONTEXT_FILE_PARSE_MS, 0L),
                        jobExecutionContext.getLong(JOB_CONTEXT_STANDARDIZE_MS, 0L),
                        null);
                taskGateway.markSuccess(taskId, resultPayload);
                jobExecutionContext.putString(JOB_CONTEXT_RESULT_PAYLOAD, resultPayload);
                publishLifecycleEvent(ParseLifecycleStage.STANDARD_LANDING, taskId, command, "标准数据落地完成");
            } catch (Exception exception) {
                publishFailureEvent(currentStage, taskId, command, exception);
                throw new IllegalStateException("Spring Batch 标准表落地阶段失败，taskId=" + taskId, exception);
            }
        }
    }

    /**
     * 按任务 ID 重新加载解析任务。
     *
     * <p>
     * Spring Batch 的步骤执行本身只接收 taskId，因此这里统一通过任务网关还原完整任务对象。
     * </p>
     */
    private WorkflowTask requireTask(Long taskId) {
        WorkflowTask workflowTask = taskGateway.findById(taskId);
        if (workflowTask == null) {
            throw new IllegalStateException("未找到解析任务，taskId=" + taskId);
        }
        return workflowTask;
    }

    /**
     * 从任务输入体中反序列化解析命令。
     */
    private ParseTaskCommand readCommand(WorkflowTask workflowTask) {
        if (workflowTask == null || !StringUtils.hasText(workflowTask.getInputPayload())) {
            throw new IllegalStateException("解析任务入参为空，taskId=" + (workflowTask == null ? null : workflowTask.getTaskId()));
        }
        try {
            return objectMapper.readValue(workflowTask.getInputPayload(), ParseTaskCommand.class);
        } catch (Exception exception) {
            throw new IllegalStateException("解析任务入参反序列化失败，taskId=" + workflowTask.getTaskId(), exception);
        }
    }

    /**
     * 构造规则链路的跟踪上下文。
     *
     * <p>
     * 这里主要是为了让解析规则、日志和异常分类都能拿到同一个 taskId/fileId 视角。
     * </p>
     */
    private ParseRuleTraceContext buildTraceContext(WorkflowTask workflowTask, Long taskId) {
        return ParseRuleTraceContext.builder()
                .profileId(null)
                .profileCode("runtime")
                .version("runtime")
                .fileId(workflowTask == null ? null : workflowTask.getFileId())
                .taskId(taskId)
                .traceEnabled(Boolean.FALSE)
                .traceScope("RUNTIME_PARSE")
                .build();
    }

    /**
     * 从命令中解析数据源类型，默认按 Excel 处理。
     */
    private DataSourceType resolveDataSourceType(ParseTaskCommand command) {
        String sourceTypeStr = command == null ? null : command.getDataSourceType();
        if (!StringUtils.hasText(sourceTypeStr)) {
            return DataSourceType.EXCEL;
        }
        return DataSourceType.valueOf(sourceTypeStr.trim().toUpperCase());
    }

    /**
     * 组装解析器所需的数据源配置。
     */
    private DataSourceConfig buildAnalysisConfig(DataSourceType type, String sourceUri, Long fileId) {
        return DataSourceConfig.builder()
                .sourceType(type)
                .sourceUri(sourceUri)
                .additionalParams(fileId == null ? null : String.valueOf(fileId))
                .build();
    }

    /**
     * 解析阶段优先选择可读的文件路径，避免把不可用路径传给解析器。
     */
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

    /**
     * 回填原始文件名，保证后续标准化和落库使用同一份文件标识。
     */
    private String resolveFileNameOriginal(WorkflowTask workflowTask) {
        ValsetFileInfo fileInfo = workflowTask == null || workflowTask.getFileId() == null
                ? null
                : subjectMatchFileInfoGateway.findById(workflowTask.getFileId());
        return fileInfo == null ? null : fileInfo.getFileNameOriginal();
    }

    /**
     * 在多个候选路径中选出第一个可读路径。
     */
    private String firstReadablePath(String... candidates) {
        if (candidates == null) {
            return null;
        }
        for (String candidate : candidates) {
            if (!StringUtils.hasText(candidate)) {
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
     * 生成步骤执行结果的简化回写载荷。
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
        publishLifecycleEvent(stage, taskId, command, message, java.util.Collections.emptyMap());
    }

    private void publishLifecycleEvent(ParseLifecycleStage stage, Long taskId, ParseTaskCommand command, String message, Map<String, Object> attributes) {
        if (parseLifecycleEventPublisher == null || stage == null) {
            return;
        }
        ParseLifecycleEvent.ParseLifecycleEventBuilder builder = ParseLifecycleEvent.builder()
                .stage(stage)
                .source("spring-batch-parse")
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
            builder.attributes(Java8Maps.of("forceRebuild", command.getForceRebuild()));
        }
        parseLifecycleEventPublisher.publish(builder.build());
    }

    private void publishFailureEvent(ParseLifecycleStage stage, Long taskId, ParseTaskCommand command, Exception exception) {
        publishLifecycleEvent(stage, taskId, command, "解析任务执行失败", Java8Maps.of(
                "errorMessage", exception == null ? null : (exception.getMessage() == null ? exception.getClass().getName() : exception.getMessage()),
                "errorType", exception == null ? null : exception.getClass().getName()
        ));
    }

    private <T> T readJson(ExecutionContext jobExecutionContext, String key, Class<T> type) {
        if (jobExecutionContext == null || !StringUtils.hasText(key) || type == null) {
            return null;
        }
        String value = jobExecutionContext.getString(key, null);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return objectMapper.readValue(value, type);
        } catch (Exception exception) {
            throw new IllegalStateException("解析作业上下文反序列化失败，key=" + key, exception);
        }
    }

    private void putJson(ExecutionContext jobExecutionContext, String key, Object value) {
        if (jobExecutionContext == null || !StringUtils.hasText(key) || value == null) {
            return;
        }
        try {
            jobExecutionContext.putString(key, objectMapper.writeValueAsString(value));
        } catch (Exception exception) {
            throw new IllegalStateException("解析作业上下文序列化失败，key=" + key, exception);
        }
    }
}
