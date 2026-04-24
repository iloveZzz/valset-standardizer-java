package com.yss.valset.application.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.application.dto.TaskViewDTO;
import com.yss.valset.application.service.TaskQueryAppService;
import com.yss.valset.domain.gateway.TaskGateway;
import com.yss.valset.domain.model.TaskInfo;
import com.yss.valset.domain.model.TaskStatus;
import com.yss.valset.domain.model.TaskType;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 任务查询的默认实现。
 */
@Service
public class DefaultTaskQueryAppService implements TaskQueryAppService {

    private final TaskGateway taskGateway;
    private final ObjectMapper objectMapper;

    public DefaultTaskQueryAppService(TaskGateway taskGateway, ObjectMapper objectMapper) {
        this.taskGateway = taskGateway;
        this.objectMapper = objectMapper;
    }

    /**
     * 查询任务并将有效负载扩展为 JSON 友好的映射。
     */
    @Override
    public TaskViewDTO queryTask(Long taskId) {
        TaskInfo taskInfo = taskGateway.findById(taskId);
        String taskStatus = taskInfo.getTaskStatus().name();
        boolean failedTask = TaskStatus.FAILED.name().equals(taskStatus);
        Map<String, Object> resultData = parsePayload(taskInfo.getResultPayload(), failedTask);
        return TaskViewDTO.builder()
                .taskId(taskInfo.getTaskId() == null ? null : String.valueOf(taskInfo.getTaskId()))
                .taskType(taskInfo.getTaskType().name())
                .taskStage(taskInfo.getTaskStage() == null ? null : taskInfo.getTaskStage().name())
                .taskStatus(taskStatus)
                .businessKey(taskInfo.getBusinessKey())
                .inputPayload(taskInfo.getInputPayload())
                .inputData(parsePayload(taskInfo.getInputPayload(), false))
                .resultPayload(taskInfo.getResultPayload())
                .resultData(resultData)
                .errorMessage(failedTask ? extractErrorMessage(resultData, taskInfo.getResultPayload()) : null)
                .errorCode(failedTask ? extractErrorCode(resultData) : null)
                .rowCount(toStringValue(extractLong(taskInfo.getTaskType(), resultData, "rowCount")))
                .fileSizeBytes(toStringValue(extractLong(taskInfo.getTaskType(), resultData, "fileSizeBytes")))
                .durationMs(toStringValue(extractLong(taskInfo.getTaskType(), resultData, "durationMs")))
                .taskStartTime(taskInfo.getTaskStartTime())
                .parseTaskTimeMs(toStringValue(taskInfo.getParseTaskTimeMs()))
                .standardizeTimeMs(toStringValue(taskInfo.getStandardizeTimeMs()))
                .matchStandardSubjectTimeMs(toStringValue(taskInfo.getMatchStandardSubjectTimeMs()))
                .build();
    }

    /**
     * 尝试将负载文本解析为 JSON。
     */
    private Map<String, Object> parsePayload(String payload, boolean failedTask) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception exception) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            if (failedTask) {
                fallback.put("errorMessage", payload);
            } else {
                fallback.put("rawText", payload);
            }
            return fallback;
        }
    }

    private Long extractLong(TaskType taskType, Map<String, Object> resultData, String fieldName) {
        if (taskType != TaskType.EXTRACT_DATA || resultData == null) {
            return null;
        }
        Object value = resultData.get(fieldName);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String extractErrorMessage(Map<String, Object> resultData, String rawPayload) {
        if (resultData != null) {
            Object errorMessage = resultData.get("errorMessage");
            if (errorMessage != null) {
                String text = String.valueOf(errorMessage).trim();
                if (!text.isEmpty()) {
                    return text;
                }
            }
            Object rootCauseMessage = resultData.get("rootCauseMessage");
            if (rootCauseMessage != null) {
                String text = String.valueOf(rootCauseMessage).trim();
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }
        return rawPayload == null ? null : rawPayload.trim();
    }

    private String extractErrorCode(Map<String, Object> resultData) {
        if (resultData == null) {
            return null;
        }
        Object errorCode = resultData.get("errorCode");
        if (errorCode == null) {
            return null;
        }
        String text = String.valueOf(errorCode).trim();
        return text.isEmpty() ? null : text;
    }

    private String toStringValue(Long value) {
        return value == null ? null : String.valueOf(value);
    }
}
