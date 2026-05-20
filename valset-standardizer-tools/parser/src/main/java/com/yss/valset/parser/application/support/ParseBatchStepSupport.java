package com.yss.valset.parser.application.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.application.command.ParseTaskCommand;
import com.yss.valset.application.event.lifecycle.ParseLifecycleEvent;
import com.yss.valset.application.event.lifecycle.ParseLifecycleEventPublisher;
import com.yss.valset.application.event.lifecycle.ParseLifecycleStage;
import com.yss.valset.common.support.TaskFailureClassifier;
import com.yss.valset.common.support.Java8Maps;
import com.yss.valset.domain.gateway.DwdExternalValuationGateway;
import com.yss.valset.domain.gateway.DwdJjhzgzbGateway;
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
 * 估值表解析 批量任务 阶段支持组件。
 *
 * <p>
 * 这个组件承载 批量任务 三个步骤的真实业务逻辑：
 * 第一步负责文件解析，第二步负责结构标准化，第三步负责标准化结果落地。
 * 各步骤之间通过 {@link ExecutionContext} 传递中间结果，避免重复读取和重复解析。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ParseBatchStepSupport {

    public static final String JOB_CONTEXT_FILE_PARSE_MS = "parse.fileParseMs";
    public static final String JOB_CONTEXT_STANDARDIZE_MS = "parse.standardizeMs";

    private final WorkflowTaskGateway taskGateway;
    private final ValuationDataParserProvider parserProvider;
    private final DwdExternalValuationGateway dwdExternalValuationGateway;
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
        long startedAt = System.currentTimeMillis();
        try {
            DataSourceType type = resolveDataSourceType(command);
            DataSourceConfig config = buildAnalysisConfig(type, resolveAnalysisWorkbookPath(command), command.getFileId());
            ValuationDataParser parser = parserProvider.getParser(type);
            log.info("批量任务 解析文件阶段开始，taskId={}, parser={}, sourceType={}, sourceUri={}",
                    taskId, parser.getClass().getSimpleName(), type, config.getSourceUri());
            ParsedValuationData parsedValuationData = parser.parse(config);
            validateParsedValuationData(parsedValuationData, command);
            String fileNameOriginal = resolveFileNameOriginal(workflowTask);
            ParsedValuationData normalizedParsedData = parsedValuationData.toBuilder()
                    .fileNameOriginal(fileNameOriginal)
                    .build();
            dwdExternalValuationGateway.saveDwdExternalValuation(taskId, workflowTask.getFileId(), normalizedParsedData);
            publishLifecycleEvent(ParseLifecycleStage.FILE_PARSE, taskId, command, "文件解析完成");
            jobExecutionContext.putLong(JOB_CONTEXT_FILE_PARSE_MS, System.currentTimeMillis() - startedAt);
        } catch (Exception exception) {
            publishFailureEvent(currentStage, taskId, command, exception);
            throw new IllegalStateException("批量任务 文件解析阶段失败，taskId=" + taskId, exception);
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
        long startedAt = System.currentTimeMillis();
        try {
            ParsedValuationData parsedValuationData = dwdExternalValuationGateway.findLatestByFileId(workflowTask.getFileId());
            if (parsedValuationData == null) {
                throw new IllegalStateException("批量任务 结构标准化阶段缺少文件解析结果，taskId=" + taskId);
            }
            ParsedValuationData standardizedValuationData = standardizationService.standardize(parsedValuationData);
            if (standardizedValuationData == null) {
                throw new IllegalStateException("批量任务 结构标准化阶段未返回标准化结果，taskId=" + taskId);
            }
            publishLifecycleEvent(ParseLifecycleStage.STRUCTURE_STANDARDIZE, taskId, command, "结构标准化完成");
            jobExecutionContext.putLong(JOB_CONTEXT_STANDARDIZE_MS, System.currentTimeMillis() - startedAt);
        } catch (Exception exception) {
            publishFailureEvent(currentStage, taskId, command, exception);
            throw new IllegalStateException("批量任务 结构标准化阶段失败，taskId=" + taskId, exception);
        }
    }

    /**
     * 估值贴源数据落地步骤。
     *
     * <p>
     * 这是整条 批量任务 流水线的最后一步，负责把标准化结果写入业务目标表，
     * 同时回写任务耗时、执行结果和生命周期事件。
     * </p>
     */
    @Transactional(rollbackFor = Exception.class)
    public void executeStandardLanding(Long taskId, ExecutionContext jobExecutionContext) {
        WorkflowTask workflowTask = requireTask(taskId);
        ParseTaskCommand command = readCommand(workflowTask);
        ParseLifecycleStage currentStage = ParseLifecycleStage.STANDARD_LANDING;
        try {
            DataSourceType type = resolveDataSourceType(command);
            String fileNameOriginal = resolveFileNameOriginal(workflowTask);
            ParsedValuationData sourceValuationData = dwdExternalValuationGateway.findLatestByFileId(workflowTask.getFileId());
            if (sourceValuationData == null) {
                throw new IllegalStateException("批量任务 估值贴源数据落地阶段缺少最新 STG 贴源结果，taskId=" + taskId);
            }
            ParsedValuationData standardizedValuationData = standardizationService.standardize(sourceValuationData);
            if (standardizedValuationData == null) {
                throw new IllegalStateException("批量任务 估值贴源数据落地阶段未返回标准化结果，taskId=" + taskId);
            }
            ParsedValuationData finalStandardizedValuationData = mergeSourceMetadata(standardizedValuationData, sourceValuationData, fileNameOriginal);
            String sourceTypeName = type.name();
            String sourceSign = fileNameOriginal;
            dwdJjhzgzbGateway.saveStandardizedJjhzgzb(taskId, workflowTask.getFileId(), sourceTypeName, sourceSign, finalStandardizedValuationData);
            trIndexGateway.saveStandardizedIndex(taskId, workflowTask.getFileId(), sourceTypeName, sourceSign, finalStandardizedValuationData);
            String resultPayload = buildResultPayload(finalStandardizedValuationData);
            taskGateway.updateTaskTimings(taskId,
                    jobExecutionContext.getLong(JOB_CONTEXT_FILE_PARSE_MS, 0L),
                    jobExecutionContext.getLong(JOB_CONTEXT_STANDARDIZE_MS, 0L),
                    null);
            taskGateway.markSuccess(taskId, resultPayload);
            publishLifecycleEvent(ParseLifecycleStage.STANDARD_LANDING, taskId, command, "标准数据落地完成");
        } catch (Exception exception) {
            log.error("批量任务 估值贴源数据落地阶段失败，taskId={}, fileId={}", taskId, workflowTask.getFileId(), exception);
            publishFailureEvent(currentStage, taskId, command, exception);
            throw new IllegalStateException("批量任务 估值贴源数据落地阶段失败，taskId=" + taskId, exception);
        }
    }

    /**
     * 按任务 ID 重新加载解析任务。
     *
     * <p>
     * 批量任务 的步骤执行本身只接收 taskId，因此这里统一通过任务网关还原完整任务对象。
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
        if (workflowTask == null) {
            throw new IllegalStateException("解析任务入参为空，taskId=" + (workflowTask == null ? null : workflowTask.getTaskId()));
        }
        if (!StringUtils.hasText(workflowTask.getInputPayload())) {
            return rebuildCommandFromFileInfo(workflowTask);
        }
        try {
            return objectMapper.readValue(workflowTask.getInputPayload(), ParseTaskCommand.class);
        } catch (Exception exception) {
            throw new IllegalStateException("解析任务入参反序列化失败，taskId=" + workflowTask.getTaskId(), exception);
        }
    }

    /**
     * 历史批次重新解析时，Batch 元数据可能已经不再保存大 JSON 入参。
     * 这里从文件主数据恢复解析命令，保证重跑路径不依赖旧的 inputPayload。
     */
    private ParseTaskCommand rebuildCommandFromFileInfo(WorkflowTask workflowTask) {
        Long fileId = workflowTask.getFileId();
        if (fileId == null) {
            throw new IllegalStateException("解析任务入参为空且缺少 fileId，taskId=" + workflowTask.getTaskId());
        }
        ValsetFileInfo fileInfo = subjectMatchFileInfoGateway.findById(fileId);
        if (fileInfo == null) {
            throw new IllegalStateException("解析任务入参为空且未找到文件主数据，taskId=" + workflowTask.getTaskId() + "，fileId=" + fileId);
        }
        ParseTaskCommand command = new ParseTaskCommand();
        command.setDataSourceType(resolveDataSourceTypeName(fileInfo));
        command.setWorkbookPath(resolveWorkbookPath(fileInfo));
        command.setFileId(fileId);
        command.setFileNameOriginal(fileInfo.getFileNameOriginal());
        command.setCreatedBy("file-manage-reanalyse");
        command.setForceRebuild(Boolean.TRUE);
        return command;
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

    private String resolveDataSourceTypeName(ValsetFileInfo fileInfo) {
        if (fileInfo == null || !StringUtils.hasText(fileInfo.getFileFormat())) {
            return DataSourceType.EXCEL.name();
        }
        return fileInfo.getFileFormat().trim().toUpperCase();
    }

    private String resolveWorkbookPath(ValsetFileInfo fileInfo) {
        if (fileInfo == null) {
            return null;
        }
        String readablePath = firstReadablePath(fileInfo.getStorageUri(), fileInfo.getLocalTempPath(), fileInfo.getRealStoragePath());
        if (readablePath != null) {
            return readablePath;
        }
        if (StringUtils.hasText(fileInfo.getStorageUri())) {
            return fileInfo.getStorageUri().trim();
        }
        if (StringUtils.hasText(fileInfo.getRealStoragePath())) {
            return fileInfo.getRealStoragePath().trim();
        }
        return StringUtils.hasText(fileInfo.getLocalTempPath()) ? fileInfo.getLocalTempPath().trim() : null;
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

    private ParsedValuationData mergeSourceMetadata(
            ParsedValuationData standardizedValuationData,
            ParsedValuationData sourceValuationData,
            String fileNameOriginal
    ) {
        if (standardizedValuationData == null) {
            return null;
        }
        ParsedValuationData.ParsedValuationDataBuilder builder = standardizedValuationData.toBuilder()
                .fileNameOriginal(fileNameOriginal);
        if (sourceValuationData != null) {
            builder.workbookPath(sourceValuationData.getWorkbookPath())
                    .sheetName(sourceValuationData.getSheetName())
                    .headerRowNumber(sourceValuationData.getHeaderRowNumber())
                    .dataStartRowNumber(sourceValuationData.getDataStartRowNumber())
                    .title(sourceValuationData.getTitle())
                    .basicInfo(sourceValuationData.getBasicInfo())
                    .headers(sourceValuationData.getHeaders())
                    .headerDetails(sourceValuationData.getHeaderDetails())
                    .headerColumns(sourceValuationData.getHeaderColumns())
                    .headerMappingDecisions(sourceValuationData.getHeaderMappingDecisions())
                    .mappingQualityReport(sourceValuationData.getMappingQualityReport());
        }
        return builder.build();
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
                "errorMessage", exception == null ? null : TaskFailureClassifier.resolveReadableMessage(exception),
                "errorType", exception == null ? null : exception.getClass().getName()
        ));
    }

}
