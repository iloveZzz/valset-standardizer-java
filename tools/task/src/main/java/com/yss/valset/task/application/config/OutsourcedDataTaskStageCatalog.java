package com.yss.valset.task.application.config;

import com.yss.valset.application.event.lifecycle.ParseLifecycleStage;
import com.yss.valset.domain.model.TaskStage;
import com.yss.valset.domain.model.TaskStatus;
import com.yss.valset.domain.model.TaskType;
import com.yss.valset.task.domain.model.OutsourcedDataTaskStatus;
import com.yss.valset.task.domain.model.OutsourcedDataTaskStage;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 估值表解析任务阶段配置目录。
 */
@Data
@Component
@ConfigurationProperties(prefix = "subject.match.outsourced-task")
public class OutsourcedDataTaskStageCatalog {

    private List<StageDefinition> stages = defaultStages();

    private String parseFallbackStage = OutsourcedDataTaskStage.FILE_PARSE.name();

    private String workflowFallbackStage = OutsourcedDataTaskStage.DATA_PROCESSING.name();

    private List<String> ignoredParseLifecycleStages = defaultIgnoredParseLifecycleStages();

    private List<String> ignoredWorkflowTaskTypes = defaultIgnoredWorkflowTaskTypes();

    private List<String> successTaskStatuses = defaultSuccessTaskStatuses();

    private List<String> failedTaskStatuses = defaultFailedTaskStatuses();

    private List<String> stoppedTaskStatuses = defaultStoppedTaskStatuses();

    private List<String> runningTaskStatuses = defaultRunningTaskStatuses();

    private List<String> parseRunningLifecycleStages = defaultParseRunningLifecycleStages();

    private List<String> parseSuccessLifecycleStages = defaultParseSuccessLifecycleStages();

    private List<String> parseStoppedLifecycleStages = defaultParseStoppedLifecycleStages();

    private List<StatusDefinition> statuses = defaultStatuses();

    public List<StageDefinition> getStages() {
        return stages == null || stages.isEmpty() ? defaultStages() : stages;
    }

    public List<StatusDefinition> getStatuses() {
        return statuses == null || statuses.isEmpty() ? defaultStatuses() : statuses;
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
        OutsourcedDataTaskStage matched = getStages().stream()
                .filter(item -> item.matchesParseLifecycleStage(stage.name()))
                .map(StageDefinition::toStage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        return matched == null ? parseFallbackStage() : matched;
    }

    public OutsourcedDataTaskStage resolveWorkflowStage(TaskType taskType, TaskStage taskStage) {
        OutsourcedDataTaskStage matched = getStages().stream()
                .filter(item -> item.matchesTaskType(taskType == null ? null : taskType.name()))
                .map(StageDefinition::toStage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (matched != null) {
            return matched;
        }
        matched = getStages().stream()
                .filter(item -> item.matchesTaskStage(taskStage == null ? null : taskStage.name()))
                .map(StageDefinition::toStage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        return matched == null ? workflowFallbackStage() : matched;
    }

    public boolean ignoreWorkflowTaskType(TaskType taskType) {
        return taskType != null && contains(ignoredWorkflowTaskTypes, taskType.name());
    }

    public OutsourcedDataTaskStatus resolveWorkflowStatus(TaskStatus status) {
        String value = status == null ? null : status.name();
        if (contains(failedTaskStatuses, value)) {
            return OutsourcedDataTaskStatus.FAILED;
        }
        if (contains(stoppedTaskStatuses, value)) {
            return OutsourcedDataTaskStatus.STOPPED;
        }
        if (contains(runningTaskStatuses, value)) {
            return OutsourcedDataTaskStatus.RUNNING;
        }
        if (contains(successTaskStatuses, value)) {
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
        if (contains(parseRunningLifecycleStages, value)) {
            return OutsourcedDataTaskStatus.RUNNING;
        }
        if (contains(parseSuccessLifecycleStages, value)) {
            return OutsourcedDataTaskStatus.SUCCESS;
        }
        if (contains(parseStoppedLifecycleStages, value)) {
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
        if (contains(parseSuccessLifecycleStages, value)) {
            return OutsourcedDataTaskStatus.SUCCESS;
        }
        if (contains(parseStoppedLifecycleStages, value)) {
            return OutsourcedDataTaskStatus.STOPPED;
        }
        return OutsourcedDataTaskStatus.RUNNING;
    }

    public StageDefinition findDefinition(String stage) {
        OutsourcedDataTaskStage normalized = normalizeStage(stage);
        return getStages().stream()
                .filter(item -> item.toStage() == normalized)
                .findFirst()
                .orElseGet(() -> defaultStages().get(0));
    }

    public String stageLabel(String stage) {
        StageDefinition definition = findDefinition(stage);
        return StringUtils.hasText(definition.getStageName()) ? definition.getStageName() : normalizeStage(stage).getLabel();
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
        return sequence.isEmpty() ? OutsourcedDataTaskStage.FILE_PARSE : sequence.get(0);
    }

    public OutsourcedDataTaskStage parseFallbackStage() {
        return resolveConfiguredStage(parseFallbackStage, OutsourcedDataTaskStage.FILE_PARSE);
    }

    public OutsourcedDataTaskStage workflowFallbackStage() {
        return resolveConfiguredStage(workflowFallbackStage, OutsourcedDataTaskStage.DATA_PROCESSING);
    }

    private static List<String> defaultIgnoredParseLifecycleStages() {
        return List.of(
                ParseLifecycleStage.CYCLE_STARTED.name(),
                ParseLifecycleStage.CYCLE_FINISHED.name(),
                ParseLifecycleStage.BATCH_STARTED.name(),
                ParseLifecycleStage.BATCH_EMPTY.name(),
                ParseLifecycleStage.BATCH_FINISHED.name()
        );
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
                ParseLifecycleStage.QUEUE_SUBSCRIBED.name()
        );
    }

    private static List<String> defaultParseSuccessLifecycleStages() {
        return List.of(
                ParseLifecycleStage.TASK_RAW_PARSED.name(),
                ParseLifecycleStage.TASK_STANDARDIZED.name(),
                ParseLifecycleStage.TASK_PERSISTED.name(),
                ParseLifecycleStage.TASK_SUCCEEDED.name(),
                ParseLifecycleStage.QUEUE_COMPLETED.name(),
                ParseLifecycleStage.TASK_REUSED.name()
        );
    }

    private static List<String> defaultParseStoppedLifecycleStages() {
        return List.of(
                ParseLifecycleStage.QUEUE_SKIPPED.name(),
                ParseLifecycleStage.QUEUE_SUBSCRIBE_CONFLICT.name(),
                ParseLifecycleStage.QUEUE_SUBSCRIBE_SKIPPED.name()
        );
    }

    private static List<String> defaultFailedParseLifecycleStages() {
        return List.of(
                ParseLifecycleStage.TASK_FAILED.name(),
                ParseLifecycleStage.QUEUE_FAILED.name(),
                ParseLifecycleStage.QUEUE_FILE_INFO_REPAIR_FAILED.name()
        );
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
            return parsed == OutsourcedDataTaskStage.RAW_DATA_EXTRACT ? OutsourcedDataTaskStage.FILE_PARSE : parsed;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static List<StageDefinition> defaultStages() {
        List<StageDefinition> stages = new ArrayList<>();
        stages.add(stage("FILE_PARSE", "FILE_PARSE", "文件解析", "文件解析", "文件识别、Sheet 解析、结构化解析", "文件识别、Sheet 解析、结构化解析",
                List.of(TaskType.EXTRACT_DATA.name()),
                List.of(TaskStage.EXTRACT.name()),
                List.of()));
        stages.add(stage("STRUCTURE_STANDARDIZE", "STRUCTURE_STANDARDIZE", "结构标准化", "结构标准化", "字段映射、数据清洗、STG 结构转换", "字段映射、数据清洗、STG 结构转换",
                List.of(),
                List.of(TaskStage.STANDARDIZE.name()),
                List.of(ParseLifecycleStage.TASK_STANDARDIZED.name())));
        stages.add(stage("SUBJECT_RECOGNIZE", "SUBJECT_RECOGNIZE", "科目识别", "科目识别", "科目匹配、属性识别、标签补全", "科目匹配、属性识别、标签补全",
                List.of(TaskType.MATCH_SUBJECT.name()),
                List.of(TaskStage.MATCH.name()),
                List.of()));
        stages.add(stage("STANDARD_LANDING", "STANDARD_LANDING", "标准表落地", "标准表落地", "STG/DWD/标准持仓/估值数据写入", "STG/DWD/标准持仓/估值数据写入",
                List.of(),
                List.of(),
                List.of(ParseLifecycleStage.TASK_PERSISTED.name())));
        stages.add(stage("DATA_PROCESSING", "DATA_PROCESSING", "加工任务", "加工任务", "后续数据加工、补充计算、派生数据生成", "后续数据加工、补充计算、派生数据生成",
                List.of(TaskType.EVALUATE_MAPPING.name(), TaskType.REFRESH_STANDARD_SUBJECT.name(), TaskType.REFRESH_MAPPING_HINT.name()),
                List.of(),
                List.of()));
        stages.add(stage("VERIFY_ARCHIVE", "VERIFY_ARCHIVE", "校验归档", "校验归档", "一致性校验、结果确认、归档完成", "一致性校验、结果确认、归档完成",
                List.of(TaskType.EXPORT_RESULT.name()),
                List.of(),
                List.of(ParseLifecycleStage.TASK_SUCCEEDED.name(), ParseLifecycleStage.QUEUE_COMPLETED.name())));
        return stages;
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

    private static StageDefinition stage(String stage,
                                         String step,
                                         String stageName,
                                         String stepName,
                                         String stageDescription,
                                         String stepDescription,
                                         List<String> taskTypes,
                                         List<String> taskStages,
                                         List<String> parseLifecycleStages) {
        StageDefinition definition = new StageDefinition();
        definition.setStage(stage);
        definition.setStep(step);
        definition.setStageName(stageName);
        definition.setStepName(stepName);
        definition.setStageDescription(stageDescription);
        definition.setStepDescription(stepDescription);
        definition.setTaskTypes(new ArrayList<>(taskTypes == null ? List.of() : taskTypes));
        definition.setTaskStages(new ArrayList<>(taskStages == null ? List.of() : taskStages));
        definition.setParseLifecycleStages(new ArrayList<>(parseLifecycleStages == null ? List.of() : parseLifecycleStages));
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
        private List<String> taskTypes = new ArrayList<>();
        private List<String> taskStages = new ArrayList<>();
        private List<String> parseLifecycleStages = new ArrayList<>();

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

        public boolean matchesTaskType(String value) {
            return contains(taskTypes, value);
        }

        public boolean matchesTaskStage(String value) {
            return contains(taskStages, value);
        }

        public boolean matchesParseLifecycleStage(String value) {
            return contains(parseLifecycleStages, value);
        }

        private boolean contains(List<String> values, String value) {
            if (values == null || values.isEmpty() || !StringUtils.hasText(value)) {
                return false;
            }
            return values.stream().anyMatch(item -> StringUtils.hasText(item) && item.trim().equals(value.trim()));
        }
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
