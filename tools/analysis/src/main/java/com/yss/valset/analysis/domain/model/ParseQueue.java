package com.yss.valset.analysis.domain.model;

import java.time.Instant;

/**
 * 待解析任务。
 */
public record ParseQueue(
        String queueId,
        String businessKey,
        String transferId,
        String originalName,
        String sourceId,
        String sourceType,
        String sourceCode,
        String routeId,
        String deliveryId,
        String tagId,
        String tagCode,
        String tagName,
        String fileStatus,
        String deliveryStatus,
        ParseStatus parseStatus,
        ParseTriggerMode triggerMode,
        Integer retryCount,
        String subscribedBy,
        Instant subscribedAt,
        Instant parsedAt,
        String lastErrorMessage,
        String objectSnapshotJson,
        String deliverySnapshotJson,
        String parseRequestJson,
        String parseResultJson,
        Instant createdAt,
        Instant updatedAt
) {
}
