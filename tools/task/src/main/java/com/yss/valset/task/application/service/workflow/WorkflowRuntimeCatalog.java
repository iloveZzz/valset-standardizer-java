package com.yss.valset.task.application.service.workflow;

import com.yss.valset.application.event.lifecycle.ParseLifecycleStage;
import com.yss.valset.domain.model.TaskStage;
import com.yss.valset.domain.model.TaskStatus;
import com.yss.valset.domain.model.TaskType;
import com.yss.valset.task.domain.model.OutsourcedDataTaskStatus;
import com.yss.valset.task.domain.model.OutsourcedDataTaskStage;
import com.yss.valset.task.infrastructure.entity.workflow.OutsourcedWorkflowDefinitionPO;
import com.yss.valset.task.infrastructure.entity.workflow.OutsourcedWorkflowStagePO;
import com.yss.valset.task.infrastructure.mapper.workflow.OutsourcedWorkflowDefinitionRepository;
import com.yss.valset.task.infrastructure.mapper.workflow.OutsourcedWorkflowStageRepository;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 工作流运行态阶段目录。
 */
@Component
public class WorkflowRuntimeCatalog {

    private static final String DEFAULT_WORKFLOW_CODE = "VALUATION_PARSE";

    private OutsourcedWorkflowDefinitionRepository definitionRepository;
    private OutsourcedWorkflowStageRepository stageRepository;

    private final AtomicReference<ActiveWorkflowDefinition> activeWorkflowDefinitionCache = new AtomicReference<>();

    private volatile boolean activeWorkflowDefinitionResolved;

    @Autowired(required = false)
    public void setDefinitionRepository(OutsourcedWorkflowDefinitionRepository definitionRepository) {
        this.definitionRepository = definitionRepository;
    }

    @Autowired(required = false)
    public void setStageRepository(OutsourcedWorkflowStageRepository stageRepository) {
        this.stageRepository = stageRepository;
    }

    public List<StageDefinition> getStages() {
        List<StageDefinition> dbStages = activeDefinition()
                .map(this::toStageDefinitions)
                .orElse(List.of());
        if (dbStages.isEmpty()) {
            throw new IllegalStateException("未找到启用中的工作流阶段配置：" + DEFAULT_WORKFLOW_CODE);
        }
        return dbStages;
    }

    public List<StatusDefinition> getStatuses() {
        return defaultStatuses();
    }

    public List<String> getIgnoredParseLifecycleStages() {
        return defaultIgnoredParseLifecycleStages();
    }

    public List<String> getIgnoredWorkflowTaskTypes() {
        List<String> defaults = defaultIgnoredWorkflowTaskTypes();
        List<String> extras = List.of(TaskType.MATCH_SUBJECT.name(), TaskType.EXPORT_RESULT.name());
        List<String> merged = new ArrayList<>(defaults);
        merged.addAll(extras);
        return merged.stream().distinct().toList();
    }

    public Optional<ActiveWorkflowDefinition> activeWorkflowDefinition() {
        return activeDefinition();
    }

    public void refreshActiveWorkflowDefinition() {
        activeWorkflowDefinitionCache.set(null);
        activeWorkflowDefinitionResolved = false;
    }

    public String activeWorkflowCode() {
        return activeWorkflowDefinition()
                .map(ActiveWorkflowDefinition::getWorkflowCode)
                .filter(StringUtils::hasText)
                .orElse(DEFAULT_WORKFLOW_CODE);
    }

    public String activeWorkflowId() {
        return activeWorkflowDefinition()
                .map(ActiveWorkflowDefinition::getWorkflowId)
                .filter(StringUtils::hasText)
                .orElse(null);
    }

    public Integer activeWorkflowVersionNo() {
        return activeWorkflowDefinition()
                .map(ActiveWorkflowDefinition::getVersionNo)
                .orElse(null);
    }

    public List<OutsourcedDataTaskStage> stageSequence() {
        return getStages().stream()
                .map(StageDefinition::toStage)
                .filter(Objects::nonNull)
                .toList();
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
        String normalized = stage.trim();
        if (Objects.equals(OutsourcedDataTaskStage.RAW_DATA_EXTRACT.name(), normalized)) {
            return OutsourcedDataTaskStage.FILE_PARSE;
        }
        try {
            OutsourcedDataTaskStage parsed = OutsourcedDataTaskStage.valueOf(normalized);
            if (parsed == OutsourcedDataTaskStage.SUBJECT_RECOGNIZE
                    || parsed == OutsourcedDataTaskStage.VERIFY_ARCHIVE
                    || parsed == OutsourcedDataTaskStage.DATA_PROCESSING) {
                return OutsourcedDataTaskStage.STANDARD_LANDING;
            }
            return parsed == OutsourcedDataTaskStage.RAW_DATA_EXTRACT
                    ? OutsourcedDataTaskStage.FILE_PARSE
                    : parsed;
        } catch (Exception ignored) {
            return firstStage();
        }
    }

    public OutsourcedDataTaskStage resolveParseLifecycleStage(ParseLifecycleStage stage) {
        if (stage == null) {
            return null;
        }
        if (getIgnoredParseLifecycleStages().stream().anyMatch(item -> matches(item, stage.name()))) {
            return null;
        }
        return switch (stage) {
            case TASK_STANDARDIZED -> OutsourcedDataTaskStage.STRUCTURE_STANDARDIZE;
            case TASK_PERSISTED, TASK_SUCCEEDED, QUEUE_COMPLETED -> OutsourcedDataTaskStage.STANDARD_LANDING;
            case TASK_RAW_PARSED, TASK_CREATED, TASK_DISPATCHED, TASK_EXECUTION_STARTED, QUEUE_SUBSCRIBED,
                    TASK_REUSED ->
                OutsourcedDataTaskStage.FILE_PARSE;
            default -> parseFallbackStage();
        };
    }

    public OutsourcedDataTaskStage resolveWorkflowStage(TaskType taskType, TaskStage taskStage) {
        if (taskType == TaskType.EXTRACT_DATA || taskStage == TaskStage.EXTRACT) {
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

    public OutsourcedDataTaskStatus resolveParseStepStatus(ParseLifecycleStage stage) {
        if (stage == null) {
            return OutsourcedDataTaskStatus.PENDING;
        }
        String value = stage.name();
        if (contains(defaultFailedParseLifecycleStages(), value)) {
            return OutsourcedDataTaskStatus.FAILED;
        }
        if (contains(defaultParseRunningLifecycleStages(), value)) {
            return OutsourcedDataTaskStatus.RUNNING;
        }
        if (contains(defaultParseSuccessLifecycleStages(), value)) {
            return OutsourcedDataTaskStatus.SUCCESS;
        }
        if (contains(defaultParseStoppedLifecycleStages(), value)) {
            return OutsourcedDataTaskStatus.STOPPED;
        }
        return OutsourcedDataTaskStatus.PENDING;
    }

    public OutsourcedDataTaskStatus resolveParseBatchStatus(ParseLifecycleStage stage) {
        if (stage == null) {
            return OutsourcedDataTaskStatus.PENDING;
        }
        String value = stage.name();
        if (contains(defaultFailedParseLifecycleStages(), value)) {
            return OutsourcedDataTaskStatus.FAILED;
        }
        if (contains(defaultParseSuccessLifecycleStages(), value)) {
            return OutsourcedDataTaskStatus.SUCCESS;
        }
        if (contains(defaultParseStoppedLifecycleStages(), value)) {
            return OutsourcedDataTaskStatus.STOPPED;
        }
        return OutsourcedDataTaskStatus.RUNNING;
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
        String dbFallback = activeDefinition()
                .map(ActiveWorkflowDefinition::getParseFallbackStage)
                .orElse(null);
        return resolveConfiguredStage(dbFallback, OutsourcedDataTaskStage.FILE_PARSE);
    }

    public OutsourcedDataTaskStage workflowFallbackStage() {
        String dbFallback = activeDefinition()
                .map(ActiveWorkflowDefinition::getWorkflowFallbackStage)
                .orElse(null);
        return resolveConfiguredStage(dbFallback, OutsourcedDataTaskStage.STANDARD_LANDING);
    }

    private Optional<ActiveWorkflowDefinition> activeDefinition() {
        if (definitionRepository == null) {
            return Optional.empty();
        }
        ActiveWorkflowDefinition cached = activeWorkflowDefinitionCache.get();
        if (activeWorkflowDefinitionResolved) {
            return Optional.ofNullable(copyWorkflowDefinition(cached));
        }
        synchronized (activeWorkflowDefinitionCache) {
            if (!activeWorkflowDefinitionResolved) {
                try {
                    activeWorkflowDefinitionCache
                            .set(copyWorkflowDefinition(loadActiveDefinition(DEFAULT_WORKFLOW_CODE)));
                } catch (Exception ignored) {
                    activeWorkflowDefinitionCache.set(null);
                } finally {
                    activeWorkflowDefinitionResolved = true;
                }
            }
        }
        return Optional.ofNullable(copyWorkflowDefinition(activeWorkflowDefinitionCache.get()));
    }

    private ActiveWorkflowDefinition loadActiveDefinition(String workflowCode) {
        if (!StringUtils.hasText(workflowCode) || definitionRepository == null) {
            return null;
        }
        OutsourcedWorkflowDefinitionPO definitionPO = definitionRepository.selectOne(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.lambdaQuery(OutsourcedWorkflowDefinitionPO.class)
                        .eq(OutsourcedWorkflowDefinitionPO::getWorkflowCode, workflowCode.trim())
                        .eq(OutsourcedWorkflowDefinitionPO::getEnabled, true)
                        .orderByDesc(OutsourcedWorkflowDefinitionPO::getVersionNo)
                        .last("limit 1"));
        if (definitionPO == null) {
            return null;
        }
        ActiveWorkflowDefinition definition = new ActiveWorkflowDefinition();
        definition.setWorkflowId(definitionPO.getWorkflowId());
        definition.setWorkflowCode(definitionPO.getWorkflowCode());
        definition.setWorkflowName(definitionPO.getWorkflowName());
        definition.setParseFallbackStage(definitionPO.getParseFallbackStage());
        definition.setWorkflowFallbackStage(definitionPO.getWorkflowFallbackStage());
        definition.setVersionNo(definitionPO.getVersionNo());
        definition.setEngineType("INTERNAL");
        definition.setStages(loadStages(definitionPO.getWorkflowId()));
        return definition;
    }

    private List<OutsourcedWorkflowStagePO> loadStages(String workflowId) {
        if (!StringUtils.hasText(workflowId) || stageRepository == null) {
            return List.of();
        }
        List<OutsourcedWorkflowStagePO> stagePOs = stageRepository.selectList(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.lambdaQuery(OutsourcedWorkflowStagePO.class)
                        .eq(OutsourcedWorkflowStagePO::getWorkflowId, workflowId.trim())
                        .eq(OutsourcedWorkflowStagePO::getEnabled, true)
                        .orderByAsc(OutsourcedWorkflowStagePO::getSortOrder));
        if (stagePOs == null || stagePOs.isEmpty()) {
            return List.of();
        }
        return stagePOs.stream()
                .filter(Objects::nonNull)
                .toList();
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
        copy.setStages(source.getStages() == null ? List.of() : new ArrayList<>(source.getStages()));
        return copy;
    }

    private List<StageDefinition> toStageDefinitions(ActiveWorkflowDefinition definition) {
        if (definition == null || definition.getStages() == null) {
            return List.of();
        }
        return definition.getStages().stream()
                .filter(stage -> stage != null && Boolean.TRUE.equals(stage.getEnabled()))
                .map(this::toStageDefinition)
                .filter(Objects::nonNull)
                .toList();
    }

    private StageDefinition toStageDefinition(OutsourcedWorkflowStagePO dto) {
        if (!StringUtils.hasText(dto.getStageCode())) {
            return null;
        }
        StageDefinition definition = new StageDefinition();
        definition.setStage(dto.getStageCode());
        definition.setStep(dto.getStageCode());
        definition.setStageName(dto.getStageName());
        definition.setStepName(dto.getStageName());
        definition.setStageDescription(dto.getStageDescription());
        definition.setStepDescription(dto.getStageDescription());
        return definition;
    }

    private static List<String> defaultIgnoredParseLifecycleStages() {
        return List.of(
                ParseLifecycleStage.CYCLE_STARTED.name(),
                ParseLifecycleStage.CYCLE_FINISHED.name(),
                ParseLifecycleStage.BATCH_STARTED.name(),
                ParseLifecycleStage.BATCH_EMPTY.name(),
                ParseLifecycleStage.BATCH_FINISHED.name());
    }

    private static List<String> defaultIgnoredWorkflowTaskTypes() {
        return List.of(TaskType.PARSE_WORKBOOK.name());
    }

    private static List<String> defaultSuccessTaskStatuses() {
        return List.of(TaskStatus.SUCCESS.name());
    }

    private static List<String> defaultFailedTaskStatuses() {
        return List.of(TaskStatus.FAILED.name());
    }

    private static List<String> defaultStoppedTaskStatuses() {
        return List.of(TaskStatus.CANCELED.name());
    }

    private static List<String> defaultRunningTaskStatuses() {
        return List.of(TaskStatus.RUNNING.name(), TaskStatus.RETRYING.name());
    }

    private static List<String> defaultParseRunningLifecycleStages() {
        return List.of(
                ParseLifecycleStage.TASK_EXECUTION_STARTED.name(),
                ParseLifecycleStage.TASK_CREATED.name(),
                ParseLifecycleStage.TASK_DISPATCHED.name(),
                ParseLifecycleStage.QUEUE_SUBSCRIBED.name());
    }

    private static List<String> defaultParseSuccessLifecycleStages() {
        return List.of(
                ParseLifecycleStage.TASK_RAW_PARSED.name(),
                ParseLifecycleStage.TASK_STANDARDIZED.name(),
                ParseLifecycleStage.TASK_PERSISTED.name(),
                ParseLifecycleStage.TASK_SUCCEEDED.name(),
                ParseLifecycleStage.QUEUE_COMPLETED.name(),
                ParseLifecycleStage.TASK_REUSED.name());
    }

    private static List<String> defaultParseStoppedLifecycleStages() {
        return List.of(
                ParseLifecycleStage.QUEUE_SKIPPED.name(),
                ParseLifecycleStage.QUEUE_SUBSCRIBE_CONFLICT.name(),
                ParseLifecycleStage.QUEUE_SUBSCRIBE_SKIPPED.name());
    }

    private static List<String> defaultFailedParseLifecycleStages() {
        return List.of(
                ParseLifecycleStage.TASK_FAILED.name(),
                ParseLifecycleStage.QUEUE_FAILED.name(),
                ParseLifecycleStage.QUEUE_FILE_INFO_REPAIR_FAILED.name());
    }

    private static boolean matches(String expected, String actual) {
        return StringUtils.hasText(expected) && StringUtils.hasText(actual) && expected.trim().equals(actual.trim());
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

    private OutsourcedDataTaskStage resolveConfiguredStage(String stage, OutsourcedDataTaskStage fallback) {
        if (!StringUtils.hasText(stage)) {
            return fallback;
        }
        try {
            OutsourcedDataTaskStage parsed = OutsourcedDataTaskStage.valueOf(stage.trim());
            if (parsed == OutsourcedDataTaskStage.SUBJECT_RECOGNIZE
                    || parsed == OutsourcedDataTaskStage.VERIFY_ARCHIVE
                    || parsed == OutsourcedDataTaskStage.DATA_PROCESSING) {
                return OutsourcedDataTaskStage.STANDARD_LANDING;
            }
            return parsed == OutsourcedDataTaskStage.RAW_DATA_EXTRACT ? OutsourcedDataTaskStage.FILE_PARSE : parsed;
        } catch (Exception ignored) {
            return fallback;
        }
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

    private StageDefinition firstStageDefinition() {
        List<StageDefinition> stages = getStages();
        if (stages.isEmpty()) {
            throw new IllegalStateException("未找到启用中的工作流阶段配置：" + DEFAULT_WORKFLOW_CODE);
        }
        return stages.get(0);
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
        private List<OutsourcedWorkflowStagePO> stages = new ArrayList<>();
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
}
