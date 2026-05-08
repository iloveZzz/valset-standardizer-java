package com.yss.valset.workflow.dolphinscheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.yss.valset.workflow.model.EtlPlatformType;
import com.yss.valset.workflow.model.WorkflowScheduleDTO;
import com.yss.valset.workflow.model.WorkflowSchedulePreviewRequest;
import com.yss.valset.workflow.service.AbstractWorkflowSchedulePlatformClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
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

    private final DolphinSchedulerApiSupport apiSupport = new DolphinSchedulerApiSupport();

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

    @Value("${valset.workflow.dolphinscheduler.base-url:}")
    public void setBaseUrl(String baseUrl) {
        apiSupport.setBaseUrl(baseUrl);
    }

    @Override
    public WorkflowScheduleDTO saveSchedule(WorkflowScheduleDTO schedule) {
        if (!apiSupport.hasBaseUrl()) {
            return schedule == null ? null : schedule.toBuilder().build();
        }
        MultiValueMap<String, String> params = buildScheduleParams(schedule);
        JsonNode response = apiSupport.postFormJson("/projects/{projectCode}/schedules",
                params,
                schedule.getProjectCode());
        return mergeRemoteSchedule(schedule, response);
    }

    @Override
    public WorkflowScheduleDTO updateSchedule(WorkflowScheduleDTO schedule) {
        if (!apiSupport.hasBaseUrl()) {
            return schedule == null ? null : schedule.toBuilder().build();
        }
        MultiValueMap<String, String> params = buildScheduleParams(schedule);
        JsonNode response;
        if (schedule != null && schedule.getScheduleId() != null) {
            response = apiSupport.putFormJson("/projects/{projectCode}/schedules/{scheduleId}",
                    params,
                    schedule.getProjectCode(),
                    schedule.getScheduleId());
        } else {
            response = apiSupport.putFormJson("/projects/{projectCode}/schedules/update/{workflowDefinitionCode}",
                    params,
                    schedule.getProjectCode(),
                    schedule.getWorkflowDefinitionCode());
        }
        return mergeRemoteSchedule(schedule, response);
    }

    @Override
    public void deleteSchedule(Long projectCode, Long scheduleId) {
        if (!apiSupport.hasBaseUrl()) {
            return;
        }
        apiSupport.deleteJson("/projects/{projectCode}/schedules/{scheduleId}", projectCode, scheduleId);
    }

    @Override
    public void onlineSchedule(Long projectCode, Long scheduleId) {
        if (!apiSupport.hasBaseUrl()) {
            return;
        }
        apiSupport.postFormJson("/projects/{projectCode}/schedules/{scheduleId}/online",
                new LinkedMultiValueMap<>(),
                projectCode,
                scheduleId);
    }

    @Override
    public void offlineSchedule(Long projectCode, Long scheduleId) {
        if (!apiSupport.hasBaseUrl()) {
            return;
        }
        apiSupport.postFormJson("/projects/{projectCode}/schedules/{scheduleId}/offline",
                new LinkedMultiValueMap<>(),
                projectCode,
                scheduleId);
    }

    @Override
    public List<WorkflowScheduleDTO> querySchedules(WorkflowScheduleDTO schedule) {
        if (!apiSupport.hasBaseUrl()) {
            return List.of();
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
        JsonNode response = apiSupport.getJson("/projects/{projectCode}/schedules", params,
                schedule == null ? 0L : schedule.getProjectCode());
        List<Map<String, Object>> items = extractScheduleItems(response);
        return items.stream()
                .map(item -> mapSchedule(schedule, item))
                .toList();
    }

    @Override
    public List<String> previewSchedule(WorkflowSchedulePreviewRequest request) {
        if (!apiSupport.hasBaseUrl()) {
            return List.of();
        }
        if (request == null || request.getProjectCode() == null) {
            throw new IllegalArgumentException("DolphinScheduler 调度预览需要配置项目编码");
        }
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("schedule", request.getScheduleJson());
        JsonNode response = apiSupport.postFormJson("/projects/{projectCode}/schedules/preview", params, request.getProjectCode());
        List<String> items = extractPreviewItems(response);
        return items.isEmpty() ? List.of() : items;
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
        params.add("tenantCode", valueOf(schedule.getTenantCode(), "default"));
        params.add("environmentCode", valueOf(schedule.getEnvironmentCode(), -1L));
        params.add("workflowInstancePriority", valueOf(schedule.getWorkflowInstancePriority(), "MEDIUM"));
        return params;
    }

    private WorkflowScheduleDTO mergeRemoteSchedule(WorkflowScheduleDTO request, JsonNode response) {
        Map<String, Object> payload = response == null || !response.hasNonNull("data")
                ? Map.of()
                : apiSupport.getObjectMapper().convertValue(response.get("data"), Map.class);
        return mapSchedule(request, payload).toBuilder()
                .message(stringValue(payload.get("message"), payload.get("msg"), request == null ? null : request.getMessage()))
                .attributes(payload)
                .build();
    }

    private WorkflowScheduleDTO mapSchedule(WorkflowScheduleDTO base, Map<String, Object> payload) {
        if (payload == null) {
            payload = Map.of();
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
        builder.tenantCode(stringValue(payload.get("tenantCode")));
        builder.environmentCode(longValue(payload.get("environmentCode")));
        builder.releaseState(stringValue(payload.get("releaseState")));
        builder.online(booleanValue(payload.get("online"), payload.get("releaseState")));
        builder.message(stringValue(payload.get("message"), payload.get("msg")));
        builder.attributes(payload);
        return builder.build();
    }

    private List<Map<String, Object>> extractScheduleItems(JsonNode response) {
        if (response == null || !response.hasNonNull("data")) {
            return List.of();
        }
        JsonNode data = response.get("data");
        if (data.isArray()) {
            return apiSupport.getObjectMapper().convertValue(data,
                    apiSupport.getObjectMapper().getTypeFactory().constructCollectionType(List.class, Map.class));
        }
        for (String key : List.of("dataList", "totalList", "records", "items")) {
            if (data.has(key) && data.get(key).isArray()) {
                return apiSupport.getObjectMapper().convertValue(data.get(key),
                        apiSupport.getObjectMapper().getTypeFactory().constructCollectionType(List.class, Map.class));
            }
        }
        return List.of();
    }

    private List<String> extractPreviewItems(JsonNode response) {
        if (response == null || !response.hasNonNull("data")) {
            return List.of();
        }
        JsonNode data = response.get("data");
        if (data.isArray()) {
            return apiSupport.getObjectMapper().convertValue(data,
                    apiSupport.getObjectMapper().getTypeFactory().constructCollectionType(List.class, String.class));
        }
        if (data.isObject()) {
            for (String key : List.of("dataList", "totalList", "records", "items")) {
                if (data.has(key) && data.get(key).isArray()) {
                    return apiSupport.getObjectMapper().convertValue(data.get(key),
                            apiSupport.getObjectMapper().getTypeFactory().constructCollectionType(List.class, String.class));
                }
            }
        }
        return List.of();
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
            if (value instanceof Number number) {
                return number.longValue();
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
            if (value instanceof Number number) {
                return number.intValue();
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
            if (value instanceof Boolean bool) {
                return bool;
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
