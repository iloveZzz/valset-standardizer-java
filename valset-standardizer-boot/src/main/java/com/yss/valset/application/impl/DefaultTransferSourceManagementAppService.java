package com.yss.valset.application.impl;

import com.yss.valset.application.command.TransferSourceUpsertCommand;
import com.yss.valset.application.dto.TransferSourceCheckpointItemViewDTO;
import com.yss.valset.application.dto.TransferSourceCheckpointViewDTO;
import com.yss.valset.application.dto.TransferSourceMutationResponse;
import com.yss.valset.application.dto.TransferSourceViewDTO;
import com.yss.valset.application.service.TransferSourceManagementAppService;
import com.yss.valset.transfer.domain.form.TransferFormTemplateNames;
import com.yss.valset.transfer.domain.gateway.TransferRouteGateway;
import com.yss.valset.transfer.domain.gateway.TransferSourceCheckpointGateway;
import com.yss.valset.transfer.domain.gateway.TransferSourceGateway;
import com.yss.valset.transfer.domain.model.SourceType;
import com.yss.valset.transfer.domain.model.TransferSource;
import com.yss.valset.transfer.domain.model.TransferSourceCheckpoint;
import com.yss.valset.transfer.domain.model.TransferSourceCheckpointItem;
import com.yss.valset.transfer.application.port.TransferJobScheduler;
import com.yss.valset.transfer.infrastructure.convertor.TransferSecretCodec;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * 默认文件来源管理服务。
 */
@Service
@RequiredArgsConstructor
public class DefaultTransferSourceManagementAppService implements TransferSourceManagementAppService {

    private static final Logger log = LoggerFactory.getLogger(DefaultTransferSourceManagementAppService.class);
    private static final Duration INGEST_WARN_THRESHOLD = Duration.ofMinutes(5);

    private final TransferSourceGateway transferSourceGateway;
    private final TransferRouteGateway transferRouteGateway;
    private final TransferSourceCheckpointGateway transferSourceCheckpointGateway;
    private final TransferSecretCodec transferSecretCodec;
    private final TransferJobScheduler transferJobScheduler;

    @Override
    public List<TransferSourceViewDTO> listSources(String sourceType, String sourceCode, String sourceName, Boolean enabled, Integer limit) {
        return transferSourceGateway.listSources(sourceType, sourceCode, sourceName, enabled, limit)
                .stream()
                .map(this::toView)
                .toList();
    }

    @Override
    public TransferSourceViewDTO getSource(String sourceId) {
        return toView(cleanupExpiredIngestIfNeeded(transferSourceGateway.findById(sourceId)
                .orElseThrow(() -> new IllegalStateException("未找到文件来源，sourceId=" + sourceId))));
    }

    @Override
    public TransferSourceMutationResponse upsertSource(TransferSourceUpsertCommand command) {
        boolean createMode = command.getSourceId() == null;
        SourceType sourceType = SourceType.valueOf(command.getSourceType());
        TransferSource existing = command.getSourceId() == null ? null : transferSourceGateway.findById(command.getSourceId())
                .orElseThrow(() -> new IllegalStateException("未找到文件来源，sourceId=" + command.getSourceId()));
        Map<String, Object> connectionConfig = mergeSensitiveConfig(
                existing == null ? null : existing.connectionConfig(),
                command.getConnectionConfig()
        );
        Map<String, Object> sourceMeta = mergeConfig(
                existing == null ? null : existing.sourceMeta(),
                command.getSourceMeta()
        );
        TransferSource transferSource = new TransferSource(
                command.getSourceId(),
                command.getSourceCode(),
                command.getSourceName(),
                sourceType,
                Boolean.TRUE.equals(command.getEnabled()),
                command.getPollCron(),
                connectionConfig,
                sourceMeta,
                existing == null ? null : existing.ingestStatus(),
                existing == null ? null : existing.ingestStartedAt(),
                existing == null ? null : existing.ingestFinishedAt(),
                existing == null ? null : existing.createdAt(),
                existing == null ? null : existing.updatedAt()
        );
        TransferSource saved = transferSourceGateway.save(transferSource);
        syncIngestSchedule(saved);
        return TransferSourceMutationResponse.builder()
                .operation(createMode ? "create" : "update")
                .message("文件来源保存成功")
                .formTemplateName(TransferFormTemplateNames.sourceTemplateName(saved.sourceType()))
                .source(toView(saved))
                .build();
    }

    @Override
    public TransferSourceMutationResponse deleteSource(String sourceId) {
        TransferSource existing = transferSourceGateway.findById(sourceId)
                .orElseThrow(() -> new IllegalStateException("未找到文件来源，sourceId=" + sourceId));
        long referencedRouteCount = transferRouteGateway.countBySourceId(sourceId);
        if (referencedRouteCount > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "存在 " + referencedRouteCount + " 条分拣路由配置引用了该来源（sourceId=" + sourceId + "，sourceCode=" + existing.sourceCode() + "），请先解除路由配置后再删除"
            );
        }
        transferSourceGateway.deleteById(sourceId);
        transferSourceCheckpointGateway.deleteProcessedItemsBySourceId(sourceId);
        transferSourceCheckpointGateway.deleteCheckpointsBySourceId(sourceId);
        transferJobScheduler.unscheduleIngest(sourceId);
        return TransferSourceMutationResponse.builder()
                .operation("delete")
                .message("文件来源删除成功")
                .formTemplateName(TransferFormTemplateNames.sourceTemplateName(existing.sourceType()))
                .source(toView(existing))
                .build();
    }

    @Override
    public TransferSourceMutationResponse triggerSource(String sourceId) {
        TransferSource source = cleanupExpiredIngestIfNeeded(transferSourceGateway.findById(sourceId)
                .orElseThrow(() -> new IllegalStateException("未找到文件来源，sourceId=" + sourceId)));
        if (isIngestBusy(source)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, buildRunningMessage(source));
        }
        String ingestLockToken = UUID.randomUUID().toString();
        boolean locked = transferSourceGateway.tryAcquireIngestLock(source.sourceId(), ingestLockToken, Instant.now());
        if (!locked) {
            TransferSource current = cleanupExpiredIngestIfNeeded(transferSourceGateway.findById(sourceId).orElse(source));
            throw new ResponseStatusException(HttpStatus.CONFLICT, buildRunningMessage(current));
        }
        try {
            transferJobScheduler.triggerIngest(
                    source.sourceId(),
                    source.sourceType() == null ? null : source.sourceType().name(),
                    source.sourceCode(),
                    source.connectionConfig(),
                    ingestLockToken
            );
        } catch (RuntimeException exception) {
            transferSourceGateway.releaseIngestLock(source.sourceId(), ingestLockToken, Instant.now());
            throw exception;
        }
        TransferSource triggered = transferSourceGateway.findById(source.sourceId()).orElse(source);
        return TransferSourceMutationResponse.builder()
                .operation("trigger")
                .message("文件来源已触发执行")
                .formTemplateName(TransferFormTemplateNames.sourceTemplateName(source.sourceType()))
                .source(toView(triggered))
                .build();
    }

    @Override
    public TransferSourceMutationResponse stopSource(String sourceId) {
        TransferSource source = cleanupExpiredIngestIfNeeded(transferSourceGateway.findById(sourceId)
                .orElseThrow(() -> new IllegalStateException("未找到文件来源，sourceId=" + sourceId)));
        if (!isIngestBusy(source)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "来源当前未在收取中，无法停止（sourceId="
                    + source.sourceId() + "，sourceCode=" + source.sourceCode() + "）");
        }
        boolean stopped = transferSourceGateway.requestIngestStop(source.sourceId(), Instant.now());
        if (!stopped) {
            TransferSource current = cleanupExpiredIngestIfNeeded(transferSourceGateway.findById(sourceId).orElse(source));
        }
        TransferSource updated = cleanupExpiredIngestIfNeeded(transferSourceGateway.findById(sourceId).orElse(source));
        return TransferSourceMutationResponse.builder()
                .operation("stop")
                .message("文件来源停止请求已提交")
                .formTemplateName(TransferFormTemplateNames.sourceTemplateName(updated.sourceType()))
                .source(toView(updated))
                .build();
    }

    @Override
    public TransferSourceMutationResponse clearProcessedMailIds(String sourceId) {
        TransferSource source = transferSourceGateway.findById(sourceId)
                .orElseThrow(() -> new IllegalStateException("未找到文件来源，sourceId=" + sourceId));
        transferSourceCheckpointGateway.deleteProcessedItemsBySourceId(source.sourceId());
        transferSourceCheckpointGateway.deleteCheckpointsBySourceId(source.sourceId());
        TransferSource updated = transferSourceGateway.save(new TransferSource(
                source.sourceId(),
                source.sourceCode(),
                source.sourceName(),
                source.sourceType(),
                source.enabled(),
                source.pollCron(),
                source.connectionConfig(),
                source.sourceMeta(),
                source.ingestStatus(),
                source.ingestStartedAt(),
                source.ingestFinishedAt(),
                source.createdAt(),
                source.updatedAt()
        ));
        return TransferSourceMutationResponse.builder()
                .operation("clear-processed-mail-ids")
                .message("已清空来源检查点记录")
                .formTemplateName(TransferFormTemplateNames.sourceTemplateName(updated.sourceType()))
                .source(toView(updated))
                .build();
    }

    @Override
    public List<TransferSourceCheckpointViewDTO> listCheckpoints(String sourceId, Integer limit) {
        return transferSourceCheckpointGateway.listCheckpointsBySourceId(sourceId, limit)
                .stream()
                .map(this::toCheckpointView)
                .toList();
    }

    @Override
    public List<TransferSourceCheckpointItemViewDTO> listCheckpointItems(String sourceId, Integer limit) {
        return transferSourceCheckpointGateway.listProcessedItemsBySourceId(sourceId, limit)
                .stream()
                .map(this::toCheckpointItemView)
                .toList();
    }

    private boolean isIngestBusy(TransferSource source) {
        return source != null
                && ("RUNNING".equalsIgnoreCase(source.ingestStatus())
                || "STOPPING".equalsIgnoreCase(source.ingestStatus()));
    }

    private TransferSource cleanupExpiredIngestIfNeeded(TransferSource source) {
        if (source == null || source.ingestStartedAt() == null) {
            return source;
        }
        if (Duration.between(source.ingestStartedAt(), Instant.now()).compareTo(INGEST_WARN_THRESHOLD) >= 0
                && isIngestBusy(source)) {
            log.warn("来源收取超过 5 分钟仍未完成，sourceId={}，sourceCode={}，ingestStatus={}，ingestStartedAt={}",
                    source.sourceId(), source.sourceCode(), source.ingestStatus(), source.ingestStartedAt());
            boolean cleaned = transferSourceGateway.forceStopIngest(source.sourceId(), Instant.now());
            if (cleaned) {
                return transferSourceGateway.findById(source.sourceId()).orElse(source);
            }
        }
        return source;
    }

    private String buildRunningMessage(TransferSource source) {
        return "来源正在收取中，请等待当前收取完成后再触发（sourceId="
                + (source == null ? null : source.sourceId())
                + "，sourceCode="
                + (source == null ? null : source.sourceCode())
                + "）";
    }

    private void syncIngestSchedule(TransferSource source) {
        if (source == null || source.sourceId() == null) {
            return;
        }
        if (!source.enabled() || source.pollCron() == null || source.pollCron().isBlank()) {
            transferJobScheduler.unscheduleIngest(source.sourceId());
            return;
        }
        transferJobScheduler.scheduleIngestCron(
                source.sourceId(),
                source.sourceType() == null ? null : source.sourceType().name(),
                source.sourceCode(),
                source.connectionConfig(),
                source.pollCron()
        );
    }

    private TransferSourceViewDTO toView(TransferSource source) {
        TransferSource current = cleanupExpiredIngestIfNeeded(source);
        return TransferSourceViewDTO.builder()
                .sourceId(current.sourceId() == null ? null : String.valueOf(current.sourceId()))
                .sourceCode(current.sourceCode())
                .sourceName(current.sourceName())
                .sourceType(current.sourceType() == null ? null : current.sourceType().name())
                .formTemplateName(TransferFormTemplateNames.sourceTemplateName(current.sourceType()))
                .enabled(current.enabled())
                .pollCron(current.pollCron())
                .ingestStatus(current.ingestStatus())
                .ingestBusy(isIngestBusy(current))
                .ingestStartedAt(current.ingestStartedAt() == null ? null : LocalDateTime.ofInstant(current.ingestStartedAt(), ZoneId.systemDefault()))
                .ingestFinishedAt(current.ingestFinishedAt() == null ? null : LocalDateTime.ofInstant(current.ingestFinishedAt(), ZoneId.systemDefault()))
                .connectionConfig(transferSecretCodec.maskMap(current.connectionConfig()))
                .sourceMeta(transferSecretCodec.maskMap(current.sourceMeta()))
                .createdAt(current.createdAt() == null ? null : java.time.LocalDateTime.ofInstant(current.createdAt(), java.time.ZoneId.systemDefault()))
                .updatedAt(current.updatedAt() == null ? null : java.time.LocalDateTime.ofInstant(current.updatedAt(), java.time.ZoneId.systemDefault()))
                .build();
    }

    private Map<String, Object> mergeConfig(Map<String, Object> existing, Map<String, Object> incoming) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (existing != null && !existing.isEmpty()) {
            merged.putAll(existing);
        }
        if (incoming != null && !incoming.isEmpty()) {
            merged.putAll(incoming);
        }
        return merged;
    }

    private Map<String, Object> mergeSensitiveConfig(Map<String, Object> existing, Map<String, Object> incoming) {
        Map<String, Object> merged = mergeConfig(existing, incoming);
        if (existing == null || existing.isEmpty() || incoming == null || incoming.isEmpty()) {
            return merged;
        }
        for (String key : List.of("password", "accessKey", "secretKey", "passphrase")) {
            Object incomingValue = incoming.get(key);
            if (incomingValue == null || String.valueOf(incomingValue).isBlank()) {
                Object existingValue = existing.get(key);
                if (existingValue != null) {
                    merged.put(key, existingValue);
                }
            }
        }
        return merged;
    }

    private TransferSourceCheckpointViewDTO toCheckpointView(TransferSourceCheckpoint checkpoint) {
        if (checkpoint == null) {
            return null;
        }
        return TransferSourceCheckpointViewDTO.builder()
                .checkpointId(checkpoint.checkpointId())
                .sourceId(checkpoint.sourceId())
                .sourceType(checkpoint.sourceType())
                .checkpointKey(checkpoint.checkpointKey())
                .checkpointValue(checkpoint.checkpointValue())
                .checkpointMeta(checkpoint.checkpointMeta())
                .createdAt(toLocalDateTime(checkpoint.createdAt()))
                .updatedAt(toLocalDateTime(checkpoint.updatedAt()))
                .build();
    }

    private TransferSourceCheckpointItemViewDTO toCheckpointItemView(TransferSourceCheckpointItem item) {
        if (item == null) {
            return null;
        }
        return TransferSourceCheckpointItemViewDTO.builder()
                .checkpointItemId(item.checkpointItemId())
                .sourceId(item.sourceId())
                .sourceType(item.sourceType())
                .itemKey(item.itemKey())
                .itemRef(item.itemRef())
                .itemName(item.itemName())
                .itemSize(item.itemSize())
                .itemMimeType(item.itemMimeType())
                .itemFingerprint(item.itemFingerprint())
                .itemMeta(item.itemMeta())
                .triggerType(item.triggerType())
                .processedAt(toLocalDateTime(item.processedAt()))
                .createdAt(toLocalDateTime(item.createdAt()))
                .updatedAt(toLocalDateTime(item.updatedAt()))
                .build();
    }

    private LocalDateTime toLocalDateTime(java.time.Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
