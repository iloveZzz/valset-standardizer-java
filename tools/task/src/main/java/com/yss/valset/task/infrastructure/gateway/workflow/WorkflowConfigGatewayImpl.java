package com.yss.valset.task.infrastructure.gateway.workflow;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yss.cloud.dto.response.PageResult;
import com.yss.valset.task.application.command.workflow.WorkflowConfigQueryCommand;
import com.yss.valset.task.application.dto.workflow.WorkflowDefinitionDTO;
import com.yss.valset.task.application.dto.workflow.WorkflowExecutorBindingDTO;
import com.yss.valset.task.application.dto.workflow.WorkflowStageDTO;
import com.yss.valset.task.application.dto.workflow.WorkflowStatusMappingDTO;
import com.yss.valset.task.application.port.workflow.WorkflowConfigGateway;
import com.yss.valset.task.infrastructure.entity.workflow.WorkflowDefinitionPO;
import com.yss.valset.task.infrastructure.entity.workflow.WorkflowExecutorBindingPO;
import com.yss.valset.task.infrastructure.entity.workflow.WorkflowStageMappingPO;
import com.yss.valset.task.infrastructure.entity.workflow.WorkflowStagePO;
import com.yss.valset.task.infrastructure.entity.workflow.WorkflowStatusMappingPO;
import com.yss.valset.task.infrastructure.mapper.workflow.WorkflowDefinitionRepository;
import com.yss.valset.task.infrastructure.mapper.workflow.WorkflowExecutorBindingRepository;
import com.yss.valset.task.infrastructure.mapper.workflow.WorkflowStageMappingRepository;
import com.yss.valset.task.infrastructure.mapper.workflow.WorkflowStageRepository;
import com.yss.valset.task.infrastructure.mapper.workflow.WorkflowStatusMappingRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * MyBatis 支持的工作流配置网关。
 */
@Component
public class WorkflowConfigGatewayImpl implements WorkflowConfigGateway {

    private static final String MAPPING_TASK_TYPE = "TASK_TYPE";
    private static final String MAPPING_TASK_STAGE = "TASK_STAGE";
    private static final String MAPPING_PARSE_LIFECYCLE = "PARSE_LIFECYCLE";
    private static final String MAPPING_IGNORE_PARSE_LIFECYCLE = "IGNORE_PARSE_LIFECYCLE";
    private static final String MAPPING_IGNORE_WORKFLOW_TASK_TYPE = "IGNORE_WORKFLOW_TASK_TYPE";

    private final WorkflowDefinitionRepository definitionRepository;
    private final WorkflowStageRepository stageRepository;
    private final WorkflowStageMappingRepository stageMappingRepository;
    private final WorkflowStatusMappingRepository statusMappingRepository;
    private final WorkflowExecutorBindingRepository executorBindingRepository;

    public WorkflowConfigGatewayImpl(WorkflowDefinitionRepository definitionRepository,
                                     WorkflowStageRepository stageRepository,
                                     WorkflowStageMappingRepository stageMappingRepository,
                                     WorkflowStatusMappingRepository statusMappingRepository,
                                     WorkflowExecutorBindingRepository executorBindingRepository) {
        this.definitionRepository = definitionRepository;
        this.stageRepository = stageRepository;
        this.stageMappingRepository = stageMappingRepository;
        this.statusMappingRepository = statusMappingRepository;
        this.executorBindingRepository = executorBindingRepository;
    }

    @Override
    public PageResult<WorkflowDefinitionDTO> pageDefinitions(WorkflowConfigQueryCommand query) {
        int pageIndex = normalizePageIndex(query == null ? null : query.getPageIndex());
        int pageSize = normalizePageSize(query == null ? null : query.getPageSize());
        Page<WorkflowDefinitionPO> page = definitionRepository.selectPage(
                new Page<>(pageIndex, pageSize),
                Wrappers.lambdaQuery(WorkflowDefinitionPO.class)
                        .eq(StringUtils.hasText(query == null ? null : query.getWorkflowCode()), WorkflowDefinitionPO::getWorkflowCode, query == null ? null : query.getWorkflowCode())
                        .like(StringUtils.hasText(query == null ? null : query.getWorkflowName()), WorkflowDefinitionPO::getWorkflowName, query == null ? null : query.getWorkflowName())
                        .eq(StringUtils.hasText(query == null ? null : query.getBusinessType()), WorkflowDefinitionPO::getBusinessType, query == null ? null : query.getBusinessType())
                        .eq(StringUtils.hasText(query == null ? null : query.getEngineType()), WorkflowDefinitionPO::getEngineType, query == null ? null : query.getEngineType())
                        .eq(StringUtils.hasText(query == null ? null : query.getStatus()), WorkflowDefinitionPO::getStatus, query == null ? null : query.getStatus())
                        .eq(query != null && query.getEnabled() != null, WorkflowDefinitionPO::getEnabled, query == null ? null : query.getEnabled())
                        .orderByDesc(WorkflowDefinitionPO::getUpdatedAt)
                        .orderByDesc(WorkflowDefinitionPO::getWorkflowId));
        List<WorkflowDefinitionDTO> records = page.getRecords().stream()
                .map(this::toDefinitionDTO)
                .toList();
        return PageResult.of(records, page.getTotal(), pageSize, pageIndex);
    }

    @Override
    public Optional<WorkflowDefinitionDTO> findById(String workflowId) {
        if (!StringUtils.hasText(workflowId)) {
            return Optional.empty();
        }
        WorkflowDefinitionPO po = definitionRepository.selectById(workflowId);
        return Optional.ofNullable(po).map(this::toDefinitionDetailDTO);
    }

    @Override
    public Optional<WorkflowDefinitionDTO> findActiveByCode(String workflowCode) {
        if (!StringUtils.hasText(workflowCode)) {
            return Optional.empty();
        }
        WorkflowDefinitionPO po = definitionRepository.selectOne(
                Wrappers.lambdaQuery(WorkflowDefinitionPO.class)
                        .eq(WorkflowDefinitionPO::getWorkflowCode, workflowCode)
                        .eq(WorkflowDefinitionPO::getEnabled, true)
                        .eq(WorkflowDefinitionPO::getStatus, "PUBLISHED")
                        .orderByDesc(WorkflowDefinitionPO::getVersionNo)
                        .last("limit 1"));
        return Optional.ofNullable(po).map(this::toDefinitionDetailDTO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowDefinitionDTO save(WorkflowDefinitionDTO definition) {
        LocalDateTime now = LocalDateTime.now();
        WorkflowDefinitionPO po = toDefinitionPO(definition);
        if (!StringUtils.hasText(po.getWorkflowId())) {
            po.setWorkflowId(newId());
            po.setCreatedAt(now);
            po.setUpdatedAt(now);
            definitionRepository.insert(po);
        } else {
            WorkflowDefinitionPO existing = definitionRepository.selectById(po.getWorkflowId());
            po.setCreatedAt(existing == null ? now : existing.getCreatedAt());
            po.setUpdatedAt(now);
            if (existing == null) {
                definitionRepository.insert(po);
            } else {
                definitionRepository.updateById(po);
            }
        }
        rewriteChildren(po.getWorkflowId(), definition, now);
        return toDefinitionDetailDTO(definitionRepository.selectById(po.getWorkflowId()));
    }

    @Override
    public void disableOtherVersions(String workflowCode, String keepWorkflowId) {
        if (!StringUtils.hasText(workflowCode)) {
            return;
        }
        definitionRepository.update(
                null,
                Wrappers.lambdaUpdate(WorkflowDefinitionPO.class)
                        .set(WorkflowDefinitionPO::getEnabled, false)
                        .set(WorkflowDefinitionPO::getUpdatedAt, LocalDateTime.now())
                        .eq(WorkflowDefinitionPO::getWorkflowCode, workflowCode)
                        .ne(StringUtils.hasText(keepWorkflowId), WorkflowDefinitionPO::getWorkflowId, keepWorkflowId));
    }

    @Override
    public void updateStatus(String workflowId, String status, Boolean enabled) {
        if (!StringUtils.hasText(workflowId)) {
            return;
        }
        definitionRepository.update(
                null,
                Wrappers.lambdaUpdate(WorkflowDefinitionPO.class)
                        .set(WorkflowDefinitionPO::getStatus, status)
                        .set(WorkflowDefinitionPO::getEnabled, enabled)
                        .set(WorkflowDefinitionPO::getUpdatedAt, LocalDateTime.now())
                        .eq(WorkflowDefinitionPO::getWorkflowId, workflowId));
    }

    private void rewriteChildren(String workflowId, WorkflowDefinitionDTO definition, LocalDateTime now) {
        stageRepository.delete(Wrappers.lambdaQuery(WorkflowStagePO.class).eq(WorkflowStagePO::getWorkflowId, workflowId));
        stageMappingRepository.delete(Wrappers.lambdaQuery(WorkflowStageMappingPO.class).eq(WorkflowStageMappingPO::getWorkflowId, workflowId));
        statusMappingRepository.delete(Wrappers.lambdaQuery(WorkflowStatusMappingPO.class).eq(WorkflowStatusMappingPO::getWorkflowId, workflowId));
        executorBindingRepository.delete(Wrappers.lambdaQuery(WorkflowExecutorBindingPO.class).eq(WorkflowExecutorBindingPO::getWorkflowId, workflowId));

        Map<String, String> stageIdByCode = new HashMap<>();
        List<WorkflowStageDTO> stages = definition.getStages() == null ? List.of() : definition.getStages();
        for (int i = 0; i < stages.size(); i++) {
            WorkflowStageDTO stage = stages.get(i);
            WorkflowStagePO stagePO = toStagePO(workflowId, stage, i, now);
            stageRepository.insert(stagePO);
            stageIdByCode.put(stagePO.getStageCode(), stagePO.getStageId());
            insertStageMappings(workflowId, stagePO.getStageId(), MAPPING_TASK_TYPE, stage.getTaskTypes(), false, now);
            insertStageMappings(workflowId, stagePO.getStageId(), MAPPING_TASK_STAGE, stage.getTaskStages(), false, now);
            insertStageMappings(workflowId, stagePO.getStageId(), MAPPING_PARSE_LIFECYCLE, stage.getParseLifecycleStages(), false, now);
        }
        insertStageMappings(workflowId, null, MAPPING_IGNORE_PARSE_LIFECYCLE, definition.getIgnoredParseLifecycleStages(), true, now);
        insertStageMappings(workflowId, null, MAPPING_IGNORE_WORKFLOW_TASK_TYPE, definition.getIgnoredWorkflowTaskTypes(), true, now);
        for (WorkflowStatusMappingDTO mapping : definition.getStatusMappings() == null ? List.<WorkflowStatusMappingDTO>of() : definition.getStatusMappings()) {
            statusMappingRepository.insert(toStatusMappingPO(workflowId, mapping, now));
        }
        for (WorkflowExecutorBindingDTO binding : definition.getExecutorBindings() == null ? List.<WorkflowExecutorBindingDTO>of() : definition.getExecutorBindings()) {
            executorBindingRepository.insert(toExecutorBindingPO(workflowId, binding, stageIdByCode, now));
        }
    }

    private void insertStageMappings(String workflowId, String stageId, String mappingType, List<String> values, boolean ignored, LocalDateTime now) {
        if (values == null) {
            return;
        }
        values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .forEach(value -> {
                    WorkflowStageMappingPO po = new WorkflowStageMappingPO();
                    po.setMappingId(newId());
                    po.setWorkflowId(workflowId);
                    po.setStageId(stageId);
                    po.setMappingType(mappingType);
                    po.setMappingValue(value);
                    po.setIgnored(ignored);
                    po.setCreatedAt(now);
                    stageMappingRepository.insert(po);
                });
    }

    private WorkflowDefinitionDTO toDefinitionDetailDTO(WorkflowDefinitionPO po) {
        WorkflowDefinitionDTO dto = toDefinitionDTO(po);
        if (dto == null) {
            return null;
        }
        List<WorkflowStagePO> stages = stageRepository.selectList(
                Wrappers.lambdaQuery(WorkflowStagePO.class)
                        .eq(WorkflowStagePO::getWorkflowId, po.getWorkflowId())
                        .orderByAsc(WorkflowStagePO::getSortOrder)
                        .orderByAsc(WorkflowStagePO::getStageId));
        List<WorkflowStageMappingPO> stageMappings = stageMappingRepository.selectList(
                Wrappers.lambdaQuery(WorkflowStageMappingPO.class).eq(WorkflowStageMappingPO::getWorkflowId, po.getWorkflowId()));
        Map<String, List<WorkflowStageMappingPO>> mappingByStageId = stageMappings.stream()
                .filter(mapping -> StringUtils.hasText(mapping.getStageId()))
                .collect(Collectors.groupingBy(WorkflowStageMappingPO::getStageId));
        dto.setStages(stages.stream()
                .map(stage -> toStageDTO(stage, mappingByStageId.getOrDefault(stage.getStageId(), List.of())))
                .toList());
        dto.setIgnoredParseLifecycleStages(filterMappingValues(stageMappings, MAPPING_IGNORE_PARSE_LIFECYCLE));
        dto.setIgnoredWorkflowTaskTypes(filterMappingValues(stageMappings, MAPPING_IGNORE_WORKFLOW_TASK_TYPE));
        dto.setStatusMappings(statusMappingRepository.selectList(
                        Wrappers.lambdaQuery(WorkflowStatusMappingPO.class).eq(WorkflowStatusMappingPO::getWorkflowId, po.getWorkflowId()))
                .stream()
                .map(this::toStatusMappingDTO)
                .toList());
        Map<String, String> stageCodeById = stages.stream()
                .collect(Collectors.toMap(WorkflowStagePO::getStageId, WorkflowStagePO::getStageCode, (left, right) -> left));
        dto.setExecutorBindings(executorBindingRepository.selectList(
                        Wrappers.lambdaQuery(WorkflowExecutorBindingPO.class).eq(WorkflowExecutorBindingPO::getWorkflowId, po.getWorkflowId()))
                .stream()
                .map(binding -> toExecutorBindingDTO(binding, stageCodeById))
                .toList());
        return dto;
    }

    private List<String> filterMappingValues(List<WorkflowStageMappingPO> mappings, String mappingType) {
        return mappings.stream()
                .filter(mapping -> Objects.equals(mapping.getMappingType(), mappingType))
                .map(WorkflowStageMappingPO::getMappingValue)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private WorkflowDefinitionDTO toDefinitionDTO(WorkflowDefinitionPO po) {
        if (po == null) {
            return null;
        }
        WorkflowDefinitionDTO dto = new WorkflowDefinitionDTO();
        dto.setWorkflowId(po.getWorkflowId());
        dto.setWorkflowCode(po.getWorkflowCode());
        dto.setWorkflowName(po.getWorkflowName());
        dto.setBusinessType(po.getBusinessType());
        dto.setEngineType(po.getEngineType());
        dto.setParseFallbackStage(po.getParseFallbackStage());
        dto.setWorkflowFallbackStage(po.getWorkflowFallbackStage());
        dto.setVersionNo(po.getVersionNo());
        dto.setEnabled(po.getEnabled());
        dto.setStatus(po.getStatus());
        dto.setDescription(po.getDescription());
        dto.setCreatedAt(po.getCreatedAt());
        dto.setUpdatedAt(po.getUpdatedAt());
        return dto;
    }

    private WorkflowDefinitionPO toDefinitionPO(WorkflowDefinitionDTO dto) {
        WorkflowDefinitionPO po = new WorkflowDefinitionPO();
        po.setWorkflowId(dto.getWorkflowId());
        po.setWorkflowCode(trim(dto.getWorkflowCode()));
        po.setWorkflowName(trim(dto.getWorkflowName()));
        po.setBusinessType(defaultText(dto.getBusinessType(), "VALUATION"));
        po.setEngineType(defaultText(dto.getEngineType(), "INTERNAL"));
        po.setParseFallbackStage(defaultText(dto.getParseFallbackStage(), "FILE_PARSE"));
        po.setWorkflowFallbackStage(defaultText(dto.getWorkflowFallbackStage(), "DATA_PROCESSING"));
        po.setVersionNo(dto.getVersionNo() == null ? 1 : dto.getVersionNo());
        po.setEnabled(Boolean.TRUE.equals(dto.getEnabled()));
        po.setStatus(defaultText(dto.getStatus(), "DRAFT"));
        po.setDescription(dto.getDescription());
        po.setCreatedAt(dto.getCreatedAt());
        po.setUpdatedAt(dto.getUpdatedAt());
        return po;
    }

    private WorkflowStagePO toStagePO(String workflowId, WorkflowStageDTO dto, int index, LocalDateTime now) {
        WorkflowStagePO po = new WorkflowStagePO();
        po.setStageId(StringUtils.hasText(dto.getStageId()) ? dto.getStageId() : newId());
        po.setWorkflowId(workflowId);
        po.setStageCode(trim(dto.getStageCode()));
        po.setStepCode(defaultText(dto.getStepCode(), dto.getStageCode()));
        po.setStageName(trim(dto.getStageName()));
        po.setStepName(defaultText(dto.getStepName(), dto.getStageName()));
        po.setStageDescription(dto.getStageDescription());
        po.setStepDescription(defaultText(dto.getStepDescription(), dto.getStageDescription()));
        po.setSortOrder(dto.getSortOrder() == null ? index + 1 : dto.getSortOrder());
        po.setRetryable(Boolean.TRUE.equals(dto.getRetryable()));
        po.setSkippable(Boolean.TRUE.equals(dto.getSkippable()));
        po.setEnabled(dto.getEnabled() == null || Boolean.TRUE.equals(dto.getEnabled()));
        po.setCreatedAt(now);
        po.setUpdatedAt(now);
        return po;
    }

    private WorkflowStageDTO toStageDTO(WorkflowStagePO po, List<WorkflowStageMappingPO> mappings) {
        WorkflowStageDTO dto = new WorkflowStageDTO();
        dto.setStageId(po.getStageId());
        dto.setWorkflowId(po.getWorkflowId());
        dto.setStageCode(po.getStageCode());
        dto.setStepCode(po.getStepCode());
        dto.setStageName(po.getStageName());
        dto.setStepName(po.getStepName());
        dto.setStageDescription(po.getStageDescription());
        dto.setStepDescription(po.getStepDescription());
        dto.setSortOrder(po.getSortOrder());
        dto.setRetryable(po.getRetryable());
        dto.setSkippable(po.getSkippable());
        dto.setEnabled(po.getEnabled());
        dto.setTaskTypes(valuesByType(mappings, MAPPING_TASK_TYPE));
        dto.setTaskStages(valuesByType(mappings, MAPPING_TASK_STAGE));
        dto.setParseLifecycleStages(valuesByType(mappings, MAPPING_PARSE_LIFECYCLE));
        return dto;
    }

    private List<String> valuesByType(List<WorkflowStageMappingPO> mappings, String type) {
        return mappings.stream()
                .filter(mapping -> Objects.equals(mapping.getMappingType(), type))
                .sorted(Comparator.comparing(WorkflowStageMappingPO::getMappingId))
                .map(WorkflowStageMappingPO::getMappingValue)
                .filter(StringUtils::hasText)
                .toList();
    }

    private WorkflowStatusMappingPO toStatusMappingPO(String workflowId, WorkflowStatusMappingDTO dto, LocalDateTime now) {
        WorkflowStatusMappingPO po = new WorkflowStatusMappingPO();
        po.setMappingId(StringUtils.hasText(dto.getMappingId()) ? dto.getMappingId() : newId());
        po.setWorkflowId(workflowId);
        po.setSourceType(defaultText(dto.getSourceType(), "WORKFLOW_TASK"));
        po.setSourceStatus(trim(dto.getSourceStatus()));
        po.setTargetStatus(trim(dto.getTargetStatus()));
        po.setStatusLabel(dto.getStatusLabel());
        po.setCreatedAt(now);
        return po;
    }

    private WorkflowStatusMappingDTO toStatusMappingDTO(WorkflowStatusMappingPO po) {
        WorkflowStatusMappingDTO dto = new WorkflowStatusMappingDTO();
        dto.setMappingId(po.getMappingId());
        dto.setWorkflowId(po.getWorkflowId());
        dto.setSourceType(po.getSourceType());
        dto.setSourceStatus(po.getSourceStatus());
        dto.setTargetStatus(po.getTargetStatus());
        dto.setStatusLabel(po.getStatusLabel());
        return dto;
    }

    private WorkflowExecutorBindingPO toExecutorBindingPO(String workflowId, WorkflowExecutorBindingDTO dto, Map<String, String> stageIdByCode, LocalDateTime now) {
        WorkflowExecutorBindingPO po = new WorkflowExecutorBindingPO();
        po.setBindingId(StringUtils.hasText(dto.getBindingId()) ? dto.getBindingId() : newId());
        po.setWorkflowId(workflowId);
        po.setStageId(StringUtils.hasText(dto.getStageId()) ? dto.getStageId() : stageIdByCode.get(dto.getStageCode()));
        po.setEngineType(defaultText(dto.getEngineType(), "INTERNAL"));
        po.setExternalRef(dto.getExternalRef());
        po.setConfigJson(dto.getConfigJson());
        po.setEnabled(dto.getEnabled() == null || Boolean.TRUE.equals(dto.getEnabled()));
        po.setCreatedAt(now);
        po.setUpdatedAt(now);
        return po;
    }

    private WorkflowExecutorBindingDTO toExecutorBindingDTO(WorkflowExecutorBindingPO po, Map<String, String> stageCodeById) {
        WorkflowExecutorBindingDTO dto = new WorkflowExecutorBindingDTO();
        dto.setBindingId(po.getBindingId());
        dto.setWorkflowId(po.getWorkflowId());
        dto.setStageId(po.getStageId());
        dto.setStageCode(stageCodeById.get(po.getStageId()));
        dto.setEngineType(po.getEngineType());
        dto.setExternalRef(po.getExternalRef());
        dto.setConfigJson(po.getConfigJson());
        dto.setEnabled(po.getEnabled());
        return dto;
    }

    private static int normalizePageIndex(Integer pageIndex) {
        return pageIndex == null || pageIndex < 1 ? 1 : pageIndex;
    }

    private static int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 20;
        }
        return Math.min(pageSize, 200);
    }

    private static String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : value;
    }

    private static String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private static String newId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
