package com.yss.valset.workflow.service;

import com.yss.valset.workflow.model.WorkflowDefinitionDTO;
import com.yss.valset.workflow.model.WorkflowInstanceDTO;
import com.yss.valset.workflow.model.WorkflowStageLogDTO;
import com.yss.valset.workflow.spi.WorkflowRuntimeStore;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentMap;

/**
 * 内存工作流运行态存储。
 */
@Component
public class InMemoryWorkflowRuntimeStore implements WorkflowRuntimeStore {

    private final ConcurrentMap<String, List<WorkflowDefinitionDTO>> definitions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, WorkflowInstanceDTO> instances = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, List<WorkflowStageLogDTO>> stageLogs = new ConcurrentHashMap<>();

    @Override
    public WorkflowDefinitionDTO saveDefinition(WorkflowDefinitionDTO definition) {
        WorkflowDefinitionDTO copy = definition == null ? null : definition.toBuilder().build();
        if (copy == null) {
            throw new IllegalArgumentException("工作流定义不能为空");
        }
        definitions.compute(copy.getWorkflowCode(), (key, existing) -> {
            List<WorkflowDefinitionDTO> list = existing == null ? new CopyOnWriteArrayList<>() : existing;
            list.removeIf(item -> item.getWorkflowVersionNo() != null && item.getWorkflowVersionNo().equals(copy.getWorkflowVersionNo()));
            list.add(copy);
            list.sort(Comparator.comparing(WorkflowDefinitionDTO::getWorkflowVersionNo));
            return list;
        });
        return copy;
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
        instances.put(copy.getInstanceId(), copy);
        return copy;
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
    public WorkflowStageLogDTO saveStageLog(WorkflowStageLogDTO log) {
        WorkflowStageLogDTO copy = log == null ? null : log.toBuilder().build();
        if (copy == null) {
            throw new IllegalArgumentException("阶段日志不能为空");
        }
        stageLogs.computeIfAbsent(copy.getInstanceId(), key -> new CopyOnWriteArrayList<>()).add(copy);
        return copy;
    }

    @Override
    public List<WorkflowStageLogDTO> listStageLogs(String instanceId, String stageCode) {
        if (!StringUtils.hasText(instanceId)) {
            return List.of();
        }
        List<WorkflowStageLogDTO> result = new ArrayList<>();
        stageLogs.getOrDefault(instanceId, List.of()).stream()
                .filter(item -> !StringUtils.hasText(stageCode) || stageCode.equals(item.getStageCode()))
                .map(item -> item.toBuilder().build())
                .forEach(result::add);
        return result;
    }
}
