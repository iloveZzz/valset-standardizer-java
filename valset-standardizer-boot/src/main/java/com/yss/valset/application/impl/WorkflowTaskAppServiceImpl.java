package com.yss.valset.application.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.application.command.EvaluateMappingTaskCommand;
import com.yss.valset.extract.application.command.ExtractDataTaskCommand;
import com.yss.valset.application.command.MatchTaskCommand;
import com.yss.valset.application.command.ParseTaskCommand;
import com.yss.valset.application.dto.TaskCreateResponse;
import com.yss.valset.application.service.WorkflowTaskAppService;
import com.yss.valset.batch.scheduler.SchedulerService;
import com.yss.valset.domain.gateway.WorkflowTaskGateway;
import com.yss.valset.domain.model.WorkflowTask;
import com.yss.valset.domain.model.TaskStatus;
import com.yss.valset.domain.model.TaskType;
import com.yss.valset.application.support.WorkflowTaskReuseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 默认工作流任务创建服务。
 */
@Slf4j
@Service
public class WorkflowTaskAppServiceImpl implements WorkflowTaskAppService {

    private final WorkflowTaskGateway taskGateway;
    private final SchedulerService schedulerService;
    private final ObjectMapper objectMapper;
    private final WorkflowTaskReuseService taskReuseService;

    public WorkflowTaskAppServiceImpl(
            WorkflowTaskGateway taskGateway,
            SchedulerService schedulerService,
            ObjectMapper objectMapper,
            WorkflowTaskReuseService taskReuseService
    ) {
        this.taskGateway = taskGateway;
        this.schedulerService = schedulerService;
        this.objectMapper = objectMapper;
        this.taskReuseService = taskReuseService;
    }

    /**
     * 创建并分派解析任务。
     */
    @Override
    public TaskCreateResponse createParseTask(ParseTaskCommand command) {
        return createAndTrigger(TaskType.PARSE_WORKBOOK, command, buildParseBusinessKey(command), command.getFileId());
    }

    /**
     * 创建并分派匹配任务。
     */
    @Override
    public TaskCreateResponse createMatchTask(MatchTaskCommand command) {
        return createAndTrigger(TaskType.MATCH_SUBJECT, command, buildMatchBusinessKey(command), command.getFileId());
    }

    /**
     * 创建并分派评估任务。
     */
    @Override
    public TaskCreateResponse createEvaluateTask(EvaluateMappingTaskCommand command) {
        return createAndTrigger(TaskType.EVALUATE_MAPPING, command, buildEvaluateBusinessKey(command), null);
    }

    /**
     * 创建并分派原始数据提取任务。
     */
    @Override
    public TaskCreateResponse createExtractTask(ExtractDataTaskCommand command) {
        return createAndTrigger(TaskType.EXTRACT_DATA, command, buildExtractBusinessKey(command), command.getFileId());
    }

    /**
     * 持久化任务记录并立即触发。
     */
    private TaskCreateResponse createAndTrigger(TaskType taskType, Object command, String businessKey, Long fileId) {
        try {
            boolean forceRebuild = isForceRebuild(command);
            WorkflowTask reusableTask = taskReuseService.findReusableSuccessfulTask(taskType, businessKey, forceRebuild);
            if (reusableTask != null) {
                log.info("任务复用成功，taskType={}, businessKey={}, taskId={}", taskType, businessKey, reusableTask.getTaskId());
                return TaskCreateResponse.builder()
                        .taskId(reusableTask.getTaskId() == null ? null : String.valueOf(reusableTask.getTaskId()))
                        .taskType(reusableTask.getTaskType().name())
                        .taskStatus(reusableTask.getTaskStatus().name())
                        .businessKey(reusableTask.getBusinessKey())
                        .reusedExistingTask(Boolean.TRUE)
                        .build();
            }

            log.info("创建任务开始，taskType={}, businessKey={}", taskType, businessKey);
            WorkflowTask workflowTask = WorkflowTask.builder()
                    .taskType(taskType)
                    .taskStatus(TaskStatus.PENDING)
                    .businessKey(businessKey)
                    .fileId(fileId)
                    .inputPayload(objectMapper.writeValueAsString(command))
                    .build();
            Long taskId = taskGateway.save(workflowTask);
            schedulerService.triggerNow(taskId);
            log.info("任务创建并触发成功，taskId={}, taskType={}, businessKey={}", taskId, taskType, businessKey);
            return TaskCreateResponse.builder()
                    .taskId(taskId == null ? null : String.valueOf(taskId))
                    .taskType(taskType.name())
                    .taskStatus(TaskStatus.PENDING.name())
                    .businessKey(businessKey)
                    .reusedExistingTask(Boolean.FALSE)
                    .build();
        } catch (JsonProcessingException e) {
            log.error("任务入参序列化失败，taskType={}, businessKey={}", taskType, businessKey, e);
            throw new IllegalStateException("Failed to serialize task command", e);
        }
    }

    /**
     * 为解析任务构建可追踪的业务密钥。
     */
    private String buildParseBusinessKey(ParseTaskCommand command) {
        return String.join(":",
                "PARSE",
                normalizeDataSourceType(command.getDataSourceType()),
                command.getFileId() == null ? "NO_FILE_ID" : String.valueOf(command.getFileId()));
    }

    /**
     * 为匹配任务构建可追溯的业务密钥。
     */
    private String buildMatchBusinessKey(MatchTaskCommand command) {
        return String.join(":",
                "MATCH",
                normalizeDataSourceType(command.getDataSourceType()),
                command.getFileId() == null ? "NO_FILE_ID" : String.valueOf(command.getFileId()),
                String.valueOf(command.getTopK() == null ? 5 : command.getTopK()));
    }

    /**
     * 为评估任务构建可追溯的业务密钥。
     */
    private String buildEvaluateBusinessKey(EvaluateMappingTaskCommand command) {
        return String.join(":",
                "EVALUATE",
                command.getMappingWorkbookPath(),
                command.getStandardWorkbookPath(),
                command.getSplitMode() == null ? "org_holdout" : command.getSplitMode(),
                String.valueOf(command.getTopK() == null ? 5 : command.getTopK()));
    }

    /**
     * 为原始数据提取任务构建可追踪的业务密钥。
     */
    private String buildExtractBusinessKey(ExtractDataTaskCommand command) {
        String fileFingerprint = resolveExtractFileFingerprint(command);
        command.setFileFingerprint(fileFingerprint);
        return buildExtractBusinessKey(command.getDataSourceType(), fileFingerprint);
    }

    private String buildExtractBusinessKey(String dataSourceType, String fileFingerprint) {
        return String.join(":",
                "EXTRACT",
                normalizeDataSourceType(dataSourceType),
                fileFingerprint);
    }

    private String resolveExtractFileFingerprint(ExtractDataTaskCommand command) {
        if (command.getFileFingerprint() != null && !command.getFileFingerprint().isBlank()) {
            return command.getFileFingerprint().trim().toLowerCase();
        }
        Path workbookPath = Paths.get(command.getWorkbookPath());
        if (!Files.exists(workbookPath) || !Files.isReadable(workbookPath)) {
            throw new IllegalStateException("原始文件不存在或不可读，无法计算文件指纹: " + command.getWorkbookPath());
        }
        try (var inputStream = Files.newInputStream(workbookPath)) {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            try (var digestInputStream = new java.security.DigestInputStream(inputStream, digest)) {
                byte[] buffer = new byte[8192];
                while (digestInputStream.read(buffer) != -1) {
                    // 仅用于驱动摘要计算
                }
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (Exception exception) {
            throw new IllegalStateException("计算原始文件指纹失败", exception);
        }
    }

    private boolean isForceRebuild(Object command) {
        if (command instanceof ParseTaskCommand parseTaskCommand) {
            return Boolean.TRUE.equals(parseTaskCommand.getForceRebuild());
        }
        if (command instanceof MatchTaskCommand matchTaskCommand) {
            return Boolean.TRUE.equals(matchTaskCommand.getForceRebuild());
        }
        if (command instanceof ExtractDataTaskCommand extractDataTaskCommand) {
            return Boolean.TRUE.equals(extractDataTaskCommand.getForceRebuild());
        }
        return false;
    }

    private String normalizeDataSourceType(String dataSourceType) {
        if (dataSourceType == null || dataSourceType.isBlank()) {
            return "EXCEL";
        }
        return dataSourceType.trim().toUpperCase();
    }
}
