package com.yss.valset.transfer.application.impl;

import com.yss.valset.transfer.application.port.RouteTransferUseCase;
import com.yss.valset.transfer.domain.gateway.TransferObjectGateway;
import com.yss.valset.transfer.domain.gateway.TransferRunLogGateway;
import com.yss.valset.transfer.domain.gateway.TransferSourceGateway;
import com.yss.valset.transfer.domain.model.MatchResult;
import com.yss.valset.transfer.domain.model.ProbeResult;
import com.yss.valset.transfer.domain.model.RecognitionContext;
import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.domain.model.TransferRunLog;
import com.yss.valset.transfer.domain.model.TransferRunStage;
import com.yss.valset.transfer.domain.model.TransferRunStatus;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferRoute;
import com.yss.valset.transfer.domain.model.TransferStatus;
import com.yss.valset.transfer.domain.model.TransferSource;
import com.yss.valset.transfer.domain.model.config.TransferConfigKeys;
import com.yss.valset.transfer.application.port.TransferJobScheduler;
import com.yss.valset.transfer.infrastructure.connector.SourceConnectorRegistry;
import com.yss.valset.transfer.infrastructure.plugin.FileProbePluginRegistry;
import com.yss.valset.transfer.infrastructure.plugin.RouteMatchPluginRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Map;

/**
 * 默认文件路由应用服务。
 */
@Slf4j
@Service
public class DefaultRouteTransferService implements RouteTransferUseCase {

    private final TransferObjectGateway transferObjectGateway;
    private final FileProbePluginRegistry fileProbePluginRegistry;
    private final RouteMatchPluginRegistry routeMatchPluginRegistry;
    private final TransferRunLogGateway transferRunLogGateway;
    private final ObjectProvider<TransferJobScheduler> transferJobSchedulerProvider;
    private final TransferSourceGateway transferSourceGateway;
    private final SourceConnectorRegistry sourceConnectorRegistry;
    private final String uploadRoot;

    public DefaultRouteTransferService(
            TransferObjectGateway transferObjectGateway,
            FileProbePluginRegistry fileProbePluginRegistry,
            RouteMatchPluginRegistry routeMatchPluginRegistry,
            TransferRunLogGateway transferRunLogGateway,
            ObjectProvider<TransferJobScheduler> transferJobSchedulerProvider,
            TransferSourceGateway transferSourceGateway,
            SourceConnectorRegistry sourceConnectorRegistry,
            @Value("${subject.match.upload-dir:${user.home}/.tmp/valset-standardizer/uploads}") String uploadRoot
    ) {
        this.transferObjectGateway = transferObjectGateway;
        this.fileProbePluginRegistry = fileProbePluginRegistry;
        this.routeMatchPluginRegistry = routeMatchPluginRegistry;
        this.transferRunLogGateway = transferRunLogGateway;
        this.transferJobSchedulerProvider = transferJobSchedulerProvider;
        this.transferSourceGateway = transferSourceGateway;
        this.sourceConnectorRegistry = sourceConnectorRegistry;
        this.uploadRoot = resolveUploadRoot(uploadRoot);
    }

    @Override
    public void execute(String transferId) {
        TransferObject transferObject = transferObjectGateway.findById(transferId)
                .orElseThrow(() -> new IllegalStateException("未找到文件记录，transferId=" + transferId));
        String triggerType = resolveTriggerType(transferObject.fileMeta());
        boolean routeFailureLogged = false;
        try {
            log.info("开始文件路由识别，transferId={}，sourceId={}，sourceCode={}，sourceType={}，originalName={}，localTempPath={}",
                    transferObject.transferId(),
                    transferObject.sourceId(),
                    transferObject.sourceCode(),
                    transferObject.sourceType(),
                    transferObject.originalName(),
                    transferObject.localTempPath());
            saveRunLog(
                    transferObject,
                    transferObject.routeId(),
                    triggerType,
                    TransferRunStage.ROUTE.name(),
                    TransferRunStatus.SUCCESS.name(),
                    "开始路由识别，transferId=" + transferId
                            + "，sourceId=" + transferObject.sourceId()
                            + "，sourceCode=" + transferObject.sourceCode()
                            + "，originalName=" + transferObject.originalName(),
                    null
            );
            RecognitionContext context = toRecognitionContext(transferObject);
            ProbeResult probeResult = resolveProbeResult(transferObject, context);
            log.info("文件探测信息已准备完成，transferId={}，detectedType={}，detected={}",
                    transferObject.transferId(),
                    probeResult == null ? null : probeResult.detectedType(),
                    probeResult != null && probeResult.detected());
            MatchResult matchResult = routeMatchPluginRegistry.getRequired(context).match(context, probeResult);
            if (matchResult.routes() == null || matchResult.routes().isEmpty()) {
                TransferObject skippedTransfer = transferObject.withStatus(
                        TransferStatus.SKIPPED,
                        "未匹配到可用的分拣规则"
                );
                transferObjectGateway.save(skippedTransfer);
                saveRunLog(
                        skippedTransfer,
                        null,
                        triggerType,
                        TransferRunStage.ROUTE.name(),
                        TransferRunStatus.SUCCESS.name(),
                        "未匹配到可用的分拣规则，transferId=" + transferId
                                + "，sourceId=" + skippedTransfer.sourceId()
                                + "，sourceCode=" + skippedTransfer.sourceCode()
                                + "，reason=" + matchResult.reason(),
                        null
                );
                return;
            }
            transferObject = ensureMaterialized(transferObject);
            TransferRoute primaryRoute = matchResult.routes().get(0);
            log.info("路由规则命中，transferId={}，routeCount={}，primaryRouteId={}，ruleId={}，targetCode={}，reason={}",
                    transferObject.transferId(),
                    matchResult.routes().size(),
                    primaryRoute.routeId(),
                    primaryRoute.ruleId(),
                    primaryRoute.targetCode(),
                    matchResult.reason());
            transferObject = transferObjectGateway.save(
                    transferObject.withRouteId(primaryRoute.routeId()).withStatus(TransferStatus.IDENTIFIED, null)
            );
            for (TransferRoute route : matchResult.routes()) {
                try {
                    log.info("准备触发文件投递，transferId={}，routeId={}，ruleId={}，targetCode={}，targetType={}",
                            transferObject.transferId(),
                            route.routeId(),
                            route.ruleId(),
                            route.targetCode(),
                            route.targetType());
                    transferJobSchedulerProvider.getObject().triggerDeliver(route.routeId(), transferObject.transferId());
                } catch (Exception exception) {
                    routeFailureLogged = true;
                    saveRunLog(
                            transferObject,
                            route.routeId(),
                            triggerType,
                            TransferRunStage.ROUTE.name(),
                            TransferRunStatus.FAILED.name(),
                            "自动触发投递失败，routeId=" + route.routeId()
                                    + "，ruleId=" + route.ruleId()
                                    + "，targetCode=" + route.targetCode(),
                            exception
                    );
                    throw exception;
                }
                saveRunLog(
                        transferObject,
                        route.routeId(),
                        triggerType,
                        TransferRunStage.ROUTE.name(),
                        TransferRunStatus.SUCCESS.name(),
                        "路由识别完成并已触发投递，routeId=" + route.routeId()
                                + "，ruleId=" + route.ruleId()
                                + "，targetCode=" + route.targetCode()
                                + "，matchedRouteCount=" + matchResult.routes().size(),
                        null
                );
            }
        } catch (Exception exception) {
            if (!routeFailureLogged) {
                saveRunLog(
                        transferObject,
                        null,
                        triggerType,
                        TransferRunStage.ROUTE.name(),
                        TransferRunStatus.FAILED.name(),
                        "路由识别失败，transferId=" + transferId,
                        exception
                );
            }
            throw exception;
        }
    }

    private TransferObject ensureMaterialized(TransferObject transferObject) {
        if (transferObject == null) {
            throw new IllegalStateException("文件对象为空，无法落盘");
        }
        if (transferObject.localTempPath() != null && !transferObject.localTempPath().isBlank()) {
            return transferObject;
        }
        if (transferObject.sourceId() == null || transferObject.sourceId().isBlank()) {
            throw new IllegalStateException("文件对象缺少 sourceId，无法按需落盘");
        }
        TransferSource source = transferSourceGateway.findById(transferObject.sourceId())
                .orElseThrow(() -> new IllegalStateException("未找到来源记录，sourceId=" + transferObject.sourceId()));
        if (source.sourceType() != SourceType.EMAIL) {
            throw new IllegalStateException("当前文件对象缺少本地临时文件路径，且来源不支持兼容性落盘，sourceType=" + source.sourceType());
        }
        log.warn("历史文件对象缺少本地临时路径，执行兼容性落盘，transferId={}，sourceId={}，sourceCode={}，sourceType={}",
                transferObject.transferId(),
                transferObject.sourceId(),
                transferObject.sourceCode(),
                source.sourceType());
        Path tempPath = sourceConnectorRegistry.getRequired(source).materialize(source, transferObject);
        if (tempPath == null) {
            throw new IllegalStateException("文件对象按需落盘失败，未获得临时文件路径，transferId=" + transferObject.transferId());
        }
        Path directory = Path.of(uploadRoot).toAbsolutePath().resolve(LocalDate.now().toString());
        try {
            Files.createDirectories(directory);
            Path storedPath = directory.resolve(resolveStoredFilename(transferObject.originalName()));
            Files.move(tempPath, storedPath, StandardCopyOption.REPLACE_EXISTING);
            TransferObject materialized = transferObject.withLocalTempPath(storedPath.toAbsolutePath().toString());
            log.info("文件对象按需落盘并转存到统一临时目录完成，transferId={}，sourceTempPath={}，storedPath={}",
                    transferObject.transferId(),
                    tempPath,
                    storedPath);
            return transferObjectGateway.save(materialized);
        } catch (Exception exception) {
            throw new IllegalStateException("文件对象按需落盘并转存到统一临时目录失败，transferId=" + transferObject.transferId(), exception);
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

    private RecognitionContext toRecognitionContext(TransferObject transferObject) {
        Map<String, Object> fileMeta = transferObject.fileMeta() == null ? Map.of() : transferObject.fileMeta();
        return new RecognitionContext(
                resolveSourceType(transferObject, fileMeta),
                resolveSourceCode(transferObject, fileMeta),
                transferObject.originalName(),
                transferObject.mimeType(),
                transferObject.sizeBytes(),
                transferObject.mailFrom(),
                transferObject.mailTo(),
                transferObject.mailCc(),
                transferObject.mailBcc(),
                transferObject.mailSubject(),
                transferObject.mailBody(),
                transferObject.mailId(),
                transferObject.mailProtocol(),
                transferObject.mailFolder(),
                transferObject.localTempPath(),
                fileMeta
        );
    }

    private ProbeResult resolveProbeResult(TransferObject transferObject, RecognitionContext context) {
        if (transferObject != null && transferObject.probeResult() != null) {
            return transferObject.probeResult();
        }
        Map<String, Object> fileMeta = context == null || context.attributes() == null ? Map.of() : context.attributes();
        Object detectedFlag = fileMeta.get(TransferConfigKeys.PROBE_DETECTED);
        Object detectedType = fileMeta.get(TransferConfigKeys.PROBE_DETECTED_TYPE);
        Object probeAttributes = fileMeta.get(TransferConfigKeys.PROBE_ATTRIBUTES);
        if (detectedFlag != null || detectedType != null || probeAttributes != null) {
            return new ProbeResult(
                    detectedFlag == null || Boolean.parseBoolean(String.valueOf(detectedFlag)),
                    detectedType == null ? null : String.valueOf(detectedType),
                    asAttributesMap(probeAttributes)
            );
        }
        return fileProbePluginRegistry.getRequired(context).probe(context);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asAttributesMap(Object value) {
        if (value == null) {
            return Map.of();
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new java.util.LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return result;
        }
        return Map.of();
    }

    private SourceType resolveSourceType(TransferObject transferObject, Map<String, Object> fileMeta) {
        if (transferObject.sourceType() != null && !transferObject.sourceType().isBlank()) {
            try {
                return SourceType.valueOf(transferObject.sourceType());
            } catch (IllegalArgumentException ignored) {
                // 继续使用文件元数据兜底。
            }
        }
        Object raw = fileMeta.get(TransferConfigKeys.SOURCE_TYPE);
        if (raw == null) {
            return null;
        }
        try {
            return SourceType.valueOf(String.valueOf(raw));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String resolveSourceCode(TransferObject transferObject, Map<String, Object> fileMeta) {
        if (transferObject.sourceCode() != null && !transferObject.sourceCode().isBlank()) {
            return transferObject.sourceCode();
        }
        Object raw = fileMeta.get(TransferConfigKeys.SOURCE_CODE);
        return raw == null ? null : String.valueOf(raw);
    }

    private String resolveTriggerType(Map<String, Object> fileMeta) {
        Object raw = fileMeta == null ? null : fileMeta.get(TransferConfigKeys.TRIGGER_TYPE);
        if (raw == null || String.valueOf(raw).isBlank()) {
            return null;
        }
        return String.valueOf(raw).trim().toUpperCase();
    }

    private void saveRunLog(TransferObject transferObject,
                            String routeId,
                            String triggerType,
                            String runStage,
                            String runStatus,
                            String logMessage,
                            Throwable error) {
        if (error == null) {
            log.info("文件路由运行日志，stage={}，status={}，sourceId={}，transferId={}，routeId={}，message={}",
                    runStage,
                    runStatus,
                    transferObject == null ? null : transferObject.sourceId(),
                    transferObject == null ? null : transferObject.transferId(),
                    routeId,
                    logMessage);
        } else {
            log.error("文件路由运行日志，stage={}，status={}，sourceId={}，transferId={}，routeId={}，message={}，error={}",
                    runStage,
                    runStatus,
                    transferObject == null ? null : transferObject.sourceId(),
                    transferObject == null ? null : transferObject.transferId(),
                    routeId,
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
                routeId,
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
