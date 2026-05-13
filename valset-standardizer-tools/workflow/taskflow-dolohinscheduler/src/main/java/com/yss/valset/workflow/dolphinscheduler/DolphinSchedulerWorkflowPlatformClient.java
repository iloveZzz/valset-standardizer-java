package com.yss.valset.workflow.dolphinscheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.yss.valset.workflow.model.EtlPlatformType;
import com.yss.valset.workflow.model.WorkflowDefinitionDTO;
import com.yss.valset.workflow.model.WorkflowEngineBindingDTO;
import com.yss.valset.workflow.model.WorkflowInstanceDTO;
import com.yss.valset.workflow.model.WorkflowInstanceQueryRequest;
import com.yss.valset.workflow.model.WorkflowInstanceViewDTO;
import com.yss.valset.workflow.model.WorkflowPlatformCommand;
import com.yss.valset.workflow.model.WorkflowPlatformExecutionResult;
import com.yss.valset.workflow.model.WorkflowPauseRequest;
import com.yss.valset.workflow.model.WorkflowResumeRequest;
import com.yss.valset.workflow.model.WorkflowStatus;
import com.yss.valset.workflow.model.WorkflowTaskInstancePageDTO;
import com.yss.valset.workflow.model.WorkflowTaskInstanceQueryRequest;
import com.yss.valset.workflow.model.WorkflowTaskInstanceDTO;
import com.yss.valset.workflow.model.WorkflowTaskListDTO;
import com.yss.valset.workflow.model.WorkflowTriggerMode;
import com.yss.valset.workflow.service.AbstractWorkflowPlatformClient;
import com.yss.cloud.dto.response.PageResult;
import org.springframework.stereotype.Component;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.time.LocalDateTime;

/**
 * DolphinScheduler 工作流客户端。
 */
@Component
public class DolphinSchedulerWorkflowPlatformClient extends AbstractWorkflowPlatformClient {

    @Nullable
    private final DolphinSchedulerRemoteApi remoteApi;
    private final DolphinSchedulerResponseSupport responseSupport;
    private final DolphinSchedulerWorkflowSyncSupport syncSupport;

    public DolphinSchedulerWorkflowPlatformClient(@Nullable DolphinSchedulerRemoteApi remoteApi,
                                                  DolphinSchedulerResponseSupport responseSupport) {
        this.remoteApi = remoteApi;
        this.responseSupport = responseSupport;
        this.syncSupport = new DolphinSchedulerWorkflowSyncSupport(remoteApi, responseSupport);
    }

    @Override
    public EtlPlatformType platformType() {
        return EtlPlatformType.DOLPHIN_SCHEDULER;
    }

    @Override
    public void validate(WorkflowDefinitionDTO definition) {
        WorkflowEngineBindingDTO binding = definition == null ? null : definition.getEngineBinding();
        if (binding == null || binding.getPlatformType() != EtlPlatformType.DOLPHIN_SCHEDULER) {
            throw new IllegalArgumentException("DolphinScheduler 绑定信息不合法");
        }
        if (!StringUtils.hasText(binding.getExternalProjectCode())) {
            throw new IllegalArgumentException("DolphinScheduler 需要配置项目编码");
        }
        if (!StringUtils.hasText(binding.getExternalWorkflowId())) {
            throw new IllegalArgumentException("DolphinScheduler 需要配置工作流名称");
        }
    }

    @Override
    public WorkflowDefinitionDTO syncDefinition(WorkflowDefinitionDTO definition) {
        if (remoteApi == null) {
            throw new IllegalStateException("未配置 DolphinScheduler 基础地址");
        }
        return syncSupport.syncDefinition(definition);
    }

    @Override
    public WorkflowDefinitionDTO onlineDefinition(WorkflowDefinitionDTO definition) {
        if (remoteApi == null) {
            return super.onlineDefinition(definition);
        }
        return syncSupport.onlineDefinition(definition);
    }

    @Override
    public WorkflowDefinitionDTO offlineDefinition(WorkflowDefinitionDTO definition) {
        if (remoteApi == null) {
            return super.offlineDefinition(definition);
        }
        return syncSupport.offlineDefinition(definition);
    }

    @Override
    public void deleteDefinition(WorkflowDefinitionDTO definition) {
        if (remoteApi == null) {
            return;
        }
        syncSupport.deleteDefinition(definition);
    }

    @Override
    public PageResult<WorkflowInstanceViewDTO> listInstances(WorkflowDefinitionDTO definition,
                                                             WorkflowInstanceQueryRequest request) {
        if (remoteApi == null) {
            return super.listInstances(definition, request);
        }
        String projectCode = resolveProjectCode(definition);
        String projectName = resolveProjectName(definition);
        int pageIndex = request == null || request.getPageIndex() == null ? 0 : Math.max(request.getPageIndex(), 0);
        int pageSize = request == null || request.getPageSize() == null ? 20 : Math.max(request.getPageSize(), 1);
        JsonNode response = queryInstancePage(projectCode, projectName, request, pageIndex, pageSize);
        List<WorkflowInstanceViewDTO> records = extractInstanceItems(response).stream()
                .map(item -> mapInstanceView(definition, request, item))
                .filter(item -> matchesInstance(item, request))
                .toList();
        long total = extractTotalCount(response, records.size());
        return PageResult.of(records, total, pageSize, pageIndex);
    }

    @Override
    public WorkflowPlatformExecutionResult trigger(WorkflowDefinitionDTO definition,
                                                   WorkflowInstanceDTO instance,
                                                   com.yss.valset.workflow.model.WorkflowTriggerRequest request) {
        if (remoteApi == null) {
            return super.trigger(definition, instance, request);
        }
        WorkflowTriggerMode triggerMode = request == null || request.getTriggerMode() == null
                ? WorkflowTriggerMode.START_PROCESS
                : request.getTriggerMode();
        String message = triggerMode == WorkflowTriggerMode.START_FAILURE_TASK_PROCESS
                ? "已提交失败任务重跑"
                : "已提交到 DolphinScheduler";
        return remoteWorkflowOperation(definition, instance, request, triggerMode, "RUNNING", message);
    }

    @Override
    public WorkflowPlatformExecutionResult stop(WorkflowDefinitionDTO definition,
                                                WorkflowInstanceDTO instance,
                                                com.yss.valset.workflow.model.WorkflowStopRequest request) {
        if (remoteApi == null) {
            return super.stop(definition, instance, request);
        }
        return remoteWorkflowControl(definition, instance, "STOP", "STOPPED", request == null ? "任务已停止" : request.getReason());
    }

    @Override
    public WorkflowPlatformExecutionResult pause(WorkflowDefinitionDTO definition,
                                                 WorkflowInstanceDTO instance,
                                                 WorkflowPauseRequest request) {
        if (remoteApi == null) {
            return super.pause(definition, instance, request);
        }
        return remoteWorkflowControl(definition, instance, "PAUSE", "STOPPED", request == null ? "任务已暂停" : request.getReason());
    }

    @Override
    public WorkflowPlatformExecutionResult resume(WorkflowDefinitionDTO definition,
                                                  WorkflowInstanceDTO instance,
                                                  WorkflowResumeRequest request) {
        if (remoteApi == null) {
            return super.resume(definition, instance, request);
        }
        return remoteWorkflowControl(definition, instance, "RECOVER_SUSPENDED_PROCESS", "RUNNING",
                request == null ? "任务已恢复运行" : request.getReason());
    }

    @Override
    public WorkflowPlatformExecutionResult retry(WorkflowDefinitionDTO definition,
                                                 WorkflowInstanceDTO instance,
                                                 com.yss.valset.workflow.model.WorkflowRetryRequest request) {
        if (remoteApi == null) {
            return super.retry(definition, instance, request);
        }
        return remoteWorkflowControl(definition, instance, "REPEAT_RUNNING", "RETRYING", "任务已重新提交");
    }

    @Override
    public WorkflowPlatformExecutionResult query(WorkflowDefinitionDTO definition,
                                                 WorkflowInstanceDTO instance) {
        if (remoteApi == null) {
            return super.query(definition, instance);
        }
        JsonNode response = responseSupport.toJsonNode(remoteApi.getWorkflowInstance(
                resolveProjectCodeAsLong(definition),
                resolveWorkflowInstanceId(instance)));
        Map<String, Object> payload = buildRemotePayload(definition, instance, response);
        payload.putAll(extractPrimaryResponseData(response));
        String rawStatus = extractString(payload, "state", "status", "workflowStatus", "rawStatus");
        String message = extractString(payload, "message", "msg", "reason");
        return WorkflowPlatformExecutionResult.builder()
                .platformType(platformType())
                .externalWorkflowId(resolveExternalWorkflowId(definition, instance))
                .externalInstanceId(resolveExternalInstanceId(definition, instance))
                .rawStatus(rawStatus)
                .message(message)
                .payload(payload)
                .stageLogs(remoteStageLogs(definition, instance, response))
                .build();
    }

    @Override
    public List<WorkflowPlatformExecutionResult> queryLogs(WorkflowDefinitionDTO definition,
                                                           WorkflowInstanceDTO instance,
                                                           com.yss.valset.workflow.model.WorkflowLogQueryRequest request) {
        if (remoteApi == null) {
            return super.queryLogs(definition, instance, request);
        }
        JsonNode response = responseSupport.toJsonNode(remoteApi.listWorkflowInstanceTasks(
                resolveProjectCodeAsLong(definition),
                resolveWorkflowInstanceId(instance)));
        List<Map<String, Object>> taskItems = extractTaskItems(response);
        if (CollectionUtils.isEmpty(taskItems)) {
            return List.of(WorkflowPlatformExecutionResult.builder()
                    .platformType(platformType())
                    .externalWorkflowId(resolveExternalWorkflowId(definition, instance))
                    .externalInstanceId(resolveExternalInstanceId(definition, instance))
                    .rawStatus(instance == null ? null : instance.getRawStatus())
                    .message("任务日志为空")
                    .payload(buildRemotePayload(definition, instance, response))
                    .stageLogs(List.of())
                    .build());
        }
        return taskItems.stream()
                .filter(item -> request == null || !StringUtils.hasText(request.getStageCode())
                        || request.getStageCode().equals(stringValue(item.get("taskCode"), item.get("name"), item.get("taskName"))))
                .map(item -> WorkflowPlatformExecutionResult.builder()
                        .platformType(platformType())
                        .externalWorkflowId(resolveExternalWorkflowId(definition, instance))
                        .externalInstanceId(resolveExternalInstanceId(definition, instance))
                        .rawStatus(stringValue(item.get("state"), item.get("status"), item.get("executionStatus")))
                        .message(stringValue(item.get("stateDesc"), item.get("state"), item.get("message")))
                        .payload(item)
                        .stageLogs(remoteStageLogs(definition, instance, item))
                        .build())
                .toList();
    }

    @Override
    public WorkflowTaskListDTO queryTasks(WorkflowDefinitionDTO definition,
                                          WorkflowInstanceDTO instance) {
        if (remoteApi == null) {
            return super.queryTasks(definition, instance);
        }
        JsonNode response = responseSupport.toJsonNode(remoteApi.listWorkflowInstanceTasks(
                resolveProjectCodeAsLong(definition),
                resolveWorkflowInstanceId(instance)));
        List<Map<String, Object>> taskItems = extractTaskItems(response);
        return WorkflowTaskListDTO.builder()
                .workflowInstanceState(extractString(extractPrimaryResponseData(response), "workflowInstanceState", "state"))
                .taskList(taskItems.stream().map(this::mapTaskInstance).toList())
                .build();
    }

    @Override
    public WorkflowTaskInstancePageDTO listTaskInstances(WorkflowDefinitionDTO definition,
                                                         WorkflowTaskInstanceQueryRequest request) {
        if (remoteApi == null) {
            return super.listTaskInstances(definition, request);
        }
        String projectCode = resolveProjectCode(definition);
        int pageIndex = request == null || request.getPageIndex() == null ? 0 : Math.max(request.getPageIndex(), 0);
        int pageSize = request == null || request.getPageSize() == null ? 20 : Math.max(request.getPageSize(), 1);
        JsonNode response = queryTaskInstancePage(projectCode, definition, request, pageIndex, pageSize);
        List<WorkflowTaskInstanceDTO> taskList = extractTaskItems(response).stream()
                .map(this::mapTaskInstance)
                .sorted(Comparator.comparing(
                        WorkflowTaskInstanceDTO::getId,
                        Comparator.nullsLast(Long::compareTo)))
                .toList();
        long total = extractTotalCount(response, taskList.size());
        return WorkflowTaskInstancePageDTO.builder()
                .workflowInstanceState(extractString(extractPrimaryResponseData(response), "workflowInstanceState", "state"))
                .taskList(taskList)
                .totalCount(total)
                .pageIndex(pageIndex)
                .pageSize(pageSize)
                .build();
    }

    @Override
    public void forceTaskSuccess(WorkflowDefinitionDTO definition, Long taskInstanceId) {
        if (remoteApi == null) {
            super.forceTaskSuccess(definition, taskInstanceId);
            return;
        }
        if (taskInstanceId == null) {
            throw new IllegalArgumentException("DolphinScheduler 需要任务实例 ID");
        }
        responseSupport.toJsonNode(remoteApi.forceTaskSuccess(resolveProjectCodeAsLong(definition), taskInstanceId.intValue()));
    }

    @Override
    public String queryTaskLog(WorkflowDefinitionDTO definition,
                               Long taskInstanceId,
                               Integer skipLineNum,
                               Integer limit) {
        if (remoteApi == null) {
            return super.queryTaskLog(definition, taskInstanceId, skipLineNum, limit);
        }
        if (taskInstanceId == null) {
            return "";
        }
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("taskInstanceId", String.valueOf(taskInstanceId));
        params.add("limit", String.valueOf(limit == null ? 1000 : Math.max(limit, 1)));
        params.add("skipLineNum", String.valueOf(skipLineNum == null ? 0 : Math.max(skipLineNum, 0)));
        JsonNode response = responseSupport.toJsonNode(remoteApi.getTaskLogByProject(
                resolveProjectCodeAsLong(definition),
                normalizeQueryParams(params)));
        String message = extractTaskLogMessage(response);
        return StringUtils.hasText(message) ? message : "";
    }

    @Override
    public String queryTaskLog(WorkflowDefinitionDTO definition,
                               WorkflowInstanceDTO instance,
                               Long taskInstanceId,
                               Integer skipLineNum,
                               Integer limit) {
        if (remoteApi == null) {
            return super.queryTaskLog(definition, instance, taskInstanceId, skipLineNum, limit);
        }
        if (taskInstanceId == null) {
            return "";
        }
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("taskInstanceId", String.valueOf(taskInstanceId));
        params.add("limit", String.valueOf(limit == null ? 1000 : Math.max(limit, 1)));
        params.add("skipLineNum", String.valueOf(skipLineNum == null ? 0 : Math.max(skipLineNum, 0)));
        JsonNode response = responseSupport.toJsonNode(remoteApi.getTaskLogByProject(
                resolveProjectCodeAsLong(definition),
                normalizeQueryParams(params)));
        String message = extractTaskLogMessage(response);
        if (StringUtils.hasText(message)) {
            return message;
        }
        return "";
    }

    @Override
    protected Map<String, Object> platformSpecificPayload(WorkflowDefinitionDTO definition,
                                                          WorkflowInstanceDTO instance,
                                                          WorkflowPlatformCommand command) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("projectCode", defaultProjectCode(definition));
        payload.put("workflowName", defaultExternalWorkflowId(definition));
        payload.put("namespace", definition == null || definition.getEngineBinding() == null ? null : definition.getEngineBinding().getExternalNamespace());
        payload.put("submitUser", definition == null ? null : definition.getWorkflowCode());
        payload.put("taskDefinitionCode", instance == null ? null : instance.getInstanceId());
        payload.put("stageCode", command == null ? null : command.getStageCode());
        payload.put("context", command == null ? Map.of() : command.getContext());
        payload.put("operationType", command == null || command.getOperationType() == null ? null : command.getOperationType().name());
        payload.put("canonicalStatus", WorkflowStatus.fromRawStatus(instance == null ? null : instance.getRawStatus()).name());
        return payload;
    }

    @Override
    protected String rawTriggerStatus() {
        return "RUNNING";
    }

    @Override
    protected String rawStopStatus() {
        return "STOPPED";
    }

    @Override
    protected String rawPauseStatus() {
        return "STOPPED";
    }

    @Override
    protected String rawResumeStatus() {
        return "RUNNING";
    }

    @Override
    protected String rawRetryStatus() {
        return "RUNNING";
    }

    private WorkflowPlatformExecutionResult remoteWorkflowOperation(WorkflowDefinitionDTO definition,
                                                                     WorkflowInstanceDTO instance,
                                                                     com.yss.valset.workflow.model.WorkflowTriggerRequest request,
                                                                     WorkflowTriggerMode triggerMode,
                                                                     String rawStatus,
                                                                     String message) {
        MultiValueMap<String, String> params = buildStartWorkflowInstanceParams(definition, triggerMode);
        if (request != null && request.getContext() != null && !request.getContext().isEmpty()) {
            params.add("startParams", json(request.getContext()));
        }
        JsonNode response = responseSupport.toJsonNode(remoteApi.startWorkflowInstance(resolveProjectCodeAsLong(definition), params));
        Map<String, Object> payload = buildRemotePayload(definition, instance, response);
        String externalInstanceId = resolveFirstId(response, payload);
        return WorkflowPlatformExecutionResult.builder()
                .platformType(platformType())
                .externalWorkflowId(resolveExternalWorkflowId(definition, instance))
                .externalInstanceId(externalInstanceId)
                .rawStatus(rawStatus)
                .message(message)
                .payload(payload)
                .stageLogs(remoteStageLogs(definition, instance, response))
                .build();
    }

    private WorkflowPlatformExecutionResult remoteWorkflowControl(WorkflowDefinitionDTO definition,
                                                                   WorkflowInstanceDTO instance,
                                                                   String executeType,
                                                                   String rawStatus,
                                                                   String message) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("workflowInstanceId", resolveWorkflowInstanceId(instance));
        params.add("executeType", executeType);
        JsonNode response = responseSupport.toJsonNode(remoteApi.executeWorkflow(resolveProjectCodeAsLong(definition), params));
        Map<String, Object> payload = buildRemotePayload(definition, instance, response);
        return WorkflowPlatformExecutionResult.builder()
                .platformType(platformType())
                .externalWorkflowId(resolveExternalWorkflowId(definition, instance))
                .externalInstanceId(resolveExternalInstanceId(definition, instance))
                .rawStatus(rawStatus)
                .message(message)
                .payload(payload)
                .stageLogs(remoteStageLogs(definition, instance, response))
                .build();
    }

    private JsonNode queryInstancePage(String projectCode,
                                       String projectName,
                                       WorkflowInstanceQueryRequest request,
                                       int pageIndex,
                                       int pageSize) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("projectName", projectName);
        params.add("pageNo", String.valueOf(pageIndex + 1));
        params.add("pageSize", String.valueOf(pageSize));
        if (request != null && StringUtils.hasText(request.getWorkflowName())) {
            params.add("workflowName", request.getWorkflowName().trim());
        }
        if (request != null && StringUtils.hasText(request.getTriggerTimeFrom())) {
            params.add("startDate", request.getTriggerTimeFrom().trim());
        }
        if (request != null && StringUtils.hasText(request.getTriggerTimeTo())) {
            params.add("endDate", request.getTriggerTimeTo().trim());
        }
        try {
            return responseSupport.toJsonNode(remoteApi.listWorkflowInstances(Long.valueOf(projectCode), normalizeQueryParams(params)));
        } catch (RuntimeException firstFailure) {
            try {
                return responseSupport.toJsonNode(remoteApi.listWorkflowInstancesLegacy(Long.valueOf(projectCode), normalizeQueryParams(params)));
            } catch (RuntimeException ignored) {
                throw firstFailure;
            }
        }
    }

    private JsonNode queryTaskInstancePage(String projectCode,
                                           WorkflowDefinitionDTO definition,
                                           WorkflowTaskInstanceQueryRequest request,
                                           int pageIndex,
                                           int pageSize) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("pageNo", String.valueOf(pageIndex + 1));
        params.add("pageSize", String.valueOf(pageSize));
        String workflowInstanceName = request != null && StringUtils.hasText(request.getWorkflowInstanceName())
                ? request.getWorkflowInstanceName().trim()
                : definition != null && StringUtils.hasText(definition.getWorkflowName())
                        ? definition.getWorkflowName().trim()
                        : resolveWorkflowDefinitionName(request);
        params.add("workflowInstanceName", workflowInstanceName);
        params.add("workflowDefinitionName", definition == null ? resolveWorkflowDefinitionName(request) : resolveWorkflowDefinitionCode(definition));
        params.add("taskExecuteType", "BATCH");
        if (request != null && StringUtils.hasText(request.getTaskName())) {
            params.add("searchVal", request.getTaskName().trim());
        }
        if (request != null && StringUtils.hasText(request.getStatus())) {
            params.add("stateType", request.getStatus().trim());
        }
        if (request != null && StringUtils.hasText(request.getStartTimeFrom())) {
            params.add("startDate", request.getStartTimeFrom().trim());
        }
        if (request != null && StringUtils.hasText(request.getEndTimeTo())) {
            params.add("endDate", request.getEndTimeTo().trim());
        }
        return responseSupport.toJsonNode(remoteApi.listTaskInstances(Long.valueOf(projectCode), normalizeQueryParams(params)));
    }

    private List<Map<String, Object>> extractInstanceItems(JsonNode response) {
        if (response == null || !response.hasNonNull("data")) {
            return List.of();
        }
        JsonNode data = response.get("data");
        if (data.isArray()) {
            return responseSupport.getObjectMapper().convertValue(data,
                    responseSupport.getObjectMapper().getTypeFactory().constructCollectionType(List.class, Map.class));
        }
        for (String key : List.of("records", "dataList", "totalList", "items")) {
            if (data.has(key) && data.get(key).isArray()) {
                return responseSupport.getObjectMapper().convertValue(data.get(key),
                        responseSupport.getObjectMapper().getTypeFactory().constructCollectionType(List.class, Map.class));
            }
        }
        return List.of();
    }

    private long extractTotalCount(JsonNode response, int fallback) {
        if (response == null || !response.hasNonNull("data")) {
            return fallback;
        }
        JsonNode data = response.get("data");
        for (String key : List.of("totalCount", "total", "totalSize")) {
            JsonNode node = data.get(key);
            if (node != null && node.canConvertToLong()) {
                return node.asLong();
            }
        }
        return fallback;
    }

    private WorkflowInstanceViewDTO mapInstanceView(WorkflowDefinitionDTO definition,
                                                    WorkflowInstanceQueryRequest request,
                                                    Map<String, Object> payload) {
        String workflowCode = definition == null ? null : definition.getWorkflowCode();
        Integer workflowVersionNo = definition == null ? null : definition.getWorkflowVersionNo();
        String stageCode = stringValue(payload.get("stageCode"), payload.get("currentStageCode"), payload.get("taskCode"));
        String rawStatus = stringValue(payload.get("state"), payload.get("status"), payload.get("workflowStatus"), payload.get("executionStatus"));
        String instanceId = stringValue(payload.get("id"), payload.get("workflowInstanceId"), payload.get("processInstanceId"));
        String externalInstanceId = stringValue(payload.get("workflowInstanceId"), payload.get("processInstanceId"), instanceId);
        String currentStageName = stringValue(payload.get("currentStageName"), payload.get("nodeName"), payload.get("taskName"));
        String message = stringValue(payload.get("message"), payload.get("stateDesc"), payload.get("desc"), payload.get("description"));
        return WorkflowInstanceViewDTO.builder()
                .instanceId(instanceId)
                .workflowCode(workflowCode)
                .workflowName(definition == null ? null : definition.getWorkflowName())
                .workflowVersionNo(workflowVersionNo)
                .platformType(platformType())
                .businessKey(stringValue(payload.get("businessKey"), payload.get("bizKey"), payload.get("runParam")))
                .externalInstanceId(externalInstanceId)
                .externalWorkflowId(stringValue(payload.get("workflowDefinitionCode"), payload.get("processDefinitionCode"), resolveExternalWorkflowId(definition, null)))
                .status(WorkflowStatus.fromRawStatus(rawStatus))
                .rawStatus(rawStatus)
                .currentStageCode(stageCode)
                .currentStageName(currentStageName)
                .triggerTime(parseDateTime(payload.get("submitTime"), payload.get("startTime"), payload.get("createTime")))
                .startTime(parseDateTime(payload.get("startTime"), payload.get("startDate")))
                .duration(stringValue(payload.get("duration"), payload.get("runningDuration"), payload.get("durationText")))
                .endTime(parseDateTime(payload.get("endTime"), payload.get("finishTime")))
                .message(message)
                .stageCount(definition == null || definition.getStages() == null ? 0 : definition.getStages().size())
                .build();
    }

    private boolean matchesInstance(WorkflowInstanceViewDTO row, WorkflowInstanceQueryRequest request) {
        if (request == null) {
            return true;
        }
        if (request.getWorkflowVersionNo() != null
                && !request.getWorkflowVersionNo().equals(row.getWorkflowVersionNo())) {
            return false;
        }
        if (StringUtils.hasText(request.getWorkflowCode())
                && !request.getWorkflowCode().trim().equalsIgnoreCase(String.valueOf(row.getWorkflowCode()))) {
            return false;
        }
        if (StringUtils.hasText(request.getWorkflowName())
                && !containsIgnoreCase(row.getWorkflowName(), request.getWorkflowName())) {
            return false;
        }
        if (StringUtils.hasText(request.getStatus())
                && !request.getStatus().trim().equalsIgnoreCase(String.valueOf(row.getStatus()))) {
            return false;
        }
        if (StringUtils.hasText(request.getBusinessKey())
                && !containsIgnoreCase(row.getBusinessKey(), request.getBusinessKey())) {
            return false;
        }
        if (StringUtils.hasText(request.getInstanceId())
                && !containsIgnoreCase(row.getInstanceId(), request.getInstanceId())) {
            return false;
        }
        if (StringUtils.hasText(request.getExternalInstanceId())
                && !containsIgnoreCase(row.getExternalInstanceId(), request.getExternalInstanceId())) {
            return false;
        }
        if (StringUtils.hasText(request.getStageCode())
                && !request.getStageCode().trim().equalsIgnoreCase(String.valueOf(row.getCurrentStageCode()))) {
            return false;
        }
        LocalDateTime from = parseDateTime(request.getTriggerTimeFrom());
        if (from != null && row.getTriggerTime() != null && row.getTriggerTime().isBefore(from)) {
            return false;
        }
        LocalDateTime to = parseDateTime(request.getTriggerTimeTo());
        if (to != null && row.getTriggerTime() != null && row.getTriggerTime().isAfter(to)) {
            return false;
        }
        return true;
    }

    private boolean containsIgnoreCase(String target, String keyword) {
        if (!StringUtils.hasText(target) || !StringUtils.hasText(keyword)) {
            return false;
        }
        return target.toLowerCase().contains(keyword.trim().toLowerCase());
    }

    private java.time.LocalDateTime parseDateTime(Object... values) {
        for (Object value : values) {
            String text = stringValue(value);
            if (!StringUtils.hasText(text)) {
                continue;
            }
            String normalized = text.trim().replace(" ", "T");
            try {
                if (normalized.length() <= 10) {
                    return java.time.LocalDate.parse(normalized).atStartOfDay();
                }
                return java.time.LocalDateTime.parse(normalized);
            } catch (Exception ignored) {
                // 继续尝试下一个候选值
            }
        }
        return null;
    }

    private String resolveProjectName(WorkflowDefinitionDTO definition) {
        if (definition != null
                && definition.getEngineBinding() != null
                && definition.getEngineBinding().getAttributes() != null) {
            Object sync = definition.getEngineBinding().getAttributes().get("dolphinschedulerSync");
            if (sync instanceof Map<?, ?> map) {
                Object storedProjectName = map.get("projectName");
                String storedText = stringValue(storedProjectName);
                if (StringUtils.hasText(storedText)) {
                    return storedText;
                }
            }
        }
        return sanitize(definition == null ? null : definition.getWorkflowCode()) + "-project";
    }

    private String resolveWorkflowDefinitionName(WorkflowTaskInstanceQueryRequest request) {
        if (request == null) {
            return "";
        }
        if (StringUtils.hasText(request.getWorkflowCode())) {
            return request.getWorkflowCode().trim();
        }
        if (request.getWorkflowVersionNo() != null) {
            return String.valueOf(request.getWorkflowVersionNo());
        }
        return "";
    }

    private Map<String, String> normalizeQueryParams(MultiValueMap<String, String> queryParams) {
        Map<String, String> result = new LinkedHashMap<>();
        if (CollectionUtils.isEmpty(queryParams)) {
            return result;
        }
        queryParams.forEach((key, values) -> {
            if (values != null && !values.isEmpty()) {
                result.put(key, values.get(0));
            }
        });
        return result;
    }

    private String sanitize(String value) {
        if (!StringUtils.hasText(value)) {
            return "workflow";
        }
        return value.trim().replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5_-]", "_");
    }

    private MultiValueMap<String, String> buildStartWorkflowInstanceParams(WorkflowDefinitionDTO definition,
                                                                           WorkflowTriggerMode triggerMode) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("workflowDefinitionCode", resolveWorkflowDefinitionCode(definition));
        params.add("version", resolveWorkflowDefinitionVersion(definition));
        params.add("execType", triggerMode == null ? WorkflowTriggerMode.START_PROCESS.name() : triggerMode.name());
        params.add("startNodeList", "");
        params.add("taskDependType", "TASK_POST");
        params.add("complementDependentMode", "OFF_MODE");
        params.add("runMode", "RUN_MODE_SERIAL");
        params.add("allLevelDependent", "false");
        params.add("executionOrder", "DESC_ORDER");
        params.add("dryRun", "0");
        params.add("scheduleTime", buildScheduleTime());
        params.add("failureStrategy", "CONTINUE");
        params.add("warningType", "NONE");
        params.add("warningGroupId", "");
        params.add("workerGroup", defaultWorkerGroup(definition));
        params.add("tenantCode", defaultTenantCode(definition));
        params.add("environmentCode", defaultEnvironmentCode(definition));
        params.add("workflowInstancePriority", "MEDIUM");
        params.add("expectedParallelismNumber", defaultExpectedParallelismNumber(definition));
        return params;
    }

    private String buildScheduleTime() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String time = now.format(formatter);
        return time + "," + time;
    }

    private String defaultWorkerGroup(WorkflowDefinitionDTO definition) {
        return "default";
    }

    private String defaultTenantCode(WorkflowDefinitionDTO definition) {
        return "default";
    }

    private String defaultEnvironmentCode(WorkflowDefinitionDTO definition) {
        if (definition != null
                && definition.getEngineBinding() != null
                && definition.getEngineBinding().getAttributes() != null
                && definition.getEngineBinding().getAttributes().containsKey("environmentCode")) {
            String value = stringValue(definition.getEngineBinding().getAttributes().get("environmentCode"));
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private String defaultExpectedParallelismNumber(WorkflowDefinitionDTO definition) {
        if (definition == null || definition.getStages() == null || definition.getStages().isEmpty()) {
            return "1";
        }
        return String.valueOf(Math.max(definition.getStages().size(), 1));
    }

    private String resolveProjectCode(WorkflowDefinitionDTO definition) {
        if (definition == null || definition.getEngineBinding() == null) {
            throw new IllegalArgumentException("DolphinScheduler 需要配置项目编码");
        }
        return definition.getEngineBinding().getExternalProjectCode();
    }

    private Long resolveProjectCodeAsLong(WorkflowDefinitionDTO definition) {
        return Long.valueOf(resolveProjectCode(definition));
    }

    private String resolveWorkflowDefinitionCode(WorkflowDefinitionDTO definition) {
        if (definition == null || definition.getEngineBinding() == null) {
            throw new IllegalArgumentException("DolphinScheduler 需要配置工作流编码");
        }
        String workflowDefinitionCode = definition.getEngineBinding().getExternalWorkflowId();
        if (!StringUtils.hasText(workflowDefinitionCode)) {
            workflowDefinitionCode = stringValue(definition.getEngineBinding().getAttributes().get("workflowDefinitionCode"));
        }
        if (!StringUtils.hasText(workflowDefinitionCode)) {
            throw new IllegalArgumentException("DolphinScheduler 需要配置工作流编码");
        }
        return workflowDefinitionCode;
    }

    private String resolveWorkflowDefinitionVersion(WorkflowDefinitionDTO definition) {
        if (definition == null) {
            return "1";
        }
        if (definition.getEngineBinding() != null && definition.getEngineBinding().getRemoteWorkflowVersionNo() != null) {
            return String.valueOf(definition.getEngineBinding().getRemoteWorkflowVersionNo());
        }
        return String.valueOf(definition.getWorkflowVersionNo() == null ? 1 : Math.max(definition.getWorkflowVersionNo(), 1));
    }

    private String resolveWorkflowInstanceId(WorkflowInstanceDTO instance) {
        if (instance == null) {
            throw new IllegalArgumentException("DolphinScheduler 需要工作流实例");
        }
        if (StringUtils.hasText(instance.getExternalInstanceId())) {
            return instance.getExternalInstanceId();
        }
        if (StringUtils.hasText(instance.getInstanceId())) {
            return instance.getInstanceId();
        }
        throw new IllegalArgumentException("DolphinScheduler 需要工作流实例");
    }

    private String resolveFirstId(JsonNode response, Map<String, Object> payload) {
        String fromData = null;
        if (response != null && response.hasNonNull("data")) {
            JsonNode data = response.get("data");
            if (data.isArray() && data.size() > 0) {
                fromData = data.get(0).asText();
            } else if (data.isNumber() || data.isTextual()) {
                fromData = data.asText();
            }
        }
        if (!StringUtils.hasText(fromData) && payload != null) {
            fromData = stringValue(payload.get("data"), payload.get("id"), payload.get("processInstanceId"));
        }
        return fromData;
    }

    private Map<String, Object> buildRemotePayload(WorkflowDefinitionDTO definition,
                                                   WorkflowInstanceDTO instance,
                                                   JsonNode response) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("platformType", platformType().name());
        payload.put("projectCode", resolveProjectCode(definition));
        payload.put("workflowDefinitionCode", resolveWorkflowDefinitionCode(definition));
        payload.put("workflowInstanceId", resolveWorkflowInstanceId(instance));
        payload.put("response", response == null ? Map.of() : responseSupport.getObjectMapper().convertValue(response, Map.class));
        return payload;
    }

    private Map<String, Object> buildRemotePayload(WorkflowDefinitionDTO definition,
                                                   WorkflowInstanceDTO instance,
                                                   Map<String, Object> response) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("platformType", platformType().name());
        payload.put("projectCode", resolveProjectCode(definition));
        payload.put("workflowDefinitionCode", resolveWorkflowDefinitionCode(definition));
        payload.put("workflowInstanceId", resolveWorkflowInstanceId(instance));
        payload.put("response", response == null ? Map.of() : response);
        return payload;
    }

    private List<com.yss.valset.workflow.model.WorkflowStageLogDTO> remoteStageLogs(WorkflowDefinitionDTO definition,
                                                                                    WorkflowInstanceDTO instance,
                                                                                    Map<String, Object> payload) {
        com.yss.valset.workflow.model.WorkflowStageLogDTO log = com.yss.valset.workflow.model.WorkflowStageLogDTO.builder()
                .instanceId(instance == null ? null : instance.getInstanceId())
                .workflowCode(definition == null ? null : definition.getWorkflowCode())
                .workflowVersionNo(definition == null ? null : definition.getWorkflowVersionNo())
                .stageCode(extractString(payload, "stageCode", "taskCode", "name"))
                .stageName(extractString(payload, "stageName", "taskName", "name"))
                .stageOrder(null)
                .status(WorkflowStatus.fromRawStatus(extractString(payload, "state", "status", "rawStatus")))
                .rawStatus(extractString(payload, "state", "status", "rawStatus"))
                .message(extractString(payload, "message", "stateDesc"))
                .payload(payload)
                .build();
        return List.of(log);
    }

    private List<com.yss.valset.workflow.model.WorkflowStageLogDTO> remoteStageLogs(WorkflowDefinitionDTO definition,
                                                                                    WorkflowInstanceDTO instance,
                                                                                    JsonNode response) {
        Map<String, Object> payload = buildRemotePayload(definition, instance, response);
        payload.putAll(extractPrimaryResponseData(response));
        return remoteStageLogs(definition, instance, payload);
    }

    private List<Map<String, Object>> extractTaskItems(JsonNode response) {
        if (response == null || !response.hasNonNull("data")) {
            return List.of();
        }
        JsonNode data = response.get("data");
        if (data.isArray()) {
            return responseSupport.getObjectMapper().convertValue(data, responseSupport.getObjectMapper().getTypeFactory().constructCollectionType(List.class, Map.class));
        }
        if (data.isObject() && data.has("taskList") && data.get("taskList").isArray()) {
            return responseSupport.getObjectMapper().convertValue(data.get("taskList"), responseSupport.getObjectMapper().getTypeFactory().constructCollectionType(List.class, Map.class));
        }
        for (String key : List.of("dataList", "totalList", "records", "items")) {
            if (data.has(key) && data.get(key).isArray()) {
                return responseSupport.getObjectMapper().convertValue(data.get(key), responseSupport.getObjectMapper().getTypeFactory().constructCollectionType(List.class, Map.class));
            }
        }
        if (data.isObject()) {
            if (data.has("dataList") && data.get("dataList").isArray()) {
                return responseSupport.getObjectMapper().convertValue(data.get("dataList"), responseSupport.getObjectMapper().getTypeFactory().constructCollectionType(List.class, Map.class));
            }
            if (data.has("totalList") && data.get("totalList").isArray()) {
                return responseSupport.getObjectMapper().convertValue(data.get("totalList"), responseSupport.getObjectMapper().getTypeFactory().constructCollectionType(List.class, Map.class));
            }
        }
        return List.of();
    }

    private WorkflowTaskInstanceDTO mapTaskInstance(Map<String, Object> item) {
        return WorkflowTaskInstanceDTO.builder()
                .id(longValue(item.get("id")))
                .name(stringValue(item.get("name"), item.get("taskName")))
                .taskType(stringValue(item.get("taskType")))
                .workflowInstanceId(stringValue(item.get("workflowInstanceId")))
                .workflowInstanceName(stringValue(item.get("workflowInstanceName")))
                .projectCode(longValue(item.get("projectCode")))
                .taskCode(longValue(item.get("taskCode")))
                .taskDefinitionVersion(integerValue(item.get("taskDefinitionVersion")))
                .processDefinitionName(stringValue(item.get("processDefinitionName")))
                .taskGroupPriority(integerValue(item.get("taskGroupPriority")))
                .state(stringValue(item.get("state"), item.get("status"), item.get("executionStatus")))
                .firstSubmitTime(stringValue(item.get("firstSubmitTime")))
                .submitTime(stringValue(item.get("submitTime")))
                .startTime(stringValue(item.get("startTime")))
                .endTime(stringValue(item.get("endTime")))
                .host(stringValue(item.get("host")))
                .executePath(stringValue(item.get("executePath")))
                .logPath(stringValue(item.get("logPath")))
                .retryTimes(integerValue(item.get("retryTimes")))
                .alertFlag(stringValue(item.get("alertFlag")))
                .workflowInstance(castMap(item.get("workflowInstance")))
                .workflowDefinition(castMap(item.get("workflowDefinition")))
                .taskDefine(castMap(item.get("taskDefine")))
                .pid(longValue(item.get("pid")))
                .appLink(stringValue(item.get("appLink")))
                .flag(stringValue(item.get("flag")))
                .duration(longValue(item.get("duration")))
                .maxRetryTimes(integerValue(item.get("maxRetryTimes")))
                .retryInterval(integerValue(item.get("retryInterval")))
                .taskInstancePriority(stringValue(item.get("taskInstancePriority")))
                .workflowInstancePriority(stringValue(item.get("workflowInstancePriority")))
                .workerGroup(stringValue(item.get("workerGroup")))
                .environmentCode(longValue(item.get("environmentCode")))
                .environmentConfig(castMap(item.get("environmentConfig")))
                .executorId(longValue(item.get("executorId")))
                .varPool(castMap(item.get("varPool")))
                .executorName(stringValue(item.get("executorName")))
                .delayTime(integerValue(item.get("delayTime")))
                .taskParams(stringValue(item.get("taskParams")))
                .dryRun(integerValue(item.get("dryRun")))
                .taskGroupId(longValue(item.get("taskGroupId")))
                .cpuQuota(integerValue(item.get("cpuQuota")))
                .memoryMax(integerValue(item.get("memoryMax")))
                .taskExecuteType(stringValue(item.get("taskExecuteType")))
                .taskInstanceDependentResults(castMap(item.get("taskInstanceDependentResults")))
                .build();
    }

    private String extractString(Map<String, Object> payload, String... keys) {
        if (payload == null) {
            return null;
        }
        for (String key : keys) {
            if (payload.containsKey(key) && payload.get(key) != null) {
                String value = String.valueOf(payload.get(key));
                if (StringUtils.hasText(value)) {
                    return value;
                }
            }
        }
        return null;
    }

    private String stringValue(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            if (value != null) {
                String string = String.valueOf(value);
                if (StringUtils.hasText(string)) {
                    return string;
                }
            }
        }
        return null;
    }

    private String json(Object value) {
        try {
            return responseSupport.getObjectMapper().writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("序列化 DolphinScheduler 参数失败：" + exception.getMessage(), exception);
        }
    }

    private Map<String, Object> extractPrimaryResponseData(JsonNode response) {
        if (response == null || !response.hasNonNull("data")) {
            return Map.of();
        }
        JsonNode data = response.get("data");
        if (data.isObject()) {
            return responseSupport.getObjectMapper().convertValue(data, Map.class);
        }
        if (data.isArray() && data.size() > 0) {
            JsonNode first = data.get(0);
            if (first.isObject()) {
                return responseSupport.getObjectMapper().convertValue(first, Map.class);
            }
            return Map.of("data", first.asText());
        }
        if (data.isNumber() || data.isTextual() || data.isBoolean()) {
            return Map.of("data", data.asText());
        }
        return Map.of();
    }

    private String extractTaskLogMessage(JsonNode response) {
        Map<String, Object> payload = extractPrimaryResponseData(response);
        String message = extractString(payload, "message", "data");
        if (StringUtils.hasText(message)) {
            return message;
        }
        if (response == null) {
            return "";
        }
        if (response.hasNonNull("message")) {
            String value = response.get("message").asText();
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        if (response.hasNonNull("data") && response.get("data").isTextual()) {
            String value = response.get("data").asText();
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return "";
    }

    private Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ignored) {
            return null;
        }
    }

    private Integer integerValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }
}
