package com.yss.valset.workflow.service;

import com.yss.cloud.dto.response.PageResult;
import com.yss.valset.workflow.model.EtlPlatformType;
import com.yss.valset.workflow.model.WorkflowCallbackRequest;
import com.yss.valset.workflow.model.WorkflowDefinitionDTO;
import com.yss.valset.workflow.model.WorkflowEngineBindingDTO;
import com.yss.valset.workflow.model.WorkflowErrorCode;
import com.yss.valset.workflow.model.WorkflowInstanceQueryRequest;
import com.yss.valset.workflow.model.WorkflowInstanceDTO;
import com.yss.valset.workflow.model.WorkflowInstanceViewDTO;
import com.yss.valset.workflow.model.WorkflowLogQueryRequest;
import com.yss.valset.workflow.model.WorkflowPlatformMetadataDTO;
import com.yss.valset.workflow.model.WorkflowPlatformExecutionResult;
import com.yss.valset.workflow.model.WorkflowRetryRequest;
import com.yss.valset.workflow.model.WorkflowStageDTO;
import com.yss.valset.workflow.model.WorkflowStageLogDTO;
import com.yss.valset.workflow.model.WorkflowTaskInstancePageDTO;
import com.yss.valset.workflow.model.WorkflowTaskInstanceQueryRequest;
import com.yss.valset.workflow.model.WorkflowTaskListDTO;
import com.yss.valset.workflow.model.WorkflowStatus;
import com.yss.valset.workflow.model.WorkflowSyncStatus;
import com.yss.valset.workflow.model.WorkflowPauseRequest;
import com.yss.valset.workflow.model.WorkflowStopRequest;
import com.yss.valset.workflow.model.WorkflowTriggerRequest;
import com.yss.valset.workflow.model.WorkflowTriggerMode;
import com.yss.valset.workflow.model.WorkflowResumeRequest;
import com.yss.valset.workflow.spi.WorkflowApplicationService;
import com.yss.valset.workflow.spi.WorkflowPlatformAdapter;
import com.yss.valset.workflow.spi.WorkflowRuntimeStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    @Transactional
    public WorkflowDefinitionDTO syncDefinition(WorkflowDefinitionDTO definition) {
        WorkflowDefinitionDTO normalized = normalizeDefinitionForSync(definition);
        WorkflowDefinitionDTO current = runtimeStore.findDefinition(
                normalized.getWorkflowCode(),
                normalized.getWorkflowVersionNo()).orElse(normalized);
        try {
            WorkflowDefinitionDTO syncDraft = withSyncMetadata(current, WorkflowSyncStatus.SYNCING, null);
            WorkflowDefinitionDTO synced = resolveAdapter(syncDraft.getPlatformType()).syncDefinition(syncDraft);
            WorkflowDefinitionDTO offlineSynced = withExternalReleaseState(synced, Boolean.FALSE, "OFFLINE");
            WorkflowDefinitionDTO completed = withSyncMetadata(
                    offlineSynced,
                    WorkflowSyncStatus.SYNCED,
                    null);
            return runtimeStore.saveDefinition(completed);
        } catch (RuntimeException ex) {
            WorkflowDefinitionDTO failed = withSyncMetadata(
                    current,
                    WorkflowSyncStatus.FAILED,
                    resolveSyncFailureReason(ex));
            runtimeStore.saveDefinition(failed);
            throw ex;
        }
    }

    @Override
    @Transactional
    public WorkflowDefinitionDTO onlineDefinition(String workflowCode, Integer workflowVersionNo) {
        WorkflowDefinitionDTO definition = loadDefinition(workflowCode, workflowVersionNo);
        WorkflowDefinitionDTO online = resolveAdapter(definition.getPlatformType()).onlineDefinition(definition);
        WorkflowDefinitionDTO enabledDefinition = withExternalReleaseState(online, Boolean.TRUE, "ONLINE")
                .toBuilder()
                .enabled(true)
                .build();
        return runtimeStore.saveDefinition(enabledDefinition);
    }

    @Override
    @Transactional
    public WorkflowDefinitionDTO offlineDefinition(String workflowCode, Integer workflowVersionNo) {
        WorkflowDefinitionDTO definition = loadDefinition(workflowCode, workflowVersionNo);
        WorkflowDefinitionDTO offline = resolveAdapter(definition.getPlatformType()).offlineDefinition(definition);
        WorkflowDefinitionDTO disabledDefinition = withExternalReleaseState(offline, Boolean.FALSE, "OFFLINE")
                .toBuilder()
                .enabled(false)
                .build();
        return runtimeStore.saveDefinition(disabledDefinition);
    }

    @Override
    @Transactional
    public boolean deleteDefinition(String workflowCode, Integer workflowVersionNo) {
        if (!StringUtils.hasText(workflowCode) || workflowVersionNo == null) {
            throw new IllegalArgumentException("工作流编码和版本号不能为空");
        }
        WorkflowDefinitionDTO definition = runtimeStore.findDefinition(workflowCode, workflowVersionNo)
                .orElseThrow(() -> new IllegalStateException(WorkflowErrorCode.WORKFLOW_VERSION_NOT_FOUND.getMessage()));
        resolveAdapter(definition.getPlatformType()).deleteDefinition(definition);
        return runtimeStore.deleteDefinition(workflowCode, workflowVersionNo);
    }

    @Override
    public WorkflowDefinitionDTO validateDefinition(WorkflowDefinitionDTO definition) {
        validateDefinitionRequiredFields(definition);
        WorkflowDefinitionDTO normalized = normalizeDefinition(definition);
        resolveAdapter(normalized.getPlatformType()).validate(normalized);
        return normalized;
    }

    @Override
    @Transactional
    public WorkflowInstanceDTO runDefinition(String workflowCode, Integer workflowVersionNo) {
        WorkflowDefinitionDTO definition = loadDefinition(workflowCode, workflowVersionNo);
        WorkflowTriggerRequest request = WorkflowTriggerRequest.builder()
                .workflowCode(definition.getWorkflowCode())
                .workflowVersionNo(definition.getWorkflowVersionNo())
                .force(true)
                .triggerMode(WorkflowTriggerMode.START_PROCESS)
                .build();
        return trigger(request);
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
    @Transactional
    public WorkflowInstanceDTO pause(String instanceId, WorkflowPauseRequest request) {
        WorkflowInstanceDTO instance = loadInstance(instanceId);
        WorkflowDefinitionDTO definition = loadDefinition(instance);
        WorkflowPlatformExecutionResult result = resolveAdapter(definition.getPlatformType())
                .pause(definition, instance, request == null ? WorkflowPauseRequest.builder().build() : request);
        return persistResult(definition, instance, result);
    }

    @Override
    @Transactional
    public WorkflowInstanceDTO resume(String instanceId, WorkflowResumeRequest request) {
        WorkflowInstanceDTO instance = loadInstance(instanceId);
        WorkflowDefinitionDTO definition = loadDefinition(instance);
        WorkflowPlatformExecutionResult result = resolveAdapter(definition.getPlatformType())
                .resume(definition, instance, request == null ? WorkflowResumeRequest.builder().build() : request);
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
        Optional<WorkflowInstanceDTO> local = runtimeStore.findInstance(instanceId);
        if (local.isEmpty()) {
            return local;
        }
        WorkflowInstanceDTO instance = local.get();
        if (!shouldHydrateRemotely(instance)) {
            return local;
        }
        try {
            WorkflowDefinitionDTO definition = loadDefinition(instance);
            WorkflowPlatformExecutionResult result = resolveAdapter(definition.getPlatformType()).query(definition, instance);
            if (result == null) {
                return local;
            }
            WorkflowInstanceDTO hydrated = persistResult(definition, instance, result);
            return Optional.of(hydrated);
        } catch (RuntimeException ex) {
            log.warn("加载工作流实例远端详情失败，继续返回本地快照：{}", ex.getMessage());
            return local;
        }
    }

    @Override
    public PageResult<WorkflowInstanceViewDTO> listInstances(WorkflowInstanceQueryRequest request) {
        if (request == null || !StringUtils.hasText(request.getWorkflowCode()) || request.getWorkflowVersionNo() == null) {
            return runtimeStore.listInstances(request);
        }
        WorkflowDefinitionDTO definition = runtimeStore.findDefinition(request.getWorkflowCode(), request.getWorkflowVersionNo())
                .orElse(null);
        if (definition == null || definition.getPlatformType() == EtlPlatformType.SPRING_BATCH) {
            return runtimeStore.listInstances(request);
        }
        WorkflowPlatformAdapter adapter = resolveAdapter(definition.getPlatformType());
        PageResult<WorkflowInstanceViewDTO> remotePage = adapter.listInstances(definition, request);
        if (remotePage == null) {
            return runtimeStore.listInstances(request);
        }
        persistInstanceSnapshots(definition, remotePage.getData());
        if (!CollectionUtils.isEmpty(remotePage.getData())) {
            return remotePage;
        }
        return runtimeStore.listInstances(request);
    }

    @Override
    public WorkflowTaskListDTO listTaskInstances(String instanceId) {
        if (!StringUtils.hasText(instanceId)) {
            return WorkflowTaskListDTO.builder().build();
        }
        WorkflowInstanceDTO instance = loadInstance(instanceId);
        WorkflowDefinitionDTO definition = loadDefinition(instance);
        WorkflowTaskListDTO tasks = resolveAdapter(definition.getPlatformType()).queryTasks(definition, instance);
        return tasks == null ? WorkflowTaskListDTO.builder().build() : tasks;
    }

    @Override
    public String queryTaskLog(String instanceId, Long taskInstanceId) {
        if (!StringUtils.hasText(instanceId) || taskInstanceId == null) {
            return "";
        }
        WorkflowInstanceDTO instance = loadInstance(instanceId);
        WorkflowDefinitionDTO definition = loadDefinition(instance);
        return resolveAdapter(definition.getPlatformType()).queryTaskLog(definition, instance, taskInstanceId);
    }

    @Override
    public WorkflowTaskInstancePageDTO listTaskInstances(WorkflowTaskInstanceQueryRequest request) {
        if (request == null || !StringUtils.hasText(request.getWorkflowCode()) || request.getWorkflowVersionNo() == null) {
            return WorkflowTaskInstancePageDTO.builder().build();
        }
        WorkflowDefinitionDTO definition = runtimeStore.findDefinition(request.getWorkflowCode(), request.getWorkflowVersionNo())
                .orElse(null);
        if (definition == null) {
            return WorkflowTaskInstancePageDTO.builder().build();
        }
        WorkflowTaskInstancePageDTO page = resolveAdapter(definition.getPlatformType()).listTaskInstances(definition, request);
        return page == null ? WorkflowTaskInstancePageDTO.builder().build() : page;
    }

    @Override
    public void forceTaskSuccess(String workflowCode, Integer workflowVersionNo, Long taskInstanceId) {
        if (!StringUtils.hasText(workflowCode) || workflowVersionNo == null || taskInstanceId == null) {
            throw new IllegalArgumentException("工作流定义和任务实例不能为空");
        }
        WorkflowDefinitionDTO definition = runtimeStore.findDefinition(workflowCode, workflowVersionNo)
                .orElseThrow(() -> new IllegalStateException(WorkflowErrorCode.WORKFLOW_VERSION_NOT_FOUND.getMessage()));
        resolveAdapter(definition.getPlatformType()).forceTaskSuccess(definition, taskInstanceId);
    }

    @Override
    public String queryTaskLog(String workflowCode,
                               Integer workflowVersionNo,
                               Long taskInstanceId,
                               Integer skipLineNum,
                               Integer limit) {
        if (!StringUtils.hasText(workflowCode) || workflowVersionNo == null || taskInstanceId == null) {
            return "";
        }
        WorkflowDefinitionDTO definition = runtimeStore.findDefinition(workflowCode, workflowVersionNo)
                .orElseThrow(() -> new IllegalStateException(WorkflowErrorCode.WORKFLOW_VERSION_NOT_FOUND.getMessage()));
        int safeSkipLineNum = skipLineNum == null ? 0 : Math.max(skipLineNum, 0);
        int safeLimit = limit == null ? 1000 : Math.max(limit, 1);
        return resolveAdapter(definition.getPlatformType())
                .queryTaskLog(definition, taskInstanceId, safeSkipLineNum, safeLimit);
    }

    @Override
    public List<WorkflowStageLogDTO> listStageLogs(WorkflowLogQueryRequest request) {
        if (request == null || !StringUtils.hasText(request.getInstanceId())) {
            return List.of();
        }
        WorkflowInstanceDTO instance = loadInstance(request.getInstanceId());
        WorkflowDefinitionDTO definition = loadDefinition(instance);
        try {
            List<WorkflowPlatformExecutionResult> results = resolveAdapter(definition.getPlatformType())
                    .queryLogs(definition, instance, request);
            if (CollectionUtils.isEmpty(results)) {
                return List.of();
            }
            List<WorkflowStageLogDTO> logs = new ArrayList<>();
            for (WorkflowPlatformExecutionResult result : results) {
                if (result == null || CollectionUtils.isEmpty(result.getStageLogs())) {
                    continue;
                }
                for (WorkflowStageLogDTO log : result.getStageLogs()) {
                    if (log != null) {
                        logs.add(log);
                    }
                }
            }
            return logs;
        } catch (RuntimeException ex) {
            log.warn("加载工作流实例阶段日志失败，继续返回空结果：{}", ex.getMessage());
            return List.of();
        }
    }

    @Override
    public WorkflowInstanceDTO callback(String instanceId, WorkflowCallbackRequest request) {
        WorkflowInstanceDTO instance = loadInstance(instanceId);
        WorkflowDefinitionDTO definition = loadDefinition(instance);
        WorkflowStageDTO stage = findStage(definition, request == null ? null : request.getStageCode());
        instance.setStatus(WorkflowStatus.fromRawStatus(request == null ? null : request.getRawStatus()));
        instance.setRawStatus(request == null ? null : request.getRawStatus());
        instance.setMessage(request == null ? null : request.getMessage());
        instance.setCurrentStageCode(stage.getStageCode());
        instance.setEndTime(LocalDateTime.now());
        runtimeStore.saveInstance(instance);
        return instance;
    }

    private WorkflowInstanceDTO persistResult(WorkflowDefinitionDTO definition,
                                              WorkflowInstanceDTO draft,
                                              WorkflowPlatformExecutionResult result) {
        WorkflowInstanceDTO persisted = draft.toBuilder()
                .platformType(definition.getPlatformType())
                .currentStageCode(resolveCurrentStageCode(draft, result))
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
            if (result.getPayload() != null) {
                persisted.getContext().putAll(result.getPayload());
            }
            if (!CollectionUtils.isEmpty(result.getStageLogs())) {
                persisted.setStageLogs(new ArrayList<>(result.getStageLogs()));
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
                .currentStageCode(resolveInitialStageCode(definition, request))
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

    private String resolveInitialStageCode(WorkflowDefinitionDTO definition, WorkflowTriggerRequest request) {
        if (request != null && StringUtils.hasText(request.getStageCode())) {
            return request.getStageCode();
        }
        if (definition != null && !CollectionUtils.isEmpty(definition.getStages())) {
            WorkflowStageDTO firstStage = definition.getStages().get(0);
            return firstStage == null ? null : firstStage.getStageCode();
        }
        return null;
    }

    private String resolveCurrentStageCode(WorkflowInstanceDTO draft, WorkflowPlatformExecutionResult result) {
        if (result != null && !CollectionUtils.isEmpty(result.getStageLogs())) {
            WorkflowStageLogDTO last = result.getStageLogs().get(result.getStageLogs().size() - 1);
            if (last != null && StringUtils.hasText(last.getStageCode())) {
                return last.getStageCode();
            }
        }
        if (draft != null && StringUtils.hasText(draft.getCurrentStageCode())) {
            return draft.getCurrentStageCode();
        }
        return null;
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
            case SPRING_BATCH -> List.of("RUN", "TRIGGER", "STOP", "PAUSE", "RETRY", "QUERY", "QUERY_LOGS");
            case DOLPHIN_SCHEDULER, XXL_JOB -> List.of("RUN", "TRIGGER", "STOP", "PAUSE", "RETRY", "QUERY", "QUERY_LOGS", "SYNC", "ONLINE", "OFFLINE");
        };
    }

    private WorkflowDefinitionDTO normalizeDefinitionForSync(WorkflowDefinitionDTO definition) {
        validateDefinitionRequiredFields(definition);
        WorkflowDefinitionDTO normalized = normalizeDefinition(definition);
        if (CollectionUtils.isEmpty(normalized.getStages())) {
            throw new IllegalArgumentException("工作流阶段不能为空");
        }
        if (normalized.getEngineBinding() == null
                || normalized.getEngineBinding().getPlatformType() == null) {
            throw new IllegalArgumentException(WorkflowErrorCode.INVALID_ENGINE_BINDING.getMessage());
        }
        if (normalized.getPlatformType() != normalized.getEngineBinding().getPlatformType()) {
            throw new IllegalArgumentException("工作流平台与绑定平台不一致");
        }
        validateSyncConstraints(normalized);
        return normalized;
    }

    private WorkflowDefinitionDTO loadDefinition(String workflowCode, Integer workflowVersionNo) {
        if (!StringUtils.hasText(workflowCode) || workflowVersionNo == null) {
            throw new IllegalArgumentException(WorkflowErrorCode.INVALID_WORKFLOW_DEFINITION.getMessage());
        }
        return runtimeStore.findDefinition(workflowCode, workflowVersionNo)
                .orElseThrow(() -> new IllegalStateException(WorkflowErrorCode.WORKFLOW_VERSION_NOT_FOUND.getMessage()));
    }

    private boolean shouldHydrateRemotely(WorkflowInstanceDTO instance) {
        if (instance == null || instance.getPlatformType() == null) {
            return false;
        }
        if (!StringUtils.hasText(instance.getExternalInstanceId()) && !StringUtils.hasText(instance.getExternalWorkflowId())) {
            return false;
        }
        return instance.getPlatformType() != EtlPlatformType.SPRING_BATCH;
    }

    private void persistInstanceSnapshots(WorkflowDefinitionDTO definition, List<WorkflowInstanceViewDTO> views) {
        if (definition == null || CollectionUtils.isEmpty(views)) {
            return;
        }
        for (WorkflowInstanceViewDTO view : views) {
            if (view == null || !StringUtils.hasText(view.getInstanceId())) {
                continue;
            }
            WorkflowInstanceDTO existing = runtimeStore.findInstance(view.getInstanceId()).orElse(null);
            WorkflowInstanceDTO snapshot = buildInstanceSnapshot(definition, view, existing);
            runtimeStore.saveInstance(snapshot);
        }
    }

    private WorkflowInstanceDTO buildInstanceSnapshot(WorkflowDefinitionDTO definition,
                                                      WorkflowInstanceViewDTO view,
                                                      WorkflowInstanceDTO existing) {
        Map<String, Object> context = existing == null || existing.getContext() == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(existing.getContext());
        List<WorkflowStageLogDTO> stageLogs = existing == null || existing.getStageLogs() == null
                ? new ArrayList<>()
                : new ArrayList<>(existing.getStageLogs());
        return WorkflowInstanceDTO.builder()
                .instanceId(view.getInstanceId())
                .workflowCode(definition.getWorkflowCode())
                .workflowVersionNo(definition.getWorkflowVersionNo())
                .platformType(definition.getPlatformType())
                .currentStageCode(view.getCurrentStageCode())
                .businessKey(view.getBusinessKey())
                .externalInstanceId(view.getExternalInstanceId())
                .externalWorkflowId(view.getExternalWorkflowId())
                .status(view.getStatus())
                .rawStatus(view.getRawStatus())
                .triggerTime(view.getTriggerTime())
                .startTime(view.getStartTime())
                .duration(view.getDuration())
                .endTime(view.getEndTime())
                .message(view.getMessage())
                .context(context)
                .stageLogs(stageLogs)
                .build();
    }

    private void validateSyncConstraints(WorkflowDefinitionDTO definition) {
        if (StringUtils.hasText(definition.getWorkflowName()) && definition.getWorkflowName().length() > 200) {
            throw new IllegalArgumentException("工作流名称过长");
        }
        if (StringUtils.hasText(definition.getWorkflowCode()) && definition.getWorkflowCode().length() > 200) {
            throw new IllegalArgumentException("工作流编码过长");
        }
        if (definition.getDescription() != null && definition.getDescription().length() > 500) {
            throw new IllegalArgumentException("工作流描述过长");
        }
        List<WorkflowStageDTO> stages = definition.getStages() == null ? List.of() : definition.getStages();
        java.util.Set<String> stageCodes = new java.util.HashSet<>();
        java.util.Set<Integer> stageOrders = new java.util.HashSet<>();
        for (WorkflowStageDTO stage : stages) {
            if (stage == null) {
                throw new IllegalArgumentException("工作流阶段不能为空");
            }
            if (!StringUtils.hasText(stage.getStageCode())) {
                throw new IllegalArgumentException("阶段编码不能为空");
            }
            if (!StringUtils.hasText(stage.getStageName())) {
                throw new IllegalArgumentException("阶段名称不能为空");
            }
            if (stage.getStageName().length() > 200) {
                throw new IllegalArgumentException("阶段名称过长");
            }
            if (stage.getStageOrder() == null || stage.getStageOrder() < 1) {
                throw new IllegalArgumentException("阶段顺序不能为空");
            }
            if (!stageCodes.add(stage.getStageCode())) {
                throw new IllegalArgumentException("阶段编码不能重复");
            }
            if (!stageOrders.add(stage.getStageOrder())) {
                throw new IllegalArgumentException("阶段顺序不能重复");
            }
        }
        WorkflowEngineBindingDTO binding = definition.getEngineBinding();
        if (binding == null || binding.getPlatformType() == null) {
            throw new IllegalArgumentException(WorkflowErrorCode.INVALID_ENGINE_BINDING.getMessage());
        }
    }

    private WorkflowDefinitionDTO withSyncMetadata(WorkflowDefinitionDTO definition,
                                                   WorkflowSyncStatus syncStatus,
                                                   String failureReason) {
        WorkflowEngineBindingDTO binding = definition.getEngineBinding();
        if (binding == null) {
            return definition;
        }
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime firstSyncedAt = binding.getFirstSyncedAt() != null
                ? binding.getFirstSyncedAt()
                : now;
        WorkflowEngineBindingDTO updatedBinding = binding.toBuilder()
                .syncStatus(syncStatus)
                .firstSyncedAt(firstSyncedAt)
                .lastSyncedAt(now)
                .syncFailureReason(failureReason)
                .build();
        if (syncStatus == WorkflowSyncStatus.SYNCED) {
            updatedBinding = updatedBinding.toBuilder()
                    .firstSyncedAt(firstSyncedAt)
                    .syncFailureReason(null)
                    .build();
        }
        return definition.toBuilder()
                .engineBinding(updatedBinding)
                .build();
    }

    private WorkflowDefinitionDTO withExternalReleaseState(WorkflowDefinitionDTO definition,
                                                           Boolean externalOnline,
                                                           String externalReleaseState) {
        if (definition == null || definition.getEngineBinding() == null) {
            return definition;
        }
        WorkflowEngineBindingDTO binding = definition.getEngineBinding();
        WorkflowEngineBindingDTO updatedBinding = binding.toBuilder()
                .externalOnline(externalOnline)
                .externalReleaseState(externalReleaseState)
                .build();
        return definition.toBuilder()
                .engineBinding(updatedBinding)
                .build();
    }

    private String resolveSyncFailureReason(RuntimeException ex) {
        Throwable current = ex;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return StringUtils.hasText(message) ? message : ex.getClass().getSimpleName();
    }
}
