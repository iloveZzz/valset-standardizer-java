package com.yss.valset.workflow.service;

import com.yss.valset.workflow.model.EtlPlatformType;
import com.yss.valset.workflow.model.WorkflowDefinitionDTO;
import com.yss.valset.workflow.model.WorkflowInstanceDTO;
import com.yss.valset.workflow.model.WorkflowInstanceQueryRequest;
import com.yss.valset.workflow.model.WorkflowInstanceViewDTO;
import com.yss.valset.workflow.model.WorkflowOperationType;
import com.yss.valset.workflow.model.WorkflowLogQueryRequest;
import com.yss.valset.workflow.model.WorkflowPauseRequest;
import com.yss.valset.workflow.model.WorkflowPlatformCommand;
import com.yss.valset.workflow.model.WorkflowPlatformExecutionResult;
import com.yss.valset.workflow.model.WorkflowRetryRequest;
import com.yss.valset.workflow.model.WorkflowResumeRequest;
import com.yss.valset.workflow.model.WorkflowTaskInstancePageDTO;
import com.yss.valset.workflow.model.WorkflowTaskInstanceQueryRequest;
import com.yss.valset.workflow.model.WorkflowTaskListDTO;
import com.yss.valset.workflow.model.WorkflowStageDTO;
import com.yss.valset.workflow.model.WorkflowStageLogDTO;
import com.yss.valset.workflow.model.WorkflowStatus;
import com.yss.valset.workflow.model.WorkflowStopRequest;
import com.yss.valset.workflow.model.WorkflowTriggerRequest;
import com.yss.valset.workflow.model.WorkflowTriggerMode;
import com.yss.valset.workflow.spi.WorkflowPlatformClient;
import com.yss.cloud.dto.response.PageResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流平台客户端基类。
 */
public abstract class AbstractWorkflowPlatformClient implements WorkflowPlatformClient {

    @Override
    public WorkflowDefinitionDTO syncDefinition(WorkflowDefinitionDTO definition) {
        throw new UnsupportedOperationException("当前平台不支持同步工作流");
    }

    @Override
    public WorkflowDefinitionDTO onlineDefinition(WorkflowDefinitionDTO definition) {
        return definition;
    }

    @Override
    public WorkflowDefinitionDTO offlineDefinition(WorkflowDefinitionDTO definition) {
        return definition;
    }

    @Override
    public void deleteDefinition(WorkflowDefinitionDTO definition) {
        // 默认平台删除为空实现。
    }

    @Override
    public PageResult<WorkflowInstanceViewDTO> listInstances(WorkflowDefinitionDTO definition,
                                                             WorkflowInstanceQueryRequest request) {
        int pageIndex = request == null || request.getPageIndex() == null ? 0 : Math.max(request.getPageIndex(), 0);
        int pageSize = request == null || request.getPageSize() == null ? 20 : Math.max(request.getPageSize(), 1);
        return PageResult.of(List.of(), 0L, pageSize, pageIndex);
    }

    @Override
    public WorkflowPlatformExecutionResult trigger(WorkflowDefinitionDTO definition,
                                                   WorkflowInstanceDTO instance,
                                                   WorkflowTriggerRequest request) {
        return buildResult(definition, instance, buildCommand(WorkflowOperationType.TRIGGER, definition, instance, request),
                rawTriggerStatus(), "已提交到平台");
    }

    @Override
    public WorkflowPlatformExecutionResult stop(WorkflowDefinitionDTO definition,
                                                WorkflowInstanceDTO instance,
                                                WorkflowStopRequest request) {
        return buildResult(definition, instance, buildCommand(WorkflowOperationType.STOP, definition, instance, request),
                rawStopStatus(), request == null ? "任务已停止" : request.getReason());
    }

    @Override
    public WorkflowPlatformExecutionResult pause(WorkflowDefinitionDTO definition,
                                                 WorkflowInstanceDTO instance,
                                                 WorkflowPauseRequest request) {
        return buildResult(definition, instance, buildCommand(WorkflowOperationType.PAUSE, definition, instance, request),
                rawPauseStatus(), request == null ? "任务已暂停" : request.getReason());
    }

    @Override
    public WorkflowPlatformExecutionResult resume(WorkflowDefinitionDTO definition,
                                                  WorkflowInstanceDTO instance,
                                                  WorkflowResumeRequest request) {
        return buildResult(definition, instance, buildCommand(WorkflowOperationType.RESUME, definition, instance, request),
                rawResumeStatus(), request == null ? "任务已恢复运行" : request.getReason());
    }

    @Override
    public WorkflowPlatformExecutionResult retry(WorkflowDefinitionDTO definition,
                                                 WorkflowInstanceDTO instance,
                                                 WorkflowRetryRequest request) {
        return buildResult(definition, instance, buildCommand(WorkflowOperationType.RETRY, definition, instance, request),
                rawRetryStatus(), "任务已重新提交");
    }

    @Override
    public WorkflowPlatformExecutionResult query(WorkflowDefinitionDTO definition,
                                                 WorkflowInstanceDTO instance) {
        return WorkflowPlatformExecutionResult.builder()
                .platformType(platformType())
                .externalWorkflowId(instance == null ? null : instance.getExternalWorkflowId())
                .externalInstanceId(instance == null ? null : instance.getExternalInstanceId())
                .rawStatus(instance == null ? null : instance.getRawStatus())
                .message(instance == null ? null : instance.getMessage())
                .payload(buildPayload(definition, instance, buildCommand(WorkflowOperationType.QUERY, definition, instance, (WorkflowTriggerRequest) null)))
                .stageLogs(instance == null ? List.of() : snapshotLogs(definition, instance))
                .build();
    }

    @Override
    public WorkflowTaskListDTO queryTasks(WorkflowDefinitionDTO definition,
                                          WorkflowInstanceDTO instance) {
        return WorkflowTaskListDTO.builder().build();
    }

    @Override
    public WorkflowTaskInstancePageDTO listTaskInstances(WorkflowDefinitionDTO definition,
                                                         WorkflowTaskInstanceQueryRequest request) {
        int pageIndex = request == null || request.getPageIndex() == null ? 0 : Math.max(request.getPageIndex(), 0);
        int pageSize = request == null || request.getPageSize() == null ? 20 : Math.max(request.getPageSize(), 1);
        return WorkflowTaskInstancePageDTO.builder()
                .pageIndex(pageIndex)
                .pageSize(pageSize)
                .totalCount(0L)
                .build();
    }

    @Override
    public void forceTaskSuccess(WorkflowDefinitionDTO definition, Long taskInstanceId) {
        throw new UnsupportedOperationException("当前平台不支持强制成功任务实例");
    }

    @Override
    public String queryTaskLog(WorkflowDefinitionDTO definition,
                               Long taskInstanceId,
                               Integer skipLineNum,
                               Integer limit) {
        return "";
    }

    @Override
    public String queryTaskLog(WorkflowDefinitionDTO definition,
                               WorkflowInstanceDTO instance,
                               Long taskInstanceId,
                               Integer skipLineNum,
                               Integer limit) {
        return queryTaskLog(definition, taskInstanceId, skipLineNum, limit);
    }

    protected WorkflowPlatformExecutionResult buildResult(WorkflowDefinitionDTO definition,
                                                          WorkflowInstanceDTO instance,
                                                          WorkflowPlatformCommand command,
                                                          String rawStatus,
                                                          String message) {
        Map<String, Object> payload = buildPayload(definition, instance, command);
        return WorkflowPlatformExecutionResult.builder()
                .platformType(platformType())
                .externalWorkflowId(resolveExternalWorkflowId(definition, instance))
                .externalInstanceId(resolveExternalInstanceId(definition, instance))
                .rawStatus(rawStatus)
                .message(message)
                .payload(payload)
                .stageLogs(snapshotLogs(definition, instance))
                .build();
    }

    protected List<WorkflowStageLogDTO> snapshotLogs(WorkflowDefinitionDTO definition, WorkflowInstanceDTO instance) {
        List<WorkflowStageLogDTO> logs = new ArrayList<>();
        if (definition == null || instance == null) {
            return logs;
        }
        LocalDateTime now = LocalDateTime.now();
        for (WorkflowStageDTO stage : definition.getStages()) {
            logs.add(WorkflowStageLogDTO.builder()
                    .instanceId(instance.getInstanceId())
                    .workflowCode(definition.getWorkflowCode())
                    .workflowVersionNo(definition.getWorkflowVersionNo())
                    .stageCode(stage.getStageCode())
                    .stageName(stage.getStageName())
                    .stageOrder(stage.getStageOrder())
                    .status(WorkflowStatus.fromRawStatus(instance.getRawStatus()))
                    .rawStatus(instance.getRawStatus())
                    .message(instance.getMessage())
                    .startTime(now)
                    .endTime(now)
                    .payload(buildPayload(definition, instance, buildStageCommand(definition, instance, stage)))
                    .build());
        }
        return logs;
    }

    protected Map<String, Object> buildPayload(WorkflowDefinitionDTO definition,
                                               WorkflowInstanceDTO instance,
                                               WorkflowPlatformCommand command) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("platformType", platformType().name());
        payload.put("workflowCode", definition == null ? null : definition.getWorkflowCode());
        payload.put("workflowVersionNo", definition == null ? null : definition.getWorkflowVersionNo());
        payload.put("workflowName", definition == null ? null : definition.getWorkflowName());
        payload.put("instanceId", instance == null ? null : instance.getInstanceId());
        payload.put("externalWorkflowId", resolveExternalWorkflowId(definition, instance));
        payload.put("externalInstanceId", resolveExternalInstanceId(definition, instance));
        payload.put("rawStatus", instance == null ? null : instance.getRawStatus());
        if (command != null) {
            payload.put("operationType", command.getOperationType() == null ? null : command.getOperationType().name());
            payload.put("businessKey", command.getBusinessKey());
            payload.put("stageCode", command.getStageCode());
            payload.put("stageName", command.getStageName());
            payload.put("stageOrder", command.getStageOrder());
            payload.put("triggerMode", command.getTriggerMode() == null ? null : command.getTriggerMode().name());
            payload.put("reason", command.getReason());
            payload.put("force", command.isForce());
            payload.put("context", command.getContext());
            payload.put("parameters", command.getParameters());
        }
        payload.putAll(platformSpecificPayload(definition, instance, command));
        return payload;
    }

    protected abstract Map<String, Object> platformSpecificPayload(WorkflowDefinitionDTO definition,
                                                                   WorkflowInstanceDTO instance,
                                                                   WorkflowPlatformCommand command);

    protected abstract String rawTriggerStatus();

    protected abstract String rawStopStatus();

    protected abstract String rawPauseStatus();

    protected abstract String rawResumeStatus();

    protected abstract String rawRetryStatus();

    protected String resolveExternalWorkflowId(WorkflowDefinitionDTO definition, WorkflowInstanceDTO instance) {
        if (definition != null && definition.getEngineBinding() != null && definition.getEngineBinding().getExternalWorkflowId() != null) {
            return definition.getEngineBinding().getExternalWorkflowId();
        }
        return instance == null ? null : instance.getExternalWorkflowId();
    }

    protected String resolveExternalInstanceId(WorkflowDefinitionDTO definition, WorkflowInstanceDTO instance) {
        if (instance != null && instance.getExternalInstanceId() != null) {
            return instance.getExternalInstanceId();
        }
        if (definition == null) {
            return null;
        }
        return platformType().name() + "-" + definition.getWorkflowCode() + "-" + definition.getWorkflowVersionNo();
    }

    protected String defaultJobName(WorkflowDefinitionDTO definition) {
        if (definition == null) {
            return null;
        }
        return definition.getWorkflowCode() + "-v" + definition.getWorkflowVersionNo();
    }

    protected String defaultProjectCode(WorkflowDefinitionDTO definition) {
        if (definition == null || definition.getEngineBinding() == null) {
            return null;
        }
        return definition.getEngineBinding().getExternalProjectCode();
    }

    protected String defaultExternalWorkflowId(WorkflowDefinitionDTO definition) {
        if (definition == null || definition.getEngineBinding() == null) {
            return null;
        }
        return definition.getEngineBinding().getExternalWorkflowId();
    }

    protected WorkflowPlatformCommand buildCommand(WorkflowOperationType operationType,
                                                   WorkflowDefinitionDTO definition,
                                                   WorkflowInstanceDTO instance,
                                                   WorkflowTriggerRequest request) {
        return WorkflowPlatformCommand.builder()
                .operationType(operationType)
                .platformType(platformType())
                .workflowCode(definition == null ? null : definition.getWorkflowCode())
                .workflowVersionNo(definition == null ? null : definition.getWorkflowVersionNo())
                .workflowName(definition == null ? null : definition.getWorkflowName())
                .instanceId(instance == null ? null : instance.getInstanceId())
                .externalWorkflowId(resolveExternalWorkflowId(definition, instance))
                .externalInstanceId(resolveExternalInstanceId(definition, instance))
                .businessKey(instance == null ? null : instance.getBusinessKey())
                .stageCode(request == null ? null : request.getStageCode())
                .force(request != null && request.isForce())
                .triggerMode(request == null || request.getTriggerMode() == null
                        ? WorkflowTriggerMode.START_PROCESS
                        : request.getTriggerMode())
                .context(request == null || request.getContext() == null
                        ? new LinkedHashMap<>()
                        : new LinkedHashMap<>(request.getContext()))
                .parameters(new LinkedHashMap<>())
                .build();
    }

    protected WorkflowPlatformCommand buildCommand(WorkflowOperationType operationType,
                                                   WorkflowDefinitionDTO definition,
                                                   WorkflowInstanceDTO instance,
                                                   WorkflowStopRequest request) {
        WorkflowPlatformCommand command = WorkflowPlatformCommand.builder()
                .operationType(operationType)
                .platformType(platformType())
                .workflowCode(definition == null ? null : definition.getWorkflowCode())
                .workflowVersionNo(definition == null ? null : definition.getWorkflowVersionNo())
                .workflowName(definition == null ? null : definition.getWorkflowName())
                .instanceId(instance == null ? null : instance.getInstanceId())
                .externalWorkflowId(resolveExternalWorkflowId(definition, instance))
                .externalInstanceId(resolveExternalInstanceId(definition, instance))
                .businessKey(instance == null ? null : instance.getBusinessKey())
                .reason(request == null ? null : request.getReason())
                .context(request == null || request.getContext() == null
                        ? new LinkedHashMap<>()
                        : new LinkedHashMap<>(request.getContext()))
                .parameters(new LinkedHashMap<>())
                .build();
        return command;
    }

    protected WorkflowPlatformCommand buildCommand(WorkflowOperationType operationType,
                                                   WorkflowDefinitionDTO definition,
                                                   WorkflowInstanceDTO instance,
                                                   WorkflowPauseRequest request) {
        return WorkflowPlatformCommand.builder()
                .operationType(operationType)
                .platformType(platformType())
                .workflowCode(definition == null ? null : definition.getWorkflowCode())
                .workflowVersionNo(definition == null ? null : definition.getWorkflowVersionNo())
                .workflowName(definition == null ? null : definition.getWorkflowName())
                .instanceId(instance == null ? null : instance.getInstanceId())
                .externalWorkflowId(resolveExternalWorkflowId(definition, instance))
                .externalInstanceId(resolveExternalInstanceId(definition, instance))
                .businessKey(instance == null ? null : instance.getBusinessKey())
                .reason(request == null ? null : request.getReason())
                .context(request == null || request.getContext() == null
                        ? new LinkedHashMap<>()
                        : new LinkedHashMap<>(request.getContext()))
                .parameters(new LinkedHashMap<>())
                .build();
    }

    protected WorkflowPlatformCommand buildCommand(WorkflowOperationType operationType,
                                                   WorkflowDefinitionDTO definition,
                                                   WorkflowInstanceDTO instance,
                                                   WorkflowResumeRequest request) {
        WorkflowPlatformCommand command = WorkflowPlatformCommand.builder()
                .operationType(operationType)
                .platformType(platformType())
                .workflowCode(definition == null ? null : definition.getWorkflowCode())
                .workflowVersionNo(definition == null ? null : definition.getWorkflowVersionNo())
                .workflowName(definition == null ? null : definition.getWorkflowName())
                .instanceId(instance == null ? null : instance.getInstanceId())
                .externalWorkflowId(resolveExternalWorkflowId(definition, instance))
                .externalInstanceId(resolveExternalInstanceId(definition, instance))
                .businessKey(instance == null ? null : instance.getBusinessKey())
                .reason(request == null ? null : request.getReason())
                .context(request == null || request.getContext() == null
                        ? new LinkedHashMap<>()
                        : new LinkedHashMap<>(request.getContext()))
                .parameters(new LinkedHashMap<>())
                .build();
        return command;
    }

    protected WorkflowPlatformCommand buildCommand(WorkflowOperationType operationType,
                                                   WorkflowDefinitionDTO definition,
                                                   WorkflowInstanceDTO instance,
                                                   WorkflowRetryRequest request) {
        WorkflowPlatformCommand command = WorkflowPlatformCommand.builder()
                .operationType(operationType)
                .platformType(platformType())
                .workflowCode(definition == null ? null : definition.getWorkflowCode())
                .workflowVersionNo(definition == null ? null : definition.getWorkflowVersionNo())
                .workflowName(definition == null ? null : definition.getWorkflowName())
                .instanceId(instance == null ? null : instance.getInstanceId())
                .externalWorkflowId(resolveExternalWorkflowId(definition, instance))
                .externalInstanceId(resolveExternalInstanceId(definition, instance))
                .businessKey(instance == null ? null : instance.getBusinessKey())
                .stageCode(request == null ? null : request.getStageCode())
                .context(request == null || request.getContext() == null
                        ? new LinkedHashMap<>()
                        : new LinkedHashMap<>(request.getContext()))
                .parameters(new LinkedHashMap<>())
                .build();
        return command;
    }

    protected WorkflowPlatformCommand buildCommand(WorkflowOperationType operationType,
                                                   WorkflowDefinitionDTO definition,
                                                   WorkflowInstanceDTO instance,
                                                   WorkflowLogQueryRequest request) {
        WorkflowPlatformCommand command = WorkflowPlatformCommand.builder()
                .operationType(operationType)
                .platformType(platformType())
                .workflowCode(definition == null ? null : definition.getWorkflowCode())
                .workflowVersionNo(definition == null ? null : definition.getWorkflowVersionNo())
                .workflowName(definition == null ? null : definition.getWorkflowName())
                .instanceId(instance == null ? null : instance.getInstanceId())
                .externalWorkflowId(resolveExternalWorkflowId(definition, instance))
                .externalInstanceId(resolveExternalInstanceId(definition, instance))
                .businessKey(instance == null ? null : instance.getBusinessKey())
                .stageCode(request == null ? null : request.getStageCode())
                .context(new LinkedHashMap<>())
                .parameters(new LinkedHashMap<>())
                .build();
        return command;
    }

    private WorkflowPlatformCommand buildStageCommand(WorkflowDefinitionDTO definition,
                                                      WorkflowInstanceDTO instance,
                                                      WorkflowStageDTO stage) {
        return WorkflowPlatformCommand.builder()
                .operationType(WorkflowOperationType.QUERY_LOGS)
                .platformType(platformType())
                .workflowCode(definition == null ? null : definition.getWorkflowCode())
                .workflowVersionNo(definition == null ? null : definition.getWorkflowVersionNo())
                .workflowName(definition == null ? null : definition.getWorkflowName())
                .instanceId(instance == null ? null : instance.getInstanceId())
                .externalWorkflowId(resolveExternalWorkflowId(definition, instance))
                .externalInstanceId(resolveExternalInstanceId(definition, instance))
                .businessKey(instance == null ? null : instance.getBusinessKey())
                .stageCode(stage == null ? null : stage.getStageCode())
                .stageName(stage == null ? null : stage.getStageName())
                .stageOrder(stage == null ? null : stage.getStageOrder())
                .context(new LinkedHashMap<>())
                .parameters(new LinkedHashMap<>())
                .build();
    }
}
