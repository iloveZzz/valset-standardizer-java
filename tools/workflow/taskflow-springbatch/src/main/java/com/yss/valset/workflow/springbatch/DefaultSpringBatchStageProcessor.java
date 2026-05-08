package com.yss.valset.workflow.springbatch;

import com.yss.valset.workflow.model.WorkflowDefinitionDTO;
import com.yss.valset.workflow.model.WorkflowInstanceDTO;
import com.yss.valset.workflow.model.WorkflowPlatformCommand;
import com.yss.valset.workflow.model.WorkflowStageDTO;
import com.yss.valset.workflow.model.WorkflowStatus;
import com.yss.valset.application.dto.workflow.WorkflowContextKeys;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 默认 Spring Batch 阶段处理器。
 *
 * <p>这个处理器不直接执行业务数据搬运，而是负责把工作流阶段转换为可解释的阶段结果：
 * <ul>
 *     <li>根据阶段码识别业务阶段语义</li>
 *     <li>整理阶段输入、输出和元数据</li>
 *     <li>把通用 ETL 上下文中的字段投影到 Spring Batch 阶段结果里</li>
 *     <li>为日志查询和回放提供结构化内容</li>
 * </ul>
 */
@Component
public class DefaultSpringBatchStageProcessor implements SpringBatchStageProcessor {

    @Override
    public SpringBatchStageExecutionResult process(WorkflowDefinitionDTO definition,
                                                   WorkflowInstanceDTO instance,
                                                   WorkflowStageDTO stage,
                                                   WorkflowPlatformCommand command) {
        // 统一入口：先解析阶段语义，再生成输入、输出和元数据快照。
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
        // 输入侧记录本次阶段执行所依赖的核心标识和上下文。
        Map<String, Object> input = new LinkedHashMap<>();
        put(input, WorkflowContextKeys.WORKFLOW_CODE, definition == null ? null : definition.getWorkflowCode());
        put(input, WorkflowContextKeys.WORKFLOW_VERSION_NO, definition == null ? null : definition.getWorkflowVersionNo());
        put(input, WorkflowContextKeys.WORKFLOW_NAME, definition == null ? null : definition.getWorkflowName());
        put(input, SpringBatchStagePayloadKeys.BUSINESS_KEY, instance == null ? null : instance.getBusinessKey());
        put(input, WorkflowContextKeys.INSTANCE_ID, instance == null ? null : instance.getInstanceId());
        put(input, WorkflowContextKeys.EXTERNAL_WORKFLOW_ID, command == null ? null : command.getExternalWorkflowId());
        put(input, WorkflowContextKeys.EXTERNAL_INSTANCE_ID, command == null ? null : command.getExternalInstanceId());
        put(input, SpringBatchStagePayloadKeys.PLATFORM_TYPE, command == null || command.getPlatformType() == null ? null : command.getPlatformType().name());
        put(input, WorkflowContextKeys.STAGE_CODE, stage == null ? null : stage.getStageCode());
        put(input, WorkflowContextKeys.WORKFLOW_STAGE_NAME, stage == null ? null : stage.getStageName());
        put(input, SpringBatchStagePayloadKeys.STAGE_ORDER, stage == null ? null : stage.getStageOrder());
        put(input, WorkflowContextKeys.RETRYABLE, stage != null && stage.isRetryable());
        put(input, WorkflowContextKeys.TIMEOUT_SECONDS, stage == null ? null : stage.getTimeoutSeconds());
        put(input, WorkflowContextKeys.CONTEXT, command == null ? Map.of() : command.getContext());
        put(input, WorkflowContextKeys.PARAMETERS, command == null ? Map.of() : command.getParameters());
        return input;
    }

    private Map<String, Object> buildMetadata(WorkflowDefinitionDTO definition,
                                              WorkflowInstanceDTO instance,
                                              WorkflowStageDTO stage,
                                              WorkflowPlatformCommand command,
                                              Map<String, Object> context,
                                              String businessStage,
                                              String normalizedStageCode) {
        // 元数据主要用于审计和排查，优先存放阶段描述、目标表和上下文摘要。
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(SpringBatchStagePayloadKeys.WORKFLOW_KEY, buildWorkflowKey(definition));
        metadata.put(SpringBatchStagePayloadKeys.BUSINESS_STAGE, businessStage);
        metadata.put(SpringBatchStagePayloadKeys.NORMALIZED_STAGE_CODE, normalizedStageCode);
        metadata.put(SpringBatchStagePayloadKeys.DESCRIPTION, stage == null ? null : stage.getDescription());
        metadata.put(WorkflowContextKeys.RETRYABLE, stage != null && stage.isRetryable());
        metadata.put(WorkflowContextKeys.TIMEOUT_SECONDS, stage == null ? null : stage.getTimeoutSeconds());
        metadata.put(WorkflowContextKeys.INSTANCE_ID, instance == null ? null : instance.getInstanceId());
        metadata.put(SpringBatchStagePayloadKeys.JOB_NAME, definition == null ? null : definition.getWorkflowCode() + "-v" + definition.getWorkflowVersionNo());
        metadata.put(SpringBatchStagePayloadKeys.BUSINESS_KEY, instance == null ? null : instance.getBusinessKey());
        metadata.put(SpringBatchStagePayloadKeys.COMMAND_TYPE, command == null || command.getOperationType() == null ? null : command.getOperationType().name());
        metadata.put(WorkflowContextKeys.DATA_SOURCE_TYPE, resolveText(context, WorkflowContextKeys.DATA_SOURCE_TYPE, "sourceType", "workflowDataSourceType"));
        metadata.put(WorkflowContextKeys.FILE_ID, resolveNumber(context, WorkflowContextKeys.FILE_ID, "sourceFileId"));
        metadata.put(WorkflowContextKeys.CREATED_BY, resolveText(context, WorkflowContextKeys.CREATED_BY, "operator"));
        metadata.put(WorkflowContextKeys.FORCE_REBUILD, resolveBoolean(context, WorkflowContextKeys.FORCE_REBUILD, "force"));
        metadata.put(WorkflowContextKeys.TOP_K, resolveNumber(context, WorkflowContextKeys.TOP_K, "candidateTopK"));
        metadata.put(WorkflowContextKeys.SPLIT_MODE, resolveText(context, WorkflowContextKeys.SPLIT_MODE));
        metadata.put(WorkflowContextKeys.WORKBOOK_PATH, resolveText(context, WorkflowContextKeys.WORKBOOK_PATH, "filePath", "sourcePath"));
        metadata.put(SpringBatchStagePayloadKeys.TARGET_TABLES, resolveTargetTables(normalizedStageCode));
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
        // 输出侧记录阶段执行完成后的结构化结果，供日志查询和回放展示。
        Map<String, Object> output = new LinkedHashMap<>();
        output.put(SpringBatchStagePayloadKeys.PROCESSED_AT, startTime.toString());
        output.put(SpringBatchStagePayloadKeys.BUSINESS_STAGE, businessStage);
        output.put(SpringBatchStagePayloadKeys.STAGE_FAMILY, resolveStageFamily(normalizedStageCode));
        output.put(SpringBatchStagePayloadKeys.RESULT, resolveResult(normalizedStageCode));
        output.put(SpringBatchStagePayloadKeys.PLATFORM_TYPE, command == null || command.getPlatformType() == null ? null : command.getPlatformType().name());
        output.put(SpringBatchStagePayloadKeys.OPERATION_TYPE, command == null || command.getOperationType() == null ? null : command.getOperationType().name());
        output.put(SpringBatchStagePayloadKeys.WORKFLOW_KEY, buildWorkflowKey(definition));
        output.put(SpringBatchStagePayloadKeys.BUSINESS_KEY, instance == null ? null : instance.getBusinessKey());
        output.put(SpringBatchStagePayloadKeys.CONTEXT_ECHO, sanitizeContext(context));
        output.put(SpringBatchStagePayloadKeys.STAGE_PLAN, resolveStagePlan(normalizedStageCode, context));
        output.put(SpringBatchStagePayloadKeys.QUALITY_CHECKS, resolveQualityChecks(normalizedStageCode, context));
        output.put(SpringBatchStagePayloadKeys.TARGET_TABLES, resolveTargetTables(normalizedStageCode));
        output.put(SpringBatchStagePayloadKeys.SOURCE_SUMMARY, resolveSourceSummary(context));
        output.put(SpringBatchStagePayloadKeys.TRACE_SUMMARY, resolveTraceSummary(definition, instance, stage, command, normalizedStageCode));
        output.put(SpringBatchStagePayloadKeys.STAGE_COMPLETED, Boolean.TRUE);
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
        // 优先使用业务阶段语义的固定文案，避免不同调用方重复拼装描述。
        if (StringUtils.hasText(businessStage)) {
            SpringBatchBusinessStage stageEnum = SpringBatchBusinessStage.fromStageCode(businessStage);
            return stageEnum.message();
        }
        if (stage == null || !StringUtils.hasText(stage.getDescription())) {
            return "阶段已完成";
        }
        return stage.getDescription();
    }

    private String resolveStageCode(WorkflowStageDTO stage, WorkflowPlatformCommand command) {
        // 阶段码优先取阶段配置，其次取命令参数，最后回退为空。
        if (stage != null && StringUtils.hasText(stage.getStageCode())) {
            return stage.getStageCode().trim().toUpperCase();
        }
        if (command != null && StringUtils.hasText(command.getStageCode())) {
            return command.getStageCode().trim().toUpperCase();
        }
        return null;
    }

    private String resolveBusinessStage(String normalizedStageCode) {
        return SpringBatchBusinessStage.fromStageCode(normalizedStageCode).code();
    }

    private String resolveStageFamily(String normalizedStageCode) {
        return SpringBatchBusinessStage.fromStageCode(normalizedStageCode).family();
    }

    private List<String> resolveTargetTables(String normalizedStageCode) {
        return SpringBatchBusinessStage.fromStageCode(normalizedStageCode).targetTables();
    }

    private Map<String, Object> resolveStagePlan(String normalizedStageCode, Map<String, Object> context) {
        // 阶段计划用于说明“这一阶段准备做什么、影响哪些表、依赖哪些上下文”。
        Map<String, Object> plan = new LinkedHashMap<>();
        String businessStage = resolveBusinessStage(normalizedStageCode);
        SpringBatchBusinessStage stageEnum = SpringBatchBusinessStage.fromStageCode(normalizedStageCode);
        plan.put(SpringBatchStagePayloadKeys.BUSINESS_STAGE, businessStage);
        plan.put(SpringBatchStagePayloadKeys.STAGE_FAMILY, stageEnum.family());
        plan.put(SpringBatchStagePayloadKeys.SUMMARY, stageEnum.message());
        String sourceType = resolveText(context, WorkflowContextKeys.DATA_SOURCE_TYPE, "sourceType", "workflowDataSourceType");
        plan.put("sourceType", sourceType);
        plan.put(WorkflowContextKeys.DATA_SOURCE_TYPE, sourceType);
        plan.put(WorkflowContextKeys.WORKBOOK_PATH, resolveText(context, WorkflowContextKeys.WORKBOOK_PATH, "filePath", "sourcePath"));
        plan.put(WorkflowContextKeys.FILE_ID, resolveNumber(context, WorkflowContextKeys.FILE_ID, "sourceFileId"));
        plan.put(WorkflowContextKeys.CREATED_BY, resolveText(context, WorkflowContextKeys.CREATED_BY, "operator"));
        plan.put(WorkflowContextKeys.FORCE_REBUILD, resolveBoolean(context, WorkflowContextKeys.FORCE_REBUILD, "force"));
        plan.put(WorkflowContextKeys.TOP_K, resolveNumber(context, WorkflowContextKeys.TOP_K, "candidateTopK"));
        plan.put(SpringBatchStagePayloadKeys.TARGET_TABLES, stageEnum.targetTables());
        return plan;
    }

    private Map<String, Object> resolveQualityChecks(String normalizedStageCode, Map<String, Object> context) {
        // 质量检查只输出可解释的布尔/等级指标，便于前端和日志直接展示。
        Map<String, Object> checks = new LinkedHashMap<>();
        SpringBatchBusinessStage stageEnum = SpringBatchBusinessStage.fromStageCode(normalizedStageCode);
        checks.put(SpringBatchStagePayloadKeys.HAS_SOURCE, StringUtils.hasText(resolveText(context, WorkflowContextKeys.WORKBOOK_PATH, "filePath", "sourcePath"))
                || resolveNumber(context, WorkflowContextKeys.FILE_ID, "sourceFileId") != null);
        checks.put(SpringBatchStagePayloadKeys.HAS_STAGE_CODE, StringUtils.hasText(normalizedStageCode));
        checks.put(WorkflowContextKeys.RETRYABLE, Boolean.TRUE);
        checks.put(SpringBatchStagePayloadKeys.AUDIT_LEVEL, stageEnum.auditLevel());
        return checks;
    }

    private Map<String, Object> resolveSourceSummary(Map<String, Object> context) {
        // 源摘要用于描述本次阶段处理的是哪类数据、来自哪里、带了哪些关键参数。
        Map<String, Object> summary = new LinkedHashMap<>();
        String sourceType = resolveText(context, WorkflowContextKeys.DATA_SOURCE_TYPE, "sourceType", "workflowDataSourceType");
        summary.put("sourceType", sourceType);
        summary.put(WorkflowContextKeys.DATA_SOURCE_TYPE, sourceType);
        summary.put(WorkflowContextKeys.FILE_ID, resolveNumber(context, WorkflowContextKeys.FILE_ID, "sourceFileId"));
        summary.put(WorkflowContextKeys.WORKBOOK_PATH, resolveText(context, WorkflowContextKeys.WORKBOOK_PATH, "filePath", "sourcePath"));
        summary.put(WorkflowContextKeys.CREATED_BY, resolveText(context, WorkflowContextKeys.CREATED_BY, "operator"));
        summary.put(WorkflowContextKeys.FORCE_REBUILD, resolveBoolean(context, WorkflowContextKeys.FORCE_REBUILD, "force"));
        summary.put(WorkflowContextKeys.TOP_K, resolveNumber(context, WorkflowContextKeys.TOP_K, "candidateTopK"));
        summary.put(WorkflowContextKeys.SPLIT_MODE, resolveText(context, WorkflowContextKeys.SPLIT_MODE));
        return summary;
    }

    private Map<String, Object> resolveTraceSummary(WorkflowDefinitionDTO definition,
                                                    WorkflowInstanceDTO instance,
                                                    WorkflowStageDTO stage,
                                                    WorkflowPlatformCommand command,
                                                    String normalizedStageCode) {
        // 追踪摘要用于跨系统排查，尽量保留 workflow、instance、stage 和 command 的核心标识。
        Map<String, Object> trace = new LinkedHashMap<>();
        trace.put(WorkflowContextKeys.WORKFLOW_CODE, definition == null ? null : definition.getWorkflowCode());
        trace.put(WorkflowContextKeys.WORKFLOW_VERSION_NO, definition == null ? null : definition.getWorkflowVersionNo());
        trace.put(SpringBatchStagePayloadKeys.BUSINESS_KEY, instance == null ? null : instance.getBusinessKey());
        trace.put(WorkflowContextKeys.STAGE_CODE, normalizedStageCode);
        trace.put(WorkflowContextKeys.WORKFLOW_STAGE_NAME, stage == null ? null : stage.getStageName());
        trace.put(SpringBatchStagePayloadKeys.OPERATION_TYPE, command == null || command.getOperationType() == null ? null : command.getOperationType().name());
        trace.put(SpringBatchStagePayloadKeys.PLATFORM_TYPE, command == null || command.getPlatformType() == null ? null : command.getPlatformType().name());
        return trace;
    }

    private String buildWorkflowKey(WorkflowDefinitionDTO definition) {
        if (definition == null) {
            return null;
        }
        return definition.getWorkflowCode() + "-v" + definition.getWorkflowVersionNo();
    }

    private Map<String, Object> resolveContext(WorkflowPlatformCommand command) {
        // 命令上下文与参数在这里合并，后续解析时统一从一个快照中取值。
        Map<String, Object> context = new LinkedHashMap<>();
        if (command != null && command.getContext() != null) {
            command.getContext().forEach(context::put);
        }
        if (command != null && command.getParameters() != null) {
            command.getParameters().forEach((key, value) -> context.putIfAbsent(key, value));
        }
        return context;
    }

    private void put(Map<String, Object> target, String key, Object value) {
        // 统一写入入口，方便后续补默认值或空值策略。
        target.put(key, value);
    }

    private Map<String, Object> sanitizeContext(Map<String, Object> context) {
        // 按 key 排序输出，避免同一份上下文在日志里顺序抖动。
        Map<String, Object> snapshot = new TreeMap<>();
        if (context != null) {
            context.forEach((key, value) -> snapshot.put(key, value));
        }
        return snapshot;
    }

    private String resolveText(Map<String, Object> context, String... keys) {
        // 按候选 key 顺序取第一个可用文本值，兼容旧上下文和新上下文。
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
        // 将文本数值统一解析为 Long，避免上层传入字符串时影响阶段逻辑。
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
        // 布尔值也允许通过字符串传入，便于兼容外部平台的参数格式。
        String text = resolveText(context, keys);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return Boolean.parseBoolean(text.trim());
    }
}
