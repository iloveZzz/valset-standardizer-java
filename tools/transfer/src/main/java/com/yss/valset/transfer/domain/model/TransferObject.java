package com.yss.valset.transfer.domain.model;

import java.time.Instant;
import java.util.Map;

/**
 * 已收取的文件主对象。
 */
public record TransferObject(
        String transferId,
        String sourceId,
        String sourceType,
        String sourceCode,
        String originalName,
        String extension,
        String mimeType,
        Long sizeBytes,
        String fingerprint,
        String sourceRef,
        String mailId,
        String mailFrom,
        String mailTo,
        String mailCc,
        String mailBcc,
        String mailSubject,
        String mailBody,
        String mailProtocol,
        String mailFolder,
        String localTempPath,
        TransferStatus status,
        Instant receivedAt,
        Instant storedAt,
        String routeId,
        String errorMessage,
        ProbeResult probeResult,
        Map<String, Object> fileMeta
) {

    public TransferObject withLocalTempPath(String localTempPath) {
        return new TransferObject(
                transferId,
                sourceId,
                sourceType,
                sourceCode,
                originalName,
                extension,
                mimeType,
                sizeBytes,
                fingerprint,
                sourceRef,
                mailId,
                mailFrom,
                mailTo,
                mailCc,
                mailBcc,
                mailSubject,
                mailBody,
                mailProtocol,
                mailFolder,
                localTempPath,
                status,
                receivedAt,
                storedAt,
                routeId,
                errorMessage,
                probeResult,
                fileMeta
        );
    }

    public TransferObject withRouteId(String routeId) {
        return new TransferObject(
                transferId,
                sourceId,
                sourceType,
                sourceCode,
                originalName,
                extension,
                mimeType,
                sizeBytes,
                fingerprint,
                sourceRef,
                mailId,
                mailFrom,
                mailTo,
                mailCc,
                mailBcc,
                mailSubject,
                mailBody,
                mailProtocol,
                mailFolder,
                localTempPath,
                status,
                receivedAt,
                storedAt,
                routeId,
                errorMessage,
                probeResult,
                fileMeta
        );
    }

    public TransferObject withStatus(TransferStatus status, String errorMessage) {
        return new TransferObject(
                transferId,
                sourceId,
                sourceType,
                sourceCode,
                originalName,
                extension,
                mimeType,
                sizeBytes,
                fingerprint,
                sourceRef,
                mailId,
                mailFrom,
                mailTo,
                mailCc,
                mailBcc,
                mailSubject,
                mailBody,
                mailProtocol,
                mailFolder,
                localTempPath,
                status,
                receivedAt,
                storedAt,
                routeId,
                errorMessage,
                probeResult,
                fileMeta
        );
    }

    public TransferObject withProbeResult(ProbeResult probeResult) {
        return new TransferObject(
                transferId,
                sourceId,
                sourceType,
                sourceCode,
                originalName,
                extension,
                mimeType,
                sizeBytes,
                fingerprint,
                sourceRef,
                mailId,
                mailFrom,
                mailTo,
                mailCc,
                mailBcc,
                mailSubject,
                mailBody,
                mailProtocol,
                mailFolder,
                localTempPath,
                status,
                receivedAt,
                storedAt,
                routeId,
                errorMessage,
                probeResult,
                fileMeta
        );
    }
}
