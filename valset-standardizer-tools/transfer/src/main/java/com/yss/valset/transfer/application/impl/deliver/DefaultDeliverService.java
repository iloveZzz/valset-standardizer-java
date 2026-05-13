package com.yss.valset.transfer.application.impl.deliver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.transfer.application.port.DeliverTransferUseCase;
import com.yss.valset.transfer.application.port.TransferParseQueueProvisionUseCase;
import com.yss.valset.transfer.application.port.TransferJobScheduler;
import com.yss.valset.transfer.domain.gateway.TransferDeliveryGateway;
import com.yss.valset.transfer.domain.gateway.TransferObjectGateway;
import com.yss.valset.transfer.domain.gateway.TransferRouteGateway;
import com.yss.valset.transfer.domain.gateway.TransferRunLogGateway;
import com.yss.valset.transfer.domain.gateway.TransferTargetGateway;
import com.yss.valset.transfer.domain.model.TransferContext;
import com.yss.valset.transfer.domain.model.TransferDeliveryRecord;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferResult;
import com.yss.valset.transfer.domain.model.TransferRoute;
import com.yss.valset.transfer.domain.model.TransferTarget;
import com.yss.valset.transfer.domain.model.TransferRunLog;
import com.yss.valset.transfer.domain.model.TransferRunStage;
import com.yss.valset.transfer.domain.model.TransferRunStatus;
import com.yss.valset.transfer.domain.model.config.TransferConfigKeys;
import com.yss.valset.transfer.domain.model.config.TransferRouteConfig;
import com.yss.valset.transfer.infrastructure.plugin.TransferActionPluginRegistry;
import com.yss.valset.domain.gateway.ValsetFileInfoGateway;
import com.yss.valset.domain.model.ValsetFileInfo;
import com.yss.valset.domain.model.ValsetFileSourceChannel;
import com.yss.valset.domain.model.ValsetFileStatus;
import com.yss.valset.domain.model.ValsetFileStorageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 默认文件投递应用服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultDeliverService implements DeliverTransferUseCase {

    private final TransferRouteGateway transferRouteGateway;
    private final TransferObjectGateway transferObjectGateway;
    private final TransferTargetGateway transferTargetGateway;
    private final TransferDeliveryGateway transferDeliveryGateway;
    private final TransferActionPluginRegistry transferActionPluginRegistry;
    private final TransferRunLogGateway transferRunLogGateway;
    private final TransferParseQueueProvisionUseCase transferParseQueueProvisionUseCase;
    private final ValsetFileInfoGateway valsetFileInfoGateway;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<TransferJobScheduler> transferJobSchedulerProvider;

    @Override
    public void execute(String routeId, String transferId) {
        TransferRoute route = transferRouteGateway.findById(routeId)
                .orElseThrow(() -> new IllegalStateException("未找到路由记录，routeId=" + routeId));
        TransferObject transferObject = transferObjectGateway.findById(transferId)
                .orElseThrow(() -> new IllegalStateException("未找到文件记录，transferId=" + transferId));
        TransferTarget target = transferTargetGateway.findByTargetCode(route.targetCode())
                .orElseThrow(() -> new IllegalStateException("未找到投递目标，targetCode=" + route.targetCode()));
        String triggerType = resolveTriggerType(route.routeMeta());
        boolean failureLogged = false;
        try {
            int attemptIndex = (int) transferDeliveryGateway.countByRouteId(routeId);
            log.info("开始文件投递，routeId={}，transferId={}，targetCode={}，targetType={}，originalName={}，retryCount={}",
                    route.routeId(),
                    transferObject.transferId(),
                    target.targetCode(),
                    target.targetType(),
                    transferObject.originalName(),
                    attemptIndex);
            saveRunLog(
                    transferObject,
                    route,
                    triggerType,
                    TransferRunStage.DELIVER.name(),
                    TransferRunStatus.SUCCESS.name(),
                    "开始投递，routeId=" + routeId
                            + "，transferId=" + transferId
                            + "，targetCode=" + target.targetCode()
                            + "，targetType=" + target.targetType(),
                    null
            );
            TransferContext context = new TransferContext(transferObject, route, target, buildAttributes(route, target));
            log.info("投递上下文已准备完成，routeId={}，transferId={}，targetCode={}，attemptIndex={}",
                    routeId,
                    transferId,
                    target.targetCode(),
                    attemptIndex);
            TransferResult result = transferActionPluginRegistry.getRequired(route).execute(context);
            TransferDeliveryRecord deliveryRecord = transferDeliveryGateway.recordResult(routeId, transferId, result, attemptIndex);
            if (!result.success()) {
                log.warn("文件投递未成功，routeId={}，transferId={}，targetCode={}，attemptIndex={}，messages={}",
                        routeId,
                        transferId,
                        target.targetCode(),
                        attemptIndex,
                        result.messages());
                scheduleRetryIfNeeded(routeId, transferId, route, attemptIndex + 1);
                saveRunLog(
                        transferObject,
                        route,
                        triggerType,
                        TransferRunStage.DELIVER.name(),
                        TransferRunStatus.FAILED.name(),
                        "文件投递失败，routeId=" + routeId
                                + "，transferId=" + transferId
                                + "，targetCode=" + target.targetCode()
                                + "，messages=" + result.messages(),
                        null
                );
                failureLogged = true;
                throw new IllegalStateException("文件投递失败，routeId=" + routeId + ", messages=" + result.messages());
            }
            clearFailedDeliverRunLogs(transferObject.transferId());
            transferObject = persistStoragePath(transferObject, result);
            updateFileInfoPaths(transferObject);
            try {
                transferParseQueueProvisionUseCase.ensureAutoGenerated(transferObject, deliveryRecord);
            } catch (RuntimeException exception) {
                log.warn("投递成功后生成待解析任务失败，routeId={}，transferId={}，error={}",
                        routeId,
                        transferId,
                        exception.getMessage(),
                        exception);
            }
            log.info("文件投递成功，routeId={}，transferId={}，targetCode={}，attemptIndex={}，messages={}",
                    routeId,
                    transferId,
                    target.targetCode(),
                    attemptIndex,
                    result.messages());
            saveRunLog(
                    transferObject,
                    route,
                    triggerType,
                    TransferRunStage.DELIVER.name(),
                    TransferRunStatus.SUCCESS.name(),
                    "文件投递完成，routeId=" + routeId
                            + "，transferId=" + transferId
                            + "，targetCode=" + route.targetCode()
                            + "，targetType=" + target.targetType(),
                    null
            );
        } catch (RuntimeException exception) {
            if (!failureLogged) {
                saveRunLog(
                        transferObject,
                        route,
                        triggerType,
                        TransferRunStage.DELIVER.name(),
                        TransferRunStatus.FAILED.name(),
                        "文件投递异常，routeId=" + routeId
                                + "，transferId=" + transferId
                                + "，targetCode=" + route.targetCode(),
                        exception
                );
            }
            throw exception;
        }
    }

    private TransferObject persistStoragePath(TransferObject transferObject, TransferResult result) {
        if (transferObject == null || result == null || result.storagePath() == null || result.storagePath().isBlank()) {
            return transferObject;
        }
        TransferObject updated = transferObject.withRealStoragePath(result.storagePath());
        return transferObjectGateway.save(updated);
    }

    private void updateFileInfoPaths(TransferObject transferObject) {
        if (transferObject == null || transferObject.fingerprint() == null || transferObject.fingerprint().isBlank()) {
            return;
        }
        try {
            ValsetFileInfo fileInfo = ensureFileInfo(transferObject);
            if (fileInfo == null || fileInfo.getFileId() == null) {
                log.warn("投递成功后未找到对应文件主数据，无法回写路径，transferId={}，fingerprint={}",
                        transferObject.transferId(),
                        transferObject.fingerprint());
                return;
            }
            valsetFileInfoGateway.updatePaths(
                    fileInfo.getFileId(),
                    firstNonBlank(transferObject.realStoragePath(), transferObject.localTempPath(), fileInfo.getStorageUri()),
                    transferObject.localTempPath(),
                    transferObject.realStoragePath()
            );
        } catch (RuntimeException exception) {
            log.warn("投递成功后回写文件主数据路径失败，transferId={}，fingerprint={}",
                    transferObject.transferId(),
                    transferObject.fingerprint(),
                    exception);
        }
    }

    private ValsetFileInfo ensureFileInfo(TransferObject transferObject) {
        ValsetFileInfo fileInfo = valsetFileInfoGateway.findByFingerprint(transferObject.fingerprint());
        if (fileInfo != null && fileInfo.getFileId() != null) {
            return fileInfo;
        }
        ValsetFileInfo snapshot = buildFileInfoSnapshot(transferObject);
        if (snapshot == null) {
            return null;
        }
        Long fileId = valsetFileInfoGateway.save(snapshot);
        snapshot.setFileId(fileId);
        log.info("投递成功后已自动补建文件主数据，transferId={}，fileId={}，fingerprint={}",
                transferObject.transferId(),
                fileId,
                transferObject.fingerprint());
        return snapshot;
    }

    private ValsetFileInfo buildFileInfoSnapshot(TransferObject transferObject) {
        if (transferObject == null || !StringUtils.hasText(transferObject.fingerprint())) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        String fileNameOriginal = firstNonBlank(transferObject.originalName(), transferObject.fingerprint());
        String fileNameNormalized = normalizeFilename(fileNameOriginal);
        String fileExtension = firstNonBlank(transferObject.extension(), resolveExtension(fileNameOriginal));
        String mimeType = firstNonBlank(transferObject.mimeType(), resolveMimeType(fileExtension));
        String localTempPath = firstNonBlank(transferObject.localTempPath(), transferObject.realStoragePath());
        String realStoragePath = firstNonBlank(transferObject.realStoragePath(), transferObject.localTempPath());
        String storageUri = firstNonBlank(realStoragePath, localTempPath, transferObject.sourceRef());
        return ValsetFileInfo.builder()
                .fileNameOriginal(fileNameOriginal)
                .fileNameNormalized(fileNameNormalized)
                .fileExtension(fileExtension)
                .mimeType(mimeType)
                .fileSizeBytes(transferObject.sizeBytes())
                .fileFingerprint(transferObject.fingerprint())
                .sourceChannel(resolveSourceChannel(transferObject))
                .sourceUri(resolveSourceUri(transferObject))
                .storageType(resolveStorageType(storageUri))
                .storageUri(storageUri)
                .localTempPath(localTempPath)
                .realStoragePath(realStoragePath)
                .fileFormat(resolveFileFormat(fileExtension, mimeType))
                .fileStatus(ValsetFileStatus.READY_FOR_EXTRACT)
                .createdBy(resolveCreatedBy(transferObject))
                .receivedAt(resolveTime(transferObject.receivedAt(), now))
                .storedAt(resolveTime(transferObject.storedAt(), now))
                .sourceMetaJson(buildSourceMetaJson(transferObject))
                .storageMetaJson(buildStorageMetaJson(transferObject))
                .remark(null)
                .build();
    }

    private ValsetFileSourceChannel resolveSourceChannel(TransferObject transferObject) {
        if (transferObject == null || !StringUtils.hasText(transferObject.sourceType())) {
            return ValsetFileSourceChannel.OBJECT_STORAGE;
        }
        if ("EMAIL".equalsIgnoreCase(transferObject.sourceType())) {
            return ValsetFileSourceChannel.EMAIL_ATTACHMENT;
        }
        return ValsetFileSourceChannel.OBJECT_STORAGE;
    }

    private ValsetFileStorageType resolveStorageType(String storageUri) {
        if (!StringUtils.hasText(storageUri)) {
            return ValsetFileStorageType.LOCAL;
        }
        String normalized = storageUri.trim().toLowerCase();
        if (normalized.startsWith("s3://")) {
            return ValsetFileStorageType.S3;
        }
        if (normalized.startsWith("oss://")) {
            return ValsetFileStorageType.OSS;
        }
        if (normalized.startsWith("minio://")) {
            return ValsetFileStorageType.MINIO;
        }
        return ValsetFileStorageType.LOCAL;
    }

    private String resolveSourceUri(TransferObject transferObject) {
        if (transferObject == null) {
            return null;
        }
        return firstNonBlank(transferObject.sourceRef(), transferObject.localTempPath(), transferObject.realStoragePath());
    }

    private String resolveFileFormat(String fileExtension, String mimeType) {
        String extension = fileExtension == null ? null : fileExtension.trim().toLowerCase();
        String mime = mimeType == null ? null : mimeType.trim().toLowerCase();
        if ((extension != null && extension.contains("csv"))
                || (mime != null && mime.contains("csv"))) {
            return "CSV";
        }
        return "EXCEL";
    }

    private String resolveMimeType(String fileExtension) {
        if (fileExtension == null) {
            return "application/octet-stream";
        }
        String normalized = fileExtension.trim().toLowerCase();
        if (normalized.contains("csv")) {
            return "text/csv";
        }
        if (normalized.contains("xls")) {
            return "application/vnd.ms-excel";
        }
        return "application/octet-stream";
    }

    private String resolveExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return null;
        }
        int index = filename.lastIndexOf('.');
        if (index < 0 || index >= filename.length() - 1) {
            return null;
        }
        return filename.substring(index + 1);
    }

    private String normalizeFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "valuation-file";
        }
        return filename.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String resolveCreatedBy(TransferObject transferObject) {
        if (transferObject == null) {
            return "system:transfer-deliver-backfill";
        }
        if (StringUtils.hasText(transferObject.sourceCode())) {
            return "system:transfer-deliver-backfill:" + transferObject.sourceCode().trim();
        }
        return "system:transfer-deliver-backfill";
    }

    private LocalDateTime resolveTime(java.time.Instant instant, LocalDateTime fallback) {
        if (instant == null) {
            return fallback;
        }
        return LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
    }

    private String buildSourceMetaJson(TransferObject transferObject) {
        LinkedHashMap<String, Object> meta = new LinkedHashMap<>();
        meta.put("transferId", transferObject == null ? null : transferObject.transferId());
        meta.put("sourceId", transferObject == null ? null : transferObject.sourceId());
        meta.put("sourceType", transferObject == null ? null : transferObject.sourceType());
        meta.put("sourceCode", transferObject == null ? null : transferObject.sourceCode());
        meta.put("sourceRef", transferObject == null ? null : transferObject.sourceRef());
        meta.put("mailId", transferObject == null ? null : transferObject.mailId());
        meta.put("mailFolder", transferObject == null ? null : transferObject.mailFolder());
        meta.put("fingerprint", transferObject == null ? null : transferObject.fingerprint());
        return buildJsonMeta(meta);
    }

    private String buildStorageMetaJson(TransferObject transferObject) {
        LinkedHashMap<String, Object> meta = new LinkedHashMap<>();
        meta.put("localTempPath", transferObject == null ? null : transferObject.localTempPath());
        meta.put("realStoragePath", transferObject == null ? null : transferObject.realStoragePath());
        meta.put("routeId", transferObject == null ? null : transferObject.routeId());
        meta.put("status", transferObject == null || transferObject.status() == null ? null : transferObject.status().name());
        meta.put("receivedAt", transferObject == null ? null : transferObject.receivedAt());
        meta.put("storedAt", transferObject == null ? null : transferObject.storedAt());
        return buildJsonMeta(meta);
    }

    private String buildJsonMeta(Map<String, Object> meta) {
        if (meta == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(meta);
        } catch (Exception exception) {
            log.warn("构建文件主数据元数据失败，transferId={}", meta.get("transferId"), exception);
            return meta.toString();
        }
    }

    private String firstNonBlank(String... values) {
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

    private void clearFailedDeliverRunLogs(String transferId) {
        try {
            long deletedCount = transferRunLogGateway.deleteFailedDeliverLogsByTransferId(transferId);
            log.info("已清理投递失败运行日志，transferId={}，deletedCount={}", transferId, deletedCount);
        } catch (RuntimeException exception) {
            log.warn("清理投递失败运行日志失败，transferId={}，error={}", transferId, exception.getMessage(), exception);
        }
    }

    private void scheduleRetryIfNeeded(String routeId, String transferId, TransferRoute route, int nextAttempt) {
        TransferRouteConfig routeConfig = TransferRouteConfig.from(route);
        int maxRetryCount = routeConfig.maxRetryCount();
        int retryDelaySeconds = routeConfig.retryDelaySeconds();
        if (nextAttempt < maxRetryCount) {
            log.info("准备调度投递重试，routeId={}，transferId={}，nextAttempt={}，maxRetryCount={}，delaySeconds={}",
                    routeId,
                    transferId,
                    nextAttempt,
                    maxRetryCount,
                    retryDelaySeconds);
            transferJobSchedulerProvider.getObject().scheduleDeliverRetry(routeId, transferId, nextAttempt, retryDelaySeconds);
        } else {
            log.info("投递重试次数已达到上限，routeId={}，transferId={}，nextAttempt={}，maxRetryCount={}",
                    routeId,
                    transferId,
                    nextAttempt,
                    maxRetryCount);
        }
    }

    private Map<String, Object> buildAttributes(TransferRoute route, TransferTarget target) {
        LinkedHashMap<String, Object> attributes = new LinkedHashMap<>();
        TransferRouteConfig routeConfig = TransferRouteConfig.from(route);
        attributes.putAll(routeConfig.toMetaMap());
        if (route != null && route.routeMeta() != null) {
            route.routeMeta().forEach(attributes::putIfAbsent);
        }
        attributes.put(TransferConfigKeys.TARGET_ID, target.targetId());
        attributes.put(TransferConfigKeys.TARGET_CODE, target.targetCode());
        attributes.put(TransferConfigKeys.TARGET_NAME, target.targetName());
        attributes.put(TransferConfigKeys.TARGET_TYPE, target.targetType() == null ? null : target.targetType().name());
        attributes.put(TransferConfigKeys.TARGET_PATH_TEMPLATE, target.targetPathTemplate());
        if (target.connectionConfig() != null) {
            attributes.putAll(target.connectionConfig());
        }
        if (target.targetMeta() != null) {
            attributes.putAll(target.targetMeta());
        }
        return attributes;
    }

    private String resolveTriggerType(Map<String, Object> routeMeta) {
        Object raw = routeMeta == null ? null : routeMeta.get(TransferConfigKeys.TRIGGER_TYPE);
        if (raw == null || String.valueOf(raw).isBlank()) {
            return null;
        }
        return String.valueOf(raw).trim().toUpperCase();
    }

    private void saveRunLog(TransferObject transferObject,
                            TransferRoute route,
                            String triggerType,
                            String runStage,
                            String runStatus,
                            String logMessage,
                            Throwable error) {
        if (error == null) {
            log.info("文件投递运行日志，stage={}，status={}，sourceId={}，transferId={}，routeId={}，message={}",
                    runStage,
                    runStatus,
                    transferObject == null ? null : transferObject.sourceId(),
                    transferObject == null ? null : transferObject.transferId(),
                    route == null ? null : route.routeId(),
                    logMessage);
        } else {
            log.error("文件投递运行日志，stage={}，status={}，sourceId={}，transferId={}，routeId={}，message={}，error={}",
                    runStage,
                    runStatus,
                    transferObject == null ? null : transferObject.sourceId(),
                    transferObject == null ? null : transferObject.transferId(),
                    route == null ? null : route.routeId(),
                    logMessage,
                    buildErrorMessage(error),
                    error);
        }
        transferRunLogGateway.save(new TransferRunLog(
                null,
                transferObject.sourceId(),
                transferObject.sourceType(),
                transferObject.sourceCode(),
                null,
                transferObject.transferId(),
                route == null ? null : route.routeId(),
                triggerType,
                runStage,
                runStatus,
                logMessage,
                error == null ? null : buildErrorMessage(error),
                LocalDateTime.now()
        ));
    }

    private String buildErrorMessage(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        Throwable current = throwable;
        int depth = 0;
        while (current != null && depth < 3) {
            if (depth > 0) {
                builder.append(" -> ");
            }
            builder.append(current.getClass().getSimpleName());
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                builder.append(": ").append(current.getMessage());
            }
            current = current.getCause();
            depth++;
        }
        return builder.toString();
    }
}
