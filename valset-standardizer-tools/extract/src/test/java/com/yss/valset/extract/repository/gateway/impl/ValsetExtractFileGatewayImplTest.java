package com.yss.valset.extract.repository.gateway.impl;

import com.yss.valset.domain.model.ValsetFileInfo;
import com.yss.valset.domain.model.ValsetFileSourceChannel;
import com.yss.valset.domain.model.ValsetFileStatus;
import com.yss.valset.domain.model.ValsetFileStorageType;
import com.yss.valset.transfer.application.impl.query.DefaultTransferObjectQueryService;
import com.yss.valset.transfer.domain.gateway.TransferObjectGateway;
import com.yss.valset.transfer.domain.gateway.TransferObjectTagGateway;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferObjectAnalysis;
import com.yss.valset.transfer.domain.model.TransferObjectPage;
import com.yss.valset.transfer.domain.model.TransferObjectTag;
import com.yss.valset.transfer.domain.model.TransferObjectTrend;
import com.yss.valset.transfer.domain.model.TransferStatus;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ValsetExtractFileGatewayImplTest {

    @Test
    void saveShouldNotAutoCreateValuationTagForGenericFileInfo() {
        RecordingTransferObjectGateway transferObjectGateway = new RecordingTransferObjectGateway();
        RecordingTransferObjectTagGateway transferObjectTagGateway = new RecordingTransferObjectTagGateway();
        ValsetExtractFileGatewayImpl gateway = new ValsetExtractFileGatewayImpl(
                null,
                transferObjectTagGateway,
                transferObjectGateway
        );
        ValsetFileInfo fileInfo = ValsetFileInfo.builder()
                .fileNameOriginal("银华基金项目_数据中台部署手册_V1.0.0.docx")
                .fileNameNormalized("银华基金项目_数据中台部署手册_V1.0.0.docx")
                .fileExtension("docx")
                .mimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .fileSizeBytes(1024L)
                .fileFingerprint("fingerprint-docx")
                .sourceChannel(ValsetFileSourceChannel.OBJECT_STORAGE)
                .sourceUri("/upload/银华基金项目_数据中台部署手册_V1.0.0.docx")
                .storageType(ValsetFileStorageType.LOCAL)
                .storageUri("/upload/银华基金项目_数据中台部署手册_V1.0.0.docx")
                .localTempPath("/upload/银华基金项目_数据中台部署手册_V1.0.0.docx")
                .realStoragePath("/upload/银华基金项目_数据中台部署手册_V1.0.0.docx")
                .fileFormat("DOCX")
                .fileStatus(ValsetFileStatus.READY_FOR_EXTRACT)
                .receivedAt(LocalDateTime.now())
                .storedAt(LocalDateTime.now())
                .build();

        Long fileId = gateway.save(fileInfo);

        assertThat(fileId).isEqualTo(1001L);
        assertThat(fileInfo.getFileId()).isEqualTo(1001L);
        assertThat(transferObjectGateway.saved).isNotNull();
        assertThat(transferObjectTagGateway.savedTags).isEmpty();
    }

    @Test
    void findByFingerprintShouldIgnoreLegacyFileInfoGatewayValuationTag() {
        RecordingTransferObjectGateway transferObjectGateway = new RecordingTransferObjectGateway();
        RecordingTransferObjectTagGateway transferObjectTagGateway = new RecordingTransferObjectTagGateway();
        TransferObject transferObject = transferObject("2001", "fingerprint-legacy");
        transferObjectGateway.byFingerprint.put("fingerprint-legacy", transferObject);
        transferObjectTagGateway.tags.add(new TransferObjectTag(
                null,
                "2001",
                "valuation-tag-id",
                "VALUATION_TABLE",
                "估值表",
                "VALUATION_TABLE",
                "SCRIPT_RULE",
                "文件主数据自动标记为估值表",
                "fileMeta",
                "VALUATION_TABLE",
                Collections.singletonMap("source", "file-info-gateway"),
                Instant.now()
        ));
        ValsetExtractFileGatewayImpl gateway = new ValsetExtractFileGatewayImpl(
                null,
                transferObjectTagGateway,
                transferObjectGateway
        );

        ValsetFileInfo fileInfo = gateway.findByFingerprint("fingerprint-legacy");

        assertThat(fileInfo).isNull();
    }

    @Test
    void findByFingerprintShouldIgnoreGenericFileMetaWithoutValuationTag() {
        RecordingTransferObjectGateway transferObjectGateway = new RecordingTransferObjectGateway();
        RecordingTransferObjectTagGateway transferObjectTagGateway = new RecordingTransferObjectTagGateway();
        transferObjectGateway.byFingerprint.put("fingerprint-meta", transferObject("2002", "fingerprint-meta"));
        ValsetExtractFileGatewayImpl gateway = new ValsetExtractFileGatewayImpl(
                null,
                transferObjectTagGateway,
                transferObjectGateway
        );

        ValsetFileInfo fileInfo = gateway.findByFingerprint("fingerprint-meta");

        assertThat(fileInfo).isNull();
    }

    @Test
    void findByFingerprintShouldReturnValuationFileWhenRecognizedTagExists() {
        RecordingTransferObjectGateway transferObjectGateway = new RecordingTransferObjectGateway();
        RecordingTransferObjectTagGateway transferObjectTagGateway = new RecordingTransferObjectTagGateway();
        transferObjectGateway.byFingerprint.put("fingerprint-valuation", transferObject("2003", "fingerprint-valuation"));
        transferObjectTagGateway.tags.add(new TransferObjectTag(
                null,
                "2003",
                "valuation-tag-id",
                "VALUATION_TABLE",
                "估值表",
                "VALUATION_TABLE",
                "SCRIPT_RULE",
                "同时命中科目代码和科目名称",
                "probeResult",
                "VALUATION_TABLE",
                Collections.singletonMap("source", "tagging-service"),
                Instant.now()
        ));
        ValsetExtractFileGatewayImpl gateway = new ValsetExtractFileGatewayImpl(
                null,
                transferObjectTagGateway,
                transferObjectGateway
        );

        ValsetFileInfo fileInfo = gateway.findByFingerprint("fingerprint-valuation");

        assertThat(fileInfo).isNotNull();
        assertThat(fileInfo.getFileId()).isEqualTo(2003L);
        assertThat(fileInfo.getFileFingerprint()).isEqualTo("fingerprint-valuation");
    }

    private static TransferObject transferObject(String transferId, String fingerprint) {
        Map<String, Object> fileMeta = new LinkedHashMap<>();
        fileMeta.put("fileNameNormalized", "普通文件.docx");
        fileMeta.put("sourceChannel", ValsetFileSourceChannel.OBJECT_STORAGE.name());
        fileMeta.put("sourceUri", "/upload/普通文件.docx");
        fileMeta.put("storageType", ValsetFileStorageType.LOCAL.name());
        fileMeta.put("storageUri", "/upload/普通文件.docx");
        fileMeta.put("localTempPath", "/upload/普通文件.docx");
        fileMeta.put("realStoragePath", "/upload/普通文件.docx");
        fileMeta.put("fileFormat", "DOCX");
        fileMeta.put("fileStatus", ValsetFileStatus.READY_FOR_EXTRACT.name());
        return new TransferObject(
                transferId,
                "source-1",
                "MANUAL",
                "manual",
                "普通文件.docx",
                "docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                1024L,
                fingerprint,
                "/upload/普通文件.docx",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "/upload/普通文件.docx",
                TransferStatus.IDENTIFIED,
                Instant.now(),
                Instant.now(),
                null,
                null,
                null,
                null,
                null,
                null,
                fileMeta,
                "/upload/普通文件.docx"
        );
    }

    private static final class RecordingTransferObjectGateway implements TransferObjectGateway {
        private final Map<String, TransferObject> byFingerprint = new LinkedHashMap<>();
        private TransferObject saved;

        @Override
        public Optional<TransferObject> findById(String transferId) {
            return Optional.empty();
        }

        @Override
        public Optional<TransferObject> findByFingerprint(String fingerprint) {
            return Optional.ofNullable(byFingerprint.get(fingerprint));
        }

        @Override
        public TransferObjectPage pageObjects(String sourceId,
                                              String sourceType,
                                              String sourceCode,
                                              String originalName,
                                              String status,
                                              String deliveryStatus,
                                              String mailId,
                                              String fingerprint,
                                              String routeId,
                                              String tagId,
                                              String tagCode,
                                              String tagValue,
                                              String taskDate,
                                              Integer pageIndex,
                                              Integer pageSize) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TransferObjectAnalysis analyzeObjects(String sourceId,
                                                     String sourceType,
                                                     String sourceCode,
                                                     String originalName,
                                                     String status,
                                                     String deliveryStatus,
                                                     String mailId,
                                                     String fingerprint,
                                                     String routeId,
                                                     String tagId,
                                                     String tagCode,
                                                     String tagValue,
                                                     String taskDate) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<TransferObjectTrend> trendObjects(String taskDate, Integer days) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<TransferObject> listEmailInboxObjects(String sourceCode, String mailId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<TransferObject> listParseQueueCandidates(String sourceId,
                                                             String sourceCode,
                                                             String routeId,
                                                             String status,
                                                             String deliveryStatus,
                                                             Integer limit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<DefaultTransferObjectQueryService.InboxMailGroup> loadMailInboxGroups(String sourceCode,
                                                                                          String mailId,
                                                                                          String deliveryStatus,
                                                                                          Integer offset,
                                                                                          Integer limit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long countMailInboxGroups(String sourceCode, String mailId, String deliveryStatus) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TransferObject save(TransferObject transferObject) {
            this.saved = withTransferId(transferObject, "1001");
            return this.saved;
        }

        private TransferObject withTransferId(TransferObject source, String transferId) {
            return new TransferObject(
                    transferId,
                    source.sourceId(),
                    source.sourceType(),
                    source.sourceCode(),
                    source.originalName(),
                    source.extension(),
                    source.mimeType(),
                    source.sizeBytes(),
                    source.fingerprint(),
                    source.sourceRef(),
                    source.mailId(),
                    source.mailFrom(),
                    source.mailTo(),
                    source.mailCc(),
                    source.mailBcc(),
                    source.mailSubject(),
                    source.mailBody(),
                    source.mailProtocol(),
                    source.mailFolder(),
                    source.localTempPath(),
                    source.status() == null ? TransferStatus.RECEIVED : source.status(),
                    source.receivedAt() == null ? Instant.now() : source.receivedAt(),
                    source.storedAt() == null ? Instant.now() : source.storedAt(),
                    source.businessDate(),
                    source.businessId(),
                    source.receiveDate(),
                    source.routeId(),
                    source.errorMessage(),
                    source.probeResult(),
                    source.fileMeta(),
                    source.realStoragePath()
            );
        }
    }

    private static final class RecordingTransferObjectTagGateway implements TransferObjectTagGateway {
        private final List<TransferObjectTag> tags = new ArrayList<>();
        private final List<TransferObjectTag> savedTags = new ArrayList<>();

        @Override
        public List<TransferObjectTag> listByTransferId(String transferId) {
            List<TransferObjectTag> result = new ArrayList<>();
            for (TransferObjectTag tag : tags) {
                if (tag != null && transferId.equals(tag.transferId())) {
                    result.add(tag);
                }
            }
            return result;
        }

        @Override
        public List<TransferObjectTag> listByTransferIds(List<String> transferIds) {
            return Collections.emptyList();
        }

        @Override
        public List<TransferObjectTag> saveAll(List<TransferObjectTag> tags) {
            if (tags != null) {
                savedTags.addAll(tags);
            }
            return tags;
        }

        @Override
        public void deleteByTransferId(String transferId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<TagSummary> summarizeTags(LocalDateTime startInclusive, LocalDateTime endExclusive) {
            throw new UnsupportedOperationException();
        }
    }
}
