package com.yss.valset.workflow.dolphinscheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.yss.valset.workflow.model.EtlPlatformType;
import com.yss.valset.workflow.model.WorkflowDefinitionDTO;
import com.yss.valset.workflow.model.WorkflowEngineBindingDTO;
import com.yss.valset.workflow.model.WorkflowInstanceDTO;
import com.yss.valset.workflow.model.WorkflowPlatformCommand;
import com.yss.valset.workflow.model.WorkflowPlatformExecutionResult;
import com.yss.valset.workflow.model.WorkflowStatus;
import com.yss.valset.workflow.service.AbstractWorkflowPlatformClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * DolphinScheduler 工作流客户端。
 */
@Component
public class DolphinSchedulerWorkflowPlatformClient extends AbstractWorkflowPlatformClient {

    private final DolphinSchedulerApiSupport apiSupport = new DolphinSchedulerApiSupport();

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

    @Value("${valset.workflow.dolphinscheduler.base-url:}")
    public void setBaseUrl(String baseUrl) {
        apiSupport.setBaseUrl(baseUrl);
    }

    @Value("${valset.workflow.dolphinscheduler.project-code-key:projectCode}")
    public void setProjectCodeKey(String ignored) {
        // 保留配置入口，方便后续接入不同字段名。
    }

    @Override
    public WorkflowPlatformExecutionResult trigger(WorkflowDefinitionDTO definition,
                                                   WorkflowInstanceDTO instance,
                                                   com.yss.valset.workflow.model.WorkflowTriggerRequest request) {
        if (!apiSupport.hasBaseUrl()) {
            return super.trigger(definition, instance, request);
        }
        return remoteWorkflowOperation(definition, instance, request, "START_PROCESS", "RUNNING", "已提交到 DolphinScheduler");
    }

    @Override
    public WorkflowPlatformExecutionResult stop(WorkflowDefinitionDTO definition,
                                                WorkflowInstanceDTO instance,
                                                com.yss.valset.workflow.model.WorkflowStopRequest request) {
        if (!apiSupport.hasBaseUrl()) {
            return super.stop(definition, instance, request);
        }
        return remoteWorkflowControl(definition, instance, "STOP", "STOPPED", request == null ? "任务已停止" : request.getReason());
    }

    @Override
    public WorkflowPlatformExecutionResult retry(WorkflowDefinitionDTO definition,
                                                 WorkflowInstanceDTO instance,
                                                 com.yss.valset.workflow.model.WorkflowRetryRequest request) {
        if (!apiSupport.hasBaseUrl()) {
            return super.retry(definition, instance, request);
        }
        return remoteWorkflowControl(definition, instance, "REPEAT_RUNNING", "RETRYING", "任务已重新提交");
    }

    @Override
    public WorkflowPlatformExecutionResult query(WorkflowDefinitionDTO definition,
                                                 WorkflowInstanceDTO instance) {
        if (!apiSupport.hasBaseUrl()) {
            return super.query(definition, instance);
        }
        JsonNode response = apiSupport.getJson("/projects/{projectCode}/workflow-instances/{workflowInstanceId}",
                null,
                resolveProjectCode(definition),
                resolveWorkflowInstanceId(instance));
        Map<String, Object> payload = buildRemotePayload(definition, instance, response);
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
        if (!apiSupport.hasBaseUrl()) {
            return super.queryLogs(definition, instance, request);
        }
        JsonNode response = apiSupport.getJson("/projects/{projectCode}/workflow-instances/{workflowInstanceId}/tasks",
                null,
                resolveProjectCode(definition),
                resolveWorkflowInstanceId(instance));
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
    protected String rawRetryStatus() {
        return "RUNNING";
    }

    private WorkflowPlatformExecutionResult remoteWorkflowOperation(WorkflowDefinitionDTO definition,
                                                                     WorkflowInstanceDTO instance,
                                                                     com.yss.valset.workflow.model.WorkflowTriggerRequest request,
                                                                     String execType,
                                                                     String rawStatus,
                                                                     String message) {
        MultiValueMap<String, String> params = buildCommonWorkflowParams(definition, instance);
        params.add("execType", execType);
        params.add("scheduleTime", buildScheduleTime());
        params.add("failureStrategy", "CONTINUE");
        params.add("warningType", "NONE");
        params.add("warningGroupId", "1");
        params.add("workerGroup", defaultWorkerGroup(definition));
        params.add("tenantCode", defaultTenantCode(definition));
        params.add("environmentCode", defaultEnvironmentCode(definition));
        params.add("workflowInstancePriority", "MEDIUM");
        if (request != null && request.getContext() != null && !request.getContext().isEmpty()) {
            params.add("startParams", json(request.getContext()));
        }
        JsonNode response = apiSupport.postFormJson("/projects/{projectCode}/executors/start-workflow-instance",
                params,
                resolveProjectCode(definition));
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
        JsonNode response = apiSupport.postFormJson("/projects/{projectCode}/executors/execute",
                params,
                resolveProjectCode(definition));
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

    private MultiValueMap<String, String> buildCommonWorkflowParams(WorkflowDefinitionDTO definition,
                                                                    WorkflowInstanceDTO instance) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("workflowDefinitionCode", resolveWorkflowDefinitionCode(definition));
        params.add("scheduleTime", buildScheduleTime());
        params.add("failureStrategy", "CONTINUE");
        params.add("warningType", "NONE");
        params.add("warningGroupId", "1");
        params.add("workerGroup", defaultWorkerGroup(definition));
        params.add("tenantCode", defaultTenantCode(definition));
        params.add("environmentCode", defaultEnvironmentCode(definition));
        params.add("workflowInstancePriority", "MEDIUM");
        params.add("execType", "START_PROCESS");
        return params;
    }

    private String buildScheduleTime() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String time = now.format(formatter);
        return time + "," + time;
    }

    private String defaultWorkerGroup(WorkflowDefinitionDTO definition) {
        return definition != null && definition.getEngineBinding() != null && StringUtils.hasText(definition.getEngineBinding().getExternalNamespace())
                ? definition.getEngineBinding().getExternalNamespace()
                : "default";
    }

    private String defaultTenantCode(WorkflowDefinitionDTO definition) {
        return definition != null && definition.getEngineBinding() != null && StringUtils.hasText(definition.getEngineBinding().getExternalProjectCode())
                ? definition.getEngineBinding().getExternalProjectCode()
                : "default";
    }

    private String defaultEnvironmentCode(WorkflowDefinitionDTO definition) {
        return "-1";
    }

    private String resolveProjectCode(WorkflowDefinitionDTO definition) {
        if (definition == null || definition.getEngineBinding() == null) {
            throw new IllegalArgumentException("DolphinScheduler 需要配置项目编码");
        }
        return definition.getEngineBinding().getExternalProjectCode();
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
        payload.put("response", response == null ? Map.of() : apiSupport.getObjectMapper().convertValue(response, Map.class));
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
            return apiSupport.getObjectMapper().convertValue(data, apiSupport.getObjectMapper().getTypeFactory().constructCollectionType(List.class, Map.class));
        }
        for (String key : List.of("dataList", "totalList", "records", "items")) {
            if (data.has(key) && data.get(key).isArray()) {
                return apiSupport.getObjectMapper().convertValue(data.get(key), apiSupport.getObjectMapper().getTypeFactory().constructCollectionType(List.class, Map.class));
            }
        }
        if (data.isObject()) {
            if (data.has("dataList") && data.get("dataList").isArray()) {
                return apiSupport.getObjectMapper().convertValue(data.get("dataList"), apiSupport.getObjectMapper().getTypeFactory().constructCollectionType(List.class, Map.class));
            }
            if (data.has("totalList") && data.get("totalList").isArray()) {
                return apiSupport.getObjectMapper().convertValue(data.get("totalList"), apiSupport.getObjectMapper().getTypeFactory().constructCollectionType(List.class, Map.class));
            }
        }
        return List.of();
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
            return apiSupport.getObjectMapper().writeValueAsString(value);
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
            return apiSupport.getObjectMapper().convertValue(data, Map.class);
        }
        if (data.isArray() && data.size() > 0) {
            JsonNode first = data.get(0);
            if (first.isObject()) {
                return apiSupport.getObjectMapper().convertValue(first, Map.class);
            }
            return Map.of("data", first.asText());
        }
        if (data.isNumber() || data.isTextual() || data.isBoolean()) {
            return Map.of("data", data.asText());
        }
        return Map.of();
    }
}
