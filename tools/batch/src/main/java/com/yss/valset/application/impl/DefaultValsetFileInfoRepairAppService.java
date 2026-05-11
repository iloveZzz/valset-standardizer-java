package com.yss.valset.application.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.application.command.ValsetFileInfoRepairCommand;
import com.yss.valset.application.dto.ValsetFileInfoRepairResultDTO;
import com.yss.valset.application.event.lifecycle.ParseLifecycleEvent;
import com.yss.valset.application.event.lifecycle.ParseLifecycleEventPublisher;
import com.yss.valset.application.event.lifecycle.ParseLifecycleStage;
import com.yss.valset.application.service.ValsetFileInfoRepairAppService;
import com.yss.valset.domain.gateway.ValsetFileInfoGateway;
import com.yss.valset.domain.model.ValsetFileInfo;
import com.yss.valset.domain.model.ValsetFileSourceChannel;
import com.yss.valset.domain.model.ValsetFileStorageType;
import com.yss.valset.domain.model.ValsetFileStatus;
import com.yss.valset.transfer.domain.gateway.TransferObjectGateway;
import com.yss.valset.transfer.domain.model.ProbeResult;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferObjectPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件主数据回填服务默认实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultValsetFileInfoRepairAppService implements ValsetFileInfoRepairAppService {

    private static final String BACKFILL_OPERATOR = "system:transfer-backfill";

    private final TransferObjectGateway transferObjectGateway;
    private final ValsetFileInfoGateway valsetFileInfoGateway;
    private final ParseLifecycleEventPublisher parseLifecycleEventPublisher;
    private final ObjectMapper objectMapper;

    @Override
    public ValsetFileInfo ensureFromTransferObject(TransferObject transferObject) {
        if (transferObject == null || !StringUtils.hasText(transferObject.fingerprint())) {
            return null;
        }
        publishRepairEvent(ParseLifecycleStage.QUEUE_FILE_INFO_REPAIR_STARTED, transferObject, "开始根据 TransferObject 回填文件主数据");
        ValsetFileInfo existing = valsetFileInfoGateway.findByFingerprint(transferObject.fingerprint());
        ValsetFileInfo snapshot = buildSnapshot(existing, transferObject);
        if (snapshot == null) {
            publishRepairEvent(ParseLifecycleStage.QUEUE_FILE_INFO_REPAIR_FAILED, transferObject, "文件主数据回填快照为空");
            return existing;
        }
        if (existing == null || existing.getFileId() == null) {
            Long fileId = valsetFileInfoGateway.save(snapshot);
            snapshot.setFileId(fileId);
            log.info("已根据 TransferObject 创建文件主数据，transferId={}, fileId={}, fingerprint={}",
                    transferObject.transferId(), fileId, transferObject.fingerprint());
            publishRepairEvent(ParseLifecycleStage.QUEUE_FILE_INFO_REPAIR_COMPLETED, snapshot, "已根据 TransferObject 创建文件主数据");
            return snapshot;
        }
        valsetFileInfoGateway.updateFromTransferObject(snapshot);
        log.info("已根据 TransferObject 回填文件主数据，transferId={}, fileId={}, fingerprint={}",
                transferObject.transferId(), snapshot.getFileId(), transferObject.fingerprint());
        publishRepairEvent(ParseLifecycleStage.QUEUE_FILE_INFO_REPAIR_COMPLETED, snapshot, "已根据 TransferObject 回填文件主数据");
        return snapshot;
    }

    @Override
    public ValsetFileInfoRepairResultDTO repair(ValsetFileInfoRepairCommand command) {
        int pageSize = resolvePageSize(command);
        boolean dryRun = Boolean.TRUE.equals(command == null ? null : command.getDryRun());
        boolean createMissing = command == null || command.getCreateMissing() == null || Boolean.TRUE.equals(command.getCreateMissing());
        String transferId = command == null ? null : command.getTransferId();
        long scannedCount = 0;
        long matchedCount = 0;
        long createdCount = 0;
        long updatedCount = 0;
        long skippedCount = 0;
        long failedCount = 0;

        if (StringUtils.hasText(transferId)) {
            scannedCount = 1;
            RepairOutcome outcome = repairSingle(transferId.trim(), createMissing, dryRun);
            switch (outcome) {
                case CREATED -> createdCount = 1;
                case UPDATED -> updatedCount = 1;
                case MATCHED -> matchedCount = 1;
                case SKIPPED -> skippedCount = 1;
                case FAILED -> failedCount = 1;
            }
            return buildResult(dryRun, transferId, pageSize, scannedCount, matchedCount, createdCount, updatedCount, skippedCount, failedCount);
        }

        int pageIndex = 0;
        while (true) {
            TransferObjectPage page = transferObjectGateway.pageObjects(null, null, null, null, null, null, null, null, null, null, null, pageIndex, pageSize);
            List<TransferObject> records = page == null || page.records() == null ? List.of() : page.records();
            if (records.isEmpty()) {
                break;
            }
            for (TransferObject transferObject : records) {
                scannedCount++;
                try {
                    RepairOutcome outcome = repairSingle(transferObject, createMissing, dryRun);
                    switch (outcome) {
                        case CREATED -> createdCount++;
                        case UPDATED -> updatedCount++;
                        case MATCHED -> matchedCount++;
                        case SKIPPED -> skippedCount++;
                        case FAILED -> failedCount++;
                    }
                } catch (Exception exception) {
                    failedCount++;
                    log.error("回填文件主数据失败，transferId={}, fingerprint={}",
                            transferObject == null ? null : transferObject.transferId(),
                            transferObject == null ? null : transferObject.fingerprint(),
                            exception);
                }
            }
            if (records.size() < pageSize) {
                break;
            }
            pageIndex++;
        }
        return buildResult(dryRun, transferId, pageSize, scannedCount, matchedCount, createdCount, updatedCount, skippedCount, failedCount);
    }

    private RepairOutcome repairSingle(String transferId, boolean createMissing, boolean dryRun) {
        TransferObject transferObject = transferObjectGateway.findById(transferId).orElse(null);
        return repairSingle(transferObject, createMissing, dryRun);
    }

    private RepairOutcome repairSingle(TransferObject transferObject, boolean createMissing, boolean dryRun) {
        if (transferObject == null) {
            return RepairOutcome.SKIPPED;
        }
        if (!StringUtils.hasText(transferObject.fingerprint())) {
            return RepairOutcome.SKIPPED;
        }
        ValsetFileInfo existing = valsetFileInfoGateway.findByFingerprint(transferObject.fingerprint());
        ValsetFileInfo snapshot = buildSnapshot(existing, transferObject);
        if (snapshot == null) {
            return RepairOutcome.SKIPPED;
        }
        if (existing == null || existing.getFileId() == null) {
            if (!createMissing) {
                return RepairOutcome.SKIPPED;
            }
            if (!dryRun) {
                Long fileId = valsetFileInfoGateway.save(snapshot);
                snapshot.setFileId(fileId);
            }
            return RepairOutcome.CREATED;
        }
        boolean changed = hasMeaningfulDifference(existing, snapshot);
        if (!changed) {
            return RepairOutcome.MATCHED;
        }
        if (!dryRun) {
            valsetFileInfoGateway.updateFromTransferObject(snapshot);
        }
        return RepairOutcome.UPDATED;
    }

    private boolean hasMeaningfulDifference(ValsetFileInfo existing, ValsetFileInfo snapshot) {
        if (existing == null || snapshot == null) {
            return true;
        }
        return !equalsText(existing.getStorageUri(), snapshot.getStorageUri())
                || !equalsText(existing.getLocalTempPath(), snapshot.getLocalTempPath())
                || !equalsText(existing.getRealStoragePath(), snapshot.getRealStoragePath())
                || !equalsText(existing.getFileFormat(), snapshot.getFileFormat())
                || !equalsText(existing.getSourceUri(), snapshot.getSourceUri())
                || !equalsText(existing.getFileNameOriginal(), snapshot.getFileNameOriginal())
                || !equalsText(existing.getFileNameNormalized(), snapshot.getFileNameNormalized())
                || !equalsText(existing.getFileExtension(), snapshot.getFileExtension())
                || !equalsText(existing.getMimeType(), snapshot.getMimeType())
                || !equalsText(existing.getCreatedBy(), snapshot.getCreatedBy())
                || existing.getFileSizeBytes() == null && snapshot.getFileSizeBytes() != null
                || !same(existing.getSourceChannel(), snapshot.getSourceChannel())
                || !same(existing.getStorageType(), snapshot.getStorageType())
                || !same(existing.getFileStatus(), snapshot.getFileStatus())
                || !same(existing.getReceivedAt(), snapshot.getReceivedAt())
                || !same(existing.getStoredAt(), snapshot.getStoredAt())
                || !equalsText(existing.getSourceMetaJson(), snapshot.getSourceMetaJson())
                || !equalsText(existing.getStorageMetaJson(), snapshot.getStorageMetaJson());
    }

    private boolean same(Object left, Object right) {
        return left == null ? right == null : left.equals(right);
    }

    private boolean equalsText(String left, String right) {
        String leftText = normalizeText(left);
        String rightText = normalizeText(right);
        return leftText == null ? rightText == null : leftText.equals(rightText);
    }

    private ValsetFileInfo buildSnapshot(ValsetFileInfo existing, TransferObject transferObject) {
        if (transferObject == null) {
            return existing;
        }
        LocalDateTime receivedAt = toLocalDateTime(transferObject.receivedAt());
        LocalDateTime storedAt = toLocalDateTime(transferObject.storedAt());
        String fileNameOriginal = firstNonBlank(existing == null ? null : existing.getFileNameOriginal(), transferObject.originalName());
        String fileExtension = firstNonBlank(existing == null ? null : existing.getFileExtension(), transferObject.extension());
        String mimeType = firstNonBlank(existing == null ? null : existing.getMimeType(), transferObject.mimeType());
        Long fileSizeBytes = existing == null || existing.getFileSizeBytes() == null ? transferObject.sizeBytes() : existing.getFileSizeBytes();
        String fileFingerprint = firstNonBlank(existing == null ? null : existing.getFileFingerprint(), transferObject.fingerprint());
        ValsetFileSourceChannel sourceChannel = existing == null || existing.getSourceChannel() == null
                ? resolveSourceChannel(transferObject)
                : existing.getSourceChannel();
        String sourceUri = firstNonBlank(existing == null ? null : existing.getSourceUri(), resolveSourceUri(transferObject));
        ValsetFileStorageType storageType = existing == null || existing.getStorageType() == null
                ? resolveStorageType(transferObject)
                : existing.getStorageType();
        String storageUri = firstNonBlank(existing == null ? null : existing.getStorageUri(), resolveStorageUri(transferObject), sourceUri);
        String localTempPath = firstNonBlank(existing == null ? null : existing.getLocalTempPath(), transferObject.localTempPath());
        String realStoragePath = firstNonBlank(existing == null ? null : existing.getRealStoragePath(), transferObject.realStoragePath());
        String fileFormat = firstNonBlank(existing == null ? null : existing.getFileFormat(), resolveFileFormat(transferObject));
        ValsetFileStatus fileStatus = existing == null || existing.getFileStatus() == null ? ValsetFileStatus.READY_FOR_EXTRACT : existing.getFileStatus();
        String createdBy = firstNonBlank(existing == null ? null : existing.getCreatedBy(), BACKFILL_OPERATOR);
        String sourceMetaJson = firstNonBlank(existing == null ? null : existing.getSourceMetaJson(), buildSourceMetaJson(transferObject));
        String storageMetaJson = firstNonBlank(existing == null ? null : existing.getStorageMetaJson(), buildStorageMetaJson(transferObject));
        String remark = existing == null ? null : existing.getRemark();

        return ValsetFileInfo.builder()
                .fileId(existing == null ? null : existing.getFileId())
                .fileNameOriginal(fileNameOriginal)
                .fileNameNormalized(existing != null && StringUtils.hasText(existing.getFileNameNormalized())
                        ? existing.getFileNameNormalized()
                        : normalizeFilename(fileNameOriginal))
                .fileExtension(fileExtension)
                .mimeType(mimeType)
                .fileSizeBytes(fileSizeBytes)
                .fileFingerprint(fileFingerprint)
                .sourceChannel(sourceChannel)
                .sourceUri(sourceUri)
                .storageType(storageType)
                .storageUri(storageUri)
                .localTempPath(localTempPath)
                .realStoragePath(realStoragePath)
                .fileFormat(fileFormat)
                .fileStatus(fileStatus)
                .createdBy(createdBy)
                .receivedAt(existing == null || existing.getReceivedAt() == null ? receivedAt : existing.getReceivedAt())
                .storedAt(existing == null || existing.getStoredAt() == null ? storedAt : existing.getStoredAt())
                .lastProcessedAt(existing == null ? null : existing.getLastProcessedAt())
                .lastTaskId(existing == null ? null : existing.getLastTaskId())
                .errorMessage(existing == null ? null : existing.getErrorMessage())
                .sourceMetaJson(sourceMetaJson)
                .storageMetaJson(storageMetaJson)
                .remark(remark)
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

    private ValsetFileStorageType resolveStorageType(TransferObject transferObject) {
        String storageUri = resolveStorageUri(transferObject);
        if (storageUri == null) {
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

    private String resolveStorageUri(TransferObject transferObject) {
        if (transferObject == null) {
            return null;
        }
        return firstNonBlank(transferObject.realStoragePath(), transferObject.localTempPath(), transferObject.sourceRef());
    }

    private String resolveFileFormat(TransferObject transferObject) {
        if (transferObject == null) {
            return "EXCEL";
        }
        if (StringUtils.hasText(transferObject.extension())) {
            String normalized = transferObject.extension().trim().toLowerCase();
            if (normalized.contains("csv")) {
                return "CSV";
            }
        }
        ProbeResult probeResult = transferObject.probeResult();
        if (probeResult != null && StringUtils.hasText(probeResult.detectedType())) {
            String detectedType = probeResult.detectedType().trim().toUpperCase();
            if (detectedType.contains("CSV")) {
                return "CSV";
            }
            if (detectedType.contains("EXCEL") || detectedType.contains("XLS")) {
                return "EXCEL";
            }
        }
        return "EXCEL";
    }

    private String buildSourceMetaJson(TransferObject transferObject) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("transferId", transferObject == null ? null : transferObject.transferId());
        meta.put("sourceId", transferObject == null ? null : transferObject.sourceId());
        meta.put("sourceType", transferObject == null ? null : transferObject.sourceType());
        meta.put("sourceCode", transferObject == null ? null : transferObject.sourceCode());
        meta.put("sourceRef", transferObject == null ? null : transferObject.sourceRef());
        meta.put("mailId", transferObject == null ? null : transferObject.mailId());
        meta.put("mailFolder", transferObject == null ? null : transferObject.mailFolder());
        meta.put("fingerprint", transferObject == null ? null : transferObject.fingerprint());
        meta.put("probeResult", transferObject == null || transferObject.probeResult() == null ? null : transferObject.probeResult().detectedType());
        try {
            return objectMapper.writeValueAsString(meta);
        } catch (Exception exception) {
            log.warn("构建文件主数据来源元数据失败，transferId={}", transferObject == null ? null : transferObject.transferId(), exception);
            return meta.toString();
        }
    }

    private String buildStorageMetaJson(TransferObject transferObject) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("localTempPath", transferObject == null ? null : transferObject.localTempPath());
        meta.put("realStoragePath", transferObject == null ? null : transferObject.realStoragePath());
        meta.put("routeId", transferObject == null ? null : transferObject.routeId());
        meta.put("status", transferObject == null || transferObject.status() == null ? null : transferObject.status().name());
        meta.put("receivedAt", transferObject == null ? null : transferObject.receivedAt());
        meta.put("storedAt", transferObject == null ? null : transferObject.storedAt());
        try {
            return objectMapper.writeValueAsString(meta);
        } catch (Exception exception) {
            log.warn("构建文件主数据存储元数据失败，transferId={}", transferObject == null ? null : transferObject.transferId(), exception);
            return meta.toString();
        }
    }

    private String normalizeFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "valuation-file";
        }
        return filename.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String firstNonBlank(String... candidates) {
        if (candidates == null) {
            return null;
        }
        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate)) {
                return candidate.trim();
            }
        }
        return null;
    }

    private LocalDateTime toLocalDateTime(java.time.Instant instant) {
        if (instant == null) {
            return null;
        }
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    private int resolvePageSize(ValsetFileInfoRepairCommand command) {
        if (command == null || command.getPageSize() == null || command.getPageSize() <= 0) {
            return 200;
        }
        return Math.min(command.getPageSize(), 1000);
    }

    private ValsetFileInfoRepairResultDTO buildResult(boolean dryRun,
                                                     String transferId,
                                                     int pageSize,
                                                     long scannedCount,
                                                     long matchedCount,
                                                     long createdCount,
                                                     long updatedCount,
                                                     long skippedCount,
                                                     long failedCount) {
        return ValsetFileInfoRepairResultDTO.builder()
                .dryRun(dryRun)
                .transferId(transferId)
                .pageSize(pageSize)
                .scannedCount(scannedCount)
                .matchedCount(matchedCount)
                .createdCount(createdCount)
                .updatedCount(updatedCount)
                .skippedCount(skippedCount)
                .failedCount(failedCount)
                .build();
    }

    private void publishRepairEvent(ParseLifecycleStage stage, TransferObject transferObject, String message) {
        if (parseLifecycleEventPublisher == null || stage == null) {
            return;
        }
        ParseLifecycleEvent.ParseLifecycleEventBuilder builder = ParseLifecycleEvent.builder()
                .stage(stage)
                .source("file-info-repair")
                .message(message);
        if (transferObject != null) {
            builder.transferId(transferObject.transferId())
                    .businessKey(transferObject.fingerprint());
        }
        if (stage == ParseLifecycleStage.QUEUE_FILE_INFO_REPAIR_COMPLETED && transferObject != null) {
            Map<String, Object> attributes = new LinkedHashMap<>();
            if (transferObject.originalName() != null) {
                attributes.put("fileNameOriginal", transferObject.originalName());
            }
            if (transferObject.realStoragePath() != null) {
                attributes.put("storagePath", transferObject.realStoragePath());
            }
            if (transferObject.localTempPath() != null) {
                attributes.put("tempPath", transferObject.localTempPath());
            }
            if (!attributes.isEmpty()) {
                builder.attributes(attributes);
            }
        }
        parseLifecycleEventPublisher.publish(builder.build());
    }

    private void publishRepairEvent(ParseLifecycleStage stage, ValsetFileInfo fileInfo, String message) {
        if (parseLifecycleEventPublisher == null || stage == null) {
            return;
        }
        ParseLifecycleEvent.ParseLifecycleEventBuilder builder = ParseLifecycleEvent.builder()
                .stage(stage)
                .source("file-info-repair")
                .message(message);
        if (fileInfo != null) {
            builder.fileId(fileInfo.getFileId())
                    .businessKey(fileInfo.getFileFingerprint());
        }
        if (stage == ParseLifecycleStage.QUEUE_FILE_INFO_REPAIR_COMPLETED && fileInfo != null) {
            Map<String, Object> attributes = new LinkedHashMap<>();
            if (fileInfo.getFileNameOriginal() != null) {
                attributes.put("fileNameOriginal", fileInfo.getFileNameOriginal());
            }
            if (fileInfo.getStorageUri() != null) {
                attributes.put("storagePath", fileInfo.getStorageUri());
            }
            if (fileInfo.getLocalTempPath() != null) {
                attributes.put("tempPath", fileInfo.getLocalTempPath());
            }
            if (!attributes.isEmpty()) {
                builder.attributes(attributes);
            }
        }
        parseLifecycleEventPublisher.publish(builder.build());
    }

    private enum RepairOutcome {
        CREATED,
        UPDATED,
        MATCHED,
        SKIPPED,
        FAILED
    }
}
