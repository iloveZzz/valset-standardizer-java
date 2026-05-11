package com.yss.valset.application.event.lifecycle;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 解析生命周期事件。
 */
@Getter
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ParseLifecycleEvent {

    @Builder.Default
    private final String eventId = UUID.randomUUID().toString();

    @Builder.Default
    private final Instant occurredAt = Instant.now();

    private final ParseLifecycleStage stage;

    private final String source;

    private final String queueId;

    private final String transferId;

    private final String businessKey;

    private final Long taskId;

    private final Long fileId;

    private final String dataSourceType;

    private final String triggerMode;

    private final String subscribedBy;

    private final String message;

    private final String errorMessage;

    @Builder.Default
    private final Map<String, Object> attributes = new LinkedHashMap<>();
}
