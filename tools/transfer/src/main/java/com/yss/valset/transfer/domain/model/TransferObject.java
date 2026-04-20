package com.yss.valset.transfer.domain.model;

import java.time.Instant;
import java.util.Map;

/**
 * 已收取的文件主对象。
 */
public record TransferObject(
        Long transferId,
        Long sourceId,
        String originalName,
        String normalizedName,
        String extension,
        String mimeType,
        Long sizeBytes,
        String fingerprint,
        String sourceRef,
        String localTempPath,
        TransferStatus status,
        Instant receivedAt,
        Instant storedAt,
        Long routeId,
        String errorMessage,
        Map<String, Object> fileMeta
) {
}
