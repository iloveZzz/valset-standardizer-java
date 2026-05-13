package com.yss.valset.application.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.application.command.EvaluateMappingTaskCommand;
import com.yss.valset.extract.application.command.ExtractDataTaskCommand;
import com.yss.valset.application.command.MatchTaskCommand;
import com.yss.valset.application.command.ParseTaskCommand;
import com.yss.valset.application.dto.TaskCreateResponse;
import com.yss.valset.application.dto.workflow.WorkflowExecutionContextDTO;
import com.yss.valset.application.service.WorkflowTaskAppService;
import com.yss.valset.application.service.workflow.WorkflowExecutionContextResolver;
import com.yss.valset.application.support.WorkflowBusinessContextBuilder;
import com.yss.valset.application.support.WorkflowCommonContextBuilder;
import com.yss.valset.application.support.WorkflowContextEnvelopeBuilder;
import com.yss.valset.task.application.service.workflow.WorkflowEngineDispatchService;
import com.yss.valset.batch.scheduler.SchedulerService;
import com.yss.valset.domain.gateway.WorkflowTaskGateway;
import com.yss.valset.domain.model.WorkflowTask;
import com.yss.valset.domain.model.TaskStatus;
import com.yss.valset.domain.model.TaskStage;
import com.yss.valset.domain.model.TaskType;
import com.yss.valset.application.support.WorkflowTaskReuseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

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
    private final WorkflowExecutionContextResolver workflowExecutionContextResolver;
    private final WorkflowCommonContextBuilder workflowCommonContextBuilder;
    private final WorkflowBusinessContextBuilder workflowBusinessContextBuilder;
    private final WorkflowContextEnvelopeBuilder workflowContextEnvelopeBuilder;
    private WorkflowEngineDispatchService workflowEngineDispatchService;

    public WorkflowTaskAppServiceImpl(
            WorkflowTaskGateway taskGateway,
            SchedulerService schedulerService,
            ObjectMapper objectMapper,
            WorkflowTaskReuseService taskReuseService,
            WorkflowExecutionContextResolver workflowExecutionContextResolver,
            WorkflowCommonContextBuilder workflowCommonContextBuilder,
            WorkflowBusinessContextBuilder workflowBusinessContextBuilder,
            WorkflowContextEnvelopeBuilder workflowContextEnvelopeBuilder
    ) {
        this.taskGateway = taskGateway;
        this.schedulerService = schedulerService;
        this.objectMapper = objectMapper;
        this.taskReuseService = taskReuseService;
        this.workflowExecutionContextResolver = workflowExecutionContextResolver;
        this.workflowCommonContextBuilder = workflowCommonContextBuilder;
        this.workflowBusinessContextBuilder = workflowBusinessContextBuilder;
        this.workflowContextEnvelopeBuilder = workflowContextEnvelopeBuilder;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void setWorkflowEngineDispatchService(WorkflowEngineDispatchService workflowEngineDispatchService) {
        this.workflowEngineDispatchService = workflowEngineDispatchService;
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
     * 创建并分派文件解析任务。
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
            WorkflowExecutionContextDTO executionContext = workflowExecutionContextResolver.resolve(taskType, inferTaskStage(taskType));
            enrichWorkflowContext(command, executionContext);
            enrichCommonContext(command, executionContext);
            enrichBusinessContext(command, executionContext);
            if (executionContext != null && Boolean.TRUE.equals(executionContext.getBindingResolved())
                    && executionContext.getEngineType() != null
                    && !"INTERNAL".equalsIgnoreCase(executionContext.getEngineType())) {
                log.warn("当前工作流绑定了非 INTERNAL 的执行配置，任务仍回退到估值内部流程执行，taskType={}, engineType={}, stageCode={}",
                        taskType,
                        executionContext.getEngineType(),
                        executionContext.getWorkflowStageCode());
            }
            WorkflowTask workflowTask = WorkflowTask.builder()
                    .taskType(taskType)
                    .taskStatus(TaskStatus.PENDING)
                    .businessKey(businessKey)
                    .fileId(fileId)
                    .inputPayload(objectMapper.writeValueAsString(command))
                    .build();
            Long taskId = taskGateway.save(workflowTask);
            triggerWorkflowTask(taskId, taskType, executionContext);
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
     * 为文件解析任务构建可追踪的业务密钥。
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
        if (command.getFileFingerprint() != null && !command.getFileFingerprint().trim().isEmpty()) {
            return command.getFileFingerprint().trim().toLowerCase();
        }
        Path workbookPath = Paths.get(command.getWorkbookPath());
        if (!Files.exists(workbookPath) || !Files.isReadable(workbookPath)) {
            throw new IllegalStateException("原始文件不存在或不可读，无法计算文件指纹: " + command.getWorkbookPath());
        }
        try (java.io.InputStream inputStream = Files.newInputStream(workbookPath)) {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            try (java.security.DigestInputStream digestInputStream = new java.security.DigestInputStream(inputStream, digest)) {
                byte[] buffer = new byte[8192];
                while (digestInputStream.read(buffer) != -1) {
                    // 仅用于驱动摘要计算
                }
            }
            return toHex(digest.digest());
        } catch (Exception exception) {
            throw new IllegalStateException("计算原始文件指纹失败", exception);
        }
    }

    private boolean isForceRebuild(Object command) {
        if (command instanceof ParseTaskCommand) {
            return Boolean.TRUE.equals(((ParseTaskCommand) command).getForceRebuild());
        }
        if (command instanceof MatchTaskCommand) {
            return Boolean.TRUE.equals(((MatchTaskCommand) command).getForceRebuild());
        }
        if (command instanceof ExtractDataTaskCommand) {
            return Boolean.TRUE.equals(((ExtractDataTaskCommand) command).getForceRebuild());
        }
        return false;
    }

    private void enrichWorkflowContext(Object command, WorkflowExecutionContextDTO executionContext) {
        if (command == null || executionContext == null) {
            return;
        }
        if (command instanceof ParseTaskCommand) {
            applyWorkflowContext((ParseTaskCommand) command, executionContext);
            return;
        }
        if (command instanceof MatchTaskCommand) {
            applyWorkflowContext((MatchTaskCommand) command, executionContext);
            return;
        }
        if (command instanceof EvaluateMappingTaskCommand) {
            applyWorkflowContext((EvaluateMappingTaskCommand) command, executionContext);
            return;
        }
        if (command instanceof ExtractDataTaskCommand) {
            applyWorkflowContext((ExtractDataTaskCommand) command, executionContext);
        }
    }

    private void enrichCommonContext(Object command, WorkflowExecutionContextDTO executionContext) {
        if (command == null || executionContext == null) {
            return;
        }
        Map<String, Object> commonContext = buildCommonContext(command);
        executionContext.setCommonContext(commonContext);
    }

    private void enrichBusinessContext(Object command, WorkflowExecutionContextDTO executionContext) {
        if (command == null || executionContext == null) {
            return;
        }
        Map<String, Object> commonContext = executionContext.getCommonContext();
        Map<String, Object> businessContext = buildBusinessContext(command);
        executionContext.setBusinessContext(businessContext);
        executionContext.setBusinessContextJson(writeWorkflowContextJson(commonContext, businessContext));
        if (command instanceof ParseTaskCommand) {
            ((ParseTaskCommand) command).setWorkflowEngineConfigJson(executionContext.getBusinessContextJson());
            return;
        }
        if (command instanceof MatchTaskCommand) {
            ((MatchTaskCommand) command).setWorkflowEngineConfigJson(executionContext.getBusinessContextJson());
            return;
        }
        if (command instanceof ExtractDataTaskCommand) {
            ((ExtractDataTaskCommand) command).setWorkflowEngineConfigJson(executionContext.getBusinessContextJson());
        }
    }

    private Map<String, Object> buildCommonContext(Object command) {
        if (command instanceof ParseTaskCommand) {
            return workflowCommonContextBuilder.build((ParseTaskCommand) command);
        }
        if (command instanceof MatchTaskCommand) {
            return workflowCommonContextBuilder.build((MatchTaskCommand) command);
        }
        if (command instanceof EvaluateMappingTaskCommand) {
            return workflowCommonContextBuilder.build((EvaluateMappingTaskCommand) command);
        }
        if (command instanceof ExtractDataTaskCommand) {
            return workflowCommonContextBuilder.build((ExtractDataTaskCommand) command);
        }
        return new java.util.LinkedHashMap<>();
    }

    private Map<String, Object> buildBusinessContext(Object command) {
        if (command instanceof ParseTaskCommand) {
            return workflowBusinessContextBuilder.build((ParseTaskCommand) command);
        }
        if (command instanceof MatchTaskCommand) {
            return workflowBusinessContextBuilder.build((MatchTaskCommand) command);
        }
        if (command instanceof EvaluateMappingTaskCommand) {
            return workflowBusinessContextBuilder.build((EvaluateMappingTaskCommand) command);
        }
        if (command instanceof ExtractDataTaskCommand) {
            return workflowBusinessContextBuilder.build((ExtractDataTaskCommand) command);
        }
        return new java.util.LinkedHashMap<>();
    }

    private String toHex(byte[] bytes) {
        char[] digits = "0123456789abcdef".toCharArray();
        char[] result = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xFF;
            result[i * 2] = digits[value >>> 4];
            result[i * 2 + 1] = digits[value & 0x0F];
        }
        return new String(result);
    }

    private String writeWorkflowContextJson(Map<String, Object> commonContext, Map<String, Object> businessContext) {
        return workflowContextEnvelopeBuilder.buildEnvelopeJson(commonContext, businessContext);
    }

    private void applyWorkflowContext(ParseTaskCommand command, WorkflowExecutionContextDTO executionContext) {
        command.setWorkflowCode(executionContext.getWorkflowCode());
        command.setWorkflowId(executionContext.getWorkflowId());
        command.setWorkflowVersionNo(executionContext.getWorkflowVersionNo());
        command.setWorkflowStageCode(executionContext.getWorkflowStageCode());
        command.setWorkflowStageName(executionContext.getWorkflowStageName());
        command.setWorkflowEngineType(executionContext.getEngineType());
        command.setWorkflowEngineExternalRef(executionContext.getExternalRef());
        command.setWorkflowEngineConfigJson(executionContext.getConfigJson());
    }

    private void applyWorkflowContext(MatchTaskCommand command, WorkflowExecutionContextDTO executionContext) {
        command.setWorkflowCode(executionContext.getWorkflowCode());
        command.setWorkflowId(executionContext.getWorkflowId());
        command.setWorkflowVersionNo(executionContext.getWorkflowVersionNo());
        command.setWorkflowStageCode(executionContext.getWorkflowStageCode());
        command.setWorkflowStageName(executionContext.getWorkflowStageName());
        command.setWorkflowEngineType(executionContext.getEngineType());
        command.setWorkflowEngineExternalRef(executionContext.getExternalRef());
        command.setWorkflowEngineConfigJson(executionContext.getConfigJson());
    }

    private void applyWorkflowContext(EvaluateMappingTaskCommand command, WorkflowExecutionContextDTO executionContext) {
        command.setWorkflowCode(executionContext.getWorkflowCode());
        command.setWorkflowId(executionContext.getWorkflowId());
        command.setWorkflowVersionNo(executionContext.getWorkflowVersionNo());
        command.setWorkflowStageCode(executionContext.getWorkflowStageCode());
        command.setWorkflowStageName(executionContext.getWorkflowStageName());
        command.setWorkflowEngineType(executionContext.getEngineType());
        command.setWorkflowEngineExternalRef(executionContext.getExternalRef());
        command.setWorkflowEngineConfigJson(executionContext.getConfigJson());
    }

    private void applyWorkflowContext(ExtractDataTaskCommand command, WorkflowExecutionContextDTO executionContext) {
        command.setWorkflowCode(executionContext.getWorkflowCode());
        command.setWorkflowId(executionContext.getWorkflowId());
        command.setWorkflowVersionNo(executionContext.getWorkflowVersionNo());
        command.setWorkflowStageCode(executionContext.getWorkflowStageCode());
        command.setWorkflowStageName(executionContext.getWorkflowStageName());
        command.setWorkflowEngineType(executionContext.getEngineType());
        command.setWorkflowEngineExternalRef(executionContext.getExternalRef());
        command.setWorkflowEngineConfigJson(executionContext.getConfigJson());
    }

    private void triggerWorkflowTask(Long taskId, TaskType taskType, WorkflowExecutionContextDTO executionContext) {
        String stageCode = executionContext == null || executionContext.getWorkflowStageCode() == null
                ? inferTaskStage(taskType).name()
                : executionContext.getWorkflowStageCode();
        if (workflowEngineDispatchService != null) {
            workflowEngineDispatchService.trigger(taskId, stageCode, executionContext);
            return;
        }
        if (schedulerService == null) {
            throw new IllegalStateException("调度器未启用，无法触发工作流任务：" + taskId);
        }
        schedulerService.triggerNow(taskId);
    }

    private String normalizeDataSourceType(String dataSourceType) {
        if (dataSourceType == null || dataSourceType.trim().isEmpty()) {
            return "EXCEL";
        }
        return dataSourceType.trim().toUpperCase();
    }

    private TaskStage inferTaskStage(TaskType taskType) {
        if (taskType == null) {
            return TaskStage.OTHER;
        }
        switch (taskType) {
            case EXTRACT_DATA:
                return TaskStage.EXTRACT;
            case PARSE_WORKBOOK:
                return TaskStage.PARSE;
            case MATCH_SUBJECT:
                return TaskStage.MATCH;
            default:
                return TaskStage.OTHER;
        }
    }
}
