package com.yss.valset.batch.dispatcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.yss.valset.application.event.lifecycle.WorkflowTaskLifecycleEvent;
import com.yss.valset.batch.task.TaskExecutor;
import com.yss.valset.common.support.TaskFailureClassifier;
import com.yss.valset.domain.gateway.WorkflowTaskGateway;
import com.yss.valset.domain.model.TaskStatus;
import com.yss.valset.domain.model.WorkflowTask;
import com.yss.valset.domain.model.TaskType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 默认任务调度程序。
 */
@Component
public class DefaultTaskDispatcher implements TaskDispatcher {

    private final WorkflowTaskGateway taskGateway;
    private final Map<TaskType, TaskExecutor> executors;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    public DefaultTaskDispatcher(
            WorkflowTaskGateway taskGateway,
            List<TaskExecutor> executorList,
            ObjectMapper objectMapper,
            ApplicationEventPublisher applicationEventPublisher
    ) {
        this.taskGateway = taskGateway;
        this.executors = executorList.stream().collect(Collectors.toMap(TaskExecutor::support, Function.identity()));
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * 通过其注册的执行器分派任务。
     */
    @Override
    public void dispatchTask(Long taskId) {
        WorkflowTask task = taskGateway.findById(taskId);
        TaskEventContext eventContext = buildEventContext(task, null);
        TaskExecutor executor = executors.get(task.getTaskType());
        if (executor == null) {
            throw new IllegalStateException("No executor for taskType=" + task.getTaskType());
        }

        boolean locked = taskGateway.markRunning(taskId);
        if (!locked) {
            publishTaskEvent(task, eventContext, TaskStatus.CANCELED, "工作流任务未获得执行锁，已跳过本次分派", null);
            return;
        }
        publishTaskEvent(task, eventContext, TaskStatus.RUNNING, "工作流任务开始执行", null);

        try {
            executor.execute(taskId);
            WorkflowTask latestTask = taskGateway.findById(taskId);
            String resultPayload = latestTask == null ? null : latestTask.getResultPayload();
            taskGateway.markSuccess(taskId, resultPayload);
            publishTaskEvent(task, buildEventContext(task, resultPayload), TaskStatus.SUCCESS, "工作流任务执行成功", null);
        } catch (Exception ex) {
            String failurePayload = buildFailurePayload(task.getTaskType(), ex);
            taskGateway.markFailed(taskId, failurePayload);
            publishTaskEvent(task, buildEventContext(task, failurePayload), TaskStatus.FAILED, "工作流任务执行失败", ex);
            throw ex;
        }
    }

    private String buildFailurePayload(TaskType taskType, Exception exception) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskType", taskType == null ? null : taskType.name());
        payload.put("errorCode", TaskFailureClassifier.classify(exception));
        payload.put("errorMessage", TaskFailureClassifier.resolveReadableMessage(exception));
        payload.put("rootCauseMessage", TaskFailureClassifier.rootCauseMessage(exception));
        payload.put("errorType", exception == null ? null : exception.getClass().getName());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception jsonException) {
            return payload.toString();
        }
    }

    private void publishTaskEvent(WorkflowTask task,
                                  TaskEventContext eventContext,
                                  TaskStatus status,
                                  String message,
                                  Exception exception) {
        if (applicationEventPublisher == null || task == null) {
            return;
        }
        applicationEventPublisher.publishEvent(WorkflowTaskLifecycleEvent.builder()
                .taskId(task.getTaskId())
                .taskType(task.getTaskType())
                .taskStage(task.getTaskStage())
                .taskStatus(status)
                .businessKey(firstText(task.getBusinessKey(), eventContext.businessKey()))
                .fileId(eventContext.fileId())
                .inputSummary(eventContext.inputSummary())
                .outputSummary(eventContext.outputSummary())
                .message(message)
                .errorCode(exception == null ? null : TaskFailureClassifier.classify(exception))
                .errorMessage(exception == null ? null : TaskFailureClassifier.resolveReadableMessage(exception))
                .attributes(eventContext.attributes())
                .build());
    }

    private TaskEventContext buildEventContext(WorkflowTask task, String outputPayload) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        if (task == null) {
            return new TaskEventContext(null, null, null, null, attributes);
        }
        JsonNode inputNode = readJson(task.getInputPayload());
        JsonNode outputNode = readJson(outputPayload);
        Long fileId = firstLong(task.getFileId(), jsonLong(inputNode, "fileId"), jsonLong(outputNode, "fileId"));
        putText(attributes, "filesysFileId", firstText(jsonText(inputNode, "filesysFileId"), jsonText(outputNode, "filesysFileId")));
        putText(attributes, "productCode", firstText(jsonText(inputNode, "productCode"), jsonText(outputNode, "productCode")));
        putText(attributes, "productName", firstText(jsonText(inputNode, "productName"), jsonText(outputNode, "productName")));
        putText(attributes, "managerName", firstText(jsonText(inputNode, "managerName"), jsonText(outputNode, "managerName")));
        putText(attributes, "valuationDate", firstText(jsonText(inputNode, "valuationDate"), jsonText(outputNode, "valuationDate")));
        putText(attributes, "dataSourceType", firstText(jsonText(inputNode, "dataSourceType"), jsonText(outputNode, "dataSourceType")));
        putText(attributes, "sourceType", firstText(jsonText(inputNode, "sourceType"), jsonText(outputNode, "sourceType")));
        putText(attributes, "workbookPath", firstText(jsonText(inputNode, "workbookPath"), jsonText(outputNode, "workbookPath")));
        putText(attributes, "fileNameOriginal", firstText(jsonText(inputNode, "fileNameOriginal"), jsonText(outputNode, "fileNameOriginal")));
        putText(attributes, "forceRebuild", jsonText(inputNode, "forceRebuild"));
        if (fileId != null) {
            attributes.put("fileId", fileId);
        }
        String inputSummary = summarizePayload(task.getInputPayload());
        String outputSummary = summarizePayload(outputPayload);
        return new TaskEventContext(
                fileId,
                firstText(task.getBusinessKey(), jsonText(inputNode, "businessKey")),
                inputSummary,
                outputSummary,
                attributes
        );
    }

    private JsonNode readJson(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(payload);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String summarizePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        String normalized = payload.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 1024 ? normalized : normalized.substring(0, 1024);
    }

    private static void putText(Map<String, Object> attributes, String key, String value) {
        if (value != null && !value.isBlank()) {
            attributes.put(key, value.trim());
        }
    }

    private static String jsonText(JsonNode node, String field) {
        if (node == null || field == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        JsonNode value = node.get(field);
        return value.isTextual() ? value.asText() : value.asText(null);
    }

    private static Long jsonLong(JsonNode node, String field) {
        if (node == null || field == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        JsonNode value = node.get(field);
        if (value.isNumber()) {
            return value.asLong();
        }
        if (value.isTextual() && !value.asText().isBlank()) {
            try {
                return Long.parseLong(value.asText().trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Long firstLong(Long... values) {
        if (values == null) {
            return null;
        }
        for (Long value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String firstText(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private record TaskEventContext(Long fileId,
                                    String businessKey,
                                    String inputSummary,
                                    String outputSummary,
                                    Map<String, Object> attributes) {
    }
}
