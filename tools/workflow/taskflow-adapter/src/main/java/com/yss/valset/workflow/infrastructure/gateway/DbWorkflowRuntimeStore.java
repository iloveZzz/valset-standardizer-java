package com.yss.valset.workflow.infrastructure.gateway;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.valset.workflow.infrastructure.entity.WorkflowDefinitionPO;
import com.yss.valset.workflow.infrastructure.entity.WorkflowEngineBindingPO;
import com.yss.valset.workflow.infrastructure.entity.WorkflowInstancePO;
import com.yss.valset.workflow.infrastructure.entity.WorkflowStageLogPO;
import com.yss.valset.workflow.infrastructure.entity.WorkflowStagePO;
import com.yss.valset.workflow.infrastructure.mapper.WorkflowDefinitionRepository;
import com.yss.valset.workflow.infrastructure.mapper.WorkflowEngineBindingRepository;
import com.yss.valset.workflow.infrastructure.mapper.WorkflowInstanceRepository;
import com.yss.valset.workflow.infrastructure.mapper.WorkflowStageLogRepository;
import com.yss.valset.workflow.infrastructure.mapper.WorkflowStageRepository;
import com.yss.valset.workflow.infrastructure.support.WorkflowJsonCodec;
import com.yss.valset.workflow.model.WorkflowDefinitionDTO;
import com.yss.valset.workflow.model.WorkflowEngineBindingDTO;
import com.yss.valset.workflow.model.WorkflowInstanceDTO;
import com.yss.valset.workflow.model.WorkflowStageDTO;
import com.yss.valset.workflow.model.WorkflowStageLogDTO;
import com.yss.valset.workflow.model.WorkflowStatus;
import com.yss.valset.workflow.spi.WorkflowRuntimeStore;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 基于 MyBatis 的工作流运行态存储。
 */
@Primary
@Component
@RequiredArgsConstructor
public class DbWorkflowRuntimeStore implements WorkflowRuntimeStore {

    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowStageRepository workflowStageRepository;
    private final WorkflowEngineBindingRepository workflowEngineBindingRepository;
    private final WorkflowInstanceRepository workflowInstanceRepository;
    private final WorkflowStageLogRepository workflowStageLogRepository;
    private final WorkflowJsonCodec workflowJsonCodec;

    @Override
    public WorkflowDefinitionDTO saveDefinition(WorkflowDefinitionDTO definition) {
        WorkflowDefinitionDTO copy = requireDefinition(definition);
        WorkflowDefinitionPO existing = workflowDefinitionRepository.selectOne(
                Wrappers.lambdaQuery(WorkflowDefinitionPO.class)
                        .eq(WorkflowDefinitionPO::getWorkflowCode, copy.getWorkflowCode())
                        .eq(WorkflowDefinitionPO::getWorkflowVersionNo, copy.getWorkflowVersionNo())
                        .last("limit 1"));
        WorkflowDefinitionPO po = toDefinitionPO(copy, existing);
        if (existing == null) {
            workflowDefinitionRepository.insert(po);
        } else {
            workflowDefinitionRepository.updateById(po);
        }
        saveStages(po.getWorkflowId(), copy.getStages());
        saveBinding(po.getWorkflowId(), copy.getEngineBinding());
        return findDefinition(copy.getWorkflowCode(), copy.getWorkflowVersionNo()).orElse(copy);
    }

    @Override
    public Optional<WorkflowDefinitionDTO> findDefinition(String workflowCode, Integer workflowVersionNo) {
        if (!StringUtils.hasText(workflowCode) || workflowVersionNo == null) {
            return Optional.empty();
        }
        WorkflowDefinitionPO po = workflowDefinitionRepository.selectOne(
                Wrappers.lambdaQuery(WorkflowDefinitionPO.class)
                        .eq(WorkflowDefinitionPO::getWorkflowCode, workflowCode)
                        .eq(WorkflowDefinitionPO::getWorkflowVersionNo, workflowVersionNo)
                        .last("limit 1"));
        return Optional.ofNullable(po).map(this::toDefinitionDTO);
    }

    @Override
    public List<WorkflowDefinitionDTO> listDefinitions() {
        return workflowDefinitionRepository.selectList(
                        Wrappers.lambdaQuery(WorkflowDefinitionPO.class)
                                .orderByAsc(WorkflowDefinitionPO::getWorkflowCode)
                                .orderByAsc(WorkflowDefinitionPO::getWorkflowVersionNo))
                .stream()
                .map(this::toDefinitionDTO)
                .toList();
    }

    @Override
    public WorkflowInstanceDTO saveInstance(WorkflowInstanceDTO instance) {
        WorkflowInstanceDTO copy = requireInstance(instance);
        WorkflowDefinitionPO definition = resolveDefinition(copy.getWorkflowCode(), copy.getWorkflowVersionNo());
        WorkflowInstancePO po = toInstancePO(copy, definition);
        WorkflowInstancePO existing = workflowInstanceRepository.selectById(po.getInstanceId());
        if (existing != null) {
            po.setCreatedAt(existing.getCreatedAt());
        }
        if (existing == null) {
            workflowInstanceRepository.insert(po);
        } else {
            workflowInstanceRepository.updateById(po);
        }
        return loadInstance(po.getInstanceId()).orElse(copy);
    }

    @Override
    public Optional<WorkflowInstanceDTO> findInstance(String instanceId) {
        if (!StringUtils.hasText(instanceId)) {
            return Optional.empty();
        }
        WorkflowInstancePO po = workflowInstanceRepository.selectById(instanceId);
        return Optional.ofNullable(po).map(this::toInstanceDTO);
    }

    @Override
    public WorkflowStageLogDTO saveStageLog(WorkflowStageLogDTO log) {
        WorkflowStageLogDTO copy = requireStageLog(log);
        WorkflowDefinitionPO definition = resolveDefinition(copy.getWorkflowCode(), copy.getWorkflowVersionNo());
        WorkflowStageLogPO po = toStageLogPO(copy, definition);
        WorkflowStageLogPO existing = workflowStageLogRepository.selectById(po.getLogId());
        if (existing != null) {
            po.setCreatedAt(existing.getCreatedAt());
        }
        if (existing == null) {
            workflowStageLogRepository.insert(po);
        } else {
            workflowStageLogRepository.updateById(po);
        }
        return copy.toBuilder().build();
    }

    @Override
    public List<WorkflowStageLogDTO> listStageLogs(String instanceId, String stageCode) {
        if (!StringUtils.hasText(instanceId)) {
            return List.of();
        }
        return workflowStageLogRepository.selectList(
                        Wrappers.lambdaQuery(WorkflowStageLogPO.class)
                                .eq(WorkflowStageLogPO::getInstanceId, instanceId)
                                .eq(StringUtils.hasText(stageCode), WorkflowStageLogPO::getStageCode, stageCode)
                                .orderByAsc(WorkflowStageLogPO::getStartTime)
                                .orderByAsc(WorkflowStageLogPO::getCreatedAt)
                                .orderByAsc(WorkflowStageLogPO::getLogId))
                .stream()
                .map(this::toStageLogDTO)
                .toList();
    }

    private Optional<WorkflowInstanceDTO> loadInstance(String instanceId) {
        return Optional.ofNullable(workflowInstanceRepository.selectById(instanceId)).map(this::toInstanceDTO);
    }

    private WorkflowDefinitionPO resolveDefinition(String workflowCode, Integer workflowVersionNo) {
        WorkflowDefinitionPO definition = workflowDefinitionRepository.selectOne(
                Wrappers.lambdaQuery(WorkflowDefinitionPO.class)
                        .eq(WorkflowDefinitionPO::getWorkflowCode, workflowCode)
                        .eq(WorkflowDefinitionPO::getWorkflowVersionNo, workflowVersionNo)
                        .last("limit 1"));
        if (definition == null) {
            throw new IllegalStateException("未找到工作流定义");
        }
        return definition;
    }

    private WorkflowDefinitionDTO requireDefinition(WorkflowDefinitionDTO definition) {
        if (definition == null) {
            throw new IllegalArgumentException("工作流定义不能为空");
        }
        return definition.toBuilder()
                .stages(new ArrayList<>(definition.getStages() == null ? List.of() : definition.getStages()))
                .build();
    }

    private WorkflowInstanceDTO requireInstance(WorkflowInstanceDTO instance) {
        if (instance == null) {
            throw new IllegalArgumentException("工作流实例不能为空");
        }
        return instance.toBuilder()
                .context(instance.getContext() == null ? new java.util.LinkedHashMap<>() : new java.util.LinkedHashMap<>(instance.getContext()))
                .stageLogs(instance.getStageLogs() == null ? new ArrayList<>() : new ArrayList<>(instance.getStageLogs()))
                .build();
    }

    private WorkflowStageLogDTO requireStageLog(WorkflowStageLogDTO log) {
        if (log == null) {
            throw new IllegalArgumentException("工作流阶段日志不能为空");
        }
        return log.toBuilder()
                .payload(log.getPayload() == null ? new java.util.LinkedHashMap<>() : new java.util.LinkedHashMap<>(log.getPayload()))
                .build();
    }

    private WorkflowDefinitionPO toDefinitionPO(WorkflowDefinitionDTO definition, WorkflowDefinitionPO existing) {
        WorkflowDefinitionPO po = new WorkflowDefinitionPO();
        po.setWorkflowId(existing == null || !StringUtils.hasText(existing.getWorkflowId())
                ? generateId("wfd")
                : existing.getWorkflowId());
        po.setWorkflowCode(definition.getWorkflowCode());
        po.setWorkflowName(definition.getWorkflowName());
        po.setWorkflowVersionNo(definition.getWorkflowVersionNo());
        po.setPlatformType(definition.getPlatformType());
        po.setDescription(definition.getDescription());
        po.setEnabled(definition.isEnabled());
        po.setCreatedAt(existing == null ? now() : existing.getCreatedAt());
        po.setUpdatedAt(now());
        return po;
    }

    private WorkflowInstancePO toInstancePO(WorkflowInstanceDTO instance, WorkflowDefinitionPO definition) {
        WorkflowInstancePO po = new WorkflowInstancePO();
        po.setInstanceId(StringUtils.hasText(instance.getInstanceId()) ? instance.getInstanceId() : generateId("wfi"));
        po.setWorkflowId(definition.getWorkflowId());
        po.setWorkflowCode(instance.getWorkflowCode());
        po.setWorkflowVersionNo(instance.getWorkflowVersionNo());
        po.setPlatformType(instance.getPlatformType());
        po.setBusinessKey(instance.getBusinessKey());
        po.setCurrentStageCode(null);
        po.setExternalInstanceId(instance.getExternalInstanceId());
        po.setExternalWorkflowId(instance.getExternalWorkflowId());
        po.setStatus(instance.getStatus() == null ? null : instance.getStatus().name());
        po.setRawStatus(instance.getRawStatus());
        po.setTriggerTime(instance.getTriggerTime());
        po.setStartTime(instance.getStartTime());
        po.setEndTime(instance.getEndTime());
        po.setMessage(instance.getMessage());
        po.setContextJson(workflowJsonCodec.toJson(instance.getContext()));
        po.setCreatedAt(now());
        po.setUpdatedAt(now());
        return po;
    }

    private WorkflowStageLogPO toStageLogPO(WorkflowStageLogDTO log, WorkflowDefinitionPO definition) {
        WorkflowStageLogPO po = new WorkflowStageLogPO();
        po.setLogId(generateId("wfl"));
        po.setInstanceId(log.getInstanceId());
        po.setWorkflowId(definition.getWorkflowId());
        po.setWorkflowCode(log.getWorkflowCode());
        po.setWorkflowVersionNo(log.getWorkflowVersionNo());
        po.setStageCode(log.getStageCode());
        po.setStageName(log.getStageName());
        po.setStageOrder(log.getStageOrder());
        po.setStatus(log.getStatus());
        po.setRawStatus(log.getRawStatus());
        po.setMessage(log.getMessage());
        po.setStartTime(log.getStartTime());
        po.setEndTime(log.getEndTime());
        po.setPayloadJson(workflowJsonCodec.toJson(log.getPayload()));
        po.setCreatedAt(now());
        po.setUpdatedAt(now());
        return po;
    }

    private void saveStages(String workflowId, List<WorkflowStageDTO> stages) {
        workflowStageRepository.delete(
                Wrappers.lambdaQuery(WorkflowStagePO.class)
                        .eq(WorkflowStagePO::getWorkflowId, workflowId));
        if (stages == null || stages.isEmpty()) {
            return;
        }
        List<WorkflowStageDTO> orderedStages = stages.stream()
                .sorted(Comparator.comparing(WorkflowStageDTO::getStageOrder))
                .toList();
        for (WorkflowStageDTO stage : orderedStages) {
            WorkflowStagePO po = new WorkflowStagePO();
            po.setStageId(generateId("wfs"));
            po.setWorkflowId(workflowId);
            po.setStageCode(stage.getStageCode());
            po.setStageName(stage.getStageName());
            po.setStageOrder(stage.getStageOrder());
            po.setDescription(stage.getDescription());
            po.setRetryable(stage.isRetryable());
            po.setTimeoutSeconds(stage.getTimeoutSeconds());
            po.setCreatedAt(now());
            po.setUpdatedAt(now());
            workflowStageRepository.insert(po);
        }
    }

    private void saveBinding(String workflowId, WorkflowEngineBindingDTO binding) {
        workflowEngineBindingRepository.delete(
                Wrappers.lambdaQuery(WorkflowEngineBindingPO.class)
                        .eq(WorkflowEngineBindingPO::getWorkflowId, workflowId));
        if (binding == null) {
            return;
        }
        WorkflowEngineBindingPO po = new WorkflowEngineBindingPO();
        po.setBindingId(generateId("wfb"));
        po.setWorkflowId(workflowId);
        po.setPlatformType(binding.getPlatformType());
        po.setExternalWorkflowId(binding.getExternalWorkflowId());
        po.setExternalProjectCode(binding.getExternalProjectCode());
        po.setExternalNamespace(binding.getExternalNamespace());
        po.setExternalJobGroup(binding.getExternalJobGroup());
        po.setExternalJobHandler(binding.getExternalJobHandler());
        po.setConfigJson(binding.getConfigJson());
        po.setAttributesJson(workflowJsonCodec.toJson(binding.getAttributes()));
        po.setCreatedAt(now());
        po.setUpdatedAt(now());
        workflowEngineBindingRepository.insert(po);
    }

    private WorkflowDefinitionDTO toDefinitionDTO(WorkflowDefinitionPO po) {
        List<WorkflowStageDTO> stages = workflowStageRepository.selectList(
                        Wrappers.lambdaQuery(WorkflowStagePO.class)
                                .eq(WorkflowStagePO::getWorkflowId, po.getWorkflowId())
                                .orderByAsc(WorkflowStagePO::getStageOrder)
                                .orderByAsc(WorkflowStagePO::getStageId))
                .stream()
                .map(this::toStageDTO)
                .toList();
        WorkflowEngineBindingPO bindingPO = workflowEngineBindingRepository.selectOne(
                Wrappers.lambdaQuery(WorkflowEngineBindingPO.class)
                        .eq(WorkflowEngineBindingPO::getWorkflowId, po.getWorkflowId())
                        .last("limit 1"));
        return WorkflowDefinitionDTO.builder()
                .workflowCode(po.getWorkflowCode())
                .workflowName(po.getWorkflowName())
                .workflowVersionNo(po.getWorkflowVersionNo())
                .platformType(po.getPlatformType())
                .description(po.getDescription())
                .enabled(Boolean.TRUE.equals(po.getEnabled()))
                .stages(new ArrayList<>(stages))
                .engineBinding(bindingPO == null ? null : toBindingDTO(bindingPO))
                .build();
    }

    private WorkflowStageDTO toStageDTO(WorkflowStagePO po) {
        return WorkflowStageDTO.builder()
                .stageCode(po.getStageCode())
                .stageName(po.getStageName())
                .stageOrder(po.getStageOrder())
                .description(po.getDescription())
                .retryable(Boolean.TRUE.equals(po.getRetryable()))
                .timeoutSeconds(po.getTimeoutSeconds())
                .build();
    }

    private WorkflowEngineBindingDTO toBindingDTO(WorkflowEngineBindingPO po) {
        return WorkflowEngineBindingDTO.builder()
                .platformType(po.getPlatformType())
                .externalWorkflowId(po.getExternalWorkflowId())
                .externalProjectCode(po.getExternalProjectCode())
                .externalNamespace(po.getExternalNamespace())
                .externalJobGroup(po.getExternalJobGroup())
                .externalJobHandler(po.getExternalJobHandler())
                .configJson(po.getConfigJson())
                .attributes(workflowJsonCodec.toMap(po.getAttributesJson()))
                .build();
    }

    private WorkflowInstanceDTO toInstanceDTO(WorkflowInstancePO po) {
        List<WorkflowStageLogDTO> stageLogs = workflowStageLogRepository.selectList(
                        Wrappers.lambdaQuery(WorkflowStageLogPO.class)
                                .eq(WorkflowStageLogPO::getInstanceId, po.getInstanceId())
                                .orderByAsc(WorkflowStageLogPO::getStartTime)
                                .orderByAsc(WorkflowStageLogPO::getCreatedAt)
                                .orderByAsc(WorkflowStageLogPO::getLogId))
                .stream()
                .map(this::toStageLogDTO)
                .toList();
        return WorkflowInstanceDTO.builder()
                .instanceId(po.getInstanceId())
                .workflowCode(po.getWorkflowCode())
                .workflowVersionNo(po.getWorkflowVersionNo())
                .platformType(po.getPlatformType())
                .businessKey(po.getBusinessKey())
                .externalInstanceId(po.getExternalInstanceId())
                .externalWorkflowId(po.getExternalWorkflowId())
                .status(po.getStatus() == null ? null : WorkflowStatus.valueOf(po.getStatus()))
                .rawStatus(po.getRawStatus())
                .triggerTime(po.getTriggerTime())
                .startTime(po.getStartTime())
                .endTime(po.getEndTime())
                .message(po.getMessage())
                .context(workflowJsonCodec.toMap(po.getContextJson()))
                .stageLogs(new ArrayList<>(stageLogs))
                .build();
    }

    private WorkflowStageLogDTO toStageLogDTO(WorkflowStageLogPO po) {
        return WorkflowStageLogDTO.builder()
                .instanceId(po.getInstanceId())
                .workflowCode(po.getWorkflowCode())
                .workflowVersionNo(po.getWorkflowVersionNo())
                .stageCode(po.getStageCode())
                .stageName(po.getStageName())
                .stageOrder(po.getStageOrder())
                .status(po.getStatus())
                .rawStatus(po.getRawStatus())
                .message(po.getMessage())
                .startTime(po.getStartTime())
                .endTime(po.getEndTime())
                .payload(workflowJsonCodec.toMap(po.getPayloadJson()))
                .build();
    }

    private String generateId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }

    private LocalDateTime now() {
        return LocalDateTime.now();
    }
}
