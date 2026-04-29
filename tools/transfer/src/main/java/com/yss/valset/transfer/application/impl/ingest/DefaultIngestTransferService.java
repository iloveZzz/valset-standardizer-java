package com.yss.valset.transfer.application.impl.ingest;

import com.yss.valset.transfer.application.command.IngestTransferSourceCommand;
import com.yss.valset.transfer.application.port.IngestTransferUseCase;
import com.yss.valset.transfer.application.service.TransferIngestProgressAppService;
import com.yss.valset.transfer.application.service.TransferTaggingUseCase;
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
import com.yss.valset.transfer.application.port.SourceConnector;
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
    private final TransferIngestProgressAppService transferIngestProgressAppService;
    private final TransferTaggingUseCase transferTaggingUseCase;
    private final String uploadRoot;

    public DefaultIngestTransferService(TransferSourceGateway transferSourceGateway,
                                        SourceConnectorRegistry sourceConnectorRegistry,
                                        TransferObjectGateway transferObjectGateway,
                                        TransferRunLogGateway transferRunLogGateway,
                                        TransferSourceCheckpointGateway transferSourceCheckpointGateway,
                                        ObjectProvider<TransferJobScheduler> transferJobSchedulerProvider,
                                        FileProbePluginRegistry fileProbePluginRegistry,
                                        TransferIngestProgressAppService transferIngestProgressAppService,
                                        TransferTaggingUseCase transferTaggingUseCase,
                                        @Value("${subject.match.upload-dir:${user.home}/.tmp/valset-standardizer/uploads}") String uploadRoot) {
        this.transferSourceGateway = transferSourceGateway;
        this.sourceConnectorRegistry = sourceConnectorRegistry;
        this.transferObjectGateway = transferObjectGateway;
        this.transferRunLogGateway = transferRunLogGateway;
        this.transferSourceCheckpointGateway = transferSourceCheckpointGateway;
        this.transferJobSchedulerProvider = transferJobSchedulerProvider;
        this.fileProbePluginRegistry = fileProbePluginRegistry;
        this.transferIngestProgressAppService = transferIngestProgressAppService;
        this.transferTaggingUseCase = transferTaggingUseCase;
        this.uploadRoot = resolveUploadRoot(uploadRoot);
    }

    @Override
    public void execute(IngestTransferSourceCommand command) {
        // 阶段 1：解析触发类型，获取执行上下文，并完成起始准备。
        String triggerType = normalizeTriggerType(command.triggerType());
        IngestExecutionContext executionContext = acquireIngestExecutionContext(command, triggerType);
        if (executionContext == null) {
            return;
        }
        IngestStartContext startContext = prepareIngestStartContext(executionContext, triggerType);
        if (startContext == null) {
            return;
        }
        TransferSource source = startContext.source();
        String startedAtText = startContext.startedAtText();
        SourceConnector connector = startContext.connector();
        boolean routeFailureLogged = false;
        boolean stopRequested = false;
        try {
            // 阶段 2：遍历候选文件，按“重复检查 -> 分析 -> 探测 -> 落盘 -> 打标 -> 检查点 -> 路由”处理单文件。
            List<RecognitionContext> contexts = connector.fetch(source);
            log.info("来源收取完成，候选记录数={}，sourceId={}，sourceCode={}，triggerType={}",
                    contexts.size(),
                    source.sourceId(),
                    source.sourceCode(),
                    triggerType);
            transferIngestProgressAppService.publishStatus(
                    source.sourceId(),
                    "running",
                    "候选文件扫描完成，准备处理 " + contexts.size() + " 个文件",
                    triggerType,
                    startedAtText
            );
            TransferSource latestSource = refreshSource(source.sourceId()).orElse(source);
            if (isStopRequested(latestSource)) {
                stopRequested = true;
                source = latestSource;
            }
            int createdCount = 0;
            int duplicateCount = 0;
            int processedCount = 0;
            Set<String> processedCheckpointKeys = new LinkedHashSet<>();
            for (RecognitionContext context : contexts) {
                latestSource = refreshSource(source.sourceId()).orElse(source);
                if (isStopRequested(latestSource)) {
                    stopRequested = true;
                    source = latestSource;
                    break;
                }

                // 第 1 步：先做文件级重复检查，避免对已存在对象重复落库和重复路由。
                String progressMessage = "正在处理文件：" + safeFileName(context);
                if (isAttachmentMaterializeFailed(context)) {
                    saveRunLog(
                            source,
                            null,
                            null,
                            triggerType,
                            TransferRunStage.INGEST.name(),
                            TransferRunStatus.FAILED.name(),
                            buildAttachmentMaterializeFailedMessage(context),
                            null
                    );
                    progressMessage = "附件抽取失败，已记录日志：" + safeFileName(context);
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
                    progressMessage = "重复文件已跳过：" + safeFileName(context);
                    processedCount++;
                    transferIngestProgressAppService.publishProgress(
                            source.sourceId(),
                            processedCount,
                            contexts.size(),
                            progressMessage
                    );
                    continue;
                }

                // 第 2 步：整理统一分析上下文，补齐文件指纹、来源信息和邮件元数据。
                log.info("来源候选文件准备分析，sourceId={}，sourceCode={}，fileName={}，path={}，fingerprint={}",
                        source.sourceId(),
                        source.sourceCode(),
                        context.fileName(),
                        context.path(),
                        fingerprint);
                Map<String, Object> fileMeta = buildFileMeta(source, context, triggerType, fingerprint);
                RecognitionContext analysisContext = buildAnalysisContext(context, fileMeta);

                // 第 3 步：执行文件探测并构建对象草稿，得到后续持久化所需的完整对象。
                ProbeResult probeResult = probeAnalysisContext(analysisContext);
                TransferObject draftTransferObject = buildDraftTransferObject(source, context, fingerprint, fileMeta, probeResult);

                // 第 4 步：统一落盘到临时目录，并把最终路径写回对象后再持久化。
                Path storedPath = storeReceivedFile(source, analysisContext, draftTransferObject);
                TransferObject transferObject = draftTransferObject.withLocalTempPath(storedPath == null ? null : storedPath.toAbsolutePath().toString());
                TransferObject savedTransfer = transferObjectGateway.save(transferObject);

                // 第 5 步：用统一后的标签上下文执行打标，标签命中后再回填业务字段。
                RecognitionContext taggingContext = toTaggingContext(analysisContext, savedTransfer);
                try {
                    transferTaggingUseCase.tag(savedTransfer, taggingContext, probeResult);
                } catch (Exception tagException) {
                    log.warn("文件对象打标失败，继续执行路由，transferId={}，sourceId={}，sourceCode={}",
                            savedTransfer.transferId(),
                            source.sourceId(),
                            source.sourceCode(),
                            tagException);
                    saveRunLog(
                            source,
                            savedTransfer.transferId(),
                            null,
                            triggerType,
                            TransferRunStage.INGEST.name(),
                            TransferRunStatus.FAILED.name(),
                            "文件对象打标失败，transferId=" + savedTransfer.transferId(),
                            tagException
                    );
                }

                // 第 6 步：补齐去重和断点恢复用的检查点，再记录本次收取结果。
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
                logReadyForRoute(source, savedTransfer);
                progressMessage = "已处理文件：" + safeFileName(context);

                // 第 7 步：对象入库完成后，交给路由调度器继续处理下游投递链路。
                try {
                    triggerRouteAfterIngest(savedTransfer);
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
                processedCount++;
                transferIngestProgressAppService.publishProgress(
                        source.sourceId(),
                        processedCount,
                        contexts.size(),
                        progressMessage
                );
            }

            // 阶段 3：根据本轮处理结果统一收尾，覆盖停止、空结果、全重复和正常完成四种场景。
            finalizeIngestSuccess(source, triggerType, startedAtText, createdCount, duplicateCount, stopRequested, contexts.isEmpty());
        } catch (Exception exception) {
            // 阶段 4：统一处理异常上报和异常转换，确保运行状态对外可见。
            handleIngestFailure(source, triggerType, startedAtText, routeFailureLogged, exception);
        } finally {
            // 阶段 5：无论成功失败，都释放本次收取锁。
            releaseIngestLock(executionContext);
        }
    }

    /**
     * 收口执行前的起始准备，包含起始日志、停止判断和连接器获取。
     */
    private IngestStartContext prepareIngestStartContext(IngestExecutionContext executionContext, String triggerType) {
        TransferSource source = executionContext.source();
        String startedAtText = executionContext.startedAt() == null ? null : executionContext.startedAt().toString();
        log.info("开始收取来源，sourceId={}，sourceCode={}，sourceType={}，triggerType={}",
                source.sourceId(),
                source.sourceCode(),
                source.sourceType() == null ? null : source.sourceType().name(),
                triggerType);
        if (isStopRequested(source)) {
            log.info("来源已收到停止请求，本轮收取未继续执行，sourceId={}，sourceCode={}，triggerType={}",
                    source.sourceId(),
                    source.sourceCode(),
                    triggerType);
            return null;
        }
        SourceConnector connector = sourceConnectorRegistry.getRequired(source);
        transferIngestProgressAppService.publishStatus(source.sourceId(), "running", "开始收取，准备扫描候选文件", triggerType, startedAtText);
        return new IngestStartContext(source, connector, startedAtText);
    }

    /**
     * 汇总候选文件的元数据，确保后续探测、打标和落库都使用同一份上下文。
     */
    private Map<String, Object> buildFileMeta(TransferSource source,
                                              RecognitionContext context,
                                              String triggerType,
                                              String fingerprint) {
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
        return fileMeta;
    }

    /**
     * 构造统一的分析上下文，供文件探测插件使用。
     */
    private RecognitionContext buildAnalysisContext(RecognitionContext context, Map<String, Object> fileMeta) {
        return new RecognitionContext(
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
    }

    /**
     * 调用文件探测插件，生成探测结果，供标签和路由阶段复用。
     */
    private ProbeResult probeAnalysisContext(RecognitionContext analysisContext) {
        return fileProbePluginRegistry.getRequired(analysisContext).probe(analysisContext);
    }

    /**
     * 基于来源信息、文件上下文和探测结果构建分拣对象草稿。
     */
    private TransferObject buildDraftTransferObject(TransferSource source,
                                                    RecognitionContext context,
                                                    String fingerprint,
                                                    Map<String, Object> fileMeta,
                                                    ProbeResult probeResult) {
        return new TransferObject(
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
            return resumeExistingIngest(source, ingestLockToken);
        }
        return acquireIngestLockForSource(source, triggerType);
    }

    /**
     * 处理已有锁令牌的恢复场景，只做有效性检查并返回执行上下文。
     */
    private IngestExecutionContext resumeExistingIngest(TransferSource source, String ingestLockToken) {
        if (source == null || isStopRequested(source)) {
            log.warn("来源收取任务已失效或已停止，跳过本次执行，sourceId={}，sourceCode={}，ingestStatus={}",
                    source == null ? null : source.sourceId(),
                    source == null ? null : source.sourceCode(),
                    source == null ? null : source.ingestStatus());
            return null;
        }
        return new IngestExecutionContext(source, ingestLockToken, source.ingestStartedAt() == null ? Instant.now() : source.ingestStartedAt(), false);
    }

    /**
     * 处理正常触发场景的锁申请逻辑。
     */
    private IngestExecutionContext acquireIngestLockForSource(TransferSource source, String triggerType) {
        String generatedToken = UUID.randomUUID().toString();
        Instant startedAt = Instant.now();
        boolean locked = transferSourceGateway.tryAcquireIngestLock(source.sourceId(), generatedToken, startedAt, triggerType);
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

    private record IngestStartContext(
            TransferSource source,
            SourceConnector connector,
            String startedAtText
    ) {
    }

    private TransferSource resolveSource(IngestTransferSourceCommand command) {
        TransferSource persisted = resolvePersistedSource(command);
        Map<String, Object> incomingParameters = command.parameters() == null ? Map.of() : command.parameters();
        if (persisted == null) {
            return createTransientSource(command, incomingParameters);
        }
        return assemblePersistedSource(command, persisted, incomingParameters);
    }

    /**
     * 按来源 ID 或来源编码查找已持久化的来源配置。
     */
    private TransferSource resolvePersistedSource(IngestTransferSourceCommand command) {
        if (command.sourceId() != null) {
            TransferSource byId = transferSourceGateway.findById(command.sourceId()).orElse(null);
            if (byId != null) {
                return byId;
            }
        }
        if (command.sourceCode() != null && !command.sourceCode().isBlank()) {
            return transferSourceGateway.findBySourceCode(command.sourceCode()).orElse(null);
        }
        return null;
    }

    /**
     * 构建临时来源对象，供尚未入库的来源触发场景使用。
     */
    private TransferSource createTransientSource(IngestTransferSourceCommand command, Map<String, Object> incomingParameters) {
        return new TransferSource(
                command.sourceId(),
                command.sourceCode(),
                resolveTransientSourceName(command),
                resolveTransientSourceType(command),
                true,
                null,
                resolveTransientConnectionConfig(incomingParameters),
                Map.of(),
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    /**
     * 临时来源名称直接回退为来源编码，保持触发时可读。
     */
    private String resolveTransientSourceName(IngestTransferSourceCommand command) {
        return command.sourceCode();
    }

    /**
     * 临时来源类型直接采用命令参数，避免在未入库场景下引入额外推断。
     */
    private SourceType resolveTransientSourceType(IngestTransferSourceCommand command) {
        return command.sourceType();
    }

    /**
     * 临时来源连接配置直接采用命令参数，避免再做额外合并。
     */
    private Map<String, Object> resolveTransientConnectionConfig(Map<String, Object> incomingParameters) {
        return incomingParameters;
    }

    /**
     * 将持久化来源和命令参数重新装配成执行时来源对象。
     */
    private TransferSource assemblePersistedSource(IngestTransferSourceCommand command,
                                                  TransferSource persisted,
                                                  Map<String, Object> incomingParameters) {
        return new TransferSource(
                persisted.sourceId(),
                persisted.sourceCode(),
                resolvePersistedSourceName(persisted),
                resolvePersistedSourceType(command, persisted),
                persisted.enabled(),
                persisted.pollCron(),
                resolveMergedConnectionConfig(persisted, incomingParameters),
                persisted.sourceMeta(),
                persisted.ingestStatus(),
                persisted.ingestTriggerType(),
                persisted.ingestStartedAt(),
                persisted.ingestFinishedAt(),
                persisted.createdAt(),
                persisted.updatedAt()
        );
    }

    /**
     * 统一来源名称回填规则，避免空名称在主装配里直接展开。
     */
    private String resolvePersistedSourceName(TransferSource persisted) {
        return persisted.sourceName() == null ? persisted.sourceCode() : persisted.sourceName();
    }

    /**
     * 统一来源类型回填规则，持久化值为空时回退到命令参数。
     */
    private SourceType resolvePersistedSourceType(IngestTransferSourceCommand command, TransferSource persisted) {
        return persisted.sourceType() == null ? command.sourceType() : persisted.sourceType();
    }

    /**
     * 合并持久化连接配置和本次命令参数，保持运行时参数优先。
     */
    private Map<String, Object> resolveMergedConnectionConfig(TransferSource persisted, Map<String, Object> incomingParameters) {
        return mergeMaps(persisted.connectionConfig(), incomingParameters);
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

    private String safeFileName(RecognitionContext context) {
        if (context == null || context.fileName() == null || context.fileName().isBlank()) {
            return "未命名文件";
        }
        return context.fileName();
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

    static RecognitionContext toTaggingContext(RecognitionContext analysisContext, TransferObject transferObject) {
        if (transferObject == null) {
            return analysisContext;
        }
        return new RecognitionContext(
                analysisContext == null ? null : analysisContext.sourceType(),
                analysisContext == null ? null : analysisContext.sourceCode(),
                analysisContext == null ? transferObject.originalName() : analysisContext.fileName(),
                analysisContext == null ? transferObject.mimeType() : analysisContext.mimeType(),
                analysisContext == null ? transferObject.sizeBytes() : analysisContext.fileSize(),
                analysisContext == null ? transferObject.mailFrom() : analysisContext.sender(),
                analysisContext == null ? transferObject.mailTo() : analysisContext.recipientsTo(),
                analysisContext == null ? transferObject.mailCc() : analysisContext.recipientsCc(),
                analysisContext == null ? transferObject.mailBcc() : analysisContext.recipientsBcc(),
                analysisContext == null ? transferObject.mailSubject() : analysisContext.subject(),
                analysisContext == null ? transferObject.mailBody() : analysisContext.body(),
                analysisContext == null ? transferObject.mailId() : analysisContext.mailId(),
                analysisContext == null ? transferObject.mailProtocol() : analysisContext.mailProtocol(),
                analysisContext == null ? transferObject.mailFolder() : analysisContext.mailFolder(),
                transferObject.localTempPath(),
                transferObject.fileMeta()
        );
    }

    private Path storeReceivedFile(TransferSource source, RecognitionContext context, TransferObject transferObject) throws Exception {
        // 第 1 步：优先使用收取阶段已经提供的本地文件路径；如果没有，再由来源连接器做临时物化。
        Path fileToStore = resolveSourceFileForStorage(source, context, transferObject);
        // 第 2 步：准备统一的落盘目录和最终文件路径，保证所有来源都落到同一目录结构。
        Path storedPath = resolveStoredFilePath(transferObject);
        // 第 3 步：根据来源类型决定是移动还是复制，邮件附件和临时物化文件优先移动。
        boolean moveInsteadOfCopy = shouldMoveStoredFile(source, context, fileToStore);
        try {
            // 第 4 步：执行实际的文件搬运动作。
            transferStoredFile(source, transferObject, fileToStore, storedPath, moveInsteadOfCopy);
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

    /**
     * 统一解析需要落盘的源文件。
     * <p>
     * 优先使用收取阶段直接提供的本地路径；如果没有，则在来源连接器侧临时物化。
     * </p>
     */
    private Path resolveSourceFileForStorage(TransferSource source, RecognitionContext context, TransferObject transferObject) throws Exception {
        Path fileToStore = resolveFilePath(context);
        if (fileToStore != null) {
            return fileToStore;
        }
        if (source == null) {
            throw new IllegalStateException("文件对象缺少来源信息，无法统一落盘");
        }
        return sourceConnectorRegistry.getRequired(source).materialize(source, transferObject);
    }

    /**
     * 统一构建最终落盘路径，按照日期目录组织，便于后续排障和归档。
     */
    private Path resolveStoredFilePath(TransferObject transferObject) throws Exception {
        Path directory = Path.of(uploadRoot).toAbsolutePath().resolve(LocalDate.now().toString());
        Files.createDirectories(directory);
        return directory.resolve(resolveStoredFilename(transferObject.originalName()));
    }

    /**
     * 判断当前对象落盘时是否应该使用移动而不是复制。
     */
    private boolean shouldMoveStoredFile(TransferSource source, RecognitionContext context, Path fileToStore) {
        boolean materializedByConnector = isConnectorMaterialized(context, fileToStore);
        return materializedByConnector || (source != null && source.sourceType() == SourceType.EMAIL);
    }

    /**
     * 执行文件搬运动作，并在必要时清理来源连接器产生的临时文件。
     */
    private void transferStoredFile(TransferSource source,
                                    TransferObject transferObject,
                                    Path fileToStore,
                                    Path storedPath,
                                    boolean moveInsteadOfCopy) throws Exception {
        if (fileToStore == null) {
            throw new IllegalStateException("文件对象缺少可落盘的源文件路径");
        }
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
        cleanupTemporaryMaterializedFile(source, transferObject, fileToStore, moveInsteadOfCopy);
    }

    /**
     * 只清理来源连接器临时生成的文件，避免误删收取阶段原始文件。
     */
    private void cleanupTemporaryMaterializedFile(TransferSource source,
                                                  TransferObject transferObject,
                                                  Path fileToStore,
                                                  boolean moveInsteadOfCopy) throws Exception {
        if (!moveInsteadOfCopy) {
            return;
        }
        if (source != null && source.sourceType() == SourceType.EMAIL) {
            return;
        }
        Files.deleteIfExists(fileToStore);
    }

    private boolean isConnectorMaterialized(RecognitionContext context, Path fileToStore) {
        if (context == null || fileToStore == null) {
            return false;
        }
        if (context.path() == null || context.path().isBlank()) {
            return true;
        }
        Object materialized = attributeValue(context, "materializedByConnector");
        return materialized != null && Boolean.parseBoolean(String.valueOf(materialized));
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
        logRunLog(source, transferId, routeId, runStage, runStatus, logMessage, error);
        transferRunLogGateway.save(buildRunLog(source, transferId, routeId, triggerType, runStage, runStatus, logMessage, error));
    }

    /**
     * 输出运行日志到应用日志，方便快速定位收取、路由和落盘阶段的问题。
     */
    private void logRunLog(TransferSource source,
                           String transferId,
                           String routeId,
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
            return;
        }
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

    /**
     * 收口收取完成后的状态分支，避免主流程里散落停止、空结果和重复结果的判断。
     */
    private void finalizeIngestSuccess(TransferSource source,
                                       String triggerType,
                                       String startedAtText,
                                       int createdCount,
                                       int duplicateCount,
                                       boolean stopRequested,
                                       boolean contextsEmpty) {
        if (stopRequested) {
            publishStopRequestedResult(source, triggerType, startedAtText, createdCount, duplicateCount);
            return;
        }
        if (contextsEmpty) {
            publishNoNewFilesResult(source, triggerType, startedAtText);
            return;
        }
        if (createdCount == 0 && duplicateCount > 0) {
            publishAllDuplicateResult(source, triggerType, startedAtText, duplicateCount);
            return;
        }
        publishCompletedResult(source, triggerType, startedAtText, createdCount, duplicateCount);
    }

    /**
     * 收口停止场景的收尾处理。
     */
    private void publishStopRequestedResult(TransferSource source,
                                            String triggerType,
                                            String startedAtText,
                                            int createdCount,
                                            int duplicateCount) {
        log.info("来源收取已停止，已提前结束，sourceId={}，sourceCode={}，文件数={}，重复文件数={}，triggerType={}",
                source.sourceId(),
                source.sourceCode(),
                createdCount,
                duplicateCount,
                triggerType);
        publishIngestCompletion(source, triggerType, startedAtText, "stopped", "来源收取已停止，已提前结束", "来源收取已停止，已提前结束，文件数=" + createdCount + "，重复文件数=" + duplicateCount);
    }

    /**
     * 收口未发现新文件时的收尾处理。
     */
    private void publishNoNewFilesResult(TransferSource source, String triggerType, String startedAtText) {
        log.info("收取完成，未发现新文件，sourceId={}，sourceCode={}，triggerType={}",
                source.sourceId(),
                source.sourceCode(),
                triggerType);
        publishIngestCompletion(source, triggerType, startedAtText, "success", "收取完成，未发现新文件", "收取完成，未发现新文件");
    }

    /**
     * 收口全部重复时的收尾处理。
     */
    private void publishAllDuplicateResult(TransferSource source,
                                           String triggerType,
                                           String startedAtText,
                                           int duplicateCount) {
        log.info("收取完成，全部文件均已存在，sourceId={}，sourceCode={}，重复文件数={}，triggerType={}",
                source.sourceId(),
                source.sourceCode(),
                duplicateCount,
                triggerType);
        publishIngestCompletion(source, triggerType, startedAtText, "success", "收取完成，全部文件均已存在", "收取完成，全部文件均已存在，重复文件数=" + duplicateCount);
    }

    /**
     * 收口普通完成场景的收尾处理。
     */
    private void publishCompletedResult(TransferSource source,
                                        String triggerType,
                                        String startedAtText,
                                        int createdCount,
                                        int duplicateCount) {
        log.info("收取完成，sourceId={}，sourceCode={}，新文件数={}，重复文件数={}，triggerType={}",
                source.sourceId(),
                source.sourceCode(),
                createdCount,
                duplicateCount,
                triggerType);
        publishIngestCompletion(source, triggerType, startedAtText, "success", "收取完成", "收取完成，新文件数=" + createdCount + "，重复文件数=" + duplicateCount);
    }

    /**
     * 收口收取完成后的统一上报动作，避免不同结果分支重复编排日志和状态。
     */
    private void publishIngestCompletion(TransferSource source,
                                         String triggerType,
                                         String startedAtText,
                                         String status,
                                         String statusMessage,
                                         String completeMessage) {
        saveRunLog(
                source,
                null,
                null,
                triggerType,
                TransferRunStage.INGEST.name(),
                TransferRunStatus.SUCCESS.name(),
                completeMessage,
                null
        );
        transferIngestProgressAppService.publishStatus(source.sourceId(), status, statusMessage, triggerType, startedAtText);
        transferIngestProgressAppService.publishComplete(source.sourceId(), completeMessage);
    }

    /**
     * 收口收取失败后的状态上报和异常转换。
     */
    private void handleIngestFailure(TransferSource source,
                                     String triggerType,
                                     String startedAtText,
                                     boolean routeFailureLogged,
                                     Exception exception) {
        if (!routeFailureLogged) {
            log.error("来源收取失败，sourceId={}，sourceCode={}，triggerType={}",
                    source == null ? null : source.sourceId(),
                    source == null ? null : source.sourceCode(),
                    triggerType,
                    exception);
        }
        transferIngestProgressAppService.publishStatus(
                source == null ? null : source.sourceId(),
                "failed",
                exception.getMessage() == null || exception.getMessage().isBlank() ? "来源收取失败" : exception.getMessage(),
                triggerType,
                startedAtText
        );
        transferIngestProgressAppService.publishError(
                source == null ? null : source.sourceId(),
                "INGEST_FAILED",
                exception.getMessage() == null || exception.getMessage().isBlank() ? "来源收取失败" : exception.getMessage()
        );
        if (exception instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        throw new IllegalStateException(exception.getMessage(), exception);
    }

    /**
     * 构建运行日志实体，收口运行日志的持久化字段装配逻辑。
     */
    private TransferRunLog buildRunLog(TransferSource source,
                                       String transferId,
                                       String routeId,
                                       String triggerType,
                                       String runStage,
                                       String runStatus,
                                       String logMessage,
                                       Throwable error) {
        return new TransferRunLog(
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
        );
    }

    private boolean isAttachmentMaterializeFailed(RecognitionContext context) {
        if (context == null || context.attributes() == null) {
            return false;
        }
        Object failed = context.attributes().get("attachmentMaterializeFailed");
        return failed != null && Boolean.parseBoolean(String.valueOf(failed));
    }

    private String buildAttachmentMaterializeFailedMessage(RecognitionContext context) {
        if (context == null) {
            return "邮件附件临时落盘失败";
        }
        String fileName = context.fileName();
        Object error = context.attributes() == null ? null : context.attributes().get("attachmentMaterializeError");
        if (error == null || String.valueOf(error).isBlank()) {
            return "邮件附件临时落盘失败，附件名=" + fileName;
        }
        return "邮件附件临时落盘失败，附件名=" + fileName + "，原因=" + error;
    }

    private void recordCheckpointItemIfNeeded(TransferSource source,
                                              RecognitionContext context,
                                              String triggerType,
                                              Set<String> processedCheckpointKeys) {
        // 第 1 步：先判断当前文件是否具备记录检查点的最小条件。
        if (!canRecordCheckpoint(source, context)) {
            return;
        }
        // 第 2 步：计算稳定的检查点 key，避免同一对象重复写入检查点。
        String itemKey = resolveCheckpointKey(source, context);
        if (itemKey == null || itemKey.isBlank()) {
            return;
        }
        if (!processedCheckpointKeys.add(itemKey)) {
            return;
        }
        // 第 3 步：组装检查点 item，再按文件级处理事实写入。
        transferSourceCheckpointGateway.saveProcessedItem(buildCheckpointItem(source, context, triggerType, itemKey));
    }

    /**
     * 判断当前文件是否具备记录检查点的最小条件。
     */
    private boolean canRecordCheckpoint(TransferSource source, RecognitionContext context) {
        return source != null && source.sourceId() != null && context != null;
    }

    /**
     * 构建文件级检查点 item，集中封装检查点落库所需字段。
     */
    private TransferSourceCheckpointItem buildCheckpointItem(TransferSource source,
                                                             RecognitionContext context,
                                                             String triggerType,
                                                             String itemKey) {
        String checkpointFingerprint = resolveCheckpointFingerprint(source, context);
        return new TransferSourceCheckpointItem(
                null,
                source.sourceId(),
                source.sourceType() == null ? null : source.sourceType().name(),
                itemKey,
                resolveCheckpointRef(source, context),
                resolveCheckpointName(context),
                context.fileSize(),
                context.mimeType(),
                normalizeCheckpointFingerprint(checkpointFingerprint),
                buildCheckpointMeta(source, context),
                triggerType,
                Instant.now(),
                Instant.now(),
                Instant.now()
        );
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
        return resolveCheckpointKeyBySourceType(sourceType, context);
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
        return resolveCheckpointRefBySourceType(sourceType, context);
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

    /**
     * 将检查点明细指纹压缩到固定长度，避免原始路径或长文本写爆数据库列。
     */
    private String normalizeCheckpointFingerprint(String fingerprint) {
        if (fingerprint == null || fingerprint.isBlank()) {
            return null;
        }
        return shortHash(fingerprint);
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

    /**
     * 按来源类型解析检查点 key，把分支判断从主流程中收口出去。
     */
    private String resolveCheckpointKeyBySourceType(SourceType sourceType, RecognitionContext context) {
        return switch (sourceType) {
            case EMAIL -> context.mailId();
            case LOCAL_DIR -> resolveLocalDirectoryCheckpointKey(context);
            case S3 -> resolveS3CheckpointKey(context);
            case SFTP -> resolveSftpCheckpointKey(context);
            case HTTP -> resolveHttpCheckpointKey(context);
        };
    }

    /**
     * 按来源类型解析检查点引用，把分支判断从主流程中收口出去。
     */
    private String resolveCheckpointRefBySourceType(SourceType sourceType, RecognitionContext context) {
        return switch (sourceType) {
            case EMAIL -> context.mailId();
            case LOCAL_DIR -> Objects.toString(attributeValue(context, "absolutePath"), context.path());
            case S3 -> Objects.toString(attributeValue(context, TransferConfigKeys.REMOTE_PATH), context.path());
            case SFTP -> Objects.toString(attributeValue(context, TransferConfigKeys.REMOTE_PATH), context.path());
            case HTTP -> resolveHttpCheckpointRef(context);
        };
    }

    /**
     * 构建本地目录来源的检查点 key。
     */
    private String resolveLocalDirectoryCheckpointKey(RecognitionContext context) {
        return String.join("|",
                Objects.toString(attributeValue(context, "absolutePath"), Objects.toString(context.path(), "")),
                Objects.toString(attributeValue(context, "lastModified"), ""),
                Objects.toString(context.fileSize(), ""));
    }

    /**
     * 构建 S3 来源的检查点 key。
     */
    private String resolveS3CheckpointKey(RecognitionContext context) {
        return String.join("|",
                Objects.toString(attributeValue(context, TransferConfigKeys.BUCKET), ""),
                Objects.toString(attributeValue(context, TransferConfigKeys.OBJECT_KEY), ""),
                Objects.toString(attributeValue(context, TransferConfigKeys.E_TAG), ""),
                Objects.toString(context.fileSize(), ""));
    }

    /**
     * 构建 SFTP 来源的检查点 key。
     */
    private String resolveSftpCheckpointKey(RecognitionContext context) {
        return String.join("|",
                Objects.toString(attributeValue(context, TransferConfigKeys.REMOTE_PATH), Objects.toString(context.path(), "")),
                Objects.toString(attributeValue(context, "lastModified"), ""),
                Objects.toString(context.fileSize(), ""));
    }

    /**
     * 构建 HTTP 来源的检查点 key。
     */
    private String resolveHttpCheckpointKey(RecognitionContext context) {
        return String.join("|",
                Objects.toString(attributeValue(context, "absolutePath"), Objects.toString(context.path(), "")),
                Objects.toString(attributeValue(context, "lastModified"), ""),
                Objects.toString(context.fileSize(), ""));
    }

    /**
     * 构建 HTTP 来源的检查点引用。
     */
    private String resolveHttpCheckpointRef(RecognitionContext context) {
        return Objects.toString(attributeValue(context, "absolutePath"), context.path());
    }

    private void recordCheckpointCursorIfNeeded(TransferSource source, RecognitionContext context, String triggerType) {
        // 第 1 步：先确认当前对象满足记录游标的最小条件。
        if (!canRecordCheckpoint(source, context)) {
            return;
        }
        // 第 2 步：确认当前对象确实有可复用的检查点值。
        String checkpointValue = resolveCheckpointKey(source, context);
        if (checkpointValue == null || checkpointValue.isBlank()) {
            return;
        }
        // 第 3 步：将检查点写成游标，供下一轮收取继续从断点恢复。
        transferSourceCheckpointGateway.saveCheckpoint(buildCheckpointCursor(source, context, triggerType, checkpointValue));
    }

    /**
     * 构建检查点游标元数据，集中封装断点恢复时需要的附加信息。
     */
    private Map<String, Object> buildCheckpointCursorMeta(TransferSource source,
                                                          RecognitionContext context,
                                                          String triggerType,
                                                          String checkpointValue) {
        Map<String, Object> checkpointMeta = new LinkedHashMap<>();
        checkpointMeta.put(TransferConfigKeys.SOURCE_TYPE, source.sourceType() == null ? null : source.sourceType().name());
        checkpointMeta.put(TransferConfigKeys.SOURCE_CODE, source.sourceCode());
        checkpointMeta.put(TransferConfigKeys.CHECKPOINT_KEY, checkpointValue);
        checkpointMeta.put(TransferConfigKeys.CHECKPOINT_REF, resolveCheckpointRef(source, context));
        checkpointMeta.put(TransferConfigKeys.CHECKPOINT_NAME, resolveCheckpointName(context));
        checkpointMeta.put(TransferConfigKeys.CHECKPOINT_FINGERPRINT, resolveCheckpointFingerprint(source, context));
        checkpointMeta.put(TransferConfigKeys.TRIGGER_TYPE, triggerType);
        checkpointMeta.put("processedAt", Instant.now().toString());
        return checkpointMeta;
    }

    /**
     * 触发对象入库后的路由处理，把下游调度调用从主流程中收口出去。
     */
    private void triggerRouteAfterIngest(TransferObject savedTransfer) throws Exception {
        transferJobSchedulerProvider.getObject().triggerRoute(savedTransfer.transferId());
    }

    /**
     * 统一输出对象入库后的路由准备日志，避免主流程里分支重复。
     */
    private void logReadyForRoute(TransferSource source, TransferObject savedTransfer) {
        if (source.sourceType() == com.yss.valset.transfer.domain.model.SourceType.EMAIL) {
            log.info("邮件附件已直转存到统一临时目录，准备触发路由，transferId={}，sourceId={}，sourceCode={}，originalName={}，localTempPath={}",
                    savedTransfer.transferId(),
                    source.sourceId(),
                    source.sourceCode(),
                    savedTransfer.originalName(),
                    savedTransfer.localTempPath());
            return;
        }
        log.info("来源文件已转存到统一临时目录，准备触发路由，transferId={}，sourceId={}，sourceCode={}，originalName={}，localTempPath={}",
                savedTransfer.transferId(),
                source.sourceId(),
                source.sourceCode(),
                savedTransfer.originalName(),
                savedTransfer.localTempPath());
    }

    /**
     * 构建检查点游标对象，避免在外层散落游标字段装配逻辑。
     */
    private TransferSourceCheckpoint buildCheckpointCursor(TransferSource source,
                                                           RecognitionContext context,
                                                           String triggerType,
                                                           String checkpointValue) {
        return new TransferSourceCheckpoint(
                null,
                source.sourceId(),
                source.sourceType() == null ? null : source.sourceType().name(),
                TransferConfigKeys.CHECKPOINT_SCAN_CURSOR,
                checkpointValue,
                buildCheckpointCursorMeta(source, context, triggerType, checkpointValue),
                Instant.now(),
                Instant.now()
        );
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
