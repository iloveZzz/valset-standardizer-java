package com.yss.valset.application.event.lifecycle;

import com.yss.valset.domain.model.TaskStage;
import com.yss.valset.domain.model.TaskStatus;
import com.yss.valset.domain.model.TaskType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 工作流任务生命周期事件。
 */
@Getter
@Builder(toBuilder = true)
public class WorkflowTaskLifecycleEvent {

    @Builder.Default
    private final String eventId = UUID.randomUUID().toString();

    @Builder.Default
    private final Instant occurredAt = Instant.now();

    private final Long taskId;

    private final TaskType taskType;

    private final TaskStage taskStage;

    private final TaskStatus taskStatus;

    private final String businessKey;

    private final Long fileId;

    private final String inputSummary;

    private final String outputSummary;

    private final String message;

    private final String errorCode;

    private final String errorMessage;

    @Builder.Default
    private final Map<String, Object> attributes = new LinkedHashMap<>();
}
