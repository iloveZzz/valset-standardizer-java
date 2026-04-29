package com.yss.valset.analysis.application.impl.management;

import com.yss.cloud.dto.response.PageResult;
import com.yss.valset.analysis.application.command.ParseQueueBackfillCommand;
import com.yss.valset.analysis.application.command.ParseQueueCompleteCommand;
import com.yss.valset.analysis.application.command.ParseQueueFailCommand;
import com.yss.valset.analysis.application.command.ParseQueueGenerateCommand;
import com.yss.valset.analysis.application.command.ParseQueueQueryCommand;
import com.yss.valset.analysis.application.command.ParseQueueRetryCommand;
import com.yss.valset.analysis.application.command.ParseQueueSubscribeCommand;
import com.yss.valset.analysis.application.dto.ParseQueueViewDTO;
import com.yss.valset.analysis.application.service.ParseQueueManagementAppService;
import com.yss.valset.analysis.domain.gateway.ParseQueueGateway;
import com.yss.valset.analysis.domain.model.ParseQueue;
import com.yss.valset.analysis.domain.model.ParseQueuePage;
import com.yss.valset.analysis.domain.model.ParseStatus;
import com.yss.valset.analysis.domain.model.ParseTriggerMode;
import com.yss.valset.transfer.application.port.TransferParseQueueProvisionUseCase;
import com.yss.valset.transfer.domain.gateway.TransferDeliveryGateway;
import com.yss.valset.transfer.domain.gateway.TransferObjectGateway;
import com.yss.valset.transfer.domain.gateway.TransferObjectTagGateway;
import com.yss.valset.transfer.domain.model.TransferDeliveryRecord;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferObjectTag;
import com.yss.valset.transfer.domain.model.TransferStatus;
import com.yss.valset.transfer.infrastructure.convertor.TransferJsonMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 默认待解析任务管理服务。
 */
@Service
@RequiredArgsConstructor
public class DefaultParseQueueManagementAppService implements ParseQueueManagementAppService, TransferParseQueueProvisionUseCase {

    private static final String VALUATION_TAG_NAME = "估值表";
    private static final String VALUATION_TAG_CODE = "VALUATION_TABLE";

    private final ParseQueueGateway transferParseQueueGateway;
    private final TransferObjectGateway transferObjectGateway;
    private final TransferObjectTagGateway transferObjectTagGateway;
    private final TransferDeliveryGateway transferDeliveryGateway;
    private final TransferJsonMapper transferJsonMapper;

    @Override
    public PageResult<ParseQueueViewDTO> pageQueues(ParseQueueQueryCommand query) {
        ParseQueuePage page = transferParseQueueGateway.pageQueues(
                query == null ? null : query.getTransferId(),
                query == null ? null : query.getBusinessKey(),
                query == null ? null : query.getSourceCode(),
                query == null ? null : query.getRouteId(),
                query == null ? null : query.getTagCode(),
                normalizeFileStatus(query == null ? null : query.getFileStatus()),
                normalizeDeliveryStatus(query == null ? null : query.getDeliveryStatus()),
                normalizeParseStatus(query == null ? null : query.getParseStatus()),
                normalizeTriggerMode(query == null ? null : query.getTriggerMode()),
                query == null ? null : query.getPageIndex(),
                query == null ? null : query.getPageSize()
        );
        List<ParseQueueViewDTO> records = page.records() == null ? List.of() : page.records().stream().map(this::toView).toList();
        return PageResult.of(records, page.total(), page.pageSize(), page.pageIndex());
    }

    @Override
    public ParseQueueViewDTO getQueue(String queueId) {
        return toView(transferParseQueueGateway.findById(queueId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到待解析任务，queueId=" + queueId)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ParseQueueViewDTO generateQueue(ParseQueueGenerateCommand command) {
        TransferObject transferObject = loadEligibleTransferObject(command == null ? null : command.getTransferId(), command == null ? null : command.getSourceId(), command == null ? null : command.getRouteId());
        TransferDeliveryRecord deliveryRecord = loadLatestDeliveryRecord(transferObject.transferId());
        TransferObjectTag valuationTag = resolveValuationTag(transferObject.transferId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "文件未命中估值表标签，transferId=" + transferObject.transferId()));
        ParseQueue queue = generateOrUpdateQueue(transferObject,
                deliveryRecord,
                valuationTag,
                normalizeBusinessKey(command == null ? null : command.getBusinessKey(), transferObject.transferId(), valuationTag),
                ParseTriggerMode.MANUAL,
                Boolean.TRUE.equals(command == null ? null : command.getForceRebuild()),
                false);
        return toView(queue);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ParseQueueViewDTO> backfillQueues(ParseQueueBackfillCommand command) {
        String fileStatus = safeDefault(normalizeFileStatus(command == null ? null : command.getStatus()), "IDENTIFIED");
        String deliveryStatus = safeDefault(normalizeDeliveryStatus(command == null ? null : command.getDeliveryStatus()), "DELIVERED");
        String parseStatus = normalizeParseStatus(command == null ? null : command.getParseStatus());
        String sourceId = command == null ? null : command.getSourceId();
        String sourceCode = command == null ? null : command.getSourceCode();
        String routeId = command == null ? null : command.getRouteId();
        List<TransferObject> candidates = command != null && StringUtils.hasText(command.getTransferId())
                ? transferObjectGateway.findById(command.getTransferId()).map(List::of).orElse(List.of())
                : transferObjectGateway.listParseQueueCandidates(sourceId, sourceCode, routeId, fileStatus, deliveryStatus, null);
        if (candidates.isEmpty()) {
            return List.of();
        }
        Map<String, TransferDeliveryRecord> deliveryRecordMap = loadLatestDeliveryRecordMap(candidates);
        Map<String, TransferObjectTag> valuationTagMap = loadValuationTagMap(candidates);
        boolean dryRun = Boolean.TRUE.equals(command == null ? null : command.getDryRun());
        boolean forceRebuild = Boolean.TRUE.equals(command == null ? null : command.getForceRebuild());
        List<ParseQueueViewDTO> results = new ArrayList<>();
        for (TransferObject candidate : candidates) {
            if (!isEligible(candidate, fileStatus, deliveryStatus)) {
                continue;
            }
            TransferObjectTag valuationTag = valuationTagMap.get(candidate.transferId());
            if (valuationTag == null) {
                continue;
            }
            String businessKey = normalizeBusinessKey(null, candidate.transferId(), valuationTag);
            if (StringUtils.hasText(parseStatus)) {
                ParseQueue existingQueue = transferParseQueueGateway.findByBusinessKey(businessKey).orElse(null);
                if (existingQueue == null || !parseStatus.equalsIgnoreCase(enumName(existingQueue.parseStatus()))) {
                    continue;
                }
            }
            TransferDeliveryRecord deliveryRecord = deliveryRecordMap.get(candidate.transferId());
            if (dryRun) {
                results.add(toPreviewView(candidate, deliveryRecord, valuationTag, businessKey));
                continue;
            }
            ParseQueue saved = generateOrUpdateQueue(candidate,
                    deliveryRecord,
                    valuationTag,
                    businessKey,
                    ParseTriggerMode.MANUAL,
                    forceRebuild,
                    false);
            if (saved != null) {
                results.add(toView(saved));
            }
        }
        return results;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ParseQueueViewDTO subscribeQueue(String queueId, ParseQueueSubscribeCommand command) {
        ParseQueue queue = loadQueue(queueId);
        if (queue.parseStatus() != ParseStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "只有待订阅事件才能接管，queueId=" + queueId + "，当前状态=" + queue.parseStatus());
        }
        String subscribedBy = subscribeName(command);
        if (!transferParseQueueGateway.subscribeIfPending(queueId, subscribedBy, Instant.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "待订阅事件已被其他观察者接管，queueId=" + queueId);
        }
        return getQueue(queueId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ParseQueueViewDTO completeQueue(String queueId, ParseQueueCompleteCommand command) {
        ParseQueue queue = loadQueue(queueId);
        if (queue.parseStatus() != ParseStatus.PARSING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "只有解析中的任务才能完成，queueId=" + queueId + "，当前状态=" + queue.parseStatus());
        }
        String parseResultJson = transferJsonMapper.toJson(command == null ? null : command.getParseResultJson());
        ParseQueue next = queueWith(queue,
                ParseStatus.PARSED,
                queue.triggerMode(),
                queue.retryCount(),
                queue.subscribedBy(),
                queue.subscribedAt(),
                Instant.now(),
                null,
                queue.objectSnapshotJson(),
                queue.deliverySnapshotJson(),
                queue.parseRequestJson(),
                parseResultJson);
        return toView(transferParseQueueGateway.save(next));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ParseQueueViewDTO failQueue(String queueId, ParseQueueFailCommand command) {
        ParseQueue queue = loadQueue(queueId);
        if (queue.parseStatus() != ParseStatus.PARSING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "只有解析中的任务才能标记失败，queueId=" + queueId + "，当前状态=" + queue.parseStatus());
        }
        int nextRetryCount = queue.retryCount() == null ? 1 : queue.retryCount() + 1;
        ParseQueue next = queueWith(queue,
                ParseStatus.FAILED,
                queue.triggerMode(),
                nextRetryCount,
                queue.subscribedBy(),
                queue.subscribedAt(),
                queue.parsedAt(),
                safeText(command == null ? null : command.getErrorMessage(), "结构化解析失败"),
                queue.objectSnapshotJson(),
                queue.deliverySnapshotJson(),
                queue.parseRequestJson(),
                queue.parseResultJson());
        return toView(transferParseQueueGateway.save(next));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ParseQueueViewDTO retryQueue(String queueId, ParseQueueRetryCommand command) {
        ParseQueue queue = loadQueue(queueId);
        boolean forceRebuild = Boolean.TRUE.equals(command == null ? null : command.getForceRebuild());
        if (!forceRebuild && queue.parseStatus() != ParseStatus.FAILED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "只有解析失败的任务才能重试，queueId=" + queueId + "，当前状态=" + queue.parseStatus());
        }
        ParseQueue next = queueWith(queue,
                ParseStatus.PENDING,
                queue.triggerMode(),
                forceRebuild ? (queue.retryCount() == null ? 1 : queue.retryCount() + 1) : queue.retryCount(),
                null,
                null,
                null,
                null,
                queue.objectSnapshotJson(),
                queue.deliverySnapshotJson(),
                queue.parseRequestJson(),
                queue.parseResultJson());
        return toView(transferParseQueueGateway.save(next));
    }

    @Override
    public void ensureAutoGenerated(TransferObject transferObject, TransferDeliveryRecord deliveryRecord) {
        if (transferObject == null || transferObject.transferId() == null) {
            return;
        }
        try {
            TransferObjectTag valuationTag = resolveValuationTag(transferObject.transferId()).orElse(null);
            if (valuationTag == null) {
                return;
            }
            generateOrUpdateQueue(
                    transferObject,
                    deliveryRecord,
                    valuationTag,
                    normalizeBusinessKey(null, transferObject.transferId(), valuationTag),
                    ParseTriggerMode.AUTO,
                    false,
                    true
            );
        } catch (RuntimeException exception) {
            throw exception;
        }
    }

    private ParseQueue generateOrUpdateQueue(TransferObject transferObject,
                                                     TransferDeliveryRecord deliveryRecord,
                                                     TransferObjectTag valuationTag,
                                                     String businessKey,
                                                     ParseTriggerMode triggerMode,
                                                     boolean forceRebuild,
                                                     boolean autoTrigger) {
        if (transferObject == null || transferObject.transferId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "文件主对象不能为空");
        }
        if (transferObject.status() != TransferStatus.IDENTIFIED) {
            if (autoTrigger) {
                return null;
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅已识别且已投递的文件才能生成待解析任务，transferId=" + transferObject.transferId());
        }
        if (valuationTag == null) {
            if (autoTrigger) {
                return null;
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "文件未命中估值表标签，transferId=" + transferObject.transferId());
        }
        if (deliveryRecord == null || !"SUCCESS".equalsIgnoreCase(deliveryRecord.executeStatus())) {
            if (autoTrigger) {
                return null;
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "文件尚未完成目标投递，transferId=" + transferObject.transferId());
        }
        String normalizedBusinessKey = StringUtils.hasText(businessKey) ? businessKey.trim() : normalizeBusinessKey(null, transferObject.transferId(), valuationTag);
        ParseQueue existing = transferParseQueueGateway.findByBusinessKey(normalizedBusinessKey).orElse(null);
        if (existing != null && !forceRebuild) {
            return existing;
        }
        Instant now = Instant.now();
        ParseQueue next = new ParseQueue(
                existing == null ? null : existing.queueId(),
                normalizedBusinessKey,
                transferObject.transferId(),
                transferObject.originalName(),
                transferObject.sourceId(),
                transferObject.sourceType(),
                transferObject.sourceCode(),
                transferObject.routeId(),
                deliveryRecord.deliveryId(),
                valuationTag.tagId(),
                valuationTag.tagCode(),
                valuationTag.tagName(),
                enumName(transferObject.status()),
                "DELIVERED",
                ParseStatus.PENDING,
                triggerMode,
                existing == null ? 0 : ((existing.retryCount() == null ? 0 : existing.retryCount()) + 1),
                null,
                null,
                null,
                null,
                safeJson(buildObjectSnapshot(transferObject, valuationTag, triggerMode, existing, forceRebuild)),
                safeJson(buildDeliverySnapshot(deliveryRecord)),
                safeJson(buildParseRequestSnapshot(transferObject, deliveryRecord, valuationTag, triggerMode, forceRebuild, normalizedBusinessKey)),
                null,
                existing == null ? now : existing.createdAt(),
                now
        );
        return transferParseQueueGateway.save(next);
    }

    private TransferObject loadEligibleTransferObject(String transferId, String sourceId, String routeId) {
        if (!StringUtils.hasText(transferId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "文件主键不能为空");
        }
        TransferObject transferObject = transferObjectGateway.findById(transferId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到文件主对象，transferId=" + transferId));
        if (StringUtils.hasText(sourceId) && (!StringUtils.hasText(transferObject.sourceId()) || !sourceId.trim().equals(transferObject.sourceId()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "来源主键不匹配，transferId=" + transferId);
        }
        if (StringUtils.hasText(routeId) && !routeId.trim().equals(transferObject.routeId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "路由主键不匹配，transferId=" + transferId);
        }
        return transferObject;
    }

    private TransferDeliveryRecord loadLatestDeliveryRecord(String transferId) {
        if (!StringUtils.hasText(transferId)) {
            return null;
        }
        return transferDeliveryGateway.listRecordsByTransferIds(List.of(transferId), "SUCCESS").stream().findFirst().orElse(null);
    }

    private Map<String, TransferDeliveryRecord> loadLatestDeliveryRecordMap(List<TransferObject> objects) {
        List<String> transferIds = objects == null ? List.of() : objects.stream()
                .map(TransferObject::transferId)
                .filter(StringUtils::hasText)
                .toList();
        if (transferIds.isEmpty()) {
            return Map.of();
        }
        Map<String, TransferDeliveryRecord> result = new LinkedHashMap<>();
        for (TransferDeliveryRecord record : transferDeliveryGateway.listRecordsByTransferIds(transferIds, "SUCCESS")) {
            if (record != null && StringUtils.hasText(record.transferId())) {
                result.putIfAbsent(record.transferId(), record);
            }
        }
        return result;
    }

    private Map<String, TransferObjectTag> loadValuationTagMap(List<TransferObject> objects) {
        List<String> transferIds = objects == null ? List.of() : objects.stream()
                .map(TransferObject::transferId)
                .filter(StringUtils::hasText)
                .toList();
        if (transferIds.isEmpty()) {
            return Map.of();
        }
        Map<String, TransferObjectTag> result = new LinkedHashMap<>();
        for (TransferObjectTag tag : transferObjectTagGateway.listByTransferIds(transferIds)) {
            if (!isValuationTag(tag) || !StringUtils.hasText(tag.transferId())) {
                continue;
            }
            result.putIfAbsent(tag.transferId(), tag);
        }
        return result;
    }

    private Optional<TransferObjectTag> resolveValuationTag(String transferId) {
        return transferObjectTagGateway.listByTransferId(transferId).stream()
                .filter(this::isValuationTag)
                .findFirst();
    }

    private boolean isValuationTag(TransferObjectTag tag) {
        if (tag == null) {
            return false;
        }
        return matchesValuationKey(tag.tagCode()) || matchesValuationKey(tag.tagName()) || matchesValuationKey(tag.tagValue());
    }

    private boolean matchesValuationKey(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.equals(VALUATION_TAG_CODE) || normalized.equals(VALUATION_TAG_NAME.toUpperCase(Locale.ROOT));
    }

    private boolean isEligible(TransferObject object, String fileStatus, String deliveryStatus) {
        if (object == null) {
            return false;
        }
        if (StringUtils.hasText(fileStatus) && !fileStatus.equalsIgnoreCase(enumName(object.status()))) {
            return false;
        }
        if (StringUtils.hasText(deliveryStatus)) {
            boolean delivered = loadLatestDeliveryRecord(object.transferId()) != null;
            if ("DELIVERED".equalsIgnoreCase(deliveryStatus) && !delivered) {
                return false;
            }
            if ("UNDELIVERED".equalsIgnoreCase(deliveryStatus) && delivered) {
                return false;
            }
        }
        return true;
    }

    private String normalizeBusinessKey(String requestedBusinessKey, String transferId, TransferObjectTag valuationTag) {
        if (StringUtils.hasText(requestedBusinessKey)) {
            return requestedBusinessKey.trim();
        }
        String suffix = valuationTag == null ? VALUATION_TAG_CODE : safeText(valuationTag.tagCode(), VALUATION_TAG_CODE);
        return transferId + ":" + suffix;
    }

    private ParseQueue queueWith(ParseQueue queue,
                                         ParseStatus parseStatus,
                                         ParseTriggerMode triggerMode,
                                         Integer retryCount,
                                         String subscribedBy,
                                         Instant subscribedAt,
                                         Instant parsedAt,
                                         String lastErrorMessage,
                                         String objectSnapshotJson,
                                         String deliverySnapshotJson,
                                         String parseRequestJson,
                                         String parseResultJson) {
        return new ParseQueue(
                queue.queueId(),
                queue.businessKey(),
                queue.transferId(),
                queue.originalName(),
                queue.sourceId(),
                queue.sourceType(),
                queue.sourceCode(),
                queue.routeId(),
                queue.deliveryId(),
                queue.tagId(),
                queue.tagCode(),
                queue.tagName(),
                queue.fileStatus(),
                queue.deliveryStatus(),
                parseStatus,
                triggerMode,
                retryCount,
                subscribedBy,
                subscribedAt,
                parsedAt,
                lastErrorMessage,
                objectSnapshotJson,
                deliverySnapshotJson,
                parseRequestJson,
                parseResultJson,
                queue.createdAt(),
                Instant.now()
        );
    }

    private ParseQueue loadQueue(String queueId) {
        return transferParseQueueGateway.findById(queueId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到待解析任务，queueId=" + queueId));
    }

    private ParseQueueViewDTO toView(ParseQueue queue) {
        if (queue == null) {
            return null;
        }
        return ParseQueueViewDTO.builder()
                .queueId(queue.queueId())
                .businessKey(queue.businessKey())
                .transferId(queue.transferId())
                .originalName(queue.originalName())
                .sourceId(queue.sourceId())
                .sourceType(queue.sourceType())
                .sourceCode(queue.sourceCode())
                .routeId(queue.routeId())
                .deliveryId(queue.deliveryId())
                .tagId(queue.tagId())
                .tagCode(queue.tagCode())
                .tagName(queue.tagName())
                .fileStatus(resolveFileStatusLabel(queue.fileStatus()))
                .deliveryStatus(resolveDeliveryStatusLabel(queue.deliveryStatus()))
                .parseStatus(enumName(queue.parseStatus()))
                .triggerMode(enumName(queue.triggerMode()))
                .retryCount(queue.retryCount())
                .subscribedBy(queue.subscribedBy())
                .subscribedAt(toLocalDateTime(queue.subscribedAt()))
                .parsedAt(toLocalDateTime(queue.parsedAt()))
                .lastErrorMessage(queue.lastErrorMessage())
                .objectSnapshotJson(queue.objectSnapshotJson())
                .deliverySnapshotJson(queue.deliverySnapshotJson())
                .parseRequestJson(queue.parseRequestJson())
                .parseResultJson(queue.parseResultJson())
                .createdAt(toLocalDateTime(queue.createdAt()))
                .updatedAt(toLocalDateTime(queue.updatedAt()))
                .build();
    }

    private ParseQueueViewDTO toPreviewView(TransferObject object,
                                                    TransferDeliveryRecord deliveryRecord,
                                                    TransferObjectTag valuationTag,
                                                    String businessKey) {
        ParseQueue preview = new ParseQueue(
                null,
                businessKey,
                object.transferId(),
                object.originalName(),
                object.sourceId(),
                object.sourceType(),
                object.sourceCode(),
                object.routeId(),
                deliveryRecord == null ? null : deliveryRecord.deliveryId(),
                valuationTag.tagId(),
                valuationTag.tagCode(),
                valuationTag.tagName(),
                enumName(object.status()),
                "DELIVERED",
                ParseStatus.PENDING,
                ParseTriggerMode.MANUAL,
                0,
                null,
                null,
                null,
                null,
                safeJson(buildObjectSnapshot(object, valuationTag, ParseTriggerMode.MANUAL, null, false)),
                safeJson(buildDeliverySnapshot(deliveryRecord)),
                safeJson(buildParseRequestSnapshot(object, deliveryRecord, valuationTag, ParseTriggerMode.MANUAL, false, businessKey)),
                null,
                null,
                null
        );
        return toView(preview);
    }

    private Map<String, Object> buildObjectSnapshot(TransferObject object,
                                                    TransferObjectTag valuationTag,
                                                    ParseTriggerMode triggerMode,
                                                    ParseQueue existing,
                                                    boolean forceRebuild) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("transferId", object == null ? null : object.transferId());
        snapshot.put("originalName", object == null ? null : object.originalName());
        snapshot.put("sourceId", object == null ? null : object.sourceId());
        snapshot.put("sourceType", object == null ? null : object.sourceType());
        snapshot.put("sourceCode", object == null ? null : object.sourceCode());
        snapshot.put("routeId", object == null ? null : object.routeId());
        snapshot.put("fileStatus", object == null || object.status() == null ? null : object.status().name());
        snapshot.put("deliveryStatus", "DELIVERED");
        snapshot.put("tagId", valuationTag == null ? null : valuationTag.tagId());
        snapshot.put("tagCode", valuationTag == null ? null : valuationTag.tagCode());
        snapshot.put("tagName", valuationTag == null ? null : valuationTag.tagName());
        snapshot.put("triggerMode", triggerMode == null ? null : triggerMode.name());
        snapshot.put("retryCount", existing == null ? 0 : existing.retryCount());
        snapshot.put("forceRebuild", forceRebuild);
        return snapshot;
    }

    private Map<String, Object> buildDeliverySnapshot(TransferDeliveryRecord deliveryRecord) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("deliveryId", deliveryRecord == null ? null : deliveryRecord.deliveryId());
        snapshot.put("routeId", deliveryRecord == null ? null : deliveryRecord.routeId());
        snapshot.put("transferId", deliveryRecord == null ? null : deliveryRecord.transferId());
        snapshot.put("targetCode", deliveryRecord == null ? null : deliveryRecord.targetCode());
        snapshot.put("executeStatus", deliveryRecord == null ? null : deliveryRecord.executeStatus());
        snapshot.put("retryCount", deliveryRecord == null ? null : deliveryRecord.retryCount());
        snapshot.put("requestSnapshotJson", deliveryRecord == null ? null : deliveryRecord.requestSnapshotJson());
        snapshot.put("responseSnapshotJson", deliveryRecord == null ? null : deliveryRecord.responseSnapshotJson());
        snapshot.put("errorMessage", deliveryRecord == null ? null : deliveryRecord.errorMessage());
        return snapshot;
    }

    private Map<String, Object> buildParseRequestSnapshot(TransferObject object,
                                                          TransferDeliveryRecord deliveryRecord,
                                                          TransferObjectTag valuationTag,
                                                          ParseTriggerMode triggerMode,
                                                          boolean forceRebuild,
                                                          String businessKey) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("businessKey", businessKey);
        snapshot.put("transferId", object == null ? null : object.transferId());
        snapshot.put("routeId", object == null ? null : object.routeId());
        snapshot.put("deliveryId", deliveryRecord == null ? null : deliveryRecord.deliveryId());
        snapshot.put("tagId", valuationTag == null ? null : valuationTag.tagId());
        snapshot.put("tagCode", valuationTag == null ? null : valuationTag.tagCode());
        snapshot.put("tagName", valuationTag == null ? null : valuationTag.tagName());
        snapshot.put("triggerMode", triggerMode == null ? null : triggerMode.name());
        snapshot.put("forceRebuild", forceRebuild);
        snapshot.put("source", forceRebuild ? "manual_rebuild" : "delivery_success");
        return snapshot;
    }

    private String resolveFileStatusLabel(String value) {
        String normalized = normalizeFileStatus(value);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        return switch (normalized) {
            case "IDENTIFIED" -> "已识别";
            default -> normalized;
        };
    }

    private String resolveDeliveryStatusLabel(String value) {
        String normalized = normalizeDeliveryStatus(value);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        return switch (normalized) {
            case "DELIVERED" -> "已投递";
            case "UNDELIVERED" -> "未投递";
            default -> normalized;
        };
    }

    private String normalizeFileStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "IDENTIFIED", "已识别" -> "IDENTIFIED";
            case "DELIVERED", "已投递" -> "DELIVERED";
            default -> normalized;
        };
    }

    private String normalizeDeliveryStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "DELIVERED", "SUCCESS", "已投递" -> "DELIVERED";
            case "UNDELIVERED", "FAILED", "NOT_DELIVERED", "未投递" -> "UNDELIVERED";
            default -> normalized;
        };
    }

    private String normalizeParseStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "PENDING", "待解析" -> "PENDING";
            case "PARSING", "解析中" -> "PARSING";
            case "PARSED", "已解析" -> "PARSED";
            case "FAILED", "解析失败" -> "FAILED";
            default -> normalized;
        };
    }

    private String normalizeTriggerMode(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "AUTO", "自动生成" -> "AUTO";
            case "MANUAL", "手工生成" -> "MANUAL";
            default -> normalized;
        };
    }

    private String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    private String subscribeName(ParseQueueSubscribeCommand command) {
        if (command == null || !StringUtils.hasText(command.getSubscribedBy())) {
            return "当前订阅者";
        }
        return command.getSubscribedBy().trim();
    }

    private String safeText(String value, String defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        return value.trim();
    }

    private String safeDefault(String value, String defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        return value;
    }

    private String safeJson(Object value) {
        return value == null ? null : transferJsonMapper.toJson(value);
    }
}
