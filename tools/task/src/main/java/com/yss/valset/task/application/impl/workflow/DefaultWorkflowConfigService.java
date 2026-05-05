package com.yss.valset.task.application.impl.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.yss.cloud.dto.response.PageResult;
import com.yss.valset.task.application.command.workflow.WorkflowConfigAuditQueryCommand;
import com.yss.valset.task.application.command.workflow.WorkflowConfigQueryCommand;
import com.yss.valset.task.application.command.workflow.WorkflowConfigAuditRecordCommand;
import com.yss.valset.task.application.command.workflow.WorkflowConfigSaveCommand;
import com.yss.valset.task.application.service.workflow.WorkflowRuntimeCatalog;
import com.yss.valset.task.application.dto.workflow.WorkflowConfigAuditDTO;
import com.yss.valset.task.application.dto.workflow.WorkflowDefinitionDTO;
import com.yss.valset.task.application.dto.workflow.WorkflowExecutorBindingDTO;
import com.yss.valset.task.application.dto.workflow.WorkflowStageDTO;
import com.yss.valset.task.application.dto.workflow.WorkflowVersionDiffDTO;
import com.yss.valset.task.application.dto.workflow.WorkflowVersionDiffItemDTO;
import com.yss.valset.task.application.event.workflow.WorkflowConfigRuntimeRefreshEvent;
import com.yss.valset.task.application.port.workflow.WorkflowConfigAuditGateway;
import com.yss.valset.task.application.port.workflow.WorkflowConfigGateway;
import com.yss.valset.task.application.service.workflow.WorkflowConfigService;
import com.yss.valset.task.application.service.workflow.engine.WorkflowEngineAdapter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 默认工作流配置应用服务。
 */
@Service
public class DefaultWorkflowConfigService implements WorkflowConfigService {

    private final WorkflowConfigGateway workflowConfigGateway;
    private final WorkflowConfigAuditGateway workflowConfigAuditGateway;
    private final WorkflowRuntimeCatalog stageCatalog;
    private final ObjectMapper objectMapper;
    private final List<WorkflowEngineAdapter> workflowEngineAdapters;
    private final ApplicationEventPublisher applicationEventPublisher;

    public DefaultWorkflowConfigService(WorkflowConfigGateway workflowConfigGateway,
                                        WorkflowConfigAuditGateway workflowConfigAuditGateway,
                                        WorkflowRuntimeCatalog stageCatalog,
                                        ObjectMapper objectMapper,
                                        List<WorkflowEngineAdapter> workflowEngineAdapters,
                                        ApplicationEventPublisher applicationEventPublisher) {
        this.workflowConfigGateway = workflowConfigGateway;
        this.workflowConfigAuditGateway = workflowConfigAuditGateway;
        this.stageCatalog = stageCatalog;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.workflowEngineAdapters = workflowEngineAdapters == null ? List.of() : workflowEngineAdapters;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public PageResult<WorkflowDefinitionDTO> pageDefinitions(WorkflowConfigQueryCommand query) {
        return workflowConfigGateway.pageDefinitions(query);
    }

    @Override
    public WorkflowDefinitionDTO getDefinition(String workflowId) {
        return workflowConfigGateway.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("工作流配置不存在：" + workflowId));
    }

    @Override
    public WorkflowDefinitionDTO getActiveDefinition(String workflowCode) {
        return workflowConfigGateway.findActiveByCode(workflowCode)
                .orElseThrow(() -> new IllegalArgumentException("未找到启用中的工作流配置：" + workflowCode));
    }

    @Override
    public PageResult<WorkflowConfigAuditDTO> pageAuditRecords(WorkflowConfigAuditQueryCommand query) {
        return workflowConfigAuditGateway.pageAudits(query);
    }

    @Override
    public WorkflowConfigAuditDTO getAuditRecord(String auditId) {
        return workflowConfigAuditGateway.findById(auditId)
                .orElseThrow(() -> new IllegalArgumentException("工作流配置审计记录不存在：" + auditId));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public WorkflowDefinitionDTO saveDraft(WorkflowConfigSaveCommand command) {
        return saveDraftInternal(command, "SAVE_DRAFT", "保存草稿");
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public WorkflowDefinitionDTO importConfig(WorkflowConfigSaveCommand command) {
        return saveDraftInternal(command, "IMPORT", "导入工作流配置");
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public WorkflowDefinitionDTO copyVersion(String workflowId) {
        WorkflowDefinitionDTO source = getDefinition(workflowId);
        WorkflowDefinitionDTO copy = cloneDefinition(source);
        copy.setWorkflowId(null);
        copy.setVersionNo(resolveNextVersionNo(source.getWorkflowCode(), source.getVersionNo()));
        normalizeDefinition(copy);
        copy.setStatus("DRAFT");
        copy.setEnabled(Boolean.FALSE);
        validate(copy);
        WorkflowDefinitionDTO saved = workflowConfigGateway.save(copy);
        recordAudit("COPY_VERSION", "SUCCESS", source, saved, "复制版本");
        return saved;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowDefinitionDTO rollbackVersion(String workflowId, String sourceWorkflowId) {
        WorkflowDefinitionDTO target = getDefinition(workflowId);
        WorkflowDefinitionDTO source = getDefinition(sourceWorkflowId);
        if (!StringUtils.hasText(target.getWorkflowCode()) || !target.getWorkflowCode().equals(source.getWorkflowCode())) {
            throw new IllegalArgumentException("回滚目标和来源必须属于同一个工作流编码");
        }
        WorkflowDefinitionDTO copy = cloneDefinition(source);
        copy.setWorkflowId(null);
        copy.setVersionNo(resolveNextVersionNo(source.getWorkflowCode(), source.getVersionNo()));
        normalizeDefinition(copy);
        copy.setStatus("DRAFT");
        copy.setEnabled(Boolean.FALSE);
        validate(copy);
        WorkflowDefinitionDTO saved = workflowConfigGateway.save(copy);
        recordAudit("ROLLBACK", "SUCCESS", target, saved, "回滚工作流版本");
        return saved;
    }

    @Override
    public void validate(WorkflowConfigSaveCommand command) {
        WorkflowDefinitionDTO definition = toDefinition(command);
        normalizeDefinition(definition);
        validate(definition);
    }

    @Override
    public WorkflowDefinitionDTO exportConfig(String workflowId) {
        return cloneDefinition(getDefinition(workflowId));
    }

    @Override
    public WorkflowVersionDiffDTO compareVersions(String leftWorkflowId, String rightWorkflowId) {
        WorkflowDefinitionDTO left = getDefinition(leftWorkflowId);
        WorkflowDefinitionDTO right = getDefinition(rightWorkflowId);
        WorkflowVersionDiffDTO diff = new WorkflowVersionDiffDTO();
        diff.setLeftWorkflowId(left.getWorkflowId());
        diff.setRightWorkflowId(right.getWorkflowId());
        diff.setLeftVersionNo(left.getVersionNo());
        diff.setRightVersionNo(right.getVersionNo());
        diff.setLeftWorkflowCode(left.getWorkflowCode());
        diff.setRightWorkflowCode(right.getWorkflowCode());
        Map<String, String> leftValues = flattenDefinition(left);
        Map<String, String> rightValues = flattenDefinition(right);
        Set<String> keys = new java.util.LinkedHashSet<>();
        keys.addAll(leftValues.keySet());
        keys.addAll(rightValues.keySet());
        List<WorkflowVersionDiffItemDTO> items = new java.util.ArrayList<>();
        for (String key : keys) {
            String leftValue = leftValues.get(key);
            String rightValue = rightValues.get(key);
            if (Objects.equals(leftValue, rightValue)) {
                continue;
            }
            WorkflowVersionDiffItemDTO item = new WorkflowVersionDiffItemDTO();
            item.setPath(key);
            item.setLeftValue(leftValue);
            item.setRightValue(rightValue);
            item.setChangeType(leftValue == null ? "ADDED" : rightValue == null ? "REMOVED" : "CHANGED");
            items.add(item);
        }
        diff.setItems(items);
        return diff;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowDefinitionDTO publish(String workflowId) {
        WorkflowDefinitionDTO definition = getDefinition(workflowId);
        WorkflowDefinitionDTO before = cloneDefinition(definition);
        normalizeDefinition(definition);
        validate(definition);
        workflowConfigGateway.save(definition);
        workflowConfigGateway.disableOtherVersions(definition.getWorkflowCode(), definition.getWorkflowId());
        workflowConfigGateway.updateStatus(workflowId, "PUBLISHED", true);
        refreshRuntimeCache();
        WorkflowDefinitionDTO published = getDefinition(workflowId);
        recordAudit("PUBLISH", "SUCCESS", before, published, "发布工作流");
        return published;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public WorkflowDefinitionDTO disable(String workflowId) {
        WorkflowDefinitionDTO before = getDefinition(workflowId);
        workflowConfigGateway.updateStatus(workflowId, "DISABLED", false);
        refreshRuntimeCache();
        WorkflowDefinitionDTO disabled = getDefinition(workflowId);
        recordAudit("DISABLE", "SUCCESS", before, disabled, "停用工作流");
        return disabled;
    }

    private WorkflowDefinitionDTO toDefinition(WorkflowConfigSaveCommand command) {
        WorkflowDefinitionDTO dto = new WorkflowDefinitionDTO();
        dto.setWorkflowId(command.getWorkflowId());
        dto.setWorkflowCode(command.getWorkflowCode());
        dto.setWorkflowName(command.getWorkflowName());
        dto.setBusinessType(command.getBusinessType());
        dto.setEngineType(command.getEngineType());
        dto.setParseFallbackStage(command.getParseFallbackStage());
        dto.setWorkflowFallbackStage(command.getWorkflowFallbackStage());
        dto.setVersionNo(command.getVersionNo());
        dto.setDescription(command.getDescription());
        dto.setStages(command.getStages());
        dto.setStatusMappings(command.getStatusMappings());
        dto.setExecutorBindings(command.getExecutorBindings());
        dto.setIgnoredParseLifecycleStages(command.getIgnoredParseLifecycleStages());
        dto.setIgnoredWorkflowTaskTypes(command.getIgnoredWorkflowTaskTypes());
        return dto;
    }

    private void validate(WorkflowDefinitionDTO definition) {
        if (!StringUtils.hasText(definition.getWorkflowCode())) {
            throw new IllegalArgumentException("工作流编码不能为空");
        }
        if (!StringUtils.hasText(definition.getWorkflowName())) {
            throw new IllegalArgumentException("工作流名称不能为空");
        }
        List<WorkflowStageDTO> stages = definition.getStages() == null ? List.of() : definition.getStages();
        if (stages.isEmpty()) {
            throw new IllegalArgumentException("工作流至少需要一个阶段");
        }
        Set<String> stageCodes = new HashSet<>();
        Set<String> stepCodes = new HashSet<>();
        for (WorkflowStageDTO stage : stages) {
            if (!StringUtils.hasText(stage.getStageCode())) {
                throw new IllegalArgumentException("阶段编码不能为空");
            }
            if (!stageCodes.add(stage.getStageCode().trim())) {
                throw new IllegalArgumentException("阶段编码重复：" + stage.getStageCode());
            }
            if (!StringUtils.hasText(stage.getStepCode())) {
                throw new IllegalArgumentException("步骤编码不能为空：" + stage.getStageCode());
            }
            if (!stepCodes.add(stage.getStepCode().trim())) {
                throw new IllegalArgumentException("步骤编码重复：" + stage.getStepCode());
            }
            if (!StringUtils.hasText(stage.getStageName())) {
                throw new IllegalArgumentException("阶段名称不能为空：" + stage.getStageCode());
            }
            if (!StringUtils.hasText(stage.getStepName())) {
                throw new IllegalArgumentException("步骤名称不能为空：" + stage.getStepCode());
            }
        }
        validateStatusMappings(definition);
        validateExecutorBindings(definition, stageCodes);
        validateFallback(definition.getParseFallbackStage(), stageCodes, "解析生命周期默认阶段");
        validateFallback(definition.getWorkflowFallbackStage(), stageCodes, "工作流任务默认阶段");
    }

    private void validateStatusMappings(WorkflowDefinitionDTO definition) {
        List<com.yss.valset.task.application.dto.workflow.WorkflowStatusMappingDTO> mappings =
                definition.getStatusMappings() == null ? List.of() : definition.getStatusMappings();
        Set<String> mappingKeys = new HashSet<>();
        for (var mapping : mappings) {
            if (mapping == null) {
                continue;
            }
            if (!StringUtils.hasText(mapping.getSourceType())) {
                throw new IllegalArgumentException("状态映射来源类型不能为空");
            }
            if (!StringUtils.hasText(mapping.getSourceStatus())) {
                throw new IllegalArgumentException("状态映射来源状态不能为空");
            }
            if (!StringUtils.hasText(mapping.getTargetStatus())) {
                throw new IllegalArgumentException("状态映射目标状态不能为空");
            }
            String key = mapping.getSourceType().trim() + "::" + mapping.getSourceStatus().trim();
            if (!mappingKeys.add(key)) {
                throw new IllegalArgumentException("状态映射重复：" + key);
            }
        }
    }

    private WorkflowDefinitionDTO cloneDefinition(WorkflowDefinitionDTO source) {
        WorkflowDefinitionDTO copy = new WorkflowDefinitionDTO();
        copy.setWorkflowId(source.getWorkflowId());
        copy.setWorkflowCode(source.getWorkflowCode());
        copy.setWorkflowName(source.getWorkflowName());
        copy.setBusinessType(source.getBusinessType());
        copy.setEngineType(source.getEngineType());
        copy.setParseFallbackStage(source.getParseFallbackStage());
        copy.setWorkflowFallbackStage(source.getWorkflowFallbackStage());
        copy.setVersionNo(source.getVersionNo());
        copy.setEnabled(source.getEnabled());
        copy.setStatus(source.getStatus());
        copy.setDescription(source.getDescription());
        copy.setStages(source.getStages() == null ? List.of() : new java.util.ArrayList<>(source.getStages()));
        copy.setStatusMappings(source.getStatusMappings() == null ? List.of() : new java.util.ArrayList<>(source.getStatusMappings()));
        copy.setExecutorBindings(source.getExecutorBindings() == null ? List.of() : new java.util.ArrayList<>(source.getExecutorBindings()));
        copy.setIgnoredParseLifecycleStages(source.getIgnoredParseLifecycleStages() == null ? List.of() : new java.util.ArrayList<>(source.getIgnoredParseLifecycleStages()));
        copy.setIgnoredWorkflowTaskTypes(source.getIgnoredWorkflowTaskTypes() == null ? List.of() : new java.util.ArrayList<>(source.getIgnoredWorkflowTaskTypes()));
        return copy;
    }

    private void validateExecutorBindings(WorkflowDefinitionDTO definition, Set<String> stageCodes) {
        List<WorkflowExecutorBindingDTO> bindings = definition.getExecutorBindings() == null ? List.of() : definition.getExecutorBindings();
        for (WorkflowExecutorBindingDTO binding : bindings) {
            if (binding == null) {
                continue;
            }
            if (!StringUtils.hasText(binding.getEngineType())) {
                throw new IllegalArgumentException("平台绑定执行类型不能为空");
            }
            if (StringUtils.hasText(binding.getStageCode()) && !stageCodes.contains(binding.getStageCode().trim())) {
                throw new IllegalArgumentException("平台绑定阶段不存在：" + binding.getStageCode());
            }
            resolveEngineAdapter(binding.getEngineType()).validate(binding);
        }
    }

    private int resolveNextVersionNo(String workflowCode, Integer currentVersionNo) {
        int baseline = currentVersionNo == null || currentVersionNo < 1 ? 1 : currentVersionNo + 1;
        return workflowConfigGateway.findLatestByCode(workflowCode)
                .map(WorkflowDefinitionDTO::getVersionNo)
                .map(versionNo -> versionNo == null || versionNo < 1 ? baseline : versionNo + 1)
                .orElse(baseline);
    }

    private void validateFallback(String fallbackStage, Set<String> stageCodes, String label) {
        String normalized = normalizeLegacyFallbackStage(fallbackStage);
        if (StringUtils.hasText(normalized) && !stageCodes.contains(normalized.trim())) {
            throw new IllegalArgumentException(label + "不存在：" + fallbackStage);
        }
    }

    private void recordAudit(String actionType, String actionResult, WorkflowDefinitionDTO before, WorkflowDefinitionDTO after, String remark) {
        if (after == null) {
            return;
        }
        WorkflowConfigAuditRecordCommand command = new WorkflowConfigAuditRecordCommand();
        command.setWorkflowId(after.getWorkflowId());
        command.setWorkflowCode(after.getWorkflowCode());
        command.setVersionNo(after.getVersionNo());
        command.setActionType(actionType);
        command.setActionResult(actionResult);
        command.setBeforeJson(serialize(before));
        command.setAfterJson(serialize(after));
        command.setRemark(remark);
        workflowConfigAuditGateway.record(command);
    }

    private void refreshRuntimeCache() {
        if (applicationEventPublisher != null) {
            applicationEventPublisher.publishEvent(new WorkflowConfigRuntimeRefreshEvent(this));
            return;
        }
        if (stageCatalog != null) {
            stageCatalog.refreshActiveWorkflowDefinition();
        }
    }

    private WorkflowDefinitionDTO saveDraftInternal(WorkflowConfigSaveCommand command, String actionType, String remark) {
        WorkflowDefinitionDTO before = StringUtils.hasText(command.getWorkflowId())
                ? workflowConfigGateway.findById(command.getWorkflowId()).orElse(null)
                : null;
        WorkflowDefinitionDTO definition = toDefinition(command);
        normalizeDefinition(definition);
        definition.setStatus("DRAFT");
        definition.setEnabled(Boolean.FALSE);
        validate(definition);
        WorkflowDefinitionDTO saved = workflowConfigGateway.save(definition);
        recordAudit(actionType, "SUCCESS", before, saved, remark);
        return saved;
    }

    private String serialize(WorkflowDefinitionDTO definition) {
        if (definition == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(definition);
        } catch (Exception ex) {
            throw new IllegalStateException("序列化工作流配置失败", ex);
        }
    }

    private Map<String, String> flattenDefinition(WorkflowDefinitionDTO definition) {
        Map<String, String> values = new LinkedHashMap<>();
        try {
            JsonNode node = objectMapper.valueToTree(definition);
            flattenNode(values, "", node);
            values.remove("workflowId");
            values.remove("createdAt");
            values.remove("updatedAt");
            return values;
        } catch (Exception ex) {
            throw new IllegalStateException("构建工作流差异失败", ex);
        }
    }

    private void flattenNode(Map<String, String> values, String path, JsonNode node) {
        if (node == null || node.isNull()) {
            if (StringUtils.hasText(path)) {
                values.put(path, null);
            }
            return;
        }
        if (node.isValueNode()) {
            if (StringUtils.hasText(path)) {
                values.put(path, node.isTextual() ? node.asText() : node.toString());
            }
            return;
        }
        if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                String childPath = path + "[" + i + "]";
                flattenNode(values, childPath, node.get(i));
            }
            if (node.size() == 0 && StringUtils.hasText(path)) {
                values.put(path, "[]");
            }
            return;
        }
        node.fieldNames().forEachRemaining(field -> {
            if ("workflowId".equals(field) || "createdAt".equals(field) || "updatedAt".equals(field)) {
                return;
            }
            String childPath = StringUtils.hasText(path) ? path + "." + field : field;
            flattenNode(values, childPath, node.get(field));
        });
    }

    private WorkflowEngineAdapter resolveEngineAdapter(String engineType) {
        if (!StringUtils.hasText(engineType)) {
            throw new IllegalArgumentException("平台绑定执行类型不能为空");
        }
        return workflowEngineAdapters.stream()
                .filter(adapter -> adapter != null && adapter.engineType().name().equalsIgnoreCase(engineType.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未找到对应的工作流执行平台适配器：" + engineType));
    }

    private void normalizeDefinition(WorkflowDefinitionDTO definition) {
        if (definition == null) {
            return;
        }
        definition.setParseFallbackStage(normalizeLegacyFallbackStage(definition.getParseFallbackStage()));
        definition.setWorkflowFallbackStage(normalizeLegacyFallbackStage(definition.getWorkflowFallbackStage()));
    }

    private String normalizeLegacyFallbackStage(String stage) {
        if (!StringUtils.hasText(stage)) {
            return stage;
        }
        String value = stage.trim();
        if ("DATA_PROCESSING".equalsIgnoreCase(value)) {
            return "STANDARD_LANDING";
        }
        if ("RAW_DATA_EXTRACT".equalsIgnoreCase(value)) {
            return "FILE_PARSE";
        }
        return value;
    }
}
