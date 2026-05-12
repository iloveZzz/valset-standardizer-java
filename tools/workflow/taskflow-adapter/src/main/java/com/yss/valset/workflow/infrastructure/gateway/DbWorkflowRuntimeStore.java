package com.yss.valset.workflow.infrastructure.gateway;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yss.cloud.dto.response.PageResult;
import com.yss.valset.common.support.DatabaseDialectSupport;
import com.yss.valset.workflow.infrastructure.entity.WorkflowDefinitionPO;
import com.yss.valset.workflow.infrastructure.entity.WorkflowEngineBindingPO;
import com.yss.valset.workflow.infrastructure.entity.WorkflowInstancePO;
import com.yss.valset.workflow.infrastructure.entity.WorkflowStagePO;
import com.yss.valset.workflow.infrastructure.mapper.WorkflowDefinitionRepository;
import com.yss.valset.workflow.infrastructure.mapper.WorkflowEngineBindingRepository;
import com.yss.valset.workflow.infrastructure.mapper.WorkflowInstanceRepository;
import com.yss.valset.workflow.infrastructure.mapper.WorkflowStageRepository;
import com.yss.valset.workflow.infrastructure.support.WorkflowJsonCodec;
import com.yss.valset.workflow.model.WorkflowDefinitionDTO;
import com.yss.valset.workflow.model.WorkflowEngineBindingDTO;
import com.yss.valset.workflow.model.WorkflowInstanceQueryRequest;
import com.yss.valset.workflow.model.WorkflowInstanceDTO;
import com.yss.valset.workflow.model.WorkflowInstanceViewDTO;
import com.yss.valset.workflow.model.WorkflowStageDTO;
import com.yss.valset.workflow.model.WorkflowStatus;
import com.yss.valset.workflow.model.WorkflowSyncStatus;
import com.yss.valset.workflow.spi.WorkflowRuntimeStore;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 基于 MyBatis 的工作流运行态存储。
 */
@Primary
@Component
@RequiredArgsConstructor
public class DbWorkflowRuntimeStore implements WorkflowRuntimeStore {

    private static final String DOLPHINSCHEDULER_SYNC_KEY = "dolphinschedulerSync";
    private static final String SYNC_STATUS_KEY = "syncStatus";
    private static final String FIRST_SYNCED_AT_KEY = "firstSyncedAt";
    private static final String LAST_SYNCED_AT_KEY = "lastSyncedAt";
    private static final String SYNC_FAILURE_REASON_KEY = "syncFailureReason";
    private static final String REMOTE_WORKFLOW_VERSION_NO_KEY = "remoteWorkflowVersionNo";
    private static final String EXTERNAL_ONLINE_KEY = "externalOnline";
    private static final String EXTERNAL_RELEASE_STATE_KEY = "externalReleaseState";

    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowStageRepository workflowStageRepository;
    private final WorkflowEngineBindingRepository workflowEngineBindingRepository;
    private final WorkflowInstanceRepository workflowInstanceRepository;
    private final WorkflowJsonCodec workflowJsonCodec;
    private final DatabaseDialectSupport databaseDialectSupport;

    @Override
    public WorkflowDefinitionDTO saveDefinition(WorkflowDefinitionDTO definition) {
        WorkflowDefinitionDTO copy = requireDefinition(definition);
        WorkflowDefinitionPO existing = workflowDefinitionRepository.selectOne(
                Wrappers.lambdaQuery(WorkflowDefinitionPO.class)
                        .eq(WorkflowDefinitionPO::getWorkflowCode, copy.getWorkflowCode())
                        .eq(WorkflowDefinitionPO::getWorkflowVersionNo, copy.getWorkflowVersionNo())
                        .last(databaseDialectSupport.limitClause(1)));
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
    public boolean deleteDefinition(String workflowCode, Integer workflowVersionNo) {
        if (!StringUtils.hasText(workflowCode) || workflowVersionNo == null) {
            return false;
        }
        WorkflowDefinitionPO definition = workflowDefinitionRepository.selectOne(
                Wrappers.lambdaQuery(WorkflowDefinitionPO.class)
                        .eq(WorkflowDefinitionPO::getWorkflowCode, workflowCode)
                        .eq(WorkflowDefinitionPO::getWorkflowVersionNo, workflowVersionNo)
                        .last(databaseDialectSupport.limitClause(1)));
        if (definition == null) {
            return false;
        }
        workflowStageRepository.delete(
                Wrappers.lambdaQuery(WorkflowStagePO.class)
                        .eq(WorkflowStagePO::getWorkflowId, definition.getWorkflowId()));
        workflowEngineBindingRepository.delete(
                Wrappers.lambdaQuery(WorkflowEngineBindingPO.class)
                        .eq(WorkflowEngineBindingPO::getWorkflowId, definition.getWorkflowId()));
        workflowDefinitionRepository.deleteById(definition.getWorkflowId());
        return true;
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
                        .last(databaseDialectSupport.limitClause(1)));
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
    public PageResult<WorkflowInstanceViewDTO> listInstances(WorkflowInstanceQueryRequest request) {
        int pageIndex = normalizePageIndex(request == null ? null : request.getPageIndex());
        int pageSize = normalizePageSize(request == null ? null : request.getPageSize());
        Page<WorkflowInstancePO> page = workflowInstanceRepository.selectPage(
                new Page<>(pageIndex + 1L, pageSize),
                buildInstanceQuery(request));
        List<WorkflowInstanceViewDTO> records = page.getRecords().stream()
                .map(this::toInstanceViewDTO)
                .toList();
        return PageResult.of(records, page.getTotal(), page.getSize(), pageIndex);
    }

    @Override
    public Optional<WorkflowInstanceDTO> findInstance(String instanceId) {
        if (!StringUtils.hasText(instanceId)) {
            return Optional.empty();
        }
        WorkflowInstancePO po = workflowInstanceRepository.selectById(instanceId);
        return Optional.ofNullable(po).map(this::toInstanceDTO);
    }

    private Optional<WorkflowInstanceDTO> loadInstance(String instanceId) {
        return Optional.ofNullable(workflowInstanceRepository.selectById(instanceId)).map(this::toInstanceDTO);
    }

    private WorkflowDefinitionPO resolveDefinition(String workflowCode, Integer workflowVersionNo) {
        WorkflowDefinitionPO definition = workflowDefinitionRepository.selectOne(
                Wrappers.lambdaQuery(WorkflowDefinitionPO.class)
                        .eq(WorkflowDefinitionPO::getWorkflowCode, workflowCode)
                        .eq(WorkflowDefinitionPO::getWorkflowVersionNo, workflowVersionNo)
                        .last(databaseDialectSupport.limitClause(1)));
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
        po.setCurrentStageCode(instance.getCurrentStageCode());
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

    private com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WorkflowInstancePO> buildInstanceQuery(WorkflowInstanceQueryRequest request) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WorkflowInstancePO> query = Wrappers.lambdaQuery(WorkflowInstancePO.class)
                .orderByDesc(WorkflowInstancePO::getTriggerTime)
                .orderByDesc(WorkflowInstancePO::getUpdatedAt)
                .orderByDesc(WorkflowInstancePO::getInstanceId);
        if (request == null) {
            return query;
        }
        if (StringUtils.hasText(request.getWorkflowCode())) {
            query.eq(WorkflowInstancePO::getWorkflowCode, request.getWorkflowCode().trim());
        }
        if (request.getWorkflowVersionNo() != null) {
            query.eq(WorkflowInstancePO::getWorkflowVersionNo, request.getWorkflowVersionNo());
        }
        if (StringUtils.hasText(request.getWorkflowName())) {
            List<String> workflowIds = workflowDefinitionRepository.selectList(
                            Wrappers.lambdaQuery(WorkflowDefinitionPO.class)
                                    .like(WorkflowDefinitionPO::getWorkflowName, request.getWorkflowName().trim()))
                    .stream()
                    .map(WorkflowDefinitionPO::getWorkflowId)
                    .filter(StringUtils::hasText)
                    .toList();
            if (workflowIds.isEmpty()) {
                query.eq(WorkflowInstancePO::getWorkflowId, "__NO_WORKFLOW_MATCH__");
            } else {
                query.in(WorkflowInstancePO::getWorkflowId, workflowIds);
            }
        }
        if (request.getPlatformType() != null) {
            query.eq(WorkflowInstancePO::getPlatformType, request.getPlatformType());
        }
        if (StringUtils.hasText(request.getStatus())) {
            query.eq(WorkflowInstancePO::getStatus, request.getStatus().trim());
        }
        if (StringUtils.hasText(request.getBusinessKey())) {
            query.like(WorkflowInstancePO::getBusinessKey, request.getBusinessKey().trim());
        }
        if (StringUtils.hasText(request.getInstanceId())) {
            query.like(WorkflowInstancePO::getInstanceId, request.getInstanceId().trim());
        }
        if (StringUtils.hasText(request.getExternalInstanceId())) {
            query.like(WorkflowInstancePO::getExternalInstanceId, request.getExternalInstanceId().trim());
        }
        if (StringUtils.hasText(request.getStageCode())) {
            query.eq(WorkflowInstancePO::getCurrentStageCode, request.getStageCode().trim());
        }
        LocalDateTime from = parseBoundary(request.getTriggerTimeFrom(), false);
        LocalDateTime to = parseBoundary(request.getTriggerTimeTo(), true);
        if (from != null) {
            query.ge(WorkflowInstancePO::getTriggerTime, from);
        }
        if (to != null) {
            query.le(WorkflowInstancePO::getTriggerTime, to);
        }
        return query;
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
        WorkflowEngineBindingPO existing = workflowEngineBindingRepository.selectOne(
                Wrappers.lambdaQuery(WorkflowEngineBindingPO.class)
                        .eq(WorkflowEngineBindingPO::getWorkflowId, workflowId)
                        .last(databaseDialectSupport.limitClause(1)));
        workflowEngineBindingRepository.delete(
                Wrappers.lambdaQuery(WorkflowEngineBindingPO.class)
                        .eq(WorkflowEngineBindingPO::getWorkflowId, workflowId));
        if (binding == null) {
            return;
        }
        Map<String, Object> attributes = new LinkedHashMap<>(
                binding.getAttributes() == null ? Map.of() : binding.getAttributes());
        Map<String, Object> existingAttributes = existing == null
                ? new LinkedHashMap<>()
                : workflowJsonCodec.toMap(existing.getAttributesJson());
        Map<String, Object> syncState = buildSyncState(binding, existingAttributes);
        if (!syncState.isEmpty()) {
            attributes.put(DOLPHINSCHEDULER_SYNC_KEY, syncState);
        } else {
            attributes.remove(DOLPHINSCHEDULER_SYNC_KEY);
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
        po.setAttributesJson(workflowJsonCodec.toJson(attributes));
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
                        .last(databaseDialectSupport.limitClause(1)));
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
        Map<String, Object> attributes = workflowJsonCodec.toMap(po.getAttributesJson());
        Map<String, Object> syncState = asMap(attributes.get(DOLPHINSCHEDULER_SYNC_KEY));
        return WorkflowEngineBindingDTO.builder()
                .platformType(po.getPlatformType())
                .externalWorkflowId(po.getExternalWorkflowId())
                .externalProjectCode(po.getExternalProjectCode())
                .externalNamespace(po.getExternalNamespace())
                .externalJobGroup(po.getExternalJobGroup())
                .externalJobHandler(po.getExternalJobHandler())
                .configJson(po.getConfigJson())
                .syncStatus(parseSyncStatus(syncState == null ? null : syncState.get(SYNC_STATUS_KEY)))
                .firstSyncedAt(parseDateTime(syncState == null ? null : syncState.get(FIRST_SYNCED_AT_KEY)))
                .lastSyncedAt(parseDateTime(syncState == null ? null : syncState.get(LAST_SYNCED_AT_KEY)))
                .syncFailureReason(syncState == null ? null : valueOf(syncState.get(SYNC_FAILURE_REASON_KEY)))
                .remoteWorkflowVersionNo(parseInteger(syncState == null ? null : syncState.get(REMOTE_WORKFLOW_VERSION_NO_KEY)))
                .externalOnline(parseBoolean(syncState == null ? null : syncState.get(EXTERNAL_ONLINE_KEY)))
                .externalReleaseState(syncState == null ? null : valueOf(syncState.get(EXTERNAL_RELEASE_STATE_KEY)))
                .attributes(attributes)
                .build();
    }

    private WorkflowInstanceDTO toInstanceDTO(WorkflowInstancePO po) {
        return WorkflowInstanceDTO.builder()
                .instanceId(po.getInstanceId())
                .workflowCode(po.getWorkflowCode())
                .workflowVersionNo(po.getWorkflowVersionNo())
                .platformType(po.getPlatformType())
                .currentStageCode(po.getCurrentStageCode())
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
                .build();
    }

    private WorkflowInstanceViewDTO toInstanceViewDTO(WorkflowInstancePO po) {
        WorkflowStagePO currentStage = null;
        if (StringUtils.hasText(po.getCurrentStageCode())) {
            currentStage = workflowStageRepository.selectOne(
                    Wrappers.lambdaQuery(WorkflowStagePO.class)
                            .eq(WorkflowStagePO::getWorkflowId, po.getWorkflowId())
                            .eq(WorkflowStagePO::getStageCode, po.getCurrentStageCode())
                            .last(databaseDialectSupport.limitClause(1)));
        }
        Long stageCount = workflowStageRepository.selectCount(
                Wrappers.lambdaQuery(WorkflowStagePO.class)
                        .eq(WorkflowStagePO::getWorkflowId, po.getWorkflowId()));
        WorkflowDefinitionPO definition = workflowDefinitionRepository.selectOne(
                Wrappers.lambdaQuery(WorkflowDefinitionPO.class)
                        .eq(WorkflowDefinitionPO::getWorkflowId, po.getWorkflowId())
                        .last(databaseDialectSupport.limitClause(1)));
        return WorkflowInstanceViewDTO.builder()
                .instanceId(po.getInstanceId())
                .workflowCode(po.getWorkflowCode())
                .workflowName(definition == null ? po.getWorkflowCode() : definition.getWorkflowName())
                .workflowVersionNo(po.getWorkflowVersionNo())
                .platformType(po.getPlatformType())
                .businessKey(po.getBusinessKey())
                .externalInstanceId(po.getExternalInstanceId())
                .externalWorkflowId(po.getExternalWorkflowId())
                .status(po.getStatus() == null ? null : WorkflowStatus.valueOf(po.getStatus()))
                .rawStatus(po.getRawStatus())
                .currentStageCode(po.getCurrentStageCode())
                .currentStageName(currentStage == null ? null : currentStage.getStageName())
                .triggerTime(po.getTriggerTime())
                .startTime(po.getStartTime())
                .duration(formatDuration(po.getStartTime(), po.getEndTime()))
                .endTime(po.getEndTime())
                .message(po.getMessage())
                .stageCount(stageCount == null ? 0 : stageCount.intValue())
                .build();
    }

    private String formatDuration(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null || endTime.isBefore(startTime)) {
            return null;
        }
        Duration elapsed = Duration.between(startTime, endTime);
        long seconds = elapsed.getSeconds();
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

    private String generateId(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
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
                LocalDate date = LocalDate.parse(text);
                return endOfDay ? date.atTime(23, 59, 59) : date.atStartOfDay();
            }
            return LocalDateTime.parse(text.replace(" ", "T"));
        } catch (Exception ignored) {
            return null;
        }
    }

    private Map<String, Object> buildSyncState(WorkflowEngineBindingDTO binding, Map<String, Object> existingAttributes) {
        Map<String, Object> bindingAttributes = binding.getAttributes() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(binding.getAttributes());
        Map<String, Object> existingSyncState = asMap(existingAttributes.get(DOLPHINSCHEDULER_SYNC_KEY));
        Map<String, Object> bindingSyncState = asMap(bindingAttributes.get(DOLPHINSCHEDULER_SYNC_KEY));
        Map<String, Object> baseSyncState = bindingSyncState != null
                ? bindingSyncState
                : existingSyncState == null ? Map.of() : existingSyncState;
        Map<String, Object> syncState = new LinkedHashMap<>(baseSyncState);
        if (binding.getSyncStatus() != null) {
            syncState.put(SYNC_STATUS_KEY, binding.getSyncStatus().name());
        } else if (existingSyncState != null && existingSyncState.get(SYNC_STATUS_KEY) != null) {
            syncState.put(SYNC_STATUS_KEY, valueOf(existingSyncState.get(SYNC_STATUS_KEY)));
        }
        if (binding.getFirstSyncedAt() != null) {
            syncState.put(FIRST_SYNCED_AT_KEY, binding.getFirstSyncedAt().toString());
        } else if (existingSyncState != null && existingSyncState.get(FIRST_SYNCED_AT_KEY) != null) {
            syncState.put(FIRST_SYNCED_AT_KEY, valueOf(existingSyncState.get(FIRST_SYNCED_AT_KEY)));
        }
        if (binding.getLastSyncedAt() != null) {
            syncState.put(LAST_SYNCED_AT_KEY, binding.getLastSyncedAt().toString());
        } else if (existingSyncState != null && existingSyncState.get(LAST_SYNCED_AT_KEY) != null) {
            syncState.put(LAST_SYNCED_AT_KEY, valueOf(existingSyncState.get(LAST_SYNCED_AT_KEY)));
        }
        if (StringUtils.hasText(binding.getSyncFailureReason())) {
            syncState.put(SYNC_FAILURE_REASON_KEY, binding.getSyncFailureReason());
        } else if (existingSyncState != null && existingSyncState.get(SYNC_FAILURE_REASON_KEY) != null) {
            syncState.put(SYNC_FAILURE_REASON_KEY, valueOf(existingSyncState.get(SYNC_FAILURE_REASON_KEY)));
        }
        if (binding.getRemoteWorkflowVersionNo() != null) {
            syncState.put(REMOTE_WORKFLOW_VERSION_NO_KEY, binding.getRemoteWorkflowVersionNo());
        } else if (existingSyncState != null && existingSyncState.get(REMOTE_WORKFLOW_VERSION_NO_KEY) != null) {
            syncState.put(REMOTE_WORKFLOW_VERSION_NO_KEY, parseInteger(existingSyncState.get(REMOTE_WORKFLOW_VERSION_NO_KEY)));
        }
        if (binding.getExternalOnline() != null) {
            syncState.put(EXTERNAL_ONLINE_KEY, binding.getExternalOnline());
        } else if (existingSyncState != null && existingSyncState.get(EXTERNAL_ONLINE_KEY) != null) {
            syncState.put(EXTERNAL_ONLINE_KEY, parseBoolean(existingSyncState.get(EXTERNAL_ONLINE_KEY)));
        }
        if (StringUtils.hasText(binding.getExternalReleaseState())) {
            syncState.put(EXTERNAL_RELEASE_STATE_KEY, binding.getExternalReleaseState());
        } else if (existingSyncState != null && existingSyncState.get(EXTERNAL_RELEASE_STATE_KEY) != null) {
            syncState.put(EXTERNAL_RELEASE_STATE_KEY, valueOf(existingSyncState.get(EXTERNAL_RELEASE_STATE_KEY)));
        }
        return syncState;
    }

    private WorkflowSyncStatus parseSyncStatus(Object value) {
        String text = valueOf(value);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return WorkflowSyncStatus.valueOf(text);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private LocalDateTime parseDateTime(Object value) {
        String text = valueOf(value);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return LocalDateTime.parse(text);
    }

    private Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = valueOf(value);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return Integer.valueOf(text);
    }

    private Boolean parseBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = valueOf(value);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return Boolean.parseBoolean(text);
    }

    private String valueOf(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, item) -> result.put(String.valueOf(key), item));
            return result;
        }
        return null;
    }

    private LocalDateTime now() {
        return LocalDateTime.now();
    }
}
