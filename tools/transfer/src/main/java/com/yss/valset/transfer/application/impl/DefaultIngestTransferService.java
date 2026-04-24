package com.yss.valset.transfer.application.impl;

import com.yss.valset.transfer.application.command.IngestTransferSourceCommand;
import com.yss.valset.transfer.application.port.IngestTransferUseCase;
import com.yss.valset.transfer.domain.gateway.TransferRunLogGateway;
import com.yss.valset.transfer.domain.gateway.TransferSourceCheckpointGateway;
import com.yss.valset.transfer.domain.gateway.TransferSourceGateway;
import com.yss.valset.transfer.domain.gateway.TransferObjectGateway;
import com.yss.valset.transfer.domain.model.ProbeResult;
import com.yss.valset.transfer.domain.model.RecognitionContext;
import com.yss.valset.transfer.domain.model.TransferRunLog;
import com.yss.valset.transfer.domain.model.TransferSourceCheckpoint;
import com.yss.valset.transfer.domain.model.TransferSourceCheckpointItem;
import com.yss.valset.transfer.domain.model.TransferRunStage;
import com.yss.valset.transfer.domain.model.TransferRunStatus;
import com.yss.valset.transfer.domain.model.TransferTriggerType;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferSource;
import com.yss.valset.transfer.domain.model.TransferStatus;
import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.domain.model.config.TransferConfigKeys;
import com.yss.valset.transfer.application.port.TransferJobScheduler;
import com.yss.valset.transfer.infrastructure.connector.SourceConnectorRegistry;
import com.yss.valset.transfer.infrastructure.plugin.FileProbePluginRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Instant;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 默认文件收取应用服务。
 */
@Service
public class DefaultIngestTransferService implements IngestTransferUseCase {

    private static final Logger log = LoggerFactory.getLogger(DefaultIngestTransferService.class);
    private static final Duration INGEST_WARN_THRESHOLD = Duration.ofMinutes(10);

    private final TransferSourceGateway transferSourceGateway;
    private final SourceConnectorRegistry sourceConnectorRegistry;
    private final TransferObjectGateway transferObjectGateway;
    private final TransferRunLogGateway transferRunLogGateway;
    private final TransferSourceCheckpointGateway transferSourceCheckpointGateway;
    private final ObjectProvider<TransferJobScheduler> transferJobSchedulerProvider;
    private final FileProbePluginRegistry fileProbePluginRegistry;
    private final String uploadRoot;

    public DefaultIngestTransferService(TransferSourceGateway transferSourceGateway,
                                        SourceConnectorRegistry sourceConnectorRegistry,
                                        TransferObjectGateway transferObjectGateway,
                                        TransferRunLogGateway transferRunLogGateway,
                                        TransferSourceCheckpointGateway transferSourceCheckpointGateway,
                                        ObjectProvider<TransferJobScheduler> transferJobSchedulerProvider,
                                        FileProbePluginRegistry fileProbePluginRegistry,
                                        @Value("${subject.match.upload-dir:${user.home}/.tmp/valset-standardizer/uploads}") String uploadRoot) {
        this.transferSourceGateway = transferSourceGateway;
        this.sourceConnectorRegistry = sourceConnectorRegistry;
        this.transferObjectGateway = transferObjectGateway;
        this.transferRunLogGateway = transferRunLogGateway;
        this.transferSourceCheckpointGateway = transferSourceCheckpointGateway;
        this.transferJobSchedulerProvider = transferJobSchedulerProvider;
        this.fileProbePluginRegistry = fileProbePluginRegistry;
        this.uploadRoot = resolveUploadRoot(uploadRoot);
    }

    @Override
    public void execute(IngestTransferSourceCommand command) {
        String triggerType = normalizeTriggerType(command.triggerType());
        IngestExecutionContext executionContext = acquireIngestExecutionContext(command, triggerType);
        if (executionContext == null) {
            return;
        }
        TransferSource source = executionContext.source();
        saveRunLog(
                source,
                null,
                null,
                triggerType,
                TransferRunStage.INGEST.name(),
                TransferRunStatus.SUCCESS.name(),
                "开始收取来源，sourceId=" + source.sourceId()
                        + "，sourceCode=" + source.sourceCode()
                        + "，sourceType=" + (source.sourceType() == null ? null : source.sourceType().name())
                        + "，triggerType=" + triggerType,
                null
        );
        if (isStopRequested(source)) {
            saveRunLog(
                    source,
                    null,
                    null,
                    triggerType,
                    TransferRunStage.INGEST.name(),
                    TransferRunStatus.SUCCESS.name(),
                    "来源已收到停止请求，本轮收取未继续执行",
                    null
            );
            return;
        }
        var connector = sourceConnectorRegistry.getRequired(source);
        boolean routeFailureLogged = false;
        boolean stopRequested = false;
        try {
            List<RecognitionContext> contexts = connector.fetch(source);
            saveRunLog(
                    source,
                    null,
                    null,
                    triggerType,
                    TransferRunStage.INGEST.name(),
                    TransferRunStatus.SUCCESS.name(),
                    "来源收取完成，候选记录数=" + contexts.size()
                            + "，sourceId=" + source.sourceId()
                            + "，sourceCode=" + source.sourceCode(),
                    null
            );
            TransferSource latestSource = refreshSource(source.sourceId()).orElse(source);
            if (isStopRequested(latestSource)) {
                stopRequested = true;
                source = latestSource;
            }
            int createdCount = 0;
            int duplicateCount = 0;
            Set<String> processedCheckpointKeys = new LinkedHashSet<>();
            for (RecognitionContext context : contexts) {
                latestSource = refreshSource(source.sourceId()).orElse(source);
                if (isStopRequested(latestSource)) {
                    stopRequested = true;
                    source = latestSource;
                    break;
                }
                String fingerprint = fingerprint(context);
                var existingTransfer = transferObjectGateway.findByFingerprint(fingerprint);
                if (existingTransfer.isPresent()) {
                    duplicateCount++;
                    saveRunLog(
                            source,
                            existingTransfer.get().transferId(),
                            null,
                            triggerType,
                            TransferRunStage.INGEST.name(),
                            TransferRunStatus.SUCCESS.name(),
                            "重复文件已跳过，fingerprint=" + fingerprint,
                            null
                    );
                    continue;
                }
                log.info("来源候选文件准备分析，sourceId={}，sourceCode={}，fileName={}，path={}，fingerprint={}",
                        source.sourceId(),
                        source.sourceCode(),
                        context.fileName(),
                        context.path(),
                        fingerprint);
                Map<String, Object> fileMeta = new LinkedHashMap<>();
                if (context.attributes() != null) {
                    fileMeta.putAll(context.attributes());
                }
                fileMeta.putIfAbsent(TransferConfigKeys.CONTENT_FINGERPRINT, fingerprint);
                fileMeta.putIfAbsent(TransferConfigKeys.SOURCE_TYPE, source.sourceType() == null ? null : source.sourceType().name());
                fileMeta.putIfAbsent(TransferConfigKeys.SOURCE_CODE, source.sourceCode());
                fileMeta.putIfAbsent(TransferConfigKeys.TRIGGER_TYPE, triggerType);
                fileMeta.putIfAbsent(TransferConfigKeys.SOURCE_REF, buildSourceRef(source, context));
                fileMeta.putIfAbsent(TransferConfigKeys.MAIL_ID, context.mailId());
                fileMeta.putIfAbsent(TransferConfigKeys.MAIL_FROM, context.sender());
                fileMeta.putIfAbsent(TransferConfigKeys.MAIL_TO, context.recipientsTo());
                fileMeta.putIfAbsent(TransferConfigKeys.MAIL_CC, context.recipientsCc());
                fileMeta.putIfAbsent(TransferConfigKeys.MAIL_BCC, context.recipientsBcc());
                fileMeta.putIfAbsent(TransferConfigKeys.MAIL_SUBJECT, context.subject());
                fileMeta.putIfAbsent(TransferConfigKeys.MAIL_BODY, context.body());
                fileMeta.putIfAbsent(TransferConfigKeys.MAIL_PROTOCOL, context.mailProtocol());
                fileMeta.putIfAbsent(TransferConfigKeys.MAIL_FOLDER, context.mailFolder());
                RecognitionContext analysisContext = new RecognitionContext(
                        context.sourceType(),
                        context.sourceCode(),
                        context.fileName(),
                        context.mimeType(),
                        context.fileSize(),
                        context.sender(),
                        context.recipientsTo(),
                        context.recipientsCc(),
                        context.recipientsBcc(),
                        context.subject(),
                        context.body(),
                        context.mailId(),
                        context.mailProtocol(),
                        context.mailFolder(),
                        context.path(),
                        fileMeta
                );
                ProbeResult probeResult = fileProbePluginRegistry.getRequired(analysisContext).probe(analysisContext);
                TransferObject draftTransferObject = new TransferObject(
                        null,
                        source.sourceId(),
                        source.sourceType() == null ? null : source.sourceType().name(),
                        source.sourceCode(),
                        context.fileName(),
                        extensionOf(context.fileName()),
                        context.mimeType(),
                        fileSizeOf(context),
                        fingerprint,
                        buildSourceRef(source, context),
                        context.mailId(),
                        context.sender(),
                        context.recipientsTo(),
                        context.recipientsCc(),
                        context.recipientsBcc(),
                        context.subject(),
                        context.body(),
                        context.mailProtocol(),
                        context.mailFolder(),
                        null,
                        TransferStatus.RECEIVED,
                        Instant.now(),
                        Instant.now(),
                        null,
                        null,
                        probeResult,
                        fileMeta
                );
                Path storedPath = storeReceivedFile(source, analysisContext, draftTransferObject);
                TransferObject transferObject = draftTransferObject.withLocalTempPath(storedPath == null ? null : storedPath.toAbsolutePath().toString());
                TransferObject savedTransfer = transferObjectGateway.save(transferObject);
                recordCheckpointItemIfNeeded(source, context, triggerType, processedCheckpointKeys);
                recordCheckpointCursorIfNeeded(source, context, triggerType);
                createdCount++;
                saveRunLog(
                        source,
                        savedTransfer.transferId(),
                        null,
                        triggerType,
                        TransferRunStage.INGEST.name(),
                        TransferRunStatus.SUCCESS.name(),
                        source.sourceType() == null || source.sourceType() != com.yss.valset.transfer.domain.model.SourceType.EMAIL
                                ? "收取文件成功，文件名=" + savedTransfer.originalName()
                                : "收取邮件元数据成功，附件名=" + savedTransfer.originalName(),
                        null
                );
                if (source.sourceType() == com.yss.valset.transfer.domain.model.SourceType.EMAIL) {
                    log.info("邮件附件已直转存到统一临时目录，准备触发路由，transferId={}，sourceId={}，sourceCode={}，originalName={}，localTempPath={}",
                            savedTransfer.transferId(),
                            source.sourceId(),
                            source.sourceCode(),
                            savedTransfer.originalName(),
                            savedTransfer.localTempPath());
                } else {
                    log.info("来源文件已转存到统一临时目录，准备触发路由，transferId={}，sourceId={}，sourceCode={}，originalName={}，localTempPath={}",
                            savedTransfer.transferId(),
                            source.sourceId(),
                            source.sourceCode(),
                            savedTransfer.originalName(),
                            savedTransfer.localTempPath());
                }
                try {
                    transferJobSchedulerProvider.getObject().triggerRoute(savedTransfer.transferId());
                } catch (Exception exception) {
                    routeFailureLogged = true;
                    saveRunLog(
                            source,
                            savedTransfer.transferId(),
                            null,
                            triggerType,
                            TransferRunStage.ROUTE.name(),
                            TransferRunStatus.FAILED.name(),
                            "自动触发路由失败，transferId=" + savedTransfer.transferId(),
                            exception
                    );
                    throw exception;
                }
            }
            if (stopRequested) {
                saveRunLog(
                        source,
                        null,
                        null,
                        triggerType,
                        TransferRunStage.INGEST.name(),
                        TransferRunStatus.SUCCESS.name(),
                        "来源收取已停止，已提前结束，文件数=" + createdCount + "，重复文件数=" + duplicateCount,
                        null
                );
                return;
            }
            if (contexts.isEmpty()) {
                saveRunLog(
                        source,
                        null,
                        null,
                        triggerType,
                        TransferRunStage.INGEST.name(),
                        TransferRunStatus.SUCCESS.name(),
                        "收取完成，未发现新文件",
                        null
                );
                return;
            }
            if (createdCount == 0 && duplicateCount > 0) {
                saveRunLog(
                        source,
                        null,
                        null,
                        triggerType,
                        TransferRunStage.INGEST.name(),
                        TransferRunStatus.SUCCESS.name(),
                        "收取完成，全部文件均已存在，重复文件数=" + duplicateCount,
                        null
                );
            } else {
                saveRunLog(
                        source,
                        null,
                        null,
                        triggerType,
                        TransferRunStage.INGEST.name(),
                        TransferRunStatus.SUCCESS.name(),
                        "收取完成，新文件数=" + createdCount + "，重复文件数=" + duplicateCount,
                        null
                );
            }
        } catch (Exception exception) {
            if (!routeFailureLogged) {
                saveRunLog(
                        source,
                        null,
                        null,
                        triggerType,
                        TransferRunStage.INGEST.name(),
                        TransferRunStatus.FAILED.name(),
                        "来源收取失败",
                        exception
                );
            }
            throw exception;
        } finally {
            releaseIngestLock(executionContext);
        }
    }

    private IngestExecutionContext acquireIngestExecutionContext(IngestTransferSourceCommand command, String triggerType) {
        TransferSource source = resolveSource(command);
        if (forceStopIfExpired(source)) {
            return null;
        }
        log.info("准备发起来源收取，sourceId={}，sourceCode={}，sourceType={}，triggerType={}，hasLockToken={}",
                source.sourceId(),
                source.sourceCode(),
                source.sourceType() == null ? null : source.sourceType().name(),
                triggerType,
                command.ingestLockToken() != null && !command.ingestLockToken().isBlank());
        String ingestLockToken = command.ingestLockToken();
        if (ingestLockToken != null && !ingestLockToken.isBlank()) {
            if (source == null || isStopRequested(source)) {
                log.warn("来源收取任务已失效或已停止，跳过本次执行，sourceId={}，sourceCode={}，ingestStatus={}",
                        source == null ? null : source.sourceId(),
                        source == null ? null : source.sourceCode(),
                        source == null ? null : source.ingestStatus());
                return null;
            }
            return new IngestExecutionContext(source, ingestLockToken, source.ingestStartedAt() == null ? Instant.now() : source.ingestStartedAt(), false);
        }
        String generatedToken = UUID.randomUUID().toString();
        Instant startedAt = Instant.now();
        boolean locked = transferSourceGateway.tryAcquireIngestLock(source.sourceId(), generatedToken, startedAt);
        if (!locked) {
            TransferSource current = transferSourceGateway.findById(source.sourceId()).orElse(source);
            forceStopIfExpired(current);
            log.warn("来源收取已在进行中，跳过本次触发，sourceId={}，sourceCode={}", current.sourceId(), current.sourceCode());
            return null;
        }
        TransferSource lockedSource = transferSourceGateway.findById(source.sourceId()).orElse(source);
        log.info("来源收取锁已获取，sourceId={}，sourceCode={}，lockToken={}", lockedSource.sourceId(), lockedSource.sourceCode(), generatedToken);
        return new IngestExecutionContext(lockedSource, generatedToken, startedAt, true);
    }

    private boolean forceStopIfExpired(TransferSource source) {
        if (source == null || source.ingestStartedAt() == null || !isIngestBusy(source)) {
            return false;
        }
        if (Duration.between(source.ingestStartedAt(), Instant.now()).compareTo(INGEST_WARN_THRESHOLD) < 0) {
            return false;
        }
        log.warn("来源收取超过 5 分钟仍未完成，sourceId={}，sourceCode={}，ingestStatus={}，ingestStartedAt={}",
                source.sourceId(), source.sourceCode(), source.ingestStatus(), source.ingestStartedAt());
        boolean cleaned = transferSourceGateway.forceStopIngest(source.sourceId(), Instant.now());
        if (cleaned) {
            log.warn("来源收取超过 5 分钟，已执行兜底停止清理，sourceId={}，sourceCode={}", source.sourceId(), source.sourceCode());
        }
        return cleaned;
    }

    private boolean isStopRequested(TransferSource source) {
        return source != null
                && ("STOPPING".equalsIgnoreCase(source.ingestStatus())
                || "STOPPED".equalsIgnoreCase(source.ingestStatus()));
    }

    private boolean isIngestBusy(TransferSource source) {
        return source != null
                && ("RUNNING".equalsIgnoreCase(source.ingestStatus())
                || "STOPPING".equalsIgnoreCase(source.ingestStatus()));
    }

    private java.util.Optional<TransferSource> refreshSource(String sourceId) {
        if (sourceId == null || sourceId.isBlank()) {
            return java.util.Optional.empty();
        }
        return transferSourceGateway.findById(sourceId);
    }

    private void releaseIngestLock(IngestExecutionContext executionContext) {
        if (executionContext == null || executionContext.lockToken() == null || executionContext.lockToken().isBlank()) {
            return;
        }
        Instant finishedAt = Instant.now();
        if (executionContext.startedAt() != null && Duration.between(executionContext.startedAt(), finishedAt).compareTo(INGEST_WARN_THRESHOLD) >= 0) {
            log.warn("来源收取超过 10 分钟后完成，sourceId={}，sourceCode={}，costMs={}",
                    executionContext.source().sourceId(),
                    executionContext.source().sourceCode(),
                    Duration.between(executionContext.startedAt(), finishedAt).toMillis());
        }
        boolean released = transferSourceGateway.releaseIngestLock(
                executionContext.source().sourceId(),
                executionContext.lockToken(),
                finishedAt
        );
        if (!released) {
            log.warn("释放来源收取锁失败，sourceId={}，sourceCode={}，lockToken={}",
                    executionContext.source().sourceId(),
                    executionContext.source().sourceCode(),
                    executionContext.lockToken());
        }
    }

    private record IngestExecutionContext(
            TransferSource source,
            String lockToken,
            Instant startedAt,
            boolean acquired
    ) {
    }

    private TransferSource resolveSource(IngestTransferSourceCommand command) {
        TransferSource persisted = null;
        if (command.sourceId() != null) {
            persisted = transferSourceGateway.findById(command.sourceId()).orElse(null);
        }
        if (persisted == null && command.sourceCode() != null && !command.sourceCode().isBlank()) {
            persisted = transferSourceGateway.findBySourceCode(command.sourceCode()).orElse(null);
        }
        Map<String, Object> incomingParameters = command.parameters() == null ? Map.of() : command.parameters();
        if (persisted == null) {
            return new TransferSource(
                    command.sourceId(),
                    command.sourceCode(),
                    command.sourceCode(),
                    command.sourceType(),
                    true,
                    null,
                    incomingParameters,
                    Map.of(),
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }
        return new TransferSource(
                persisted.sourceId(),
                persisted.sourceCode(),
                persisted.sourceName() == null ? persisted.sourceCode() : persisted.sourceName(),
                persisted.sourceType() == null ? command.sourceType() : persisted.sourceType(),
                persisted.enabled(),
                persisted.pollCron(),
                mergeMaps(persisted.connectionConfig(), incomingParameters),
                persisted.sourceMeta(),
                persisted.ingestStatus(),
                persisted.ingestStartedAt(),
                persisted.ingestFinishedAt(),
                persisted.createdAt(),
                persisted.updatedAt()
        );
    }

    private Map<String, Object> mergeMaps(Map<String, Object> existing, Map<String, Object> incoming) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (existing != null && !existing.isEmpty()) {
            merged.putAll(existing);
        }
        if (incoming != null && !incoming.isEmpty()) {
            merged.putAll(incoming);
        }
        return merged;
    }

    private String buildSourceRef(TransferSource source, RecognitionContext context) {
        String stableSourceRef = stableSourceRefFromAttributes(source, context);
        if (stableSourceRef != null && !stableSourceRef.isBlank()) {
            return compactSourceRef(source, stableSourceRef);
        }
        if (context.mailId() != null && !context.mailId().isBlank()) {
            String raw = source.sourceType() + ":" + Objects.toString(source.sourceCode(), "") + ":" + context.mailId() + ":" + Objects.toString(context.fileName(), "")
                    + ":" + Objects.toString(attributeValue(context, TransferConfigKeys.ATTACHMENT_INDEX), "");
            return compactSourceRef(source, raw);
        }
        return compactSourceRef(source, Objects.toString(context.path(), ""));
    }

    private String stableSourceRefFromAttributes(TransferSource source, RecognitionContext context) {
        if (context.attributes() != null) {
            Object remotePath = context.attributes().get(TransferConfigKeys.REMOTE_PATH);
            if (remotePath != null && !String.valueOf(remotePath).isBlank()) {
                return String.valueOf(remotePath);
            }
            Object objectKey = context.attributes().get(TransferConfigKeys.OBJECT_KEY);
            if (objectKey != null && !String.valueOf(objectKey).isBlank()) {
                Object bucket = context.attributes().get(TransferConfigKeys.BUCKET);
                if (bucket != null && !String.valueOf(bucket).isBlank()) {
                    return bucket + ":" + objectKey;
                }
                return String.valueOf(objectKey);
            }
        }
        if (source.sourceMeta() != null) {
            Object bucket = source.sourceMeta().get(TransferConfigKeys.BUCKET);
            Object objectKey = source.sourceMeta().get(TransferConfigKeys.OBJECT_KEY);
            if (bucket != null && objectKey != null) {
                return bucket + ":" + objectKey;
            }
        }
        return null;
    }

    private String compactSourceRef(TransferSource source, String rawRef) {
        String prefix = Objects.toString(source.sourceType(), "UNKNOWN") + ":" + Objects.toString(source.sourceCode(), "");
        return prefix + ":" + shortHash(rawRef);
    }

    private Long fileSizeOf(RecognitionContext context) {
        return context.fileSize();
    }

    private String extensionOf(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return null;
        }
        return fileName.substring(dotIndex + 1);
    }

    private String shortHash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < Math.min(hashed.length, 8); i++) {
                builder.append(String.format("%02x", hashed[i]));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(value == null ? 0 : value.hashCode());
        }
    }

    private String fingerprint(RecognitionContext context) {
        String explicitFingerprint = stringAttribute(context, TransferConfigKeys.CONTENT_FINGERPRINT);
        if (explicitFingerprint != null) {
            return explicitFingerprint;
        }
        return nameAndSizeFingerprint(context);
    }

    private String nameAndSizeFingerprint(RecognitionContext context) {
        String name = context == null ? null : context.fileName();
        Long size = context == null ? null : context.fileSize();
        if ((name == null || name.isBlank()) && size == null) {
            throw new IllegalStateException("缺少文件名称和大小，无法计算文件指纹");
        }
        String raw = Objects.toString(name, "attachment") + ":" + Objects.toString(size, "0");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < Math.min(hashed.length, 8); i++) {
                builder.append(String.format("%02x", hashed[i]));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(raw.hashCode());
        }
    }

    private Object attributeValue(RecognitionContext context, String key) {
        if (context == null || context.attributes() == null || key == null) {
            return null;
        }
        return context.attributes().get(key);
    }

    private String stringAttribute(RecognitionContext context, String key) {
        Object value = attributeValue(context, key);
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return String.valueOf(value).trim();
    }

    private Path resolveFilePath(RecognitionContext context) {
        if (context == null || context.path() == null || context.path().isBlank()) {
            return null;
        }
        Path path = Path.of(context.path());
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new IllegalStateException("文件不存在或不是普通文件，path=" + path);
        }
        return path;
    }

    private Path storeReceivedFile(TransferSource source, RecognitionContext context, TransferObject transferObject) {
        Path fileToStore = resolveFilePath(context);
        Path temporaryMaterializedPath = null;
        Path storedPath = null;
        try {
            if (fileToStore == null) {
                if (source == null) {
                    throw new IllegalStateException("文件对象缺少来源信息，无法统一落盘");
                }
                temporaryMaterializedPath = sourceConnectorRegistry.getRequired(source).materialize(source, transferObject);
                fileToStore = temporaryMaterializedPath;
            }
            if (fileToStore == null) {
                throw new IllegalStateException("文件对象缺少可落盘的源文件路径");
            }
            Path directory = Path.of(uploadRoot).toAbsolutePath().resolve(LocalDate.now().toString());
            Files.createDirectories(directory);
            storedPath = directory.resolve(resolveStoredFilename(transferObject.originalName()));
            boolean moveInsteadOfCopy = temporaryMaterializedPath != null
                    || (source != null && source.sourceType() == SourceType.EMAIL);
            String transferOperation = moveInsteadOfCopy ? "MOVE" : "COPY";
            if (moveInsteadOfCopy) {
                log.info("邮件附件临时文件直接转存，operation={}，transferId={}，sourceId={}，sourceCode={}，originalName={}，sourcePath={}，storedPath={}",
                        transferOperation,
                        transferObject.transferId(),
                        source == null ? null : source.sourceId(),
                        source == null ? null : source.sourceCode(),
                        transferObject.originalName(),
                        fileToStore,
                        storedPath);
                Files.move(fileToStore, storedPath, StandardCopyOption.REPLACE_EXISTING);
            } else {
                log.info("来源文件复制转存，operation={}，transferId={}，sourceId={}，sourceCode={}，originalName={}，sourcePath={}，storedPath={}",
                        transferOperation,
                        transferObject.transferId(),
                        source == null ? null : source.sourceId(),
                        source == null ? null : source.sourceCode(),
                        transferObject.originalName(),
                        fileToStore,
                        storedPath);
                Files.copy(fileToStore, storedPath, StandardCopyOption.REPLACE_EXISTING);
            }
            if (temporaryMaterializedPath != null) {
                Files.deleteIfExists(temporaryMaterializedPath);
            }
            return storedPath;
        } catch (Exception exception) {
            log.error("统一落盘失败，transferId={}，sourceId={}，sourceCode={}，originalName={}，sourcePath={}，storedPath={}，error={}",
                    transferObject == null ? null : transferObject.transferId(),
                    source == null ? null : source.sourceId(),
                    source == null ? null : source.sourceCode(),
                    transferObject == null ? null : transferObject.originalName(),
                    fileToStore,
                    storedPath,
                    buildErrorMessage(exception),
                    exception);
            throw new IllegalStateException("统一落盘失败，fileName=" + transferObject.originalName(), exception);
        }
    }

    private String resolveStoredFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "transfer-file";
        }
        String sanitized = originalFilename.trim();
        int lastSlash = Math.max(sanitized.lastIndexOf('/'), sanitized.lastIndexOf('\\'));
        if (lastSlash >= 0 && lastSlash < sanitized.length() - 1) {
            sanitized = sanitized.substring(lastSlash + 1);
        }
        sanitized = sanitized.replaceAll("[\\p{Cntrl}]", "_").trim();
        return sanitized.isBlank() ? "transfer-file" : sanitized;
    }

    private String resolveUploadRoot(String configuredUploadRoot) {
        if (configuredUploadRoot != null && !configuredUploadRoot.isBlank()) {
            return configuredUploadRoot;
        }
        return Path.of(System.getProperty("user.home"), ".tmp", "valset-standardizer", "uploads").toString();
    }

    private String normalizeTriggerType(String triggerType) {
        if (triggerType == null || triggerType.isBlank()) {
            return TransferTriggerType.SYSTEM.name();
        }
        return triggerType.trim().toUpperCase();
    }

    private void saveRunLog(TransferSource source,
                            String transferId,
                            String routeId,
                            String triggerType,
                            String runStage,
                            String runStatus,
                            String logMessage,
                            Throwable error) {
        if (error == null) {
            log.info("文件收取运行日志，stage={}，status={}，sourceId={}，transferId={}，routeId={}，message={}",
                    runStage,
                    runStatus,
                    source == null ? null : source.sourceId(),
                    transferId,
                    routeId,
                    logMessage);
        } else {
            log.error("文件收取运行日志，stage={}，status={}，sourceId={}，transferId={}，routeId={}，message={}，error={}",
                    runStage,
                    runStatus,
                    source == null ? null : source.sourceId(),
                    transferId,
                    routeId,
                    logMessage,
                    buildErrorMessage(error),
                    error);
        }
        transferRunLogGateway.save(new TransferRunLog(
                null,
                source == null ? null : source.sourceId(),
                source == null || source.sourceType() == null ? null : source.sourceType().name(),
                source == null ? null : source.sourceCode(),
                source == null ? null : source.sourceName(),
                transferId,
                routeId,
                triggerType,
                runStage,
                runStatus,
                logMessage,
                error == null ? null : buildErrorMessage(error),
                LocalDateTime.now()
        ));
    }

    private void recordCheckpointItemIfNeeded(TransferSource source,
                                              RecognitionContext context,
                                              String triggerType,
                                              Set<String> processedCheckpointKeys) {
        if (source == null || source.sourceId() == null || context == null) {
            return;
        }
        String itemKey = resolveCheckpointKey(source, context);
        if (itemKey == null || itemKey.isBlank()) {
            return;
        }
        if (!processedCheckpointKeys.add(itemKey)) {
            return;
        }
        transferSourceCheckpointGateway.saveProcessedItem(new TransferSourceCheckpointItem(
                null,
                source.sourceId(),
                source.sourceType() == null ? null : source.sourceType().name(),
                itemKey,
                resolveCheckpointRef(source, context),
                resolveCheckpointName(context),
                context.fileSize(),
                context.mimeType(),
                resolveCheckpointFingerprint(source, context),
                buildCheckpointMeta(source, context),
                triggerType,
                Instant.now(),
                Instant.now(),
                Instant.now()
        ));
    }

    private String resolveCheckpointKey(TransferSource source, RecognitionContext context) {
        Object explicit = attributeValue(context, TransferConfigKeys.CHECKPOINT_KEY);
        if (explicit != null && !String.valueOf(explicit).isBlank()) {
            return String.valueOf(explicit);
        }
        SourceType sourceType = source == null ? null : source.sourceType();
        if (sourceType == null) {
            return fingerprint(context);
        }
        return switch (sourceType) {
            case EMAIL -> context.mailId();
            case LOCAL_DIR -> String.join("|",
                    Objects.toString(attributeValue(context, "absolutePath"), Objects.toString(context.path(), "")),
                    Objects.toString(attributeValue(context, "lastModified"), ""),
                    Objects.toString(context.fileSize(), ""));
            case S3 -> String.join("|",
                    Objects.toString(attributeValue(context, TransferConfigKeys.BUCKET), ""),
                    Objects.toString(attributeValue(context, TransferConfigKeys.OBJECT_KEY), ""),
                    Objects.toString(attributeValue(context, TransferConfigKeys.E_TAG), ""),
                    Objects.toString(context.fileSize(), ""));
            case SFTP -> String.join("|",
                    Objects.toString(attributeValue(context, TransferConfigKeys.REMOTE_PATH), Objects.toString(context.path(), "")),
                    Objects.toString(attributeValue(context, "lastModified"), ""),
                    Objects.toString(context.fileSize(), ""));
        };
    }

    private String resolveCheckpointRef(TransferSource source, RecognitionContext context) {
        Object explicit = attributeValue(context, TransferConfigKeys.CHECKPOINT_REF);
        if (explicit != null && !String.valueOf(explicit).isBlank()) {
            return String.valueOf(explicit);
        }
        SourceType sourceType = source == null ? null : source.sourceType();
        if (sourceType == null) {
            return buildSourceRef(source, context);
        }
        return switch (sourceType) {
            case EMAIL -> context.mailId();
            case LOCAL_DIR -> Objects.toString(attributeValue(context, "absolutePath"), context.path());
            case S3 -> Objects.toString(attributeValue(context, TransferConfigKeys.REMOTE_PATH), context.path());
            case SFTP -> Objects.toString(attributeValue(context, TransferConfigKeys.REMOTE_PATH), context.path());
        };
    }

    private String resolveCheckpointName(RecognitionContext context) {
        Object explicit = attributeValue(context, TransferConfigKeys.CHECKPOINT_NAME);
        if (explicit != null && !String.valueOf(explicit).isBlank()) {
            return String.valueOf(explicit);
        }
        return context.fileName();
    }

    private String resolveCheckpointFingerprint(TransferSource source, RecognitionContext context) {
        Object explicit = attributeValue(context, TransferConfigKeys.CHECKPOINT_FINGERPRINT);
        if (explicit != null && !String.valueOf(explicit).isBlank()) {
            return String.valueOf(explicit);
        }
        return fingerprint(context);
    }

    private Map<String, Object> buildCheckpointMeta(TransferSource source, RecognitionContext context) {
        Map<String, Object> meta = new LinkedHashMap<>();
        if (context.attributes() != null && !context.attributes().isEmpty()) {
            meta.putAll(context.attributes());
        }
        meta.putIfAbsent(TransferConfigKeys.SOURCE_TYPE, source == null || source.sourceType() == null ? null : source.sourceType().name());
        meta.putIfAbsent(TransferConfigKeys.SOURCE_CODE, source == null ? null : source.sourceCode());
        meta.putIfAbsent(TransferConfigKeys.CHECKPOINT_KEY, resolveCheckpointKey(source, context));
        meta.putIfAbsent(TransferConfigKeys.CHECKPOINT_REF, resolveCheckpointRef(source, context));
        meta.putIfAbsent(TransferConfigKeys.CHECKPOINT_NAME, resolveCheckpointName(context));
        meta.putIfAbsent(TransferConfigKeys.CHECKPOINT_FINGERPRINT, resolveCheckpointFingerprint(source, context));
        return meta;
    }

    private void recordCheckpointCursorIfNeeded(TransferSource source, RecognitionContext context, String triggerType) {
        if (source == null || source.sourceId() == null || context == null) {
            return;
        }
        String checkpointValue = resolveCheckpointKey(source, context);
        if (checkpointValue == null || checkpointValue.isBlank()) {
            return;
        }
        Map<String, Object> checkpointMeta = new LinkedHashMap<>();
        checkpointMeta.put(TransferConfigKeys.SOURCE_TYPE, source.sourceType() == null ? null : source.sourceType().name());
        checkpointMeta.put(TransferConfigKeys.SOURCE_CODE, source.sourceCode());
        checkpointMeta.put(TransferConfigKeys.CHECKPOINT_KEY, checkpointValue);
        checkpointMeta.put(TransferConfigKeys.CHECKPOINT_REF, resolveCheckpointRef(source, context));
        checkpointMeta.put(TransferConfigKeys.CHECKPOINT_NAME, resolveCheckpointName(context));
        checkpointMeta.put(TransferConfigKeys.CHECKPOINT_FINGERPRINT, resolveCheckpointFingerprint(source, context));
        checkpointMeta.put(TransferConfigKeys.TRIGGER_TYPE, triggerType);
        checkpointMeta.put("processedAt", Instant.now().toString());
        transferSourceCheckpointGateway.saveCheckpoint(new TransferSourceCheckpoint(
                null,
                source.sourceId(),
                source.sourceType() == null ? null : source.sourceType().name(),
                TransferConfigKeys.CHECKPOINT_SCAN_CURSOR,
                checkpointValue,
                checkpointMeta,
                Instant.now(),
                Instant.now()
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
