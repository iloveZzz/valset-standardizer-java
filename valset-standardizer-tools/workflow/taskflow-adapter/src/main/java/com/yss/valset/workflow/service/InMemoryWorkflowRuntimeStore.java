package com.yss.valset.workflow.service;

import com.yss.cloud.dto.response.PageResult;
import com.yss.valset.workflow.model.WorkflowDefinitionDTO;
import com.yss.valset.workflow.model.WorkflowInstanceQueryRequest;
import com.yss.valset.workflow.model.WorkflowInstanceDTO;
import com.yss.valset.workflow.model.WorkflowInstanceViewDTO;
import com.yss.valset.workflow.model.WorkflowStatus;
import com.yss.valset.workflow.spi.WorkflowRuntimeStore;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 内存工作流运行态存储。
 */
@Component
public class InMemoryWorkflowRuntimeStore implements WorkflowRuntimeStore {

    private final ConcurrentMap<String, List<WorkflowDefinitionDTO>> definitions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, WorkflowInstanceDTO> instances = new ConcurrentHashMap<>();

    @Override
    public WorkflowDefinitionDTO saveDefinition(WorkflowDefinitionDTO definition) {
        WorkflowDefinitionDTO copy = definition == null ? null : definition.toBuilder().build();
        if (copy == null) {
            throw new IllegalArgumentException("工作流定义不能为空");
        }
        definitions.compute(copy.getWorkflowCode(), (key, existing) -> {
            List<WorkflowDefinitionDTO> list = existing == null ? new ArrayList<>() : existing;
            list.removeIf(item -> item.getWorkflowVersionNo() != null && item.getWorkflowVersionNo().equals(copy.getWorkflowVersionNo()));
            list.add(copy);
            list.sort(Comparator.comparing(WorkflowDefinitionDTO::getWorkflowVersionNo));
            return list;
        });
        return copy;
    }

    @Override
    public boolean deleteDefinition(String workflowCode, Integer workflowVersionNo) {
        if (!StringUtils.hasText(workflowCode) || workflowVersionNo == null) {
            return false;
        }
        List<WorkflowDefinitionDTO> items = definitions.get(workflowCode);
        if (items == null || items.isEmpty()) {
            return false;
        }
        boolean removed = items.removeIf(item -> workflowVersionNo.equals(item.getWorkflowVersionNo()));
        if (items.isEmpty()) {
            definitions.remove(workflowCode, items);
        }
        return removed;
    }

    @Override
    public Optional<WorkflowDefinitionDTO> findDefinition(String workflowCode, Integer workflowVersionNo) {
        if (!StringUtils.hasText(workflowCode) || workflowVersionNo == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(definitions.get(workflowCode))
                .stream()
                .flatMap(List::stream)
                .filter(item -> workflowVersionNo.equals(item.getWorkflowVersionNo()))
                .findFirst()
                .map(item -> item.toBuilder().build());
    }

    @Override
    public List<WorkflowDefinitionDTO> listDefinitions() {
        List<WorkflowDefinitionDTO> result = new ArrayList<>();
        definitions.values().forEach(items -> items.forEach(item -> result.add(item.toBuilder().build())));
        result.sort(Comparator.comparing(WorkflowDefinitionDTO::getWorkflowCode)
                .thenComparing(WorkflowDefinitionDTO::getWorkflowVersionNo));
        return result;
    }

    @Override
    public WorkflowInstanceDTO saveInstance(WorkflowInstanceDTO instance) {
        WorkflowInstanceDTO copy = instance == null ? null : instance.toBuilder().build();
        if (copy == null) {
            throw new IllegalArgumentException("工作流实例不能为空");
        }
        WorkflowInstanceDTO sanitized = copy.toBuilder()
                .stageLogs(new ArrayList<>())
                .build();
        instances.put(sanitized.getInstanceId(), sanitized);
        return sanitized.toBuilder().build();
    }

    @Override
    public Optional<WorkflowInstanceDTO> findInstance(String instanceId) {
        if (!StringUtils.hasText(instanceId)) {
            return Optional.empty();
        }
        WorkflowInstanceDTO instance = instances.get(instanceId);
        return instance == null ? Optional.empty() : Optional.of(instance.toBuilder().build());
    }

    @Override
    public PageResult<WorkflowInstanceViewDTO> listInstances(WorkflowInstanceQueryRequest request) {
        List<WorkflowInstanceViewDTO> result = new ArrayList<>();
        for (WorkflowInstanceDTO instance : instances.values()) {
            if (!matches(instance, request)) {
                continue;
            }
            result.add(toView(instance));
        }
        result.sort(Comparator
                .comparing(WorkflowInstanceViewDTO::getTriggerTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed()
                .thenComparing(WorkflowInstanceViewDTO::getInstanceId, Comparator.nullsLast(String::compareTo)));
        int pageIndex = normalizePageIndex(request == null ? null : request.getPageIndex());
        int pageSize = normalizePageSize(request == null ? null : request.getPageSize());
        int fromIndex = Math.min(pageIndex * pageSize, result.size());
        int toIndex = Math.min(fromIndex + pageSize, result.size());
        return PageResult.of(result.subList(fromIndex, toIndex), result.size(), pageSize, pageIndex);
    }

    private boolean matches(WorkflowInstanceDTO instance, WorkflowInstanceQueryRequest request) {
        if (instance == null) {
            return false;
        }
        if (request == null) {
            return true;
        }
        if (StringUtils.hasText(request.getWorkflowCode())
                && !request.getWorkflowCode().trim().equals(instance.getWorkflowCode())) {
            return false;
        }
        if (request.getWorkflowVersionNo() != null
                && !request.getWorkflowVersionNo().equals(instance.getWorkflowVersionNo())) {
            return false;
        }
        if (request.getPlatformType() != null
                && request.getPlatformType() != instance.getPlatformType()) {
            return false;
        }
        if (StringUtils.hasText(request.getStatus())) {
            String status = String.valueOf(instance.getStatus() == null ? null : instance.getStatus().name());
            if (!request.getStatus().trim().equalsIgnoreCase(status)) {
                return false;
            }
        }
        if (StringUtils.hasText(request.getBusinessKey())
                && !containsIgnoreCase(instance.getBusinessKey(), request.getBusinessKey())) {
            return false;
        }
        if (StringUtils.hasText(request.getInstanceId())
                && !containsIgnoreCase(instance.getInstanceId(), request.getInstanceId())) {
            return false;
        }
        if (StringUtils.hasText(request.getExternalInstanceId())
                && !containsIgnoreCase(instance.getExternalInstanceId(), request.getExternalInstanceId())) {
            return false;
        }
        if (StringUtils.hasText(request.getStageCode())
                && !StringUtils.hasText(instance.getCurrentStageCode())) {
            return false;
        }
        if (StringUtils.hasText(request.getStageCode())
                && !request.getStageCode().trim().equalsIgnoreCase(instance.getCurrentStageCode())) {
            return false;
        }
        if (!matchesTimeRange(instance, request)) {
            return false;
        }
        return true;
    }

    private boolean matchesTimeRange(WorkflowInstanceDTO instance, WorkflowInstanceQueryRequest request) {
        if (request == null) {
            return true;
        }
        LocalDateTime triggerTime = instance.getTriggerTime();
        if (triggerTime == null) {
            return true;
        }
        LocalDateTime from = parseBoundary(request.getTriggerTimeFrom(), false);
        LocalDateTime to = parseBoundary(request.getTriggerTimeTo(), true);
        if (from != null && triggerTime.isBefore(from)) {
            return false;
        }
        if (to != null && triggerTime.isAfter(to)) {
            return false;
        }
        return true;
    }

    private WorkflowInstanceViewDTO toView(WorkflowInstanceDTO instance) {
        WorkflowStatus status = instance.getStatus();
        WorkflowDefinitionDTO definition = findDefinition(instance.getWorkflowCode(), instance.getWorkflowVersionNo())
                .orElse(null);
        int stageCount = definition == null || definition.getStages() == null ? 0 : definition.getStages().size();
        String currentStageName = definition == null || definition.getStages() == null
                ? null
                : definition.getStages().stream()
                .filter(stage -> instance.getCurrentStageCode() != null
                        && instance.getCurrentStageCode().equals(stage.getStageCode()))
                .findFirst()
                .map(stage -> stage.getStageName())
                .orElse(instance.getCurrentStageCode());
        return WorkflowInstanceViewDTO.builder()
                .instanceId(instance.getInstanceId())
                .workflowCode(instance.getWorkflowCode())
                .workflowVersionNo(instance.getWorkflowVersionNo())
                .platformType(instance.getPlatformType())
                .businessKey(instance.getBusinessKey())
                .externalInstanceId(instance.getExternalInstanceId())
                .externalWorkflowId(instance.getExternalWorkflowId())
                .status(status)
                .rawStatus(instance.getRawStatus())
                .currentStageCode(instance.getCurrentStageCode())
                .currentStageName(currentStageName)
                .triggerTime(instance.getTriggerTime())
                .startTime(instance.getStartTime())
                .duration(resolveDuration(instance.getStartTime(), instance.getEndTime(), instance.getDuration()))
                .endTime(instance.getEndTime())
                .message(instance.getMessage())
                .stageCount(stageCount)
                .build();
    }

    private String resolveDuration(LocalDateTime startTime, LocalDateTime endTime, String duration) {
        if (StringUtils.hasText(duration)) {
            return duration;
        }
        if (startTime == null || endTime == null || endTime.isBefore(startTime)) {
            return null;
        }
        Duration elapsed = Duration.between(startTime, endTime);
        return formatDuration(elapsed);
    }

    private String formatDuration(Duration duration) {
        if (duration == null || duration.isNegative()) {
            return null;
        }
        long seconds = duration.getSeconds();
        long days = seconds / 86_400;
        seconds %= 86_400;
        long hours = seconds / 3_600;
        seconds %= 3_600;
        long minutes = seconds / 60;
        seconds %= 60;
        StringBuilder builder = new StringBuilder();
        if (days > 0) {
            builder.append(days).append("天");
        }
        if (hours > 0) {
            builder.append(hours).append("小时");
        }
        if (minutes > 0) {
            builder.append(minutes).append("分钟");
        }
        if (seconds > 0 || builder.length() == 0) {
            builder.append(seconds).append("秒");
        }
        return builder.toString();
    }

    private boolean containsIgnoreCase(String actual, String expected) {
        if (!StringUtils.hasText(expected)) {
            return true;
        }
        return StringUtils.hasText(actual) && actual.toLowerCase().contains(expected.trim().toLowerCase());
    }

    private int normalizePageIndex(Integer pageIndex) {
        return pageIndex == null || pageIndex < 0 ? 0 : pageIndex;
    }

    private int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 20 : pageSize;
    }

    private LocalDateTime parseBoundary(String value, boolean endOfDay) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String text = value.trim();
        try {
            if (text.length() <= 10) {
                java.time.LocalDate date = java.time.LocalDate.parse(text);
                return endOfDay ? date.atTime(23, 59, 59) : date.atStartOfDay();
            }
            return LocalDateTime.parse(text.replace(" ", "T"));
        } catch (Exception ignored) {
            return null;
        }
    }
}
