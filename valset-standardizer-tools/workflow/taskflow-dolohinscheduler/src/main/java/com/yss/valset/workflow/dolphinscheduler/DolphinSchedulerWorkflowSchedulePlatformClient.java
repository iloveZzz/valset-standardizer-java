package com.yss.valset.workflow.dolphinscheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.yss.valset.workflow.model.EtlPlatformType;
import com.yss.valset.workflow.model.WorkflowScheduleDTO;
import com.yss.valset.workflow.model.WorkflowSchedulePreviewRequest;
import com.yss.valset.workflow.service.AbstractWorkflowSchedulePlatformClient;
import org.springframework.stereotype.Component;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DolphinScheduler 工作流调度客户端。
 */
@Component
public class DolphinSchedulerWorkflowSchedulePlatformClient extends AbstractWorkflowSchedulePlatformClient {

    @Nullable
    private final DolphinSchedulerRemoteApi remoteApi;
    private final DolphinSchedulerResponseSupport responseSupport;

    public DolphinSchedulerWorkflowSchedulePlatformClient(@Nullable DolphinSchedulerRemoteApi remoteApi,
                                                          DolphinSchedulerResponseSupport responseSupport) {
        this.remoteApi = remoteApi;
        this.responseSupport = responseSupport;
    }

    @Override
    public EtlPlatformType platformType() {
        return EtlPlatformType.DOLPHIN_SCHEDULER;
    }

    @Override
    public void validate(WorkflowScheduleDTO schedule) {
        if (schedule == null) {
            throw new IllegalArgumentException("DolphinScheduler 调度配置不能为空");
        }
        if (schedule.getProjectCode() == null) {
            throw new IllegalArgumentException("DolphinScheduler 调度需要配置项目编码");
        }
        if (schedule.getWorkflowDefinitionCode() == null && !StringUtils.hasText(schedule.getScheduleJson())) {
            throw new IllegalArgumentException("DolphinScheduler 调度需要配置工作流编码或调度表达式");
        }
        if (!StringUtils.hasText(schedule.getScheduleJson())) {
            throw new IllegalArgumentException("DolphinScheduler 调度需要配置调度表达式");
        }
    }

    @Override
    public WorkflowScheduleDTO saveSchedule(WorkflowScheduleDTO schedule) {
        if (remoteApi == null) {
            return schedule == null ? null : schedule.toBuilder().build();
        }
        MultiValueMap<String, String> params = buildScheduleParams(schedule);
        JsonNode response = responseSupport.toJsonNode(remoteApi.createSchedule(schedule.getProjectCode(), params));
        return mergeRemoteSchedule(schedule, response);
    }

    @Override
    public WorkflowScheduleDTO updateSchedule(WorkflowScheduleDTO schedule) {
        if (remoteApi == null) {
            return schedule == null ? null : schedule.toBuilder().build();
        }
        MultiValueMap<String, String> params = buildScheduleParams(schedule);
        JsonNode response;
        if (schedule != null && schedule.getScheduleId() != null) {
            response = responseSupport.toJsonNode(remoteApi.updateSchedule(schedule.getProjectCode(), schedule.getScheduleId(), params));
        } else {
            response = responseSupport.toJsonNode(remoteApi.updateScheduleByWorkflowDefinitionCode(
                    schedule.getProjectCode(),
                    schedule.getWorkflowDefinitionCode(),
                    params));
        }
        return mergeRemoteSchedule(schedule, response);
    }

    @Override
    public void deleteSchedule(Long projectCode, Long scheduleId) {
        if (remoteApi == null) {
            return;
        }
        responseSupport.toJsonNode(remoteApi.deleteSchedule(projectCode, scheduleId));
    }

    @Override
    public void onlineSchedule(Long projectCode, Long scheduleId) {
        if (remoteApi == null) {
            return;
        }
        responseSupport.toJsonNode(remoteApi.onlineSchedule(projectCode, scheduleId));
    }

    @Override
    public void offlineSchedule(Long projectCode, Long scheduleId) {
        if (remoteApi == null) {
            return;
        }
        responseSupport.toJsonNode(remoteApi.offlineSchedule(projectCode, scheduleId));
    }

    @Override
    public List<WorkflowScheduleDTO> querySchedules(WorkflowScheduleDTO schedule) {
        if (remoteApi == null) {
            return java.util.Arrays.asList();
        }
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        if (schedule != null && schedule.getWorkflowDefinitionCode() != null) {
            params.add("workflowDefinitionCode", String.valueOf(schedule.getWorkflowDefinitionCode()));
        }
        if (schedule != null && StringUtils.hasText(schedule.getSearchVal())) {
            params.add("searchVal", schedule.getSearchVal());
        }
        params.add("pageNo", "1");
        params.add("pageSize", "100");
        JsonNode response = responseSupport.toJsonNode(remoteApi.listSchedules(
                schedule == null ? 0L : schedule.getProjectCode(),
                normalizeQueryParams(params)));
        List<Map<String, Object>> items = extractScheduleItems(response);
        return items.stream()
                .map(item -> mapSchedule(schedule, item))
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<String> previewSchedule(WorkflowSchedulePreviewRequest request) {
        if (remoteApi == null) {
            return java.util.Arrays.asList();
        }
        if (request == null || request.getProjectCode() == null) {
            throw new IllegalArgumentException("DolphinScheduler 调度预览需要配置项目编码");
        }
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("schedule", request.getScheduleJson());
        JsonNode response = responseSupport.toJsonNode(remoteApi.previewSchedule(request.getProjectCode(), params));
        List<String> items = extractPreviewItems(response);
        return items.isEmpty() ? java.util.Arrays.asList() : items;
    }

    private MultiValueMap<String, String> buildScheduleParams(WorkflowScheduleDTO schedule) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        if (schedule == null) {
            return params;
        }
        params.add("workflowDefinitionCode", valueOf(schedule.getWorkflowDefinitionCode(), schedule.getWorkflowCode()));
        params.add("schedule", schedule.getScheduleJson());
        params.add("warningType", valueOf(schedule.getWarningType(), "NONE"));
        params.add("warningGroupId", valueOf(schedule.getWarningGroupId(), 1));
        params.add("failureStrategy", valueOf(schedule.getFailureStrategy(), "CONTINUE"));
        params.add("workerGroup", valueOf(schedule.getWorkerGroup(), "default"));
        params.add("tenantCode", "default");
        params.add("environmentCode", valueOf(schedule.getEnvironmentCode(), -1L));
        params.add("workflowInstancePriority", valueOf(schedule.getWorkflowInstancePriority(), "MEDIUM"));
        return params;
    }

    private WorkflowScheduleDTO mergeRemoteSchedule(WorkflowScheduleDTO request, JsonNode response) {
        Map<String, Object> payload = response == null || !response.hasNonNull("data")
                ? java.util.Collections.emptyMap()
                : responseSupport.getObjectMapper().convertValue(response.get("data"), Map.class);
        return mapSchedule(request, payload).toBuilder()
                .message(stringValue(payload.get("message"), payload.get("msg"), request == null ? null : request.getMessage()))
                .attributes(payload)
                .build();
    }

    private WorkflowScheduleDTO mapSchedule(WorkflowScheduleDTO base, Map<String, Object> payload) {
        if (payload == null) {
            payload = java.util.Collections.emptyMap();
        }
        WorkflowScheduleDTO.WorkflowScheduleDTOBuilder builder = (base == null ? WorkflowScheduleDTO.builder() : base.toBuilder());
        builder.scheduleId(longValue(payload.get("id"), payload.get("scheduleId"), payload.get("code")));
        builder.projectCode(longValue(payload.get("projectCode"), payload.get("project")));
        builder.workflowDefinitionCode(longValue(payload.get("workflowDefinitionCode"), payload.get("workflowCode")));
        builder.scheduleJson(stringValue(payload.get("schedule"), payload.get("crontab"), payload.get("cron")));
        builder.warningType(stringValue(payload.get("warningType")));
        builder.warningGroupId(intValue(payload.get("warningGroupId")));
        builder.failureStrategy(stringValue(payload.get("failureStrategy")));
        builder.workflowInstancePriority(stringValue(payload.get("workflowInstancePriority")));
        builder.workerGroup(stringValue(payload.get("workerGroup")));
        builder.environmentCode(longValue(payload.get("environmentCode")));
        builder.releaseState(stringValue(payload.get("releaseState")));
        builder.online(booleanValue(payload.get("online"), payload.get("releaseState")));
        builder.message(stringValue(payload.get("message"), payload.get("msg")));
        builder.attributes(payload);
        return builder.build();
    }

    private List<Map<String, Object>> extractScheduleItems(JsonNode response) {
        if (response == null || !response.hasNonNull("data")) {
            return java.util.Arrays.asList();
        }
        JsonNode data = response.get("data");
        if (data.isArray()) {
            return responseSupport.getObjectMapper().convertValue(data,
                    responseSupport.getObjectMapper().getTypeFactory().constructCollectionType(List.class, Map.class));
        }
        for (String key : java.util.Arrays.asList("dataList", "totalList", "records", "items")) {
            if (data.has(key) && data.get(key).isArray()) {
                return responseSupport.getObjectMapper().convertValue(data.get(key),
                        responseSupport.getObjectMapper().getTypeFactory().constructCollectionType(List.class, Map.class));
            }
        }
        return java.util.Arrays.asList();
    }

    private List<String> extractPreviewItems(JsonNode response) {
        if (response == null || !response.hasNonNull("data")) {
            return java.util.Arrays.asList();
        }
        JsonNode data = response.get("data");
        if (data.isArray()) {
            return responseSupport.getObjectMapper().convertValue(data,
                    responseSupport.getObjectMapper().getTypeFactory().constructCollectionType(List.class, String.class));
        }
        if (data.isObject()) {
            for (String key : java.util.Arrays.asList("dataList", "totalList", "records", "items")) {
                if (data.has(key) && data.get(key).isArray()) {
                    return responseSupport.getObjectMapper().convertValue(data.get(key),
                            responseSupport.getObjectMapper().getTypeFactory().constructCollectionType(List.class, String.class));
                }
            }
        }
        return java.util.Arrays.asList();
    }

    private Map<String, String> normalizeQueryParams(MultiValueMap<String, String> queryParams) {
        Map<String, String> result = new LinkedHashMap<>();
        if (queryParams == null || queryParams.isEmpty()) {
            return result;
        }
        queryParams.forEach((key, values) -> {
            if (values != null && !values.isEmpty()) {
                result.put(key, values.get(0));
            }
        });
        return result;
    }

    private String valueOf(Object primary, Object fallback) {
        if (primary != null) {
            String value = String.valueOf(primary);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return fallback == null ? null : String.valueOf(fallback);
    }

    private Long longValue(Object... values) {
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            try {
                return Long.parseLong(String.valueOf(value));
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private Integer intValue(Object... values) {
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private Boolean booleanValue(Object... values) {
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            String string = String.valueOf(value);
            if (StringUtils.hasText(string)) {
                if ("ONLINE".equalsIgnoreCase(string) || "1".equals(string)) {
                    return true;
                }
                if ("OFFLINE".equalsIgnoreCase(string) || "0".equals(string)) {
                    return false;
                }
            }
        }
        return null;
    }

    private String stringValue(Object... values) {
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
}
