package com.yss.subjectmatch.application.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.subjectmatch.application.command.ExtractDataTaskCommand;
import com.yss.subjectmatch.application.command.MatchTaskCommand;
import com.yss.subjectmatch.application.command.ParseTaskCommand;
import com.yss.subjectmatch.application.dto.FullWorkflowResponse;
import com.yss.subjectmatch.application.dto.StoredFileDTO;
import com.yss.subjectmatch.application.dto.TaskViewDTO;
import com.yss.subjectmatch.application.dto.UploadValuationFileResponse;
import com.yss.subjectmatch.application.port.ExtractDataExecutionUseCase;
import com.yss.subjectmatch.application.port.MatchExecutionUseCase;
import com.yss.subjectmatch.application.port.ParseExecutionUseCase;
import com.yss.subjectmatch.application.service.TaskQueryAppService;
import com.yss.subjectmatch.application.service.ValuationWorkflowAppService;
import com.yss.subjectmatch.domain.gateway.SubjectMatchFileInfoGateway;
import com.yss.subjectmatch.domain.gateway.SubjectMatchFileIngestLogGateway;
import com.yss.subjectmatch.application.support.UploadedFileStorageService;
import com.yss.subjectmatch.application.support.TaskReuseService;
import com.yss.subjectmatch.domain.gateway.TaskGateway;
import com.yss.subjectmatch.domain.model.SubjectMatchFileInfo;
import com.yss.subjectmatch.domain.model.SubjectMatchFileIngestLog;
import com.yss.subjectmatch.domain.model.SubjectMatchFileSourceChannel;
import com.yss.subjectmatch.domain.model.SubjectMatchFileStatus;
import com.yss.subjectmatch.domain.model.SubjectMatchFileStorageType;
import com.yss.subjectmatch.domain.model.TaskInfo;
import com.yss.subjectmatch.domain.model.TaskStatus;
import com.yss.subjectmatch.domain.model.TaskStage;
import com.yss.subjectmatch.domain.model.TaskType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;

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
    private final SubjectMatchFileInfoGateway subjectMatchFileInfoGateway;
    private final SubjectMatchFileIngestLogGateway subjectMatchFileIngestLogGateway;
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
                                              SubjectMatchFileInfoGateway subjectMatchFileInfoGateway,
                                              SubjectMatchFileIngestLogGateway subjectMatchFileIngestLogGateway) {
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
    }

    @Override
    public UploadValuationFileResponse uploadAndExtract(MultipartFile file, String dataSourceType, String createdBy, Boolean forceRebuild) {
        StoredFileDTO storedFile = uploadedFileStorageService.store(file, dataSourceType);
        SubjectMatchFileInfo fileInfo = registerUploadedFile(storedFile, createdBy);
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
                SubjectMatchFileStatus.EXTRACTED,
                extractTask.getTaskId(),
                LocalDateTime.now(),
                null
        );
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
        TaskViewDTO taskViewDTO = runTask(
                TaskType.PARSE_WORKBOOK,
                buildParseBusinessKey(command),
                command.getFileId(),
                command,
                parseExecutionUseCase::execute
        );
        subjectMatchFileInfoGateway.updateStatus(
                command.getFileId(),
                SubjectMatchFileStatus.PARSED,
                taskViewDTO.getTaskId(),
                LocalDateTime.now(),
                null
        );
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
                SubjectMatchFileStatus.MATCHED,
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
        UploadValuationFileResponse uploadResponse = uploadAndExtract(file, dataSourceType, createdBy, forceRebuild);

        ParseTaskCommand parseTaskCommand = new ParseTaskCommand();
        parseTaskCommand.setDataSourceType(uploadResponse.getDataSourceType());
        parseTaskCommand.setWorkbookPath(uploadResponse.getWorkbookPath());
        parseTaskCommand.setFileId(uploadResponse.getFileId());
        parseTaskCommand.setCreatedBy(createdBy);
        parseTaskCommand.setForceRebuild(Boolean.TRUE.equals(forceRebuild));
        TaskViewDTO parseTask = analyze(parseTaskCommand);

        MatchTaskCommand matchTaskCommand = new MatchTaskCommand();
        matchTaskCommand.setDataSourceType(uploadResponse.getDataSourceType());
        matchTaskCommand.setWorkbookPath(uploadResponse.getWorkbookPath());
        matchTaskCommand.setFileId(uploadResponse.getFileId());
        matchTaskCommand.setTopK(topK == null ? 5 : topK);
        matchTaskCommand.setCreatedBy(createdBy);
        matchTaskCommand.setForceRebuild(Boolean.TRUE.equals(forceRebuild));
        TaskViewDTO matchTask = enableMatchProcess
                ? match(matchTaskCommand)
                : buildSkippedMatchTask(matchTaskCommand, "subject.match.workflow.enable-match-process=false");

        return FullWorkflowResponse.builder()
                .fileId(uploadResponse.getFileId())
                .workbookPath(uploadResponse.getWorkbookPath())
                .dataSourceType(uploadResponse.getDataSourceType())
                .fileFingerprint(uploadResponse.getFileFingerprint())
                .extractTask(uploadResponse.getExtractTask())
                .parseTask(parseTask)
                .matchTask(matchTask)
                .build();
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
        Long taskId = createTask(taskType, businessKey, fileId, command);
        try {
            LocalDateTime taskStartTime = LocalDateTime.now();
            taskGateway.markRunning(taskId, inferTaskStage(taskType).name(), taskStartTime);
            log.info("同步执行任务开始，taskId={}, taskType={}", taskId, taskType);
            taskExecution.execute(taskId);
            return taskQueryAppService.queryTask(taskId);
        } catch (Exception exception) {
            taskGateway.markFailed(taskId, exception.getMessage());
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

    private SubjectMatchFileInfo registerUploadedFile(StoredFileDTO storedFile, String createdBy) {
        LocalDateTime now = LocalDateTime.now();
        SubjectMatchFileInfo existingFile = subjectMatchFileInfoGateway.findByFingerprint(storedFile.getFileFingerprint());
        if (existingFile != null) {
            uploadedFileStorageService.deleteStoredFile(storedFile.getAbsolutePath());
            subjectMatchFileIngestLogGateway.save(SubjectMatchFileIngestLog.builder()
                    .fileId(existingFile.getFileId())
                    .sourceChannel(SubjectMatchFileSourceChannel.MANUAL_UPLOAD)
                    .sourceUri(storedFile.getAbsolutePath())
                    .channelMessageId(storedFile.getFileFingerprint())
                    .ingestStatus("REUSED")
                    .ingestTime(now)
                    .createdBy(createdBy)
                    .build());
            return existingFile;
        }

        SubjectMatchFileInfo fileInfo = SubjectMatchFileInfo.builder()
                .fileNameOriginal(storedFile.getOriginalFilename())
                .fileNameNormalized(normalizeFilename(storedFile.getOriginalFilename()))
                .fileExtension(resolveExtension(storedFile.getOriginalFilename()))
                .fileSizeBytes(storedFile.getFileSizeBytes())
                .fileFingerprint(storedFile.getFileFingerprint())
                .sourceChannel(SubjectMatchFileSourceChannel.MANUAL_UPLOAD)
                .sourceUri(storedFile.getAbsolutePath())
                .storageType(SubjectMatchFileStorageType.LOCAL)
                .storageUri(storedFile.getAbsolutePath())
                .fileFormat(normalizeDataSourceType(storedFile.getDataSourceType()))
                .fileStatus(SubjectMatchFileStatus.READY_FOR_EXTRACT)
                .createdBy(createdBy)
                .receivedAt(now)
                .storedAt(now)
                .build();
        subjectMatchFileInfoGateway.save(fileInfo);
        subjectMatchFileIngestLogGateway.save(SubjectMatchFileIngestLog.builder()
                .fileId(fileInfo.getFileId())
                .sourceChannel(SubjectMatchFileSourceChannel.MANUAL_UPLOAD)
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
