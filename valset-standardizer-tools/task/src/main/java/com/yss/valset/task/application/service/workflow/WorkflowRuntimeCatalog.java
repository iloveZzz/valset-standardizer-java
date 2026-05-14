package com.yss.valset.task.application.service.workflow;

import com.yss.valset.domain.model.TaskStage;
import com.yss.valset.domain.model.TaskStatus;
import com.yss.valset.domain.model.TaskType;
import com.yss.valset.task.domain.model.OutsourcedDataTaskStage;
import com.yss.valset.task.domain.model.OutsourcedDataTaskStatus;
import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 工作流运行态阶段目录。
 *
 * <p>
 * 当前版本直接使用固定的三段式 Spring Batch 业务阶段定义。
 * 这样做的目的，是把估值表解析的运行态收敛为代码内常量，避免业务链路继续依赖外部工作流配置表。
 * </p>
 */
@Component
public class WorkflowRuntimeCatalog {

    private static final String DEFAULT_WORKFLOW_CODE = "VALUATION_PARSE";
    private static final String DEFAULT_WORKFLOW_ID = "VALUATION_PARSE";
    private static final Integer DEFAULT_WORKFLOW_VERSION = 1;

    private static final List<StageDefinition> STAGES = Collections.unmodifiableList(Arrays.asList(
            stage(OutsourcedDataTaskStage.FILE_PARSE, "原始文件抽取与解析"),
            stage(OutsourcedDataTaskStage.STRUCTURE_STANDARDIZE, "字段映射与结构标准化"),
            stage(OutsourcedDataTaskStage.STANDARD_LANDING, "标准表落地")
    ));

    private static final ActiveWorkflowDefinition ACTIVE_WORKFLOW_DEFINITION;

    static {
        ActiveWorkflowDefinition definition = new ActiveWorkflowDefinition();
        definition.setWorkflowId(DEFAULT_WORKFLOW_ID);
        definition.setWorkflowCode(DEFAULT_WORKFLOW_CODE);
        definition.setWorkflowName("估值表解析工作流");
        definition.setEngineType("INTERNAL");
        definition.setParseFallbackStage(OutsourcedDataTaskStage.FILE_PARSE.name());
        definition.setWorkflowFallbackStage(OutsourcedDataTaskStage.STANDARD_LANDING.name());
        definition.setVersionNo(DEFAULT_WORKFLOW_VERSION);
        definition.setStages(new ArrayList<>(STAGES));
        ACTIVE_WORKFLOW_DEFINITION = definition;
    }

    public List<StageDefinition> getStages() {
        return new ArrayList<>(STAGES);
    }

    public List<StatusDefinition> getStatuses() {
        return defaultStatuses();
    }

    public List<String> getIgnoredWorkflowTaskTypes() {
        List<String> defaults = defaultIgnoredWorkflowTaskTypes();
        List<String> extras = Arrays.asList(TaskType.MATCH_SUBJECT.name(), TaskType.EXPORT_RESULT.name());
        List<String> merged = new ArrayList<>(defaults);
        merged.addAll(extras);
        return merged.stream().distinct().collect(java.util.stream.Collectors.toList());
    }

    public Optional<ActiveWorkflowDefinition> activeWorkflowDefinition() {
        return Optional.of(copyWorkflowDefinition(ACTIVE_WORKFLOW_DEFINITION));
    }

    public void refreshActiveWorkflowDefinition() {
        // 静态目录无需刷新，保留该方法仅为了兼容旧调用点。
    }

    public String activeWorkflowCode() {
        return ACTIVE_WORKFLOW_DEFINITION.getWorkflowCode();
    }

    public String activeWorkflowId() {
        return ACTIVE_WORKFLOW_DEFINITION.getWorkflowId();
    }

    public Integer activeWorkflowVersionNo() {
        return ACTIVE_WORKFLOW_DEFINITION.getVersionNo();
    }

    public List<OutsourcedDataTaskStage> stageSequence() {
        return getStages().stream()
                .map(StageDefinition::toStage)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());
    }

    public int stageOrder(String stage) {
        OutsourcedDataTaskStage normalized = normalizeStage(stage);
        List<OutsourcedDataTaskStage> sequence = stageSequence();
        for (int i = 0; i < sequence.size(); i++) {
            if (Objects.equals(sequence.get(i), normalized)) {
                return i;
            }
        }
        return sequence.size();
    }

    public OutsourcedDataTaskStage normalizeStage(String stage) {
        if (!StringUtils.hasText(stage)) {
            return firstStage();
        }
        String normalized = stage.trim().toUpperCase();
        if (Objects.equals(OutsourcedDataTaskStage.RAW_DATA_EXTRACT.name(), normalized)
                || Objects.equals("PARSE", normalized)
                || Objects.equals("TASK_RAW_PARSED", normalized)
                || Objects.equals("TASK_CREATED", normalized)
                || Objects.equals("TASK_DISPATCHED", normalized)
                || Objects.equals("TASK_EXECUTION_STARTED", normalized)
                || Objects.equals("TASK_REUSED", normalized)
                || Objects.equals("QUEUE_DISCOVERED", normalized)
                || Objects.equals("QUEUE_GENERATED", normalized)
                || Objects.equals("QUEUE_BACKFILLED", normalized)
                || Objects.equals("QUEUE_REUSED", normalized)
                || Objects.equals("QUEUE_UPDATED", normalized)
                || Objects.equals("QUEUE_RETRIED", normalized)
                || Objects.equals("QUEUE_SUBSCRIBED", normalized)
                || Objects.equals("QUEUE_SUBSCRIBE_ATTEMPTED", normalized)
                || Objects.equals("QUEUE_SUBSCRIBE_CONFLICT", normalized)
                || Objects.equals("QUEUE_SUBSCRIBE_SKIPPED", normalized)
                || Objects.equals("QUEUE_FILE_INFO_REPAIR_STARTED", normalized)
                || Objects.equals("QUEUE_FILE_INFO_REPAIR_COMPLETED", normalized)
                || Objects.equals("QUEUE_FILE_INFO_REPAIR_FAILED", normalized)) {
            return OutsourcedDataTaskStage.FILE_PARSE;
        }
        if (Objects.equals("TASK_STANDARDIZED", normalized)) {
            return OutsourcedDataTaskStage.STRUCTURE_STANDARDIZE;
        }
        if (Objects.equals("TASK_PERSISTED", normalized)
                || Objects.equals("TASK_SUCCEEDED", normalized)
                || Objects.equals("QUEUE_COMPLETED", normalized)) {
            return OutsourcedDataTaskStage.STANDARD_LANDING;
        }
        try {
            OutsourcedDataTaskStage parsed = OutsourcedDataTaskStage.valueOf(normalized);
            switch (parsed) {
                case RAW_DATA_EXTRACT:
                    return OutsourcedDataTaskStage.FILE_PARSE;
                case SUBJECT_RECOGNIZE:
                case VERIFY_ARCHIVE:
                case DATA_PROCESSING:
                    return OutsourcedDataTaskStage.STANDARD_LANDING;
                default:
                    return parsed;
            }
        } catch (Exception ignored) {
            return firstStage();
        }
    }

    public OutsourcedDataTaskStage resolveWorkflowStage(TaskType taskType, TaskStage taskStage) {
        if (taskType == TaskType.EXTRACT_DATA || taskStage == TaskStage.EXTRACT
                || taskType == TaskType.PARSE_WORKBOOK || taskStage == TaskStage.PARSE) {
            return OutsourcedDataTaskStage.FILE_PARSE;
        }
        if (taskStage == TaskStage.STANDARDIZE) {
            return OutsourcedDataTaskStage.STRUCTURE_STANDARDIZE;
        }
        return workflowFallbackStage();
    }

    public boolean ignoreWorkflowTaskType(TaskType taskType) {
        return taskType != null && contains(getIgnoredWorkflowTaskTypes(), taskType.name());
    }

    public OutsourcedDataTaskStatus resolveWorkflowStatus(TaskStatus status) {
        String value = status == null ? null : status.name();
        if (contains(defaultFailedTaskStatuses(), value)) {
            return OutsourcedDataTaskStatus.FAILED;
        }
        if (contains(defaultStoppedTaskStatuses(), value)) {
            return OutsourcedDataTaskStatus.STOPPED;
        }
        if (contains(defaultRunningTaskStatuses(), value)) {
            return OutsourcedDataTaskStatus.RUNNING;
        }
        if (contains(defaultSuccessTaskStatuses(), value)) {
            return OutsourcedDataTaskStatus.SUCCESS;
        }
        return OutsourcedDataTaskStatus.PENDING;
    }

    public StageDefinition findDefinition(String stage) {
        OutsourcedDataTaskStage normalized = normalizeStage(stage);
        return getStages().stream()
                .filter(item -> item.toStage() == normalized)
                .findFirst()
                .orElseGet(this::firstStageDefinition);
    }

    public String stageLabel(String stage) {
        StageDefinition definition = findDefinition(stage);
        return StringUtils.hasText(definition.getStageName()) ? definition.getStageName()
                : normalizeStage(stage).getLabel();
    }

    public String stageDescription(String stage) {
        StageDefinition definition = findDefinition(stage);
        return StringUtils.hasText(definition.getStageDescription())
                ? definition.getStageDescription()
                : normalizeStage(stage).getDescription();
    }

    public String statusLabel(String status) {
        if (!StringUtils.hasText(status)) {
            return "";
        }
        return getStatuses().stream()
                .filter(item -> item.matches(status))
                .map(StatusDefinition::getLabel)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElseGet(() -> resolveStatusFallbackLabel(status));
    }

    public OutsourcedDataTaskStage firstStage() {
        List<OutsourcedDataTaskStage> sequence = stageSequence();
        if (sequence.isEmpty()) {
            throw new IllegalStateException("未找到启用中的工作流阶段配置：" + DEFAULT_WORKFLOW_CODE);
        }
        return sequence.get(0);
    }

    public OutsourcedDataTaskStage parseFallbackStage() {
        return OutsourcedDataTaskStage.FILE_PARSE;
    }

    public OutsourcedDataTaskStage workflowFallbackStage() {
        return OutsourcedDataTaskStage.STANDARD_LANDING;
    }

    private ActiveWorkflowDefinition copyWorkflowDefinition(ActiveWorkflowDefinition source) {
        if (source == null) {
            return null;
        }
        ActiveWorkflowDefinition copy = new ActiveWorkflowDefinition();
        copy.setWorkflowId(source.getWorkflowId());
        copy.setWorkflowCode(source.getWorkflowCode());
        copy.setWorkflowName(source.getWorkflowName());
        copy.setEngineType(source.getEngineType());
        copy.setParseFallbackStage(source.getParseFallbackStage());
        copy.setWorkflowFallbackStage(source.getWorkflowFallbackStage());
        copy.setVersionNo(source.getVersionNo());
        copy.setStages(source.getStages() == null ? new ArrayList<>() : new ArrayList<>(source.getStages()));
        return copy;
    }

    private static List<String> defaultIgnoredWorkflowTaskTypes() {
        return Arrays.asList(TaskType.PARSE_WORKBOOK.name());
    }

    private static List<String> defaultSuccessTaskStatuses() {
        return Arrays.asList(TaskStatus.SUCCESS.name());
    }

    private static List<String> defaultFailedTaskStatuses() {
        return Arrays.asList(TaskStatus.FAILED.name());
    }

    private static List<String> defaultStoppedTaskStatuses() {
        return Arrays.asList(TaskStatus.CANCELED.name());
    }

    private static List<String> defaultRunningTaskStatuses() {
        return Arrays.asList(TaskStatus.RUNNING.name(), TaskStatus.RETRYING.name());
    }

    private static boolean contains(List<String> values, String value) {
        if (values == null || values.isEmpty() || !StringUtils.hasText(value)) {
            return false;
        }
        return values.stream().anyMatch(item -> StringUtils.hasText(item) && item.trim().equals(value.trim()));
    }

    private String resolveStatusFallbackLabel(String status) {
        try {
            return OutsourcedDataTaskStatus.valueOf(status.trim()).getLabel();
        } catch (Exception ignored) {
            return status;
        }
    }

    private StageDefinition firstStageDefinition() {
        List<StageDefinition> stages = getStages();
        if (stages.isEmpty()) {
            throw new IllegalStateException("未找到启用中的工作流阶段配置：" + DEFAULT_WORKFLOW_CODE);
        }
        return stages.get(0);
    }

    private static StageDefinition stage(OutsourcedDataTaskStage stage, String description) {
        StageDefinition definition = new StageDefinition();
        definition.setStage(stage.name());
        definition.setStep(stage.name());
        definition.setStageName(stage.getLabel());
        definition.setStepName(stage.getLabel());
        definition.setStageDescription(description);
        definition.setStepDescription(description);
        return definition;
    }

    @Data
    public static class StageDefinition {
        private String stage;
        private String step;
        private String stageName;
        private String stepName;
        private String stageDescription;
        private String stepDescription;

        public OutsourcedDataTaskStage toStage() {
            String value = StringUtils.hasText(stage) ? stage.trim() : step;
            if (!StringUtils.hasText(value)) {
                return null;
            }
            try {
                OutsourcedDataTaskStage parsed = OutsourcedDataTaskStage.valueOf(value);
                return parsed == OutsourcedDataTaskStage.RAW_DATA_EXTRACT ? OutsourcedDataTaskStage.FILE_PARSE : parsed;
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    @Data
    public static class ActiveWorkflowDefinition {
        private String workflowId;
        private String workflowCode;
        private String workflowName;
        private String engineType;
        private String parseFallbackStage;
        private String workflowFallbackStage;
        private Integer versionNo;
        private List<StageDefinition> stages = new ArrayList<>();
    }

    @Data
    public static class StatusDefinition {
        private String status;
        private String label;

        public boolean matches(String value) {
            return StringUtils.hasText(status) && StringUtils.hasText(value) && status.trim().equals(value.trim());
        }
    }

    private static StatusDefinition status(String status, String label) {
        StatusDefinition definition = new StatusDefinition();
        definition.setStatus(status);
        definition.setLabel(label);
        return definition;
    }

    private static List<StatusDefinition> defaultStatuses() {
        List<StatusDefinition> statuses = new ArrayList<>();
        statuses.add(status("PENDING", "待处理"));
        statuses.add(status("RUNNING", "处理中"));
        statuses.add(status("SUCCESS", "已完成"));
        statuses.add(status("FAILED", "失败"));
        statuses.add(status("STOPPED", "已停止"));
        statuses.add(status("BLOCKED", "阻塞"));
        return statuses;
    }
}
