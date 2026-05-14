package com.yss.valset.transfer.application.impl.query;

import com.yss.cloud.dto.result.PageResult;
import com.yss.valset.transfer.application.dto.TransferObjectDownloadViewDTO;
import com.yss.valset.transfer.application.dto.TransferObjectAnalysisViewDTO;
import com.yss.valset.transfer.application.dto.TransferObjectAttachmentViewDTO;
import com.yss.valset.transfer.application.dto.TransferObjectExtensionCountViewDTO;
import com.yss.valset.transfer.application.dto.TransferObjectMailFolderCountViewDTO;
import com.yss.valset.transfer.application.dto.TransferMailInfoViewDTO;
import com.yss.valset.transfer.application.dto.TransferObjectTagViewDTO;
import com.yss.valset.transfer.application.dto.TransferObjectSourceAnalysisViewDTO;
import com.yss.valset.transfer.application.dto.TransferObjectStatusCountViewDTO;
import com.yss.valset.transfer.application.dto.TransferObjectSizeAnalysisViewDTO;
import com.yss.valset.transfer.application.dto.TransferObjectViewDTO;
import com.yss.valset.transfer.application.service.TransferObjectQueryService;
import com.yss.valset.transfer.domain.gateway.TransferObjectTagGateway;
import com.yss.valset.transfer.domain.gateway.TransferDeliveryGateway;
import com.yss.valset.transfer.domain.gateway.TransferMailInfoGateway;
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
import com.yss.valset.transfer.domain.model.TransferMailInfo;
import com.yss.valset.transfer.domain.model.TransferStatus;
import com.yss.valset.transfer.infrastructure.convertor.TransferJsonMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
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
    private final TransferMailInfoGateway transferMailInfoGateway;
    private final TransferJsonMapper transferJsonMapper;

    @Override
    public TransferObjectViewDTO getObject(String transferId) {
        TransferObject transferObject = transferObjectGateway.findById(transferId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到文件主对象，transferId=" + transferId));
        Map<String, List<TransferObjectTag>> tagMap = loadTags(java.util.Arrays.asList(transferObject));
        Map<String, Boolean> deliveryMap = loadDeliveryStatus(java.util.Arrays.asList(transferObject));
        return toView(transferObject,
                tagMap.getOrDefault(transferObject.transferId(), java.util.Arrays.asList()),
                deliveryMap.getOrDefault(transferObject.transferId(), Boolean.FALSE));
    }

    @Override
    public TransferMailInfoViewDTO getMailInfo(String transferId) {
        transferObjectGateway.findById(transferId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到文件主对象，transferId=" + transferId));
        return transferMailInfoGateway.findByTransferId(transferId)
                .map(mailInfo -> TransferMailInfoViewDTO.builder()
                        .transferId(mailInfo.transferId())
                        .mailId(mailInfo.mailId())
                        .mailFrom(mailInfo.mailFrom())
                        .mailTo(mailInfo.mailTo())
                        .mailCc(mailInfo.mailCc())
                        .mailBcc(mailInfo.mailBcc())
                        .mailSubject(mailInfo.mailSubject())
                        .mailBody(mailInfo.mailBody())
                        .mailProtocol(mailInfo.mailProtocol())
                        .mailFolder(mailInfo.mailFolder())
                        .build())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到邮件信息，transferId=" + transferId));
    }

    @Override
    public TransferObjectDownloadViewDTO downloadObject(String transferId) {
        TransferObject transferObject = transferObjectGateway.findById(transferId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到文件主对象，transferId=" + transferId));
        String localTempPath = transferObject.localTempPath();
        if (!StringUtils.hasText(localTempPath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文件主对象没有可下载的本地临时路径，transferId=" + transferId);
        }
        Path filePath = Paths.get(localTempPath).toAbsolutePath().normalize();
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "本地临时文件不存在，path=" + filePath);
        }
        try {
            long contentLength = Files.size(filePath);
            String contentType = resolveContentType(transferObject.mimeType(), filePath);
            return TransferObjectDownloadViewDTO.builder()
                    .transferId(transferObject.transferId())
                    .filePath(filePath)
                    .fileName(resolveDownloadFileName(transferObject, filePath))
                    .contentType(contentType)
                    .contentLength(contentLength)
                    .build();
        } catch (Exception exception) {
            throw new IllegalStateException("文件下载准备失败，transferId=" + transferId + "，path=" + filePath, exception);
        }
    }

    @Override
    public PageResult<TransferObjectViewDTO> pageObjects(String sourceId,
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
        String normalizedStatus = normalizeStatus(status);
        String normalizedTaskDate = normalizeTaskDate(taskDate);
        TransferObjectPage page = transferObjectGateway.pageObjects(
                sourceId,
                sourceType,
                sourceCode,
                originalName,
                normalizedStatus,
                normalizeDeliveryStatus(deliveryStatus),
                mailId,
                fingerprint,
                routeId,
                tagId,
                tagCode,
                tagValue,
                normalizedTaskDate,
                pageIndex,
                pageSize);
        List<TransferObject> records = page.records() == null ? java.util.Arrays.asList() : page.records();
        Map<String, List<TransferObjectTag>> tagMap = loadTags(records);
        Map<String, Boolean> deliveryMap = loadDeliveryStatus(records);
        List<TransferObjectViewDTO> data = records.stream()
                .map(record -> toView(
                        record,
                        tagMap.getOrDefault(record.transferId(), java.util.Arrays.asList()),
                        deliveryMap.getOrDefault(record.transferId(), Boolean.FALSE)))
                .collect(java.util.stream.Collectors.toList());
        return PageResult.of(data,
                        page.total(),
                        page.pageSize(),
                pageIndex
                );
    }

    @Override
    public PageResult<TransferObjectViewDTO> pageMailInbox(String sourceCode,
                                                           String mailId,
                                                           String deliveryStatus,
                                                           Integer pageIndex,
                                                           Integer pageSize) {
        String normalizedDeliveryStatus = normalizeDeliveryStatus(deliveryStatus);
        int current = pageIndex == null || pageIndex < 0 ? 0 : pageIndex;
        int size = pageSize == null || pageSize <= 0 ? 10 : pageSize;
        int offset = current * size;
        
        // 查询总数
        long totalCount = transferObjectGateway.countMailInboxGroups(sourceCode, mailId, normalizedDeliveryStatus);
        
        // 分页查询
        List<InboxMailGroup> groups = transferObjectGateway.loadMailInboxGroups(sourceCode, mailId, normalizedDeliveryStatus, offset, size);
        
        List<TransferObjectViewDTO> data = groups.stream()
                .map(this::toInboxView)
                .collect(java.util.stream.Collectors.toList());
        
        return PageResult.of(data, totalCount, size, current);
    }

    @Override
    public TransferObjectAnalysisViewDTO analyzeObjects(String sourceId,
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
        String normalizedStatus = normalizeStatus(status);
        String normalizedTaskDate = normalizeTaskDate(taskDate);
        TransferObjectAnalysis analysis = transferObjectGateway.analyzeObjects(sourceId, sourceType, sourceCode, originalName, normalizedStatus, normalizeDeliveryStatus(deliveryStatus), mailId, fingerprint, routeId, tagId, tagCode, tagValue, normalizedTaskDate);
        return TransferObjectAnalysisViewDTO.builder()
                .totalCount(analysis.totalCount())
                .taggedCount(analysis.taggedCount())
                .untaggedCount(analysis.untaggedCount())
                .sourceAnalyses(analysis.sourceAnalyses() == null ? java.util.Arrays.asList() : analysis.sourceAnalyses().stream().map(this::toSourceAnalysisView).collect(java.util.stream.Collectors.toList()))
                .sizeAnalysis(analysis.sizeAnalysis() == null ? null : toSizeAnalysisView(analysis.sizeAnalysis()))
                .build();
    }

    @Override
    public TransferObjectAnalysisViewDTO analyzeMailInbox(String sourceCode,
                                                          String mailId,
                                                          String deliveryStatus) {
        String normalizedDeliveryStatus = normalizeDeliveryStatus(deliveryStatus);
        // 分析时不需要分页，传入null获取所有数据
        List<InboxMailGroup> groups = transferObjectGateway.loadMailInboxGroups(sourceCode, mailId, normalizedDeliveryStatus, null, null);
        long totalCount = groups.size();
        long taggedCount = groups.stream().filter(InboxMailGroup::tagged).count();
        long untaggedCount = Math.max(0L, totalCount - taggedCount);
        return TransferObjectAnalysisViewDTO.builder()
                .totalCount(totalCount)
                .taggedCount(taggedCount)
                .untaggedCount(untaggedCount)
                .sourceAnalyses(java.util.Arrays.asList(toSourceAnalysisView(buildInboxSourceAnalysis(groups))))
                .sizeAnalysis(toSizeAnalysisView(buildInboxSizeAnalysis(groups)))
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

    private String normalizeDeliveryStatus(String deliveryStatus) {
        if (!StringUtils.hasText(deliveryStatus)) {
            return null;
        }
        String value = deliveryStatus.trim().toUpperCase(Locale.ROOT);
        if ("DELIVERED".equals(value) || "SUCCESS".equals(value) || "已投递".equals(value)) {
            return "DELIVERED";
        }
        if ("UNDELIVERED".equals(value) || "NOT_DELIVERED".equals(value) || "FAILED".equals(value) || "未投递".equals(value)) {
            return "UNDELIVERED";
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的投递状态: " + deliveryStatus);
    }

    private String normalizeTaskDate(String taskDate) {
        if (!StringUtils.hasText(taskDate)) {
            return null;
        }
        try {
            LocalDate.parse(taskDate.trim());
            return taskDate.trim();
        } catch (DateTimeParseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的任务日期: " + taskDate, exception);
        }
    }

    private TransferObjectSourceAnalysisViewDTO toSourceAnalysisView(TransferObjectSourceAnalysis sourceAnalysis) {
        return TransferObjectSourceAnalysisViewDTO.builder()
                .sourceType(sourceAnalysis.sourceType())
                .totalCount(sourceAnalysis.totalCount())
                .statusCounts(sourceAnalysis.statusCounts() == null ? java.util.Arrays.asList() : sourceAnalysis.statusCounts().stream().map(this::toStatusCountView).collect(java.util.stream.Collectors.toList()))
                .mailFolderCounts(sourceAnalysis.mailFolderCounts() == null ? java.util.Arrays.asList() : sourceAnalysis.mailFolderCounts().stream().map(this::toMailFolderCountView).collect(java.util.stream.Collectors.toList()))
                .undeliveredCount(sourceAnalysis.undeliveredCount())
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
                .extensionCounts(sizeAnalysis.extensionCounts() == null ? java.util.Arrays.asList() : sizeAnalysis.extensionCounts().stream().map(this::toExtensionCountView).collect(java.util.stream.Collectors.toList()))
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
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if ("PENDING".equals(normalized)) {
            return "待处理";
        }
        if ("RECEIVED".equals(normalized)) {
            return "已收取";
        }
        if ("IDENTIFIED".equals(normalized)) {
            return "已识别";
        }
        if ("ROUTED".equals(normalized)) {
            return "已路由";
        }
        if ("DELIVERING".equals(normalized)) {
            return "投递中";
        }
        if ("DELIVERED".equals(normalized)) {
            return "已投递";
        }
        if ("ARCHIVED".equals(normalized)) {
            return "已归档";
        }
        if ("SKIPPED".equals(normalized)) {
            return "已跳过";
        }
        if ("QUARANTINED".equals(normalized)) {
            return "已隔离";
        }
        if ("FAILED".equals(normalized)) {
            return "失败";
        }
        return status;
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
        List<String> transferIds = records == null ? java.util.Arrays.asList() : records.stream()
                .map(TransferObject::transferId)
                .filter(StringUtils::hasText)
                .collect(java.util.stream.Collectors.toList());
        if (transferIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        return transferObjectTagGateway.listByTransferIds(transferIds).stream()
                .collect(Collectors.groupingBy(
                        TransferObjectTag::transferId,
                        java.util.stream.Collectors.toList()
                ));
    }

    private Map<String, Boolean> loadDeliveryStatus(List<TransferObject> records) {
        List<String> transferIds = records == null ? java.util.Arrays.asList() : records.stream()
                .map(TransferObject::transferId)
                .filter(StringUtils::hasText)
                .collect(java.util.stream.Collectors.toList());
        if (transferIds.isEmpty()) {
            return java.util.Collections.emptyMap();
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

    private InboxMailGroup buildInboxMailGroup(String mailKey,
                                               List<TransferObject> items,
                                               Set<String> deliveredTransferIds,
                                               Set<String> taggedTransferIds) {
        List<TransferObject> sortedItems = items == null ? java.util.Arrays.asList() : items.stream()
                .sorted(inboxAttachmentComparator())
                .collect(java.util.stream.Collectors.toList());
        if (sortedItems.isEmpty()) {
            return new InboxMailGroup(mailKey, null, java.util.Arrays.asList(), java.util.Arrays.asList(), null, false, false);
        }
        TransferObject representative = sortedItems.get(0);
        List<String> transferIds = sortedItems.stream()
                .map(TransferObject::transferId)
                .filter(StringUtils::hasText)
                .collect(java.util.stream.Collectors.toList());
        boolean delivered = !transferIds.isEmpty() && transferIds.stream().allMatch(deliveredTransferIds::contains);
        boolean tagged = transferIds.stream().anyMatch(taggedTransferIds::contains);
        return new InboxMailGroup(mailKey, representative, sortedItems, transferIds, null, delivered, tagged);
    }

    private Map<String, TransferMailInfo> loadMailInfoMap(List<InboxMailGroup> groups) {
        List<String> representativeIds = groups == null ? java.util.Arrays.asList() : groups.stream()
                .filter(group -> group != null && group.representative() != null)
                .map(group -> group.representative().transferId())
                .filter(StringUtils::hasText)
                .collect(java.util.stream.Collectors.toList());
        if (representativeIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        return transferMailInfoGateway.listByTransferIds(representativeIds).stream()
                .collect(Collectors.toMap(
                        TransferMailInfo::transferId,
                        mailInfo -> mailInfo,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private Set<String> loadTaggedTransferIds(List<TransferObject> objects) {
        List<String> transferIds = objects == null ? java.util.Arrays.asList() : objects.stream()
                .map(TransferObject::transferId)
                .filter(StringUtils::hasText)
                .collect(java.util.stream.Collectors.toList());
        if (transferIds.isEmpty()) {
            return new java.util.LinkedHashSet<>(java.util.Arrays.asList());
        }
        return transferObjectTagGateway.listByTransferIds(transferIds).stream()
                .map(tag -> tag.transferId() == null ? null : tag.transferId().trim())
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> loadDeliveredTransferIds(List<TransferObject> objects) {
        List<String> transferIds = objects == null ? java.util.Arrays.asList() : objects.stream()
                .map(TransferObject::transferId)
                .filter(StringUtils::hasText)
                .collect(java.util.stream.Collectors.toList());
        if (transferIds.isEmpty()) {
            return new java.util.LinkedHashSet<>(java.util.Arrays.asList());
        }
        return transferDeliveryGateway.listRecordsByTransferIds(transferIds, "SUCCESS").stream()
                .map(TransferDeliveryRecord::transferId)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean matchesInboxDeliveryStatus(InboxMailGroup group, String deliveryStatus) {
        if (!StringUtils.hasText(deliveryStatus)) {
            return true;
        }
        if ("DELIVERED".equalsIgnoreCase(deliveryStatus)) {
            return group.delivered();
        }
        if ("UNDELIVERED".equalsIgnoreCase(deliveryStatus)) {
            return !group.delivered();
        }
        return true;
    }

    private TransferObjectSourceAnalysis buildInboxSourceAnalysis(List<InboxMailGroup> groups) {
        List<InboxMailGroup> safeGroups = groups == null ? java.util.Arrays.asList() : groups;
        Map<String, Long> statusCountMap = safeGroups.stream()
                .collect(Collectors.groupingBy(
                        group -> normalizeStatusKey(group.representative() == null || group.representative().status() == null ? null : group.representative().status().name()),
                        Collectors.counting()
                ));
        Map<String, Long> mailFolderCountMap = safeGroups.stream()
                .collect(Collectors.groupingBy(
                        group -> normalizeMailFolderKey(group.representative() == null ? null : group.representative().mailFolder()),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
        long undeliveredCount = safeGroups.stream().filter(group -> !group.delivered()).count();
        return new TransferObjectSourceAnalysis(
                "EMAIL",
                (long) safeGroups.size(),
                orderStatusCounts(statusCountMap),
                orderMailFolderCounts(mailFolderCountMap),
                undeliveredCount
        );
    }

    private TransferObjectSizeAnalysis buildInboxSizeAnalysis(List<InboxMailGroup> groups) {
        List<InboxMailGroup> safeGroups = groups == null ? java.util.Arrays.asList() : groups;
        long totalSizeBytes = 0L;
        Map<String, Long> extensionCountMap = new LinkedHashMap<>();
        for (InboxMailGroup group : safeGroups) {
            if (group.attachments() == null) {
                continue;
            }
            for (TransferObject attachment : group.attachments()) {
                if (attachment != null && attachment.sizeBytes() != null) {
                    totalSizeBytes += attachment.sizeBytes();
                }
                String extensionKey = normalizeExtensionKey(attachment == null ? null : attachment.extension());
                extensionCountMap.merge(extensionKey, 1L, Long::sum);
            }
        }
        List<TransferObjectExtensionCount> extensionCounts = extensionCountMap.entrySet().stream()
                .map(entry -> new TransferObjectExtensionCount(entry.getKey(), entry.getValue()))
                .sorted((left, right) -> {
                    int countCompare = Long.compare(right.count(), left.count());
                    if (countCompare != 0) {
                        return countCompare;
                    }
                    return String.valueOf(left.extension()).compareToIgnoreCase(String.valueOf(right.extension()));
                })
                .collect(java.util.stream.Collectors.toList());
        return new TransferObjectSizeAnalysis((long) safeGroups.size(), totalSizeBytes, extensionCounts);
    }

    private String normalizeMailGroupKey(String mailId, String transferId) {
        if (StringUtils.hasText(mailId)) {
            return mailId.trim();
        }
        return "transfer:" + (transferId == null ? "" : transferId.trim());
    }

    private String normalizeStatusKey(String value) {
        if (value == null) {
            return "-";
        }
        String text = value.trim();
        if (text.isEmpty()) {
            return "-";
        }
        return text.toUpperCase(Locale.ROOT);
    }

    private String normalizeMailFolderKey(String value) {
        if (value == null) {
            return "未分类";
        }
        String text = value.trim();
        return text.isEmpty() ? "未分类" : text;
    }

    private String normalizeExtensionKey(String value) {
        if (value == null) {
            return "无后缀";
        }
        String text = value.trim().toLowerCase(Locale.ROOT);
        if (text.isEmpty()) {
            return "无后缀";
        }
        return text.startsWith(".") ? text : "." + text;
    }

    private List<TransferObjectStatusCount> orderStatusCounts(Map<String, Long> statusCountMap) {
        return java.util.Arrays.asList(TransferStatus.values()).stream()
                .map(status -> {
                    String key = normalizeStatusKey(status.name());
                    Long count = statusCountMap.get(key);
                    if (count == null) {
                        return null;
                    }
                    return new TransferObjectStatusCount(status.name(), count);
                })
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());
    }

    private List<TransferObjectMailFolderCount> orderMailFolderCounts(Map<String, Long> mailFolderCountMap) {
        return mailFolderCountMap.entrySet().stream()
                .map(entry -> new TransferObjectMailFolderCount(entry.getKey(), entry.getValue()))
                .sorted((left, right) -> {
                    int countCompare = Long.compare(right.count(), left.count());
                    if (countCompare != 0) {
                        return countCompare;
                    }
                    return String.valueOf(left.mailFolder()).compareToIgnoreCase(String.valueOf(right.mailFolder()));
                })
                .collect(java.util.stream.Collectors.toList());
    }

    private Comparator<TransferObject> inboxAttachmentComparator() {
        return Comparator.comparing(TransferObject::receivedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(TransferObject::transferId, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private Comparator<InboxMailGroup> inboxGroupComparator() {
        return Comparator.comparing((InboxMailGroup group) -> group.representative() == null ? null : group.representative().receivedAt(), Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(group -> group.representative() == null ? null : group.representative().transferId(), Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private TransferObjectViewDTO toView(TransferObject transferObject, List<TransferObjectTag> tags, boolean delivered) {
        return TransferObjectViewDTO.builder()
                .transferId(transferObject.transferId())
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
                .realStoragePath(transferObject.realStoragePath())
                .status(transferObject.status() == null ? null : transferObject.status().name())
                .deliveryStatus(delivered ? "已投递" : "未投递")
                .receivedAt(transferObject.receivedAt() == null ? null : java.time.LocalDateTime.ofInstant(transferObject.receivedAt(), java.time.ZoneId.systemDefault()))
                .storedAt(transferObject.storedAt() == null ? null : java.time.LocalDateTime.ofInstant(transferObject.storedAt(), java.time.ZoneId.systemDefault()))
                .businessDate(transferObject.businessDate())
                .businessId(transferObject.businessId())
                .receiveDate(transferObject.receiveDate())
                .routeId(transferObject.routeId() == null ? null : String.valueOf(transferObject.routeId()))
                .errorMessage(transferObject.errorMessage())
                .fileMetaJson(transferJsonMapper.toCompactJson(transferObject.fileMeta()))
                .tags(tags == null ? java.util.Arrays.asList() : tags.stream().map(this::toTagView).collect(java.util.stream.Collectors.toList()))
                .build();
    }

    private TransferObjectViewDTO toInboxView(InboxMailGroup group) {
        TransferObject representative = group.representative();
        if (representative == null) {
            throw new IllegalStateException("邮件分组缺少主文件主对象");
        }
        TransferObject hydrated = group.mailInfo() == null ? representative : representative.withMailInfo(group.mailInfo());
        List<TransferObject> attachments = group.attachments();
        // 通过transferIds查询附件信息
        List<String> transferIds = group.transferIds() == null ? java.util.Arrays.asList() : group.transferIds();

        TransferObjectViewDTO view = toView(hydrated, java.util.Arrays.asList(), group.delivered());
        view.setPrimaryTransferId(hydrated.transferId());
        view.setTransferIds(transferIds);
        view.setAttachments(toTransferObjectAttachmentViewDTO(attachments));
        view.setAttachmentCount(attachments.size());
        return view;
    }

    private List<TransferObjectAttachmentViewDTO> toTransferObjectAttachmentViewDTO(List<TransferObject> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return java.util.Arrays.asList();
        }
        return attachments.stream()
                .map(this::toAttachmentView)
                .collect(java.util.stream.Collectors.toList());
    }

    private List<TransferObjectAttachmentViewDTO> loadAttachmentsByTransferIds(List<String> transferIds, String primaryTransferId) {
        if (transferIds == null || transferIds.isEmpty()) {
            return java.util.Arrays.asList();
        }
        
        // 过滤出附件的transferIds（排除主对象）
        List<String> attachmentIds = transferIds.stream()
                .filter(id -> !id.equals(primaryTransferId))
                .collect(java.util.stream.Collectors.toList());
        
        if (attachmentIds.isEmpty()) {
            return java.util.Arrays.asList();
        }
        
        // 批量查询附件对象
        return attachmentIds.stream()
                .map(transferId -> transferObjectGateway.findById(transferId))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(this::toAttachmentView)
                .collect(java.util.stream.Collectors.toList());
    }

    private TransferObjectAttachmentViewDTO toAttachmentView(TransferObject transferObject) {
        return TransferObjectAttachmentViewDTO.builder()
                .transferId(transferObject.transferId())
                .originalName(transferObject.originalName())
                .localTempPath(transferObject.localTempPath())
                .realStoragePath(transferObject.realStoragePath())
                .mimeType(transferObject.mimeType())
                .sizeBytes(transferObject.sizeBytes() == null ? null : String.valueOf(transferObject.sizeBytes()))
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

    private String resolveDownloadFileName(TransferObject transferObject, Path filePath) {
        String fileName = transferObject.originalName();
        if (StringUtils.hasText(fileName)) {
            return fileName.trim();
        }
        Path fileNamePath = filePath.getFileName();
        return fileNamePath == null ? "transfer-file" : fileNamePath.toString();
    }

    private String resolveContentType(String mimeType, Path filePath) {
        if (StringUtils.hasText(mimeType)) {
            return mimeType.trim();
        }
        try {
            String contentType = Files.probeContentType(filePath);
            return StringUtils.hasText(contentType) ? contentType : "application/octet-stream";
        } catch (Exception exception) {
            return "application/octet-stream";
        }
    }

    public static final class InboxMailGroup {
        private final String mailKey;
        private final TransferObject representative;
        private final List<TransferObject> attachments;
        private final List<String> transferIds;
        private final TransferMailInfo mailInfo;
        private final boolean delivered;
        private final boolean tagged;

        public InboxMailGroup(String mailKey,
                              TransferObject representative,
                              List<TransferObject> attachments,
                              List<String> transferIds,
                              TransferMailInfo mailInfo,
                              boolean delivered,
                              boolean tagged) {
            this.mailKey = mailKey;
            this.representative = representative;
            this.attachments = attachments;
            this.transferIds = transferIds;
            this.mailInfo = mailInfo;
            this.delivered = delivered;
            this.tagged = tagged;
        }

        public String mailKey() {
            return mailKey;
        }

        public TransferObject representative() {
            return representative;
        }

        public List<TransferObject> attachments() {
            return attachments;
        }

        public List<String> transferIds() {
            return transferIds;
        }

        public TransferMailInfo mailInfo() {
            return mailInfo;
        }

        public boolean delivered() {
            return delivered;
        }

        public boolean tagged() {
            return tagged;
        }

        private InboxMailGroup withMailInfo(TransferMailInfo mailInfo) {
            return new InboxMailGroup(mailKey, representative, attachments, transferIds, mailInfo, delivered, tagged);
        }
    }
}
