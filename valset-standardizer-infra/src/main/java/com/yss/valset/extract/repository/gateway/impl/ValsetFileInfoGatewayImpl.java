package com.yss.valset.extract.repository.gateway.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yss.valset.domain.gateway.ValsetFileInfoGateway;
import com.yss.valset.domain.model.ValsetFileInfo;
import com.yss.valset.domain.model.ValsetFileSourceChannel;
import com.yss.valset.domain.model.ValsetFileStatus;
import com.yss.valset.domain.model.ValsetFileStorageType;
import com.yss.valset.transfer.domain.gateway.TransferObjectGateway;
import com.yss.valset.transfer.domain.gateway.TransferObjectTagGateway;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferObjectTag;
import com.yss.valset.transfer.domain.model.TransferStatus;
import com.yss.valset.transfer.infrastructure.entity.TransferObjectPO;
import com.yss.valset.transfer.infrastructure.mapper.TransferObjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 文件主数据网关实现。
 *
 * <p>文件主数据不再单独落 {@code t_valset_file_info}，统一写入 {@code t_transfer_object}
 * 以及对应的 {@code t_transfer_object_tag} 记录。</p>
 */
@Repository
@RequiredArgsConstructor
public class ValsetFileInfoGatewayImpl implements ValsetFileInfoGateway {

    private static final String VALUATION_TAG_CODE = "VALUATION_TABLE";
    private static final String VALUATION_TAG_VALUE = "VALUATION_TABLE";
    private static final String VALUATION_TAG_NAME = "估值表";

    private static final String META_FILE_NAME_NORMALIZED = "fileNameNormalized";
    private static final String META_SOURCE_CHANNEL = "sourceChannel";
    private static final String META_SOURCE_URI = "sourceUri";
    private static final String META_STORAGE_TYPE = "storageType";
    private static final String META_STORAGE_URI = "storageUri";
    private static final String META_LOCAL_TEMP_PATH = "localTempPath";
    private static final String META_REAL_STORAGE_PATH = "realStoragePath";
    private static final String META_FILE_FORMAT = "fileFormat";
    private static final String META_FILE_STATUS = "fileStatus";
    private static final String META_CREATED_BY = "createdBy";
    private static final String META_RECEIVED_AT = "receivedAt";
    private static final String META_STORED_AT = "storedAt";
    private static final String META_LAST_PROCESSED_AT = "lastProcessedAt";
    private static final String META_LAST_TASK_ID = "lastTaskId";
    private static final String META_ERROR_MESSAGE = "errorMessage";
    private static final String META_SOURCE_META_JSON = "sourceMetaJson";
    private static final String META_STORAGE_META_JSON = "storageMetaJson";
    private static final String META_REMARK = "remark";

    private final TransferObjectRepository transferObjectRepository;
    private final TransferObjectTagGateway transferObjectTagGateway;
    private final TransferObjectGateway transferObjectGateway;

    @Override
    public Long save(ValsetFileInfo fileInfo) {
        if (fileInfo == null) {
            return null;
        }
        TransferObject existing = resolveExistingTransferObject(fileInfo);
        TransferObject merged = existing == null ? createTransferObject(fileInfo) : mergeTransferObject(existing, fileInfo);
        TransferObject saved = transferObjectGateway.save(merged);
        ensureValuationTag(saved);
        Long fileId = parseLong(saved.transferId());
        fileInfo.setFileId(fileId);
        return fileId;
    }

    @Override
    public ValsetFileInfo findById(Long fileId) {
        if (fileId == null) {
            return null;
        }
        TransferObject transferObject = loadValuationTransferObject(String.valueOf(fileId));
        return transferObject == null ? null : toDomain(transferObject);
    }

    @Override
    public ValsetFileInfo findByPath(String path) {
        if (!StringUtils.hasText(path)) {
            return null;
        }
        String normalizedPath = path.trim();
        List<TransferObjectPO> poList = transferObjectRepository.selectList(
                Wrappers.lambdaQuery(TransferObjectPO.class)
                        .eq(TransferObjectPO::getLocalTempPath, normalizedPath)
                        .or()
                        .eq(TransferObjectPO::getRealStoragePath, normalizedPath)
                        .orderByDesc(TransferObjectPO::getTransferId)
        );
        if (poList == null || poList.isEmpty()) {
            return null;
        }
        for (TransferObjectPO po : poList) {
            TransferObject transferObject = loadValuationTransferObject(String.valueOf(po.getTransferId()));
            if (transferObject != null) {
                return toDomain(transferObject);
            }
        }
        return null;
    }

    @Override
    public ValsetFileInfo findByFingerprint(String fileFingerprint) {
        if (!StringUtils.hasText(fileFingerprint)) {
            return null;
        }
        TransferObject transferObject = transferObjectGateway.findByFingerprint(fileFingerprint.trim()).orElse(null);
        if (transferObject == null || !isValuationTransferObject(transferObject)) {
            return null;
        }
        return toDomain(transferObject);
    }

    @Override
    public List<ValsetFileInfo> search(ValsetFileSourceChannel sourceChannel,
                                       ValsetFileStatus fileStatus,
                                       String fileFingerprint) {
        var query = Wrappers.lambdaQuery(TransferObjectPO.class)
                .inSql(TransferObjectPO::getTransferId, buildValuationTagSql())
                .eq(StringUtils.hasText(fileFingerprint), TransferObjectPO::getFingerprint, trimToNull(fileFingerprint))
                .orderByDesc(TransferObjectPO::getReceivedAt)
                .orderByDesc(TransferObjectPO::getTransferId);
        List<TransferObjectPO> poList = transferObjectRepository.selectList(query);
        if (poList == null || poList.isEmpty()) {
            return List.of();
        }
        List<ValsetFileInfo> results = new ArrayList<>();
        for (TransferObjectPO po : poList) {
            TransferObject transferObject = loadValuationTransferObject(String.valueOf(po.getTransferId()));
            if (transferObject == null) {
                continue;
            }
            ValsetFileInfo fileInfo = toDomain(transferObject);
            if (matchesSearchFilters(fileInfo, sourceChannel, fileStatus, fileFingerprint)) {
                results.add(fileInfo);
            }
        }
        return results;
    }

    @Override
    public void updateStatus(Long fileId,
                             ValsetFileStatus fileStatus,
                             Long lastTaskId,
                             LocalDateTime lastProcessedAt,
                             String errorMessage) {
        if (fileId == null) {
            return;
        }
        TransferObject transferObject = transferObjectGateway.findById(String.valueOf(fileId)).orElse(null);
        if (transferObject == null) {
            return;
        }
        Map<String, Object> fileMeta = mergeFileMeta(transferObject.fileMeta(), null);
        putIfHasText(fileMeta, META_FILE_STATUS, fileStatus == null ? null : fileStatus.name());
        putIfNotNull(fileMeta, META_LAST_TASK_ID, lastTaskId);
        putIfNotNull(fileMeta, META_LAST_PROCESSED_AT, lastProcessedAt);
        putIfHasText(fileMeta, META_ERROR_MESSAGE, errorMessage);
        TransferObject updated = transferObject
                .withFileMeta(fileMeta)
                .withStatus(transferObject.status(), errorMessage);
        transferObjectGateway.save(updated);
    }

    @Override
    public void updatePaths(Long fileId,
                            String storageUri,
                            String localTempPath,
                            String realStoragePath) {
        if (fileId == null) {
            return;
        }
        TransferObject transferObject = transferObjectGateway.findById(String.valueOf(fileId)).orElse(null);
        if (transferObject == null) {
            return;
        }
        Map<String, Object> fileMeta = mergeFileMeta(transferObject.fileMeta(), null);
        putIfHasText(fileMeta, META_STORAGE_URI, storageUri);
        putIfHasText(fileMeta, META_LOCAL_TEMP_PATH, localTempPath);
        putIfHasText(fileMeta, META_REAL_STORAGE_PATH, realStoragePath);
        TransferObject updated = transferObject
                .withLocalTempPath(firstNonBlank(localTempPath, transferObject.localTempPath()))
                .withRealStoragePath(firstNonBlank(realStoragePath, transferObject.realStoragePath()))
                .withFileMeta(fileMeta);
        transferObjectGateway.save(updated);
    }

    @Override
    public void updateFromTransferObject(ValsetFileInfo fileInfo) {
        if (fileInfo == null || fileInfo.getFileId() == null) {
            return;
        }
        TransferObject transferObject = transferObjectGateway.findById(String.valueOf(fileInfo.getFileId())).orElse(null);
        if (transferObject == null) {
            return;
        }
        transferObjectGateway.save(mergeTransferObject(transferObject, fileInfo));
    }

    private TransferObject resolveExistingTransferObject(ValsetFileInfo fileInfo) {
        if (fileInfo.getFileId() != null) {
            TransferObject byId = transferObjectGateway.findById(String.valueOf(fileInfo.getFileId())).orElse(null);
            if (byId != null) {
                return byId;
            }
        }
        if (StringUtils.hasText(fileInfo.getFileFingerprint())) {
            return transferObjectGateway.findByFingerprint(fileInfo.getFileFingerprint().trim()).orElse(null);
        }
        return null;
    }

    private TransferObject loadValuationTransferObject(String transferId) {
        TransferObject transferObject = transferObjectGateway.findById(transferId).orElse(null);
        return transferObject != null && isValuationTransferObject(transferObject) ? transferObject : null;
    }

    private boolean isValuationTransferObject(TransferObject transferObject) {
        if (transferObject == null || !StringUtils.hasText(transferObject.transferId())) {
            return false;
        }
        List<TransferObjectTag> tags = transferObjectTagGateway.listByTransferId(transferObject.transferId());
        for (TransferObjectTag tag : tags) {
            if (tag == null) {
                continue;
            }
            if (matchesValuationKey(tag.tagCode()) || matchesValuationKey(tag.tagName()) || matchesValuationKey(tag.tagValue())) {
                return true;
            }
        }
        return hasValuationMeta(transferObject.fileMeta());
    }

    private boolean hasValuationMeta(Map<String, Object> meta) {
        return meta != null && (
                meta.containsKey(META_FILE_STATUS)
                        || meta.containsKey(META_FILE_FORMAT)
                        || meta.containsKey(META_SOURCE_META_JSON)
                        || meta.containsKey(META_STORAGE_META_JSON)
        );
    }

    private boolean matchesSearchFilters(ValsetFileInfo fileInfo,
                                         ValsetFileSourceChannel sourceChannel,
                                         ValsetFileStatus fileStatus,
                                         String fileFingerprint) {
        if (fileInfo == null) {
            return false;
        }
        if (sourceChannel != null && sourceChannel != fileInfo.getSourceChannel()) {
            return false;
        }
        if (fileStatus != null && fileStatus != fileInfo.getFileStatus()) {
            return false;
        }
        if (StringUtils.hasText(fileFingerprint) && !fileFingerprint.trim().equals(fileInfo.getFileFingerprint())) {
            return false;
        }
        return true;
    }

    private TransferObject createTransferObject(ValsetFileInfo fileInfo) {
        Map<String, Object> fileMeta = mergeFileMeta(null, fileInfo);
        LocalDateTime receivedAt = fileInfo.getReceivedAt() == null ? LocalDateTime.now() : fileInfo.getReceivedAt();
        LocalDateTime storedAt = fileInfo.getStoredAt() == null ? receivedAt : fileInfo.getStoredAt();
        return new TransferObject(
                null,
                null,
                resolveSourceType(fileInfo),
                resolveSourceCode(fileInfo),
                fileInfo.getFileNameOriginal(),
                fileInfo.getFileExtension(),
                fileInfo.getMimeType(),
                fileInfo.getFileSizeBytes(),
                fileInfo.getFileFingerprint(),
                fileInfo.getSourceUri(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                fileInfo.getLocalTempPath(),
                resolveTransferStatus(fileInfo),
                toInstant(receivedAt),
                toInstant(storedAt),
                null,
                fileInfo.getErrorMessage(),
                null,
                fileMeta
        ).withRealStoragePath(fileInfo.getRealStoragePath());
    }

    private TransferObject mergeTransferObject(TransferObject existing, ValsetFileInfo fileInfo) {
        if (existing == null) {
            return createTransferObject(fileInfo);
        }
        Map<String, Object> mergedMeta = mergeFileMeta(existing.fileMeta(), fileInfo);
        return new TransferObject(
                existing.transferId(),
                existing.sourceId(),
                firstNonBlank(existing.sourceType(), resolveSourceType(fileInfo)),
                firstNonBlank(existing.sourceCode(), resolveSourceCode(fileInfo)),
                firstNonBlank(fileInfo.getFileNameOriginal(), existing.originalName()),
                firstNonBlank(fileInfo.getFileExtension(), existing.extension()),
                firstNonBlank(fileInfo.getMimeType(), existing.mimeType()),
                fileInfo.getFileSizeBytes() != null ? fileInfo.getFileSizeBytes() : existing.sizeBytes(),
                firstNonBlank(fileInfo.getFileFingerprint(), existing.fingerprint()),
                firstNonBlank(fileInfo.getSourceUri(), existing.sourceRef()),
                existing.mailId(),
                existing.mailFrom(),
                existing.mailTo(),
                existing.mailCc(),
                existing.mailBcc(),
                existing.mailSubject(),
                existing.mailBody(),
                existing.mailProtocol(),
                existing.mailFolder(),
                firstNonBlank(fileInfo.getLocalTempPath(), existing.localTempPath()),
                existing.status(),
                fileInfo.getReceivedAt() == null ? existing.receivedAt() : toInstant(fileInfo.getReceivedAt()),
                fileInfo.getStoredAt() == null ? existing.storedAt() : toInstant(fileInfo.getStoredAt()),
                existing.routeId(),
                firstNonBlank(fileInfo.getErrorMessage(), existing.errorMessage()),
                existing.probeResult(),
                mergedMeta
        )
                .withBusinessFields(existing.businessDate(), existing.businessId(), existing.receiveDate())
                .withRealStoragePath(firstNonBlank(fileInfo.getRealStoragePath(), existing.realStoragePath()));
    }

    private void ensureValuationTag(TransferObject transferObject) {
        if (transferObject == null || !StringUtils.hasText(transferObject.transferId())) {
            return;
        }
        List<TransferObjectTag> tags = transferObjectTagGateway.listByTransferId(transferObject.transferId());
        boolean exists = tags.stream().anyMatch(tag ->
                tag != null && (matchesValuationKey(tag.tagCode()) || matchesValuationKey(tag.tagName()) || matchesValuationKey(tag.tagValue())));
        if (exists) {
            return;
        }
        TransferObjectTag valuationTag = new TransferObjectTag(
                null,
                transferObject.transferId(),
                null,
                VALUATION_TAG_CODE,
                VALUATION_TAG_NAME,
                VALUATION_TAG_VALUE,
                "SCRIPT_RULE",
                "文件主数据自动标记为估值表",
                "fileMeta",
                VALUATION_TAG_VALUE,
                Map.of("source", "file-info-gateway"),
                Instant.now()
        );
        transferObjectTagGateway.saveAll(List.of(valuationTag));
    }

    private Map<String, Object> mergeFileMeta(Map<String, Object> existingMeta, ValsetFileInfo fileInfo) {
        Map<String, Object> meta = existingMeta == null ? new LinkedHashMap<>() : new LinkedHashMap<>(existingMeta);
        if (fileInfo == null) {
            return meta;
        }
        putIfHasText(meta, META_FILE_NAME_NORMALIZED, fileInfo.getFileNameNormalized());
        putIfHasText(meta, META_SOURCE_CHANNEL, fileInfo.getSourceChannel() == null ? null : fileInfo.getSourceChannel().name());
        putIfHasText(meta, META_SOURCE_URI, fileInfo.getSourceUri());
        putIfHasText(meta, META_STORAGE_TYPE, fileInfo.getStorageType() == null ? null : fileInfo.getStorageType().name());
        putIfHasText(meta, META_STORAGE_URI, fileInfo.getStorageUri());
        putIfHasText(meta, META_LOCAL_TEMP_PATH, fileInfo.getLocalTempPath());
        putIfHasText(meta, META_REAL_STORAGE_PATH, fileInfo.getRealStoragePath());
        putIfHasText(meta, META_FILE_FORMAT, fileInfo.getFileFormat());
        putIfHasText(meta, META_FILE_STATUS, fileInfo.getFileStatus() == null ? null : fileInfo.getFileStatus().name());
        putIfHasText(meta, META_CREATED_BY, fileInfo.getCreatedBy());
        putIfNotNull(meta, META_RECEIVED_AT, fileInfo.getReceivedAt());
        putIfNotNull(meta, META_STORED_AT, fileInfo.getStoredAt());
        putIfNotNull(meta, META_LAST_PROCESSED_AT, fileInfo.getLastProcessedAt());
        putIfNotNull(meta, META_LAST_TASK_ID, fileInfo.getLastTaskId());
        putIfHasText(meta, META_ERROR_MESSAGE, fileInfo.getErrorMessage());
        putIfHasText(meta, META_SOURCE_META_JSON, fileInfo.getSourceMetaJson());
        putIfHasText(meta, META_STORAGE_META_JSON, fileInfo.getStorageMetaJson());
        putIfHasText(meta, META_REMARK, fileInfo.getRemark());
        return meta;
    }

    private ValsetFileInfo toDomain(TransferObject transferObject) {
        Map<String, Object> meta = transferObject.fileMeta() == null ? Map.of() : transferObject.fileMeta();
        return ValsetFileInfo.builder()
                .fileId(parseLong(transferObject.transferId()))
                .fileNameOriginal(transferObject.originalName())
                .fileNameNormalized(firstNonBlank(stringValue(meta, META_FILE_NAME_NORMALIZED), normalizeFilename(transferObject.originalName())))
                .fileExtension(transferObject.extension())
                .mimeType(transferObject.mimeType())
                .fileSizeBytes(transferObject.sizeBytes())
                .fileFingerprint(transferObject.fingerprint())
                .sourceChannel(resolveSourceChannel(transferObject, meta))
                .sourceUri(firstNonBlank(stringValue(meta, META_SOURCE_URI), transferObject.sourceRef()))
                .storageType(resolveStorageType(transferObject, meta))
                .storageUri(firstNonBlank(stringValue(meta, META_STORAGE_URI), transferObject.realStoragePath(), transferObject.localTempPath()))
                .localTempPath(firstNonBlank(stringValue(meta, META_LOCAL_TEMP_PATH), transferObject.localTempPath()))
                .realStoragePath(firstNonBlank(stringValue(meta, META_REAL_STORAGE_PATH), transferObject.realStoragePath()))
                .fileFormat(firstNonBlank(stringValue(meta, META_FILE_FORMAT), resolveFileFormat(transferObject)))
                .fileStatus(resolveFileStatus(transferObject, meta))
                .createdBy(firstNonBlank(stringValue(meta, META_CREATED_BY)))
                .receivedAt(firstNonNull(toLocalDateTime(transferObject.receivedAt()), parseLocalDateTime(stringValue(meta, META_RECEIVED_AT))))
                .storedAt(firstNonNull(toLocalDateTime(transferObject.storedAt()), parseLocalDateTime(stringValue(meta, META_STORED_AT))))
                .lastProcessedAt(parseLocalDateTime(stringValue(meta, META_LAST_PROCESSED_AT)))
                .lastTaskId(parseLong(stringValue(meta, META_LAST_TASK_ID)))
                .errorMessage(firstNonBlank(stringValue(meta, META_ERROR_MESSAGE), transferObject.errorMessage()))
                .sourceMetaJson(firstNonBlank(stringValue(meta, META_SOURCE_META_JSON)))
                .storageMetaJson(firstNonBlank(stringValue(meta, META_STORAGE_META_JSON)))
                .remark(firstNonBlank(stringValue(meta, META_REMARK)))
                .build();
    }

    private ValsetFileSourceChannel resolveSourceChannel(TransferObject transferObject, Map<String, Object> meta) {
        String value = firstNonBlank(stringValue(meta, META_SOURCE_CHANNEL));
        if (StringUtils.hasText(value)) {
            try {
                return ValsetFileSourceChannel.valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // fallback
            }
        }
        String sourceType = transferObject == null ? null : transferObject.sourceType();
        if ("EMAIL".equalsIgnoreCase(sourceType)) {
            return ValsetFileSourceChannel.EMAIL_ATTACHMENT;
        }
        if ("MANUAL_UPLOAD".equalsIgnoreCase(sourceType)) {
            return ValsetFileSourceChannel.MANUAL_UPLOAD;
        }
        return ValsetFileSourceChannel.OBJECT_STORAGE;
    }

    private ValsetFileStorageType resolveStorageType(TransferObject transferObject, Map<String, Object> meta) {
        String value = firstNonBlank(stringValue(meta, META_STORAGE_TYPE));
        if (StringUtils.hasText(value)) {
            try {
                return ValsetFileStorageType.valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // fallback
            }
        }
        String storageUri = firstNonBlank(stringValue(meta, META_STORAGE_URI), transferObject == null ? null : transferObject.realStoragePath(), transferObject == null ? null : transferObject.localTempPath());
        if (!StringUtils.hasText(storageUri)) {
            return ValsetFileStorageType.LOCAL;
        }
        String normalized = storageUri.trim().toLowerCase(Locale.ROOT);
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

    private ValsetFileStatus resolveFileStatus(TransferObject transferObject, Map<String, Object> meta) {
        String value = firstNonBlank(stringValue(meta, META_FILE_STATUS));
        if (StringUtils.hasText(value)) {
            try {
                return ValsetFileStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // fallback
            }
        }
        TransferStatus transferStatus = transferObject == null ? null : transferObject.status();
        if (transferStatus == null) {
            return ValsetFileStatus.READY_FOR_EXTRACT;
        }
        return switch (transferStatus) {
            case FAILED, QUARANTINED -> ValsetFileStatus.FAILED;
            case ARCHIVED -> ValsetFileStatus.ARCHIVED;
            case DELIVERED -> ValsetFileStatus.STORED;
            default -> ValsetFileStatus.READY_FOR_EXTRACT;
        };
    }

    private TransferStatus resolveTransferStatus(ValsetFileInfo fileInfo) {
        if (fileInfo == null || fileInfo.getFileStatus() == null) {
            return TransferStatus.RECEIVED;
        }
        return switch (fileInfo.getFileStatus()) {
            case FAILED -> TransferStatus.FAILED;
            case ARCHIVED -> TransferStatus.ARCHIVED;
            case READY_FOR_EXTRACT, STORED, EXTRACTED, PARSED, MATCHED -> TransferStatus.RECEIVED;
            default -> TransferStatus.RECEIVED;
        };
    }

    private String resolveSourceType(ValsetFileInfo fileInfo) {
        if (fileInfo == null || fileInfo.getSourceChannel() == null) {
            return "MANUAL_UPLOAD";
        }
        return switch (fileInfo.getSourceChannel()) {
            case EMAIL_ATTACHMENT -> "EMAIL";
            case MANUAL_UPLOAD -> "MANUAL_UPLOAD";
            case OBJECT_STORAGE -> "OBJECT_STORAGE";
        };
    }

    private String resolveSourceCode(ValsetFileInfo fileInfo) {
        if (fileInfo == null) {
            return null;
        }
        if (StringUtils.hasText(fileInfo.getCreatedBy())) {
            return fileInfo.getCreatedBy().trim();
        }
        if (StringUtils.hasText(fileInfo.getSourceUri())) {
            return fileInfo.getSourceUri().trim();
        }
        return resolveSourceType(fileInfo);
    }

    private String resolveFileFormat(TransferObject transferObject) {
        if (transferObject == null) {
            return "EXCEL";
        }
        String extension = transferObject.extension();
        String mimeType = transferObject.mimeType();
        String normalizedExtension = extension == null ? null : extension.trim().toLowerCase(Locale.ROOT);
        String normalizedMimeType = mimeType == null ? null : mimeType.trim().toLowerCase(Locale.ROOT);
        if ((normalizedExtension != null && normalizedExtension.contains("csv"))
                || (normalizedMimeType != null && normalizedMimeType.contains("csv"))) {
            return "CSV";
        }
        return "EXCEL";
    }

    private boolean matchesValuationKey(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return VALUATION_TAG_CODE.equals(normalized) || VALUATION_TAG_NAME.equals(normalized);
    }

    private String normalizeFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "valuation-file";
        }
        return filename.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String buildValuationTagSql() {
        return "select transfer_id from t_transfer_object_tag where tag_code = '"
                + escapeSql(VALUATION_TAG_CODE)
                + "' or tag_value = '"
                + escapeSql(VALUATION_TAG_VALUE)
                + "' or tag_name = '"
                + escapeSql(VALUATION_TAG_NAME)
                + "'";
    }

    private String stringValue(Map<String, Object> source, String key) {
        if (source == null) {
            return null;
        }
        Object value = source.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private void putIfHasText(Map<String, Object> target, String key, String value) {
        if (target != null && StringUtils.hasText(value)) {
            target.put(key, value.trim());
        }
    }

    private void putIfNotNull(Map<String, Object> target, String key, Object value) {
        if (target != null && value != null) {
            target.put(key, value);
        }
    }

    private Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String escapeSql(String value) {
        return value == null ? null : value.replace("'", "''");
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    private Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.atZone(ZoneId.systemDefault()).toInstant();
    }

    private LocalDateTime parseLocalDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim());
        } catch (Exception ignored) {
            return null;
        }
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

    private <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }
}
