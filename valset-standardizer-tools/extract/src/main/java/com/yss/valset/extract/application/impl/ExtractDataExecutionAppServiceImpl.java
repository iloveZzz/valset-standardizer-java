package com.yss.valset.extract.application.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.extract.application.command.ExtractDataTaskCommand;
import com.yss.valset.extract.application.port.ExtractDataExecutionUseCase;
import com.yss.valset.domain.exception.FileAccessException;
import com.yss.valset.domain.exception.UnsupportedDataSourceException;
import com.yss.valset.domain.gateway.WorkflowTaskGateway;
import com.yss.valset.domain.model.DataSourceConfig;
import com.yss.valset.domain.model.DataSourceType;
import com.yss.valset.domain.model.WorkflowTask;
import com.yss.valset.extract.domain.extractor.RawDataExtractor;
import com.yss.valset.extract.extractor.RawDataExtractorRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 原始数据提取任务执行实现。
 */
@Slf4j
@Service
public class ExtractDataExecutionAppServiceImpl implements ExtractDataExecutionUseCase {

    private final WorkflowTaskGateway taskGateway;
    private final RawDataExtractorRegistry rawDataExtractorRegistry;
    private final ObjectMapper objectMapper;

    public ExtractDataExecutionAppServiceImpl(WorkflowTaskGateway taskGateway,
                                              RawDataExtractorRegistry rawDataExtractorRegistry,
                                              ObjectMapper objectMapper) {
        this.taskGateway = taskGateway;
        this.rawDataExtractorRegistry = rawDataExtractorRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void execute(Long taskId) {
        long startedAt = System.currentTimeMillis();
        log.info("开始执行原始数据提取任务，taskId={}", taskId);
        long taskLoadedAt;
        long commandParsedAt;
        long configBuiltAt;
        long extractFinishedAt;
        WorkflowTask workflowTask = taskGateway.findById(taskId);
        taskLoadedAt = System.currentTimeMillis();
        ExtractDataTaskCommand command = readCommand(workflowTask);
        commandParsedAt = System.currentTimeMillis();
        DataSourceType dataSourceType = resolveDataSourceType(command.getDataSourceType());
        Path workbookPath = Paths.get(command.getWorkbookPath());
        if (!Files.exists(workbookPath) || !Files.isReadable(workbookPath)) {
            throw new FileAccessException(command.getWorkbookPath());
        }

        Long fileId = workflowTask.getFileId() != null ? workflowTask.getFileId() : taskId;
        log.info("任务 {} 解析完成，数据源类型={}, 文件路径={}, fileId={}", taskId, dataSourceType, workbookPath, fileId);
        DataSourceConfig config = DataSourceConfig.builder()
                .sourceType(dataSourceType)
                .sourceUri(command.getWorkbookPath())
                .build();
        configBuiltAt = System.currentTimeMillis();

        RawDataExtractor extractor = rawDataExtractorRegistry.getExtractor(dataSourceType);
        log.debug("任务 {} 使用提取器 {}", taskId, extractor.getClass().getSimpleName());
        int rowCount = extractor.extract(config, taskId, fileId);
        extractFinishedAt = System.currentTimeMillis();
        long durationMs = extractFinishedAt - startedAt;

        taskGateway.updateTaskTimings(taskId, durationMs, null, null);
        String resultPayload = buildResultPayload(command, fileId, rowCount, workbookPath, durationMs);
        taskGateway.markSuccess(taskId, resultPayload);
        log.info(
                "原始数据提取任务执行完成，taskId={}, rowCount={}, parseTaskTimeMs={}, loadTaskMs={}, parseCommandMs={}, buildConfigMs={}, extractMs={}, sourceType={}, fileId={}",
                taskId,
                rowCount,
                durationMs,
                taskLoadedAt - startedAt,
                commandParsedAt - taskLoadedAt,
                configBuiltAt - commandParsedAt,
                extractFinishedAt - configBuiltAt,
                dataSourceType,
                fileId
        );
    }

    private ExtractDataTaskCommand readCommand(WorkflowTask workflowTask) {
        try {
            return objectMapper.readValue(workflowTask.getInputPayload(), ExtractDataTaskCommand.class);
        } catch (Exception e) {
            log.error("解析原始数据提取任务入参失败，taskId={}", workflowTask.getTaskId(), e);
            throw new IllegalStateException("Failed to deserialize extract task payload for task " + workflowTask.getTaskId(), e);
        }
    }

    private DataSourceType resolveDataSourceType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return DataSourceType.EXCEL;
        }
        try {
            return DataSourceType.valueOf(rawType.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new UnsupportedDataSourceException(rawType);
        }
    }

    private String buildResultPayload(ExtractDataTaskCommand command,
                                      Long fileId,
                                      int rowCount,
                                      Path workbookPath,
                                      long durationMs) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("workbookPath", command.getWorkbookPath());
            payload.put("dataSourceType", command.getDataSourceType() == null ? "EXCEL" : command.getDataSourceType().toUpperCase());
            payload.put("fileId", fileId);
            payload.put("rowCount", rowCount);
            payload.put("fileSizeBytes", Files.size(workbookPath));
            payload.put("durationMs", durationMs);
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.warn("原始数据提取结果序列化失败，任务将回退为最简结果", e);
            return "{\"rowCount\":" + rowCount + ",\"fileId\":" + fileId + "}";
        } catch (Exception e) {
            log.error("构建原始数据提取结果失败", e);
            throw new IllegalStateException("Failed to build extract result payload", e);
        }
    }
}
