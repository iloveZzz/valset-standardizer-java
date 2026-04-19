package com.yss.valset.application.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.application.command.ExtractDataTaskCommand;
import com.yss.valset.application.command.MatchTaskCommand;
import com.yss.valset.application.command.ParseTaskCommand;
import com.yss.valset.application.dto.FullWorkflowResponse;
import com.yss.valset.application.dto.StoredFileDTO;
import com.yss.valset.application.dto.TaskViewDTO;
import com.yss.valset.application.dto.UploadValuationFileResponse;
import com.yss.valset.application.port.ExtractDataExecutionUseCase;
import com.yss.valset.application.port.MatchExecutionUseCase;
import com.yss.valset.application.port.ParseExecutionUseCase;
import com.yss.valset.application.service.TaskQueryAppService;
import com.yss.valset.application.service.ValuationWorkflowAppService;
import com.yss.valset.domain.gateway.ValsetFileInfoGateway;
import com.yss.valset.domain.gateway.ValsetFileIngestLogGateway;
import com.yss.valset.application.support.UploadedFileStorageService;
import com.yss.valset.application.support.TaskReuseService;
import com.yss.valset.domain.gateway.TaskGateway;
import com.yss.valset.domain.model.ValsetFileInfo;
import com.yss.valset.domain.model.ValsetFileIngestLog;
import com.yss.valset.domain.model.ValsetFileSourceChannel;
import com.yss.valset.domain.model.ValsetFileStatus;
import com.yss.valset.domain.model.ValsetFileStorageType;
import com.yss.valset.domain.model.TaskInfo;
import com.yss.valset.domain.model.TaskStatus;
import com.yss.valset.domain.model.TaskStage;
import com.yss.valset.domain.model.TaskType;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * 外部估值全流程编排服务默认实现。
 */
@Slf4j
@Service
public class DefaultValuationWorkflowAppService implements ValuationWorkflowAppService {

    private final UploadedFileStorageService uploadedFileStorageService;
    private final TaskGateway taskGateway;
    private final TaskQueryAppService taskQueryAppService;
    private final ExtractDataExecutionUseCase extractDataExecutionUseCase;
    private final ParseExecutionUseCase parseExecutionUseCase;
    private final MatchExecutionUseCase matchExecutionUseCase;
    private final ObjectMapper objectMapper;
    private final TaskReuseService taskReuseService;
    private final ValsetFileInfoGateway subjectMatchFileInfoGateway;
    private final ValsetFileIngestLogGateway subjectMatchFileIngestLogGateway;
    private final Tracer tracer;
    @Value("${subject.match.workflow.enable-match-process:true}")
    private boolean enableMatchProcess;

    public DefaultValuationWorkflowAppService(UploadedFileStorageService uploadedFileStorageService,
                                              TaskGateway taskGateway,
                                              TaskQueryAppService taskQueryAppService,
                                              ExtractDataExecutionUseCase extractDataExecutionUseCase,
                                              ParseExecutionUseCase parseExecutionUseCase,
                                              MatchExecutionUseCase matchExecutionUseCase,
                                              ObjectMapper objectMapper,
                                              TaskReuseService taskReuseService,
                                              ValsetFileInfoGateway subjectMatchFileInfoGateway,
                                              ValsetFileIngestLogGateway subjectMatchFileIngestLogGateway,
                                              Tracer tracer) {
        this.uploadedFileStorageService = uploadedFileStorageService;
        this.taskGateway = taskGateway;
        this.taskQueryAppService = taskQueryAppService;
        this.extractDataExecutionUseCase = extractDataExecutionUseCase;
        this.parseExecutionUseCase = parseExecutionUseCase;
        this.matchExecutionUseCase = matchExecutionUseCase;
        this.objectMapper = objectMapper;
        this.taskReuseService = taskReuseService;
        this.subjectMatchFileInfoGateway = subjectMatchFileInfoGateway;
        this.subjectMatchFileIngestLogGateway = subjectMatchFileIngestLogGateway;
        this.tracer = tracer;
    }

    @Override
    public UploadValuationFileResponse uploadAndExtract(MultipartFile file, String dataSourceType, String createdBy, Boolean forceRebuild) {
        log.info("全流程-上传与提取开始，fileName={}, dataSourceType={}, createdBy={}, forceRebuild={}",
                file == null ? null : file.getOriginalFilename(),
                dataSourceType,
                createdBy,
                forceRebuild);
        // Step 1: 文件入库与指纹登记
        StoredFileDTO storedFile = uploadedFileStorageService.store(file, dataSourceType);
        ValsetFileInfo fileInfo = registerUploadedFile(storedFile, createdBy);
        ExtractDataTaskCommand command = new ExtractDataTaskCommand();
        command.setWorkbookPath(fileInfo.getStorageUri());
        command.setDataSourceType(storedFile.getDataSourceType());
        command.setCreatedBy(createdBy);
        command.setFileFingerprint(storedFile.getFileFingerprint());
        command.setFileId(fileInfo.getFileId());
        command.setForceRebuild(Boolean.TRUE.equals(forceRebuild));

        String businessKey = buildExtractBusinessKey(command);
        TaskInfo reusableTask = taskReuseService.findReusableSuccessfulTask(TaskType.EXTRACT_DATA, businessKey, command.getForceRebuild());
        boolean reusedExistingTask = reusableTask != null;
        TaskViewDTO extractTask = reusedExistingTask
                ? taskQueryAppService.queryTask(reusableTask.getTaskId())
                : runTask(
                        TaskType.EXTRACT_DATA,
                        businessKey,
                        fileInfo.getFileId(),
                        command,
                        extractDataExecutionUseCase::execute
                );
        Long fileId = extractFileId(extractTask);
        subjectMatchFileInfoGateway.updateStatus(
                fileInfo.getFileId(),
                ValsetFileStatus.EXTRACTED,
                extractTask.getTaskId(),
                LocalDateTime.now(),
                null
        );
        log.info("全流程-上传与提取完成，fileId={}, extractTaskId={}, reusedTask={}",
                fileInfo.getFileId() == null ? fileId : fileInfo.getFileId(),
                extractTask.getTaskId(),
                reusedExistingTask);
        return UploadValuationFileResponse.builder()
                .fileId(fileInfo.getFileId() == null ? fileId : fileInfo.getFileId())
                .workbookPath(fileInfo.getStorageUri())
                .dataSourceType(storedFile.getDataSourceType())
                .fileSizeBytes(storedFile.getFileSizeBytes())
                .fileFingerprint(storedFile.getFileFingerprint())
                .reusedExistingExtractTask(reusedExistingTask)
                .extractTask(extractTask)
                .build();
    }

    @Override
    public TaskViewDTO analyze(ParseTaskCommand command) {
        log.info("全流程-解析阶段开始，fileId={}, workbookPath={}, dataSourceType={}",
                command == null ? null : command.getFileId(),
                command == null ? null : command.getWorkbookPath(),
                command == null ? null : command.getDataSourceType());
        TaskViewDTO taskViewDTO = runTask(
                TaskType.PARSE_WORKBOOK,
                buildParseBusinessKey(command),
                command.getFileId(),
                command,
                parseExecutionUseCase::execute
        );
        subjectMatchFileInfoGateway.updateStatus(
                command.getFileId(),
                ValsetFileStatus.PARSED,
                taskViewDTO.getTaskId(),
                LocalDateTime.now(),
                null
        );
        log.info("全流程-解析阶段完成，fileId={}, parseTaskId={}, status={}",
                command == null ? null : command.getFileId(),
                taskViewDTO.getTaskId(),
                taskViewDTO.getTaskStatus());
        return taskViewDTO;
    }

    @Override
    public TaskViewDTO match(MatchTaskCommand command) {
        if (!enableMatchProcess) {
            log.info("科目匹配流程已关闭，直接返回跳过结果，fileId={}, workbookPath={}", command.getFileId(), command.getWorkbookPath());
            return buildSkippedMatchTask(command, "subject.match.workflow.enable-match-process=false");
        }
        TaskViewDTO taskViewDTO = runTask(
                TaskType.MATCH_SUBJECT,
                buildMatchBusinessKey(command),
                command.getFileId(),
                command,
                matchExecutionUseCase::execute
        );
        subjectMatchFileInfoGateway.updateStatus(
                command.getFileId(),
                ValsetFileStatus.MATCHED,
                taskViewDTO.getTaskId(),
                LocalDateTime.now(),
                null
        );
        return taskViewDTO;
    }

    @Override
    public FullWorkflowResponse runFullWorkflow(MultipartFile file,
                                                String dataSourceType,
                                                Integer topK,
                                                String createdBy,
                                                Boolean forceRebuild) {
        Span rootSpan = tracer.nextSpan().name("workflow.full.execute")
                .tag("data.source.type", dataSourceType == null ? "EXCEL" : dataSourceType)
                .start();
        try (Tracer.SpanInScope ws = tracer.withSpan(rootSpan)) {
            long startedAt = System.currentTimeMillis();
            log.info("全流程执行开始，fileName={}, dataSourceType={}, topK={}, createdBy={}, forceRebuild={}",
                    file == null ? null : file.getOriginalFilename(),
                    dataSourceType,
                    topK,
                    createdBy,
                    forceRebuild);

            // Step 1: 上传 + 原始提取
            UploadValuationFileResponse uploadResponse = traceSpan("workflow.full.extract",
                    () -> uploadAndExtract(file, dataSourceType, createdBy, forceRebuild));

            ParseTaskCommand parseTaskCommand = new ParseTaskCommand();
            parseTaskCommand.setDataSourceType(uploadResponse.getDataSourceType());
            parseTaskCommand.setWorkbookPath(uploadResponse.getWorkbookPath());
            parseTaskCommand.setFileId(uploadResponse.getFileId());
            parseTaskCommand.setFileNameOriginal(file.getOriginalFilename());
            parseTaskCommand.setCreatedBy(createdBy);
            parseTaskCommand.setForceRebuild(Boolean.TRUE.equals(forceRebuild));
            // Step 2: 结构化解析 + 标准化落地
            TaskViewDTO parseTask = traceSpan("workflow.full.parse", () -> analyze(parseTaskCommand));

            MatchTaskCommand matchTaskCommand = new MatchTaskCommand();
            matchTaskCommand.setDataSourceType(uploadResponse.getDataSourceType());
            matchTaskCommand.setWorkbookPath(uploadResponse.getWorkbookPath());
            matchTaskCommand.setFileId(uploadResponse.getFileId());
            matchTaskCommand.setTopK(topK == null ? 5 : topK);
            matchTaskCommand.setCreatedBy(createdBy);
            matchTaskCommand.setForceRebuild(Boolean.TRUE.equals(forceRebuild));
            // Step 3: 科目匹配（可配置跳过）
            TaskViewDTO matchTask = traceSpan("workflow.full.match", () -> enableMatchProcess
                    ? match(matchTaskCommand)
                    : buildSkippedMatchTask(matchTaskCommand, "subject.match.workflow.enable-match-process=false"));

            log.info("全流程执行完成，fileId={}, extractTaskId={}, parseTaskId={}, matchTaskId={}, totalMs={}",
                    uploadResponse.getFileId(),
                    uploadResponse.getExtractTask() == null ? null : uploadResponse.getExtractTask().getTaskId(),
                    parseTask == null ? null : parseTask.getTaskId(),
                    matchTask == null ? null : matchTask.getTaskId(),
                    System.currentTimeMillis() - startedAt);
            return FullWorkflowResponse.builder()
                    .fileId(uploadResponse.getFileId())
                    .workbookPath(uploadResponse.getWorkbookPath())
                    .dataSourceType(uploadResponse.getDataSourceType())
                    .fileFingerprint(uploadResponse.getFileFingerprint())
                    .extractTask(uploadResponse.getExtractTask())
                    .parseTask(parseTask)
                    .matchTask(matchTask)
                    .build();
        } catch (Exception exception) {
            rootSpan.error(exception);
            throw exception;
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

    private TaskViewDTO buildSkippedMatchTask(MatchTaskCommand command, String reason) {
        Map<String, Object> resultData = new LinkedHashMap<>();
        resultData.put("skipped", true);
        resultData.put("reason", reason);
        resultData.put("fileId", command == null ? null : command.getFileId());
        resultData.put("workbookPath", command == null ? null : command.getWorkbookPath());
        return TaskViewDTO.builder()
                .taskType(TaskType.MATCH_SUBJECT.name())
                .taskStage(TaskStage.MATCH.name())
                .taskStatus("SKIPPED")
                .businessKey(buildMatchBusinessKey(command))
                .resultData(resultData)
                .build();
    }

    private TaskViewDTO runTask(TaskType taskType,
                                String businessKey,
                                Long fileId,
                                Object command,
                                TaskExecution taskExecution) {
        boolean forceRebuild = isForceRebuild(command);
        TaskInfo reusableTask = taskReuseService.findReusableSuccessfulTask(taskType, businessKey, forceRebuild);
        if (reusableTask != null) {
            log.info("任务复用成功，taskType={}, businessKey={}, taskId={}", taskType, businessKey, reusableTask.getTaskId());
            return taskQueryAppService.queryTask(reusableTask.getTaskId());
        }
        long startedAt = System.currentTimeMillis();
        Long taskId = createTask(taskType, businessKey, fileId, command);
        try {
            LocalDateTime taskStartTime = LocalDateTime.now();
            taskGateway.markRunning(taskId, inferTaskStage(taskType).name(), taskStartTime);
            log.info("同步执行任务开始，taskId={}, taskType={}", taskId, taskType);
            taskExecution.execute(taskId);
            log.info("同步执行任务完成，taskId={}, taskType={}, elapsedMs={}",
                    taskId, taskType, System.currentTimeMillis() - startedAt);
            return taskQueryAppService.queryTask(taskId);
        } catch (Exception exception) {
            taskGateway.markFailed(taskId, exception.getMessage());
            log.error("同步执行任务失败，taskId={}, taskType={}, elapsedMs={}",
                    taskId, taskType, System.currentTimeMillis() - startedAt, exception);
            throw new IllegalStateException("执行任务失败，taskType=" + taskType + ", taskId=" + taskId, exception);
        }
    }

    private Long createTask(TaskType taskType, String businessKey, Long fileId, Object command) {
        try {
            TaskInfo taskInfo = TaskInfo.builder()
                    .taskType(taskType)
                    .taskStatus(TaskStatus.PENDING)
                    .taskStage(inferTaskStage(taskType))
                    .businessKey(businessKey)
                    .fileId(fileId)
                    .inputPayload(objectMapper.writeValueAsString(command))
                    .build();
            return taskGateway.save(taskInfo);
        } catch (Exception exception) {
            throw new IllegalStateException("创建任务失败，taskType=" + taskType, exception);
        }
    }

    /**
     * 根据任务类型推导当前任务阶段。
     */
    private TaskStage inferTaskStage(TaskType taskType) {
        if (taskType == null) {
            return TaskStage.OTHER;
        }
        return switch (taskType) {
            case EXTRACT_DATA -> TaskStage.EXTRACT;
            case PARSE_WORKBOOK -> TaskStage.PARSE;
            case MATCH_SUBJECT -> TaskStage.MATCH;
            default -> TaskStage.OTHER;
        };
    }

    private Long extractFileId(TaskViewDTO extractTask) {
        Map<String, Object> resultData = extractTask.getResultData();
        if (resultData != null) {
            Object fileId = resultData.get("fileId");
            if (fileId instanceof Number number) {
                return number.longValue();
            }
            if (fileId instanceof String text && !text.isBlank()) {
                return Long.parseLong(text);
            }
        }
        return extractTask.getTaskId();
    }

    private String buildExtractBusinessKey(ExtractDataTaskCommand command) {
        return String.join(":",
                "WORKFLOW",
                "EXTRACT",
                normalizeDataSourceType(command.getDataSourceType()),
                resolveExtractFileFingerprint(command));
    }

    private ValsetFileInfo registerUploadedFile(StoredFileDTO storedFile, String createdBy) {
        LocalDateTime now = LocalDateTime.now();
        ValsetFileInfo existingFile = subjectMatchFileInfoGateway.findByFingerprint(storedFile.getFileFingerprint());
        if (existingFile != null) {
            uploadedFileStorageService.deleteStoredFile(storedFile.getAbsolutePath());
            subjectMatchFileIngestLogGateway.save(ValsetFileIngestLog.builder()
                    .fileId(existingFile.getFileId())
                    .sourceChannel(ValsetFileSourceChannel.MANUAL_UPLOAD)
                    .sourceUri(storedFile.getAbsolutePath())
                    .channelMessageId(storedFile.getFileFingerprint())
                    .ingestStatus("REUSED")
                    .ingestTime(now)
                    .createdBy(createdBy)
                    .build());
            return existingFile;
        }

        ValsetFileInfo fileInfo = ValsetFileInfo.builder()
                .fileNameOriginal(storedFile.getOriginalFilename())
                .fileNameNormalized(normalizeFilename(storedFile.getOriginalFilename()))
                .fileExtension(resolveExtension(storedFile.getOriginalFilename()))
                .fileSizeBytes(storedFile.getFileSizeBytes())
                .fileFingerprint(storedFile.getFileFingerprint())
                .sourceChannel(ValsetFileSourceChannel.MANUAL_UPLOAD)
                .sourceUri(storedFile.getAbsolutePath())
                .storageType(ValsetFileStorageType.LOCAL)
                .storageUri(storedFile.getAbsolutePath())
                .fileFormat(normalizeDataSourceType(storedFile.getDataSourceType()))
                .fileStatus(ValsetFileStatus.READY_FOR_EXTRACT)
                .createdBy(createdBy)
                .receivedAt(now)
                .storedAt(now)
                .build();
        subjectMatchFileInfoGateway.save(fileInfo);
        subjectMatchFileIngestLogGateway.save(ValsetFileIngestLog.builder()
                .fileId(fileInfo.getFileId())
                .sourceChannel(ValsetFileSourceChannel.MANUAL_UPLOAD)
                .sourceUri(storedFile.getAbsolutePath())
                .channelMessageId(storedFile.getFileFingerprint())
                .ingestStatus("SUCCESS")
                .ingestTime(now)
                .createdBy(createdBy)
                .build());
        return fileInfo;
    }

    private String normalizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "valuation-file";
        }
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String resolveExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            return null;
        }
        return filename.substring(index + 1).toUpperCase(Locale.ROOT);
    }

    private String buildParseBusinessKey(ParseTaskCommand command) {
        return String.join(":",
                "WORKFLOW",
                "PARSE",
                normalizeDataSourceType(command.getDataSourceType()),
                String.valueOf(command.getFileId()));
    }

    private String buildMatchBusinessKey(MatchTaskCommand command) {
        return String.join(":",
                "WORKFLOW",
                "MATCH",
                normalizeDataSourceType(command.getDataSourceType()),
                String.valueOf(command.getFileId()),
                String.valueOf(command.getTopK() == null ? 5 : command.getTopK()));
    }

    private boolean isForceRebuild(Object command) {
        if (command instanceof ExtractDataTaskCommand extractDataTaskCommand) {
            return Boolean.TRUE.equals(extractDataTaskCommand.getForceRebuild());
        }
        if (command instanceof ParseTaskCommand parseTaskCommand) {
            return Boolean.TRUE.equals(parseTaskCommand.getForceRebuild());
        }
        if (command instanceof MatchTaskCommand matchTaskCommand) {
            return Boolean.TRUE.equals(matchTaskCommand.getForceRebuild());
        }
        return false;
    }

    private String resolveExtractFileFingerprint(ExtractDataTaskCommand command) {
        if (command.getFileFingerprint() != null && !command.getFileFingerprint().isBlank()) {
            return command.getFileFingerprint().trim().toLowerCase();
        }
        return command.getWorkbookPath();
    }

    private String normalizeDataSourceType(String dataSourceType) {
        if (dataSourceType == null || dataSourceType.isBlank()) {
            return "EXCEL";
        }
        return dataSourceType.trim().toUpperCase();
    }

    @FunctionalInterface
    private interface TaskExecution {
        void execute(Long taskId);
    }
}
