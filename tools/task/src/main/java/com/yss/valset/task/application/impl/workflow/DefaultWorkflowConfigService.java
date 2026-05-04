package com.yss.valset.task.application.impl.workflow;

import com.yss.cloud.dto.response.PageResult;
import com.yss.valset.task.application.command.workflow.WorkflowConfigQueryCommand;
import com.yss.valset.task.application.command.workflow.WorkflowConfigSaveCommand;
import com.yss.valset.task.application.dto.workflow.WorkflowDefinitionDTO;
import com.yss.valset.task.application.dto.workflow.WorkflowStageDTO;
import com.yss.valset.task.application.port.workflow.WorkflowConfigGateway;
import com.yss.valset.task.application.service.workflow.WorkflowConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 默认工作流配置应用服务。
 */
@Service
public class DefaultWorkflowConfigService implements WorkflowConfigService {

    private final WorkflowConfigGateway workflowConfigGateway;

    public DefaultWorkflowConfigService(WorkflowConfigGateway workflowConfigGateway) {
        this.workflowConfigGateway = workflowConfigGateway;
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
    public WorkflowDefinitionDTO saveDraft(WorkflowConfigSaveCommand command) {
        WorkflowDefinitionDTO definition = toDefinition(command);
        definition.setStatus("DRAFT");
        definition.setEnabled(Boolean.FALSE);
        validate(definition);
        return workflowConfigGateway.save(definition);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkflowDefinitionDTO publish(String workflowId) {
        WorkflowDefinitionDTO definition = getDefinition(workflowId);
        validate(definition);
        workflowConfigGateway.disableOtherVersions(definition.getWorkflowCode(), definition.getWorkflowId());
        workflowConfigGateway.updateStatus(workflowId, "PUBLISHED", true);
        return getDefinition(workflowId);
    }

    @Override
    public WorkflowDefinitionDTO disable(String workflowId) {
        workflowConfigGateway.updateStatus(workflowId, "DISABLED", false);
        return getDefinition(workflowId);
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
        for (WorkflowStageDTO stage : stages) {
            if (!StringUtils.hasText(stage.getStageCode())) {
                throw new IllegalArgumentException("阶段编码不能为空");
            }
            if (!stageCodes.add(stage.getStageCode().trim())) {
                throw new IllegalArgumentException("阶段编码重复：" + stage.getStageCode());
            }
            if (!StringUtils.hasText(stage.getStageName())) {
                throw new IllegalArgumentException("阶段名称不能为空：" + stage.getStageCode());
            }
        }
        validateFallback(definition.getParseFallbackStage(), stageCodes, "解析生命周期默认阶段");
        validateFallback(definition.getWorkflowFallbackStage(), stageCodes, "工作流任务默认阶段");
    }

    private void validateFallback(String fallbackStage, Set<String> stageCodes, String label) {
        if (StringUtils.hasText(fallbackStage) && !stageCodes.contains(fallbackStage.trim())) {
            throw new IllegalArgumentException(label + "不存在：" + fallbackStage);
        }
    }
}
