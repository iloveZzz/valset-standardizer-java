package com.yss.valset.transfer.application.impl.query;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.cloud.dto.response.PageResult;
import com.yss.valset.transfer.application.dto.TransferObjectAnalysisViewDTO;
import com.yss.valset.transfer.application.dto.TransferObjectExtensionCountViewDTO;
import com.yss.valset.transfer.application.dto.TransferObjectMailFolderCountViewDTO;
import com.yss.valset.transfer.application.dto.TransferObjectTagViewDTO;
import com.yss.valset.transfer.application.dto.TransferObjectSourceAnalysisViewDTO;
import com.yss.valset.transfer.application.dto.TransferObjectStatusCountViewDTO;
import com.yss.valset.transfer.application.dto.TransferObjectSizeAnalysisViewDTO;
import com.yss.valset.transfer.application.dto.TransferObjectViewDTO;
import com.yss.valset.transfer.application.service.TransferObjectQueryService;
import com.yss.valset.transfer.domain.gateway.TransferObjectTagGateway;
import com.yss.valset.transfer.domain.gateway.TransferDeliveryGateway;
import com.yss.valset.transfer.domain.gateway.TransferObjectGateway;
import com.yss.valset.transfer.domain.model.TransferDeliveryRecord;
import com.yss.valset.transfer.domain.model.TransferObjectAnalysis;
import com.yss.valset.transfer.domain.model.TransferObjectExtensionCount;
import com.yss.valset.transfer.domain.model.TransferObjectMailFolderCount;
import com.yss.valset.transfer.domain.model.TransferObjectPage;
import com.yss.valset.transfer.domain.model.TransferObjectSourceAnalysis;
import com.yss.valset.transfer.domain.model.TransferObjectStatusCount;
import com.yss.valset.transfer.domain.model.TransferObjectSizeAnalysis;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferObjectTag;
import com.yss.valset.transfer.domain.model.TransferStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Collectors;

/**
 * 默认文件主对象查询服务。
 */
@Service
@RequiredArgsConstructor
public class DefaultTransferObjectQueryService implements TransferObjectQueryService {

    private final TransferObjectGateway transferObjectGateway;
    private final TransferObjectTagGateway transferObjectTagGateway;
    private final TransferDeliveryGateway transferDeliveryGateway;
    private final ObjectMapper objectMapper;

    @Override
    public TransferObjectViewDTO getObject(String transferId) {
        TransferObject transferObject = transferObjectGateway.findById(transferId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到文件主对象，transferId=" + transferId));
        Map<String, List<TransferObjectTag>> tagMap = loadTags(List.of(transferObject));
        Map<String, Boolean> deliveryMap = loadDeliveryStatus(List.of(transferObject));
        return toView(transferObject,
                tagMap.getOrDefault(transferObject.transferId(), List.of()),
                deliveryMap.getOrDefault(transferObject.transferId(), Boolean.FALSE));
    }

    @Override
    public PageResult<TransferObjectViewDTO> pageObjects(String sourceId,
                                                         String sourceType,
                                                         String sourceCode,
                                                         String status,
                                                         String mailId,
                                                         String fingerprint,
                                                         String routeId,
                                                         String tagId,
                                                         String tagCode,
                                                         String tagValue,
                                                         Integer pageIndex,
                                                         Integer pageSize) {
        String normalizedStatus = normalizeStatus(status);
        TransferObjectPage page = transferObjectGateway.pageObjects(
                sourceId,
                sourceType,
                sourceCode,
                normalizedStatus,
                mailId,
                fingerprint,
                routeId,
                tagId,
                tagCode,
                tagValue,
                pageIndex,
                pageSize);
        List<TransferObject> records = page.records() == null ? List.of() : page.records();
        Map<String, List<TransferObjectTag>> tagMap = loadTags(records);
        Map<String, Boolean> deliveryMap = loadDeliveryStatus(records);
        List<TransferObjectViewDTO> data = records.stream()
                .map(record -> toView(
                        record,
                        tagMap.getOrDefault(record.transferId(), List.of()),
                        deliveryMap.getOrDefault(record.transferId(), Boolean.FALSE)))
                .collect(Collectors.toList());
        return PageResult.of(data,
                        page.total(),
                        page.pageSize(),
                pageIndex
                );
    }

    @Override
    public TransferObjectAnalysisViewDTO analyzeObjects(String sourceId,
                                                        String sourceType,
                                                        String sourceCode,
                                                        String status,
                                                        String mailId,
                                                        String fingerprint,
                                                        String routeId,
                                                        String tagId,
                                                        String tagCode,
                                                        String tagValue) {
        String normalizedStatus = normalizeStatus(status);
        TransferObjectAnalysis analysis = transferObjectGateway.analyzeObjects(sourceId, sourceType, sourceCode, normalizedStatus, mailId, fingerprint, routeId, tagId, tagCode, tagValue);
        return TransferObjectAnalysisViewDTO.builder()
                .totalCount(analysis.totalCount())
                .taggedCount(analysis.taggedCount())
                .untaggedCount(analysis.untaggedCount())
                .sourceAnalyses(analysis.sourceAnalyses() == null ? List.of() : analysis.sourceAnalyses().stream().map(this::toSourceAnalysisView).toList())
                .sizeAnalysis(analysis.sizeAnalysis() == null ? null : toSizeAnalysisView(analysis.sizeAnalysis()))
                .build();
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return TransferStatus.valueOf(status.trim().toUpperCase()).name();
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的文件状态: " + status, exception);
        }
    }

    private TransferObjectSourceAnalysisViewDTO toSourceAnalysisView(TransferObjectSourceAnalysis sourceAnalysis) {
        return TransferObjectSourceAnalysisViewDTO.builder()
                .sourceType(sourceAnalysis.sourceType())
                .totalCount(sourceAnalysis.totalCount())
                .statusCounts(sourceAnalysis.statusCounts() == null ? List.of() : sourceAnalysis.statusCounts().stream().map(this::toStatusCountView).toList())
                .mailFolderCounts(sourceAnalysis.mailFolderCounts() == null ? List.of() : sourceAnalysis.mailFolderCounts().stream().map(this::toMailFolderCountView).toList())
                .build();
    }

    private TransferObjectMailFolderCountViewDTO toMailFolderCountView(TransferObjectMailFolderCount mailFolderCount) {
        return TransferObjectMailFolderCountViewDTO.builder()
                .mailFolder(mailFolderCount.mailFolder())
                .mailFolderLabel(resolveMailFolderLabel(mailFolderCount.mailFolder()))
                .count(mailFolderCount.count())
                .build();
    }

    private TransferObjectStatusCountViewDTO toStatusCountView(TransferObjectStatusCount statusCount) {
        return TransferObjectStatusCountViewDTO.builder()
                .status(statusCount.status())
                .statusLabel(resolveStatusLabel(statusCount.status()))
                .count(statusCount.count())
                .build();
    }

    private TransferObjectSizeAnalysisViewDTO toSizeAnalysisView(TransferObjectSizeAnalysis sizeAnalysis) {
        return TransferObjectSizeAnalysisViewDTO.builder()
                .totalCount(sizeAnalysis.totalCount())
                .totalSizeBytes(sizeAnalysis.totalSizeBytes())
                .extensionCounts(sizeAnalysis.extensionCounts() == null ? List.of() : sizeAnalysis.extensionCounts().stream().map(this::toExtensionCountView).toList())
                .build();
    }

    private TransferObjectExtensionCountViewDTO toExtensionCountView(TransferObjectExtensionCount extensionCount) {
        return TransferObjectExtensionCountViewDTO.builder()
                .extension(extensionCount.extension())
                .extensionLabel(resolveExtensionLabel(extensionCount.extension()))
                .count(extensionCount.count())
                .build();
    }

    private String resolveStatusLabel(String status) {
        if (!StringUtils.hasText(status)) {
            return "-";
        }
        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "PENDING" -> "待处理";
            case "RECEIVED" -> "已收取";
            case "IDENTIFIED" -> "已识别";
            case "ROUTED" -> "已路由";
            case "DELIVERING" -> "投递中";
            case "DELIVERED" -> "已投递";
            case "ARCHIVED" -> "已归档";
            case "SKIPPED" -> "已跳过";
            case "QUARANTINED" -> "已隔离";
            case "FAILED" -> "失败";
            default -> status;
        };
    }

    private String resolveMailFolderLabel(String mailFolder) {
        if (!StringUtils.hasText(mailFolder)) {
            return "未分类";
        }
        return mailFolder.trim();
    }

    private String resolveExtensionLabel(String extension) {
        if (!StringUtils.hasText(extension)) {
            return "无后缀";
        }
        String text = extension.trim().toLowerCase(Locale.ROOT);
        return text.startsWith(".") ? text : "." + text;
    }

    private Map<String, List<TransferObjectTag>> loadTags(List<TransferObject> records) {
        List<String> transferIds = records == null ? List.of() : records.stream()
                .map(TransferObject::transferId)
                .filter(StringUtils::hasText)
                .toList();
        if (transferIds.isEmpty()) {
            return Map.of();
        }
        return transferObjectTagGateway.listByTransferIds(transferIds).stream()
                .collect(Collectors.groupingBy(
                        TransferObjectTag::transferId,
                        Collectors.toList()
                ));
    }

    private Map<String, Boolean> loadDeliveryStatus(List<TransferObject> records) {
        List<String> transferIds = records == null ? List.of() : records.stream()
                .map(TransferObject::transferId)
                .filter(StringUtils::hasText)
                .toList();
        if (transferIds.isEmpty()) {
            return Map.of();
        }
        return transferDeliveryGateway.listRecordsByTransferIds(transferIds, "SUCCESS").stream()
                .map(TransferDeliveryRecord::transferId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toMap(
                        transferId -> transferId,
                        transferId -> Boolean.TRUE,
                        (left, right) -> left
                ));
    }

    private TransferObjectViewDTO toView(TransferObject transferObject, List<TransferObjectTag> tags, boolean delivered) {
        return TransferObjectViewDTO.builder()
                .transferId(transferObject.transferId() == null ? null : String.valueOf(transferObject.transferId()))
                .sourceId(transferObject.sourceId() == null ? null : String.valueOf(transferObject.sourceId()))
                .sourceType(transferObject.sourceType())
                .sourceCode(transferObject.sourceCode())
                .originalName(transferObject.originalName())
                .extension(transferObject.extension())
                .mimeType(transferObject.mimeType())
                .sizeBytes(transferObject.sizeBytes() == null ? null : String.valueOf(transferObject.sizeBytes()))
                .fingerprint(transferObject.fingerprint())
                .sourceRef(transferObject.sourceRef())
                .mailId(transferObject.mailId())
                .mailFrom(transferObject.mailFrom())
                .mailTo(transferObject.mailTo())
                .mailCc(transferObject.mailCc())
                .mailBcc(transferObject.mailBcc())
                .mailSubject(transferObject.mailSubject())
                .mailBody(transferObject.mailBody())
                .mailProtocol(transferObject.mailProtocol())
                .mailFolder(transferObject.mailFolder())
                .localTempPath(transferObject.localTempPath())
                .status(transferObject.status() == null ? null : transferObject.status().name())
                .deliveryStatus(delivered ? "已投递" : "未投递")
                .receivedAt(transferObject.receivedAt() == null ? null : java.time.LocalDateTime.ofInstant(transferObject.receivedAt(), java.time.ZoneId.systemDefault()))
                .storedAt(transferObject.storedAt() == null ? null : java.time.LocalDateTime.ofInstant(transferObject.storedAt(), java.time.ZoneId.systemDefault()))
                .routeId(transferObject.routeId() == null ? null : String.valueOf(transferObject.routeId()))
                .errorMessage(transferObject.errorMessage())
                .fileMetaJson(toJson(transferObject.fileMeta()))
                .tags(tags == null ? List.of() : tags.stream().map(this::toTagView).toList())
                .build();
    }

    private TransferObjectTagViewDTO toTagView(TransferObjectTag tag) {
        return TransferObjectTagViewDTO.builder()
                .id(tag.id())
                .transferId(tag.transferId())
                .tagId(tag.tagId())
                .tagCode(tag.tagCode())
                .tagName(tag.tagName())
                .tagValue(tag.tagValue())
                .matchStrategy(tag.matchStrategy())
                .matchReason(tag.matchReason())
                .matchedField(tag.matchedField())
                .matchedValue(tag.matchedValue())
                .createdAt(tag.createdAt() == null ? null : java.time.LocalDateTime.ofInstant(tag.createdAt(), java.time.ZoneId.systemDefault()))
                .build();
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("文件元数据序列化失败", exception);
        }
    }
}
