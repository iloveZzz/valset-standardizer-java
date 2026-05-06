package com.yss.valset.workflow.springbatch;

import com.yss.valset.workflow.model.WorkflowDefinitionDTO;
import com.yss.valset.workflow.model.WorkflowInstanceDTO;
import com.yss.valset.workflow.model.WorkflowPlatformCommand;
import com.yss.valset.workflow.model.WorkflowStageDTO;
import com.yss.valset.workflow.model.WorkflowStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 默认 Spring Batch 阶段处理器。
 */
@Component
public class DefaultSpringBatchStageProcessor implements SpringBatchStageProcessor {

    @Override
    public SpringBatchStageExecutionResult process(WorkflowDefinitionDTO definition,
                                                   WorkflowInstanceDTO instance,
                                                   WorkflowStageDTO stage,
                                                   WorkflowPlatformCommand command) {
        LocalDateTime startTime = LocalDateTime.now();
        String normalizedStageCode = resolveStageCode(stage, command);
        String businessStage = resolveBusinessStage(normalizedStageCode);
        Map<String, Object> context = resolveContext(command);
        Map<String, Object> input = buildInput(definition, instance, stage, command);
        Map<String, Object> output = buildOutput(definition, instance, stage, command, context, businessStage, normalizedStageCode, startTime);
        LocalDateTime endTime = LocalDateTime.now();
        return SpringBatchStageExecutionResult.builder()
                .status(resolveStatus(stage))
                .rawStatus(resolveStatus(stage).name())
                .message(resolveMessage(stage, businessStage))
                .startTime(startTime)
                .endTime(endTime)
                .input(input)
                .output(output)
                .metadata(buildMetadata(definition, instance, stage, command, context, businessStage, normalizedStageCode))
                .build();
    }

    private Map<String, Object> buildInput(WorkflowDefinitionDTO definition,
                                           WorkflowInstanceDTO instance,
                                           WorkflowStageDTO stage,
                                           WorkflowPlatformCommand command) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("workflowCode", definition == null ? null : definition.getWorkflowCode());
        input.put("workflowVersionNo", definition == null ? null : definition.getWorkflowVersionNo());
        input.put("workflowName", definition == null ? null : definition.getWorkflowName());
        input.put("businessKey", instance == null ? null : instance.getBusinessKey());
        input.put("instanceId", instance == null ? null : instance.getInstanceId());
        input.put("externalWorkflowId", command == null ? null : command.getExternalWorkflowId());
        input.put("externalInstanceId", command == null ? null : command.getExternalInstanceId());
        input.put("platformType", command == null || command.getPlatformType() == null ? null : command.getPlatformType().name());
        input.put("stageCode", stage == null ? null : stage.getStageCode());
        input.put("stageName", stage == null ? null : stage.getStageName());
        input.put("stageOrder", stage == null ? null : stage.getStageOrder());
        input.put("retryable", stage != null && stage.isRetryable());
        input.put("timeoutSeconds", stage == null ? null : stage.getTimeoutSeconds());
        input.put("context", command == null ? Map.of() : command.getContext());
        input.put("parameters", command == null ? Map.of() : command.getParameters());
        return input;
    }

    private Map<String, Object> buildMetadata(WorkflowDefinitionDTO definition,
                                              WorkflowInstanceDTO instance,
                                              WorkflowStageDTO stage,
                                              WorkflowPlatformCommand command,
                                              Map<String, Object> context,
                                              String businessStage,
                                              String normalizedStageCode) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("workflowKey", buildWorkflowKey(definition));
        metadata.put("businessStage", businessStage);
        metadata.put("normalizedStageCode", normalizedStageCode);
        metadata.put("description", stage == null ? null : stage.getDescription());
        metadata.put("retryable", stage != null && stage.isRetryable());
        metadata.put("timeoutSeconds", stage == null ? null : stage.getTimeoutSeconds());
        metadata.put("instanceId", instance == null ? null : instance.getInstanceId());
        metadata.put("jobName", definition == null ? null : definition.getWorkflowCode() + "-v" + definition.getWorkflowVersionNo());
        metadata.put("businessKey", instance == null ? null : instance.getBusinessKey());
        metadata.put("commandType", command == null || command.getOperationType() == null ? null : command.getOperationType().name());
        metadata.put("dataSourceType", resolveText(context, "dataSourceType", "sourceType", "workflowDataSourceType"));
        metadata.put("fileId", resolveNumber(context, "fileId", "sourceFileId"));
        metadata.put("createdBy", resolveText(context, "createdBy", "operator"));
        metadata.put("forceRebuild", resolveBoolean(context, "forceRebuild", "force"));
        metadata.put("topK", resolveNumber(context, "topK", "candidateTopK"));
        metadata.put("splitMode", resolveText(context, "splitMode"));
        metadata.put("sourcePath", resolveText(context, "workbookPath", "filePath", "sourcePath"));
        metadata.put("targetTables", resolveTargetTables(normalizedStageCode));
        return metadata;
    }

    private Map<String, Object> buildOutput(WorkflowDefinitionDTO definition,
                                            WorkflowInstanceDTO instance,
                                            WorkflowStageDTO stage,
                                            WorkflowPlatformCommand command,
                                            Map<String, Object> context,
                                            String businessStage,
                                            String normalizedStageCode,
                                            LocalDateTime startTime) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("processedAt", startTime.toString());
        output.put("businessStage", businessStage);
        output.put("stageFamily", resolveStageFamily(normalizedStageCode));
        output.put("result", resolveResult(normalizedStageCode));
        output.put("platform", command == null || command.getPlatformType() == null ? null : command.getPlatformType().name());
        output.put("operationType", command == null || command.getOperationType() == null ? null : command.getOperationType().name());
        output.put("workflowKey", buildWorkflowKey(definition));
        output.put("businessKey", instance == null ? null : instance.getBusinessKey());
        output.put("contextEcho", sanitizeContext(context));
        output.put("stagePlan", resolveStagePlan(normalizedStageCode, context));
        output.put("qualityChecks", resolveQualityChecks(normalizedStageCode, context));
        output.put("targetTables", resolveTargetTables(normalizedStageCode));
        output.put("sourceSummary", resolveSourceSummary(context));
        output.put("traceSummary", resolveTraceSummary(definition, instance, stage, command, normalizedStageCode));
        output.put("stageCompleted", Boolean.TRUE);
        return output;
    }

    private WorkflowStatus resolveStatus(WorkflowStageDTO stage) {
        return WorkflowStatus.SUCCEEDED;
    }

    private String resolveResult(String normalizedStageCode) {
        if (!StringUtils.hasText(normalizedStageCode)) {
            return "DEFAULT";
        }
        return normalizedStageCode;
    }

    private String resolveMessage(WorkflowStageDTO stage, String businessStage) {
        if (StringUtils.hasText(businessStage)) {
            return switch (businessStage) {
                case "FILE_PARSE" -> "原始文件抽取与解析已完成";
                case "STRUCTURE_STANDARDIZE" -> "字段映射与结构标准化已完成";
                case "SUBJECT_RECOGNIZE" -> "科目识别与标签补全已完成";
                case "STANDARD_LANDING" -> "标准表落地已完成";
                case "VERIFY_ARCHIVE" -> "一致性校验与归档已完成";
                case "DATA_PROCESSING" -> "后续加工任务已完成";
                default -> "阶段已完成";
            };
        }
        if (stage == null || !StringUtils.hasText(stage.getDescription())) {
            return "阶段已完成";
        }
        return stage.getDescription();
    }

    private String resolveStageCode(WorkflowStageDTO stage, WorkflowPlatformCommand command) {
        if (stage != null && StringUtils.hasText(stage.getStageCode())) {
            return stage.getStageCode().trim().toUpperCase();
        }
        if (command != null && StringUtils.hasText(command.getStageCode())) {
            return command.getStageCode().trim().toUpperCase();
        }
        return null;
    }

    private String resolveBusinessStage(String normalizedStageCode) {
        if (!StringUtils.hasText(normalizedStageCode)) {
            return "FILE_PARSE";
        }
        return switch (normalizedStageCode) {
            case "RAW_DATA_EXTRACT", "FILE_PARSE" -> "FILE_PARSE";
            case "STRUCTURE_STANDARDIZE" -> "STRUCTURE_STANDARDIZE";
            case "SUBJECT_RECOGNIZE" -> "SUBJECT_RECOGNIZE";
            case "STANDARD_LANDING" -> "STANDARD_LANDING";
            case "DATA_PROCESSING" -> "DATA_PROCESSING";
            case "VERIFY_ARCHIVE" -> "VERIFY_ARCHIVE";
            default -> "FILE_PARSE";
        };
    }

    private String resolveStageFamily(String normalizedStageCode) {
        if (!StringUtils.hasText(normalizedStageCode)) {
            return "EXTRACT";
        }
        return switch (resolveBusinessStage(normalizedStageCode)) {
            case "FILE_PARSE" -> "EXTRACT";
            case "STRUCTURE_STANDARDIZE" -> "STANDARDIZE";
            case "SUBJECT_RECOGNIZE" -> "RECOGNIZE";
            case "STANDARD_LANDING" -> "LANDING";
            case "DATA_PROCESSING" -> "PROCESS";
            case "VERIFY_ARCHIVE" -> "ARCHIVE";
            default -> "EXTRACT";
        };
    }

    private List<String> resolveTargetTables(String normalizedStageCode) {
        String businessStage = resolveBusinessStage(normalizedStageCode);
        return switch (businessStage) {
            case "FILE_PARSE" -> List.of("t_ods_valuation_filedata");
            case "STRUCTURE_STANDARDIZE" -> List.of("t_stg_external_valuation", "t_stg_external_valuation_detail");
            case "SUBJECT_RECOGNIZE" -> List.of("t_subject_match_result");
            case "STANDARD_LANDING" -> List.of("t_dwd_external_valuation", "t_dwd_external_valuation_detail");
            case "DATA_PROCESSING" -> List.of("t_valset_parse_task", "t_valset_parse_task_step");
            case "VERIFY_ARCHIVE" -> List.of("t_valset_parse_task_log");
            default -> List.of();
        };
    }

    private Map<String, Object> resolveStagePlan(String normalizedStageCode, Map<String, Object> context) {
        Map<String, Object> plan = new LinkedHashMap<>();
        String businessStage = resolveBusinessStage(normalizedStageCode);
        plan.put("businessStage", businessStage);
        plan.put("stageFamily", resolveStageFamily(normalizedStageCode));
        plan.put("summary", resolveMessage(null, businessStage));
        plan.put("sourceType", resolveText(context, "dataSourceType", "sourceType", "workflowDataSourceType"));
        plan.put("sourcePath", resolveText(context, "workbookPath", "filePath", "sourcePath"));
        plan.put("fileId", resolveNumber(context, "fileId", "sourceFileId"));
        plan.put("createdBy", resolveText(context, "createdBy", "operator"));
        plan.put("forceRebuild", resolveBoolean(context, "forceRebuild", "force"));
        plan.put("topK", resolveNumber(context, "topK", "candidateTopK"));
        plan.put("targetTables", resolveTargetTables(normalizedStageCode));
        return plan;
    }

    private Map<String, Object> resolveQualityChecks(String normalizedStageCode, Map<String, Object> context) {
        Map<String, Object> checks = new LinkedHashMap<>();
        String businessStage = resolveBusinessStage(normalizedStageCode);
        checks.put("hasSource", StringUtils.hasText(resolveText(context, "workbookPath", "filePath", "sourcePath"))
                || resolveNumber(context, "fileId", "sourceFileId") != null);
        checks.put("hasStageCode", StringUtils.hasText(normalizedStageCode));
        checks.put("retryable", true);
        checks.put("auditLevel", switch (businessStage) {
            case "FILE_PARSE" -> "RAW";
            case "STRUCTURE_STANDARDIZE" -> "STAGING";
            case "SUBJECT_RECOGNIZE" -> "MATCH";
            case "STANDARD_LANDING" -> "DWD";
            case "DATA_PROCESSING" -> "PROCESS";
            case "VERIFY_ARCHIVE" -> "ARCHIVE";
            default -> "RAW";
        });
        return checks;
    }

    private Map<String, Object> resolveSourceSummary(Map<String, Object> context) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("dataSourceType", resolveText(context, "dataSourceType", "sourceType", "workflowDataSourceType"));
        summary.put("fileId", resolveNumber(context, "fileId", "sourceFileId"));
        summary.put("sourcePath", resolveText(context, "workbookPath", "filePath", "sourcePath"));
        summary.put("createdBy", resolveText(context, "createdBy", "operator"));
        summary.put("forceRebuild", resolveBoolean(context, "forceRebuild", "force"));
        summary.put("topK", resolveNumber(context, "topK", "candidateTopK"));
        summary.put("splitMode", resolveText(context, "splitMode"));
        return summary;
    }

    private Map<String, Object> resolveTraceSummary(WorkflowDefinitionDTO definition,
                                                    WorkflowInstanceDTO instance,
                                                    WorkflowStageDTO stage,
                                                    WorkflowPlatformCommand command,
                                                    String normalizedStageCode) {
        Map<String, Object> trace = new LinkedHashMap<>();
        trace.put("workflowCode", definition == null ? null : definition.getWorkflowCode());
        trace.put("workflowVersionNo", definition == null ? null : definition.getWorkflowVersionNo());
        trace.put("businessKey", instance == null ? null : instance.getBusinessKey());
        trace.put("stageCode", normalizedStageCode);
        trace.put("stageName", stage == null ? null : stage.getStageName());
        trace.put("operationType", command == null || command.getOperationType() == null ? null : command.getOperationType().name());
        trace.put("platformType", command == null || command.getPlatformType() == null ? null : command.getPlatformType().name());
        return trace;
    }

    private String buildWorkflowKey(WorkflowDefinitionDTO definition) {
        if (definition == null) {
            return null;
        }
        return definition.getWorkflowCode() + "-v" + definition.getWorkflowVersionNo();
    }

    private Map<String, Object> resolveContext(WorkflowPlatformCommand command) {
        Map<String, Object> context = new LinkedHashMap<>();
        if (command != null && command.getContext() != null) {
            command.getContext().forEach(context::put);
        }
        if (command != null && command.getParameters() != null) {
            command.getParameters().forEach((key, value) -> context.putIfAbsent(key, value));
        }
        return context;
    }

    private Map<String, Object> sanitizeContext(Map<String, Object> context) {
        Map<String, Object> snapshot = new TreeMap<>();
        if (context != null) {
            context.forEach((key, value) -> snapshot.put(key, value));
        }
        return snapshot;
    }

    private String resolveText(Map<String, Object> context, String... keys) {
        if (context == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            Object value = context.get(key);
            if (value != null && StringUtils.hasText(String.valueOf(value))) {
                return String.valueOf(value).trim();
            }
        }
        return null;
    }

    private Long resolveNumber(Map<String, Object> context, String... keys) {
        String text = resolveText(context, keys);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return Long.parseLong(text.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private Boolean resolveBoolean(Map<String, Object> context, String... keys) {
        String text = resolveText(context, keys);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return Boolean.parseBoolean(text.trim());
    }
}
