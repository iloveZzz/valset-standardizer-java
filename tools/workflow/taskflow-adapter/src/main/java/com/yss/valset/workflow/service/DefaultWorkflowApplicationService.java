package com.yss.valset.workflow.service;

import com.yss.valset.workflow.model.EtlPlatformType;
import com.yss.valset.workflow.model.WorkflowCallbackRequest;
import com.yss.valset.workflow.model.WorkflowDefinitionDTO;
import com.yss.valset.workflow.model.WorkflowEngineBindingDTO;
import com.yss.valset.workflow.model.WorkflowErrorCode;
import com.yss.valset.workflow.model.WorkflowInstanceDTO;
import com.yss.valset.workflow.model.WorkflowLogQueryRequest;
import com.yss.valset.workflow.model.WorkflowPlatformMetadataDTO;
import com.yss.valset.workflow.model.WorkflowPlatformExecutionResult;
import com.yss.valset.workflow.model.WorkflowRetryRequest;
import com.yss.valset.workflow.model.WorkflowStageDTO;
import com.yss.valset.workflow.model.WorkflowStageLogDTO;
import com.yss.valset.workflow.model.WorkflowStatus;
import com.yss.valset.workflow.model.WorkflowStopRequest;
import com.yss.valset.workflow.model.WorkflowTriggerRequest;
import com.yss.valset.workflow.spi.WorkflowApplicationService;
import com.yss.valset.workflow.spi.WorkflowPlatformAdapter;
import com.yss.valset.workflow.spi.WorkflowRuntimeStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 默认通用 ETL 工作流应用服务。
 */
@Slf4j
@Service
public class DefaultWorkflowApplicationService implements WorkflowApplicationService {

    private final WorkflowRuntimeStore runtimeStore;
    private final List<WorkflowPlatformAdapter> adapters;

    public DefaultWorkflowApplicationService(WorkflowRuntimeStore runtimeStore,
                                             List<WorkflowPlatformAdapter> adapters) {
        this.runtimeStore = runtimeStore;
        this.adapters = adapters == null ? List.of() : adapters;
    }

    @Override
    public WorkflowDefinitionDTO saveDefinition(WorkflowDefinitionDTO definition) {
        WorkflowDefinitionDTO normalized = validateDefinition(definition);
        return runtimeStore.saveDefinition(normalized);
    }

    @Override
    public WorkflowDefinitionDTO validateDefinition(WorkflowDefinitionDTO definition) {
        validateDefinitionRequiredFields(definition);
        WorkflowDefinitionDTO normalized = normalizeDefinition(definition);
        resolveAdapter(normalized.getPlatformType()).validate(normalized);
        return normalized;
    }

    @Override
    public List<WorkflowDefinitionDTO> listDefinitions() {
        return runtimeStore.listDefinitions();
    }

    @Override
    public List<WorkflowPlatformMetadataDTO> listPlatforms() {
        return adapters.stream()
                .filter(adapter -> adapter != null)
                .map(adapter -> WorkflowPlatformMetadataDTO.builder()
                        .platformType(adapter.platformType())
                        .platformName(resolvePlatformName(adapter.platformType()))
                        .description(resolvePlatformDescription(adapter.platformType()))
                        .requiredBindingFields(resolveRequiredBindingFields(adapter.platformType()))
                        .supportedOperations(resolveSupportedOperations(adapter.platformType()))
                        .build())
                .toList();
    }

    @Override
    public Optional<WorkflowDefinitionDTO> findDefinition(String workflowCode, Integer workflowVersionNo) {
        return runtimeStore.findDefinition(workflowCode, workflowVersionNo);
    }

    @Override
    public WorkflowInstanceDTO trigger(WorkflowTriggerRequest request) {
        validateTriggerRequest(request);
        WorkflowDefinitionDTO definition = runtimeStore.findDefinition(request.getWorkflowCode(), request.getWorkflowVersionNo())
                .orElseThrow(() -> new IllegalStateException(WorkflowErrorCode.WORKFLOW_VERSION_NOT_FOUND.getMessage()));
        WorkflowInstanceDTO draft = buildDraftInstance(definition, request);
        WorkflowPlatformExecutionResult result = resolveAdapter(definition.getPlatformType())
                .trigger(definition, draft, request);
        return persistResult(definition, draft, result);
    }

    @Override
    public WorkflowInstanceDTO stop(String instanceId, WorkflowStopRequest request) {
        WorkflowInstanceDTO instance = loadInstance(instanceId);
        WorkflowDefinitionDTO definition = loadDefinition(instance);
        WorkflowPlatformExecutionResult result = resolveAdapter(definition.getPlatformType())
                .stop(definition, instance, request == null ? WorkflowStopRequest.builder().build() : request);
        return persistResult(definition, instance, result);
    }

    @Override
    public WorkflowInstanceDTO retry(String instanceId, WorkflowRetryRequest request) {
        WorkflowInstanceDTO instance = loadInstance(instanceId);
        WorkflowDefinitionDTO definition = loadDefinition(instance);
        WorkflowPlatformExecutionResult result = resolveAdapter(definition.getPlatformType())
                .retry(definition, instance, request == null ? WorkflowRetryRequest.builder().build() : request);
        return persistResult(definition, instance, result);
    }

    @Override
    public Optional<WorkflowInstanceDTO> findInstance(String instanceId) {
        return runtimeStore.findInstance(instanceId);
    }

    @Override
    public List<WorkflowStageLogDTO> listStageLogs(WorkflowLogQueryRequest request) {
        if (request == null || !StringUtils.hasText(request.getInstanceId())) {
            return List.of();
        }
        return runtimeStore.listStageLogs(request.getInstanceId(), request.getStageCode());
    }

    @Override
    public WorkflowInstanceDTO callback(String instanceId, WorkflowCallbackRequest request) {
        WorkflowInstanceDTO instance = loadInstance(instanceId);
        WorkflowDefinitionDTO definition = loadDefinition(instance);
        WorkflowStageDTO stage = findStage(definition, request == null ? null : request.getStageCode());
        WorkflowStageLogDTO log = WorkflowStageLogDTO.builder()
                .instanceId(instanceId)
                .workflowCode(definition.getWorkflowCode())
                .workflowVersionNo(definition.getWorkflowVersionNo())
                .stageCode(stage.getStageCode())
                .stageName(stage.getStageName())
                .stageOrder(stage.getStageOrder())
                .status(WorkflowStatus.fromRawStatus(request == null ? null : request.getRawStatus()))
                .rawStatus(request == null ? null : request.getRawStatus())
                .message(request == null ? null : request.getMessage())
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now())
                .payload(request == null ? Map.of() : request.getPayload())
                .build();
        runtimeStore.saveStageLog(log);
        instance.getStageLogs().add(log);
        instance.setStatus(log.getStatus());
        instance.setRawStatus(log.getRawStatus());
        instance.setMessage(log.getMessage());
        instance.setEndTime(log.getEndTime());
        runtimeStore.saveInstance(instance);
        return instance;
    }

    private WorkflowInstanceDTO persistResult(WorkflowDefinitionDTO definition,
                                              WorkflowInstanceDTO draft,
                                              WorkflowPlatformExecutionResult result) {
        WorkflowInstanceDTO persisted = draft.toBuilder()
                .platformType(definition.getPlatformType())
                .externalWorkflowId(result == null ? draft.getExternalWorkflowId() : result.getExternalWorkflowId())
                .externalInstanceId(result == null ? draft.getExternalInstanceId() : result.getExternalInstanceId())
                .rawStatus(result == null ? draft.getRawStatus() : result.getRawStatus())
                .status(WorkflowStatus.fromRawStatus(result == null ? draft.getRawStatus() : result.getRawStatus()))
                .message(result == null ? draft.getMessage() : result.getMessage())
                .endTime(result != null && result.getStageLogs() != null && !result.getStageLogs().isEmpty()
                        ? result.getStageLogs().get(result.getStageLogs().size() - 1).getEndTime()
                        : LocalDateTime.now())
                .build();
        if (result != null) {
            if (!CollectionUtils.isEmpty(result.getStageLogs())) {
                persisted.getStageLogs().addAll(result.getStageLogs());
                result.getStageLogs().forEach(runtimeStore::saveStageLog);
            }
            if (result.getPayload() != null) {
                persisted.getContext().putAll(result.getPayload());
            }
        }
        runtimeStore.saveInstance(persisted);
        return persisted;
    }

    private WorkflowInstanceDTO buildDraftInstance(WorkflowDefinitionDTO definition, WorkflowTriggerRequest request) {
        return WorkflowInstanceDTO.builder()
                .instanceId(UUID.randomUUID().toString())
                .workflowCode(definition.getWorkflowCode())
                .workflowVersionNo(definition.getWorkflowVersionNo())
                .platformType(definition.getPlatformType())
                .businessKey(request == null ? null : request.getBusinessKey())
                .externalWorkflowId(resolveExternalWorkflowId(definition))
                .status(WorkflowStatus.SUBMITTED)
                .rawStatus(WorkflowStatus.SUBMITTED.name())
                .triggerTime(LocalDateTime.now())
                .startTime(LocalDateTime.now())
                .message("已提交到通用 ETL 平台")
                .context(request == null || request.getContext() == null
                        ? new java.util.LinkedHashMap<>()
                        : new java.util.LinkedHashMap<>(request.getContext()))
                .build();
    }

    private String resolveExternalWorkflowId(WorkflowDefinitionDTO definition) {
        WorkflowEngineBindingDTO engineBinding = definition.getEngineBinding();
        if (engineBinding != null && StringUtils.hasText(engineBinding.getExternalWorkflowId())) {
            return engineBinding.getExternalWorkflowId();
        }
        return definition.getWorkflowCode() + "-" + definition.getWorkflowVersionNo();
    }

    private WorkflowDefinitionDTO normalizeDefinition(WorkflowDefinitionDTO definition) {
        List<WorkflowStageDTO> stages = new ArrayList<>(definition.getStages() == null ? List.of() : definition.getStages());
        stages.sort(Comparator.comparing(WorkflowStageDTO::getStageOrder));
        return definition.toBuilder()
                .stages(stages)
                .build();
    }

    private void validateDefinitionRequiredFields(WorkflowDefinitionDTO definition) {
        if (definition == null) {
            throw new IllegalArgumentException(WorkflowErrorCode.INVALID_WORKFLOW_DEFINITION.getMessage());
        }
        if (!StringUtils.hasText(definition.getWorkflowCode())
                || !StringUtils.hasText(definition.getWorkflowName())
                || definition.getWorkflowVersionNo() == null
                || definition.getPlatformType() == null) {
            throw new IllegalArgumentException(WorkflowErrorCode.INVALID_WORKFLOW_DEFINITION.getMessage());
        }
        if (definition.getEngineBinding() == null
                || definition.getEngineBinding().getPlatformType() == null) {
            throw new IllegalArgumentException(WorkflowErrorCode.INVALID_ENGINE_BINDING.getMessage());
        }
        if (CollectionUtils.isEmpty(definition.getStages())) {
            throw new IllegalArgumentException(WorkflowErrorCode.INVALID_WORKFLOW_DEFINITION.getMessage());
        }
    }

    private void validateTriggerRequest(WorkflowTriggerRequest request) {
        if (request == null || !StringUtils.hasText(request.getWorkflowCode()) || request.getWorkflowVersionNo() == null) {
            throw new IllegalArgumentException(WorkflowErrorCode.INVALID_TRIGGER_REQUEST.getMessage());
        }
    }

    private WorkflowPlatformAdapter resolveAdapter(EtlPlatformType platformType) {
        return adapters.stream()
                .filter(adapter -> adapter != null && adapter.platformType() == platformType)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(WorkflowErrorCode.ADAPTER_NOT_FOUND.getMessage() + "：" + platformType));
    }

    private WorkflowInstanceDTO loadInstance(String instanceId) {
        return runtimeStore.findInstance(instanceId)
                .orElseThrow(() -> new IllegalStateException(WorkflowErrorCode.INSTANCE_NOT_FOUND.getMessage()));
    }

    private WorkflowDefinitionDTO loadDefinition(WorkflowInstanceDTO instance) {
        return runtimeStore.findDefinition(instance.getWorkflowCode(), instance.getWorkflowVersionNo())
                .orElseThrow(() -> new IllegalStateException(WorkflowErrorCode.WORKFLOW_VERSION_NOT_FOUND.getMessage()));
    }

    private WorkflowStageDTO findStage(WorkflowDefinitionDTO definition, String stageCode) {
        if (!StringUtils.hasText(stageCode)) {
            return definition.getStages().get(0);
        }
        return definition.getStages().stream()
                .filter(item -> stageCode.equals(item.getStageCode()))
                .findFirst()
                .orElse(definition.getStages().get(0));
    }

    private String resolvePlatformName(EtlPlatformType platformType) {
        if (platformType == null) {
            return null;
        }
        return switch (platformType) {
            case SPRING_BATCH -> "Spring Batch";
            case DOLPHIN_SCHEDULER -> "DolphinScheduler";
            case XXL_JOB -> "XXL-JOB";
        };
    }

    private String resolvePlatformDescription(EtlPlatformType platformType) {
        if (platformType == null) {
            return null;
        }
        return switch (platformType) {
            case SPRING_BATCH -> "基于 Spring Batch 的批处理执行平台";
            case DOLPHIN_SCHEDULER -> "基于 DolphinScheduler 的 DAG 调度平台";
            case XXL_JOB -> "基于 XXL-JOB 的轻量任务调度平台";
        };
    }

    private List<String> resolveRequiredBindingFields(EtlPlatformType platformType) {
        if (platformType == null) {
            return List.of();
        }
        return switch (platformType) {
            case SPRING_BATCH -> List.of("externalWorkflowId");
            case DOLPHIN_SCHEDULER -> List.of("externalProjectCode", "externalWorkflowId");
            case XXL_JOB -> List.of("externalJobGroup", "externalJobHandler");
        };
    }

    private List<String> resolveSupportedOperations(EtlPlatformType platformType) {
        if (platformType == null) {
            return List.of();
        }
        return switch (platformType) {
            case SPRING_BATCH, DOLPHIN_SCHEDULER, XXL_JOB -> List.of("TRIGGER", "STOP", "RETRY", "QUERY", "QUERY_LOGS");
        };
    }
}
