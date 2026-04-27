package com.yss.valset.transfer.infrastructure.gateway;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yss.valset.transfer.domain.gateway.TransferObjectGateway;
import com.yss.valset.transfer.domain.gateway.TransferObjectTagGateway;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferObjectAnalysis;
import com.yss.valset.transfer.domain.model.TransferObjectExtensionCount;
import com.yss.valset.transfer.domain.model.TransferObjectMailFolderCount;
import com.yss.valset.transfer.domain.model.TransferObjectPage;
import com.yss.valset.transfer.domain.model.TransferObjectSourceAnalysis;
import com.yss.valset.transfer.domain.model.TransferObjectStatusCount;
import com.yss.valset.transfer.domain.model.TransferObjectSizeAnalysis;
import com.yss.valset.transfer.domain.model.TransferStatus;
import com.yss.valset.transfer.infrastructure.convertor.TransferJsonMapper;
import com.yss.valset.transfer.infrastructure.convertor.TransferObjectMapper;
import com.yss.valset.transfer.infrastructure.entity.TransferObjectPO;
import com.yss.valset.transfer.infrastructure.mapper.TransferObjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MyBatis 支持的文件主对象网关。
 */
@Primary
@Repository
@RequiredArgsConstructor
public class TransferObjectGatewayImpl implements TransferObjectGateway {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final TransferObjectRepository transferObjectRepository;
    private final TransferObjectTagGateway transferObjectTagGateway;
    private final TransferJsonMapper transferJsonMapper;
    private final TransferObjectMapper transferObjectMapper;

    @Override
    public Optional<TransferObject> findById(String transferId) {
        TransferObjectPO po = transferObjectRepository.selectById(parseLong(transferId));
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public Optional<TransferObject> findByFingerprint(String fingerprint) {
        TransferObjectPO po = transferObjectRepository.selectOne(
                Wrappers.lambdaQuery(TransferObjectPO.class)
                        .eq(TransferObjectPO::getFingerprint, fingerprint)
                        .last("limit 1")
        );
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public TransferObjectPage pageObjects(String sourceId,
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
        int current = pageIndex == null || pageIndex < 0 ? 1 : pageIndex + 1;
        int size = pageSize == null || pageSize <= 0 ? DEFAULT_PAGE_SIZE : pageSize;
        Long sourceIdValue = parseLong(sourceId);
        Long routeIdValue = parseLong(routeId);
        String tagFilterSql = buildTagFilterSql(tagId, tagCode, tagValue);
        Page<TransferObjectPO> page = transferObjectRepository.selectPage(
                new Page<>(current, size),
                Wrappers.lambdaQuery(TransferObjectPO.class)
                        .eq(sourceIdValue != null, TransferObjectPO::getSourceId, sourceIdValue)
                        .eq(sourceType != null && !sourceType.isBlank(), TransferObjectPO::getSourceType, sourceType)
                        .eq(sourceCode != null && !sourceCode.isBlank(), TransferObjectPO::getSourceCode, sourceCode)
                        .eq(status != null && !status.isBlank(), TransferObjectPO::getStatus, status)
                        .eq(mailId != null && !mailId.isBlank(), TransferObjectPO::getMailId, mailId)
                        .eq(fingerprint != null && !fingerprint.isBlank(), TransferObjectPO::getFingerprint, fingerprint)
                        .eq(routeIdValue != null, TransferObjectPO::getRouteId, routeIdValue)
                        .inSql(tagFilterSql != null, TransferObjectPO::getTransferId, tagFilterSql)
                        .orderByDesc(TransferObjectPO::getReceivedAt)
                        .orderByDesc(TransferObjectPO::getTransferId)
        );
        List<TransferObject> records = page.getRecords() == null ? Collections.emptyList() : page.getRecords().stream()
                .map(this::toDomain)
                .toList();
        return new TransferObjectPage(records, page.getTotal(), page.getCurrent() - 1, page.getSize());
    }

    @Override
    public TransferObjectAnalysis analyzeObjects(String sourceId,
                                                 String sourceType,
                                                 String sourceCode,
                                                 String status,
                                                 String mailId,
                                                 String fingerprint,
                                                 String routeId,
                                                 String tagId,
                                                 String tagCode,
                                                 String tagValue) {
        Long sourceIdValue = parseLong(sourceId);
        Long routeIdValue = parseLong(routeId);
        String tagFilterSql = buildTagFilterSql(tagId, tagCode, tagValue);
        List<TransferObject> objects = transferObjectRepository.selectList(
                        Wrappers.lambdaQuery(TransferObjectPO.class)
                                .eq(sourceIdValue != null, TransferObjectPO::getSourceId, sourceIdValue)
                                .eq(sourceType != null && !sourceType.isBlank(), TransferObjectPO::getSourceType, sourceType)
                                .eq(sourceCode != null && !sourceCode.isBlank(), TransferObjectPO::getSourceCode, sourceCode)
                                .eq(status != null && !status.isBlank(), TransferObjectPO::getStatus, status)
                                .eq(mailId != null && !mailId.isBlank(), TransferObjectPO::getMailId, mailId)
                                .eq(fingerprint != null && !fingerprint.isBlank(), TransferObjectPO::getFingerprint, fingerprint)
                                .eq(routeIdValue != null, TransferObjectPO::getRouteId, routeIdValue)
                                .inSql(tagFilterSql != null, TransferObjectPO::getTransferId, tagFilterSql)
                                .orderByDesc(TransferObjectPO::getReceivedAt)
                                .orderByDesc(TransferObjectPO::getTransferId)
                )
                .stream()
                .map(this::toDomain)
                .toList();
        long taggedCount = countTaggedObjects(objects);
        long untaggedCount = Math.max(0L, objects.size() - taggedCount);
        Map<String, List<TransferObject>> sourceGroups = objects.stream()
                .collect(Collectors.groupingBy(
                        item -> normalizeSourceTypeKey(item.sourceType()),
                        java.util.LinkedHashMap::new,
                        Collectors.toList()
                ));
        List<TransferObjectSourceAnalysis> sourceAnalyses = sourceGroups.entrySet().stream()
                .map(entry -> {
                    List<TransferObject> sourceObjects = entry.getValue();
                    Map<String, Long> statusCountMap = sourceObjects.stream()
                            .collect(Collectors.groupingBy(
                                    item -> normalizeStatusKey(item.status() == null ? null : item.status().name()),
                                    Collectors.counting()
                            ));
                    Map<String, Long> mailFolderCountMap = sourceObjects.stream()
                            .collect(Collectors.groupingBy(
                                    item -> normalizeMailFolderKey(item.mailFolder()),
                                    java.util.LinkedHashMap::new,
                                    Collectors.counting()
                            ));
                    List<TransferObjectStatusCount> statusCounts = orderStatusCounts(statusCountMap);
                    List<TransferObjectMailFolderCount> mailFolderCounts = orderMailFolderCounts(mailFolderCountMap);
                    return new TransferObjectSourceAnalysis(
                            entry.getKey(),
                            (long) sourceObjects.size(),
                            statusCounts,
                            mailFolderCounts
                    );
                })
                .sorted((left, right) -> {
                    int countCompare = Long.compare(right.totalCount(), left.totalCount());
                    if (countCompare != 0) {
                        return countCompare;
                    }
                    return String.valueOf(left.sourceType()).compareToIgnoreCase(String.valueOf(right.sourceType()));
                })
                .toList();
        return new TransferObjectAnalysis(
                (long) objects.size(),
                taggedCount,
                untaggedCount,
                sourceAnalyses,
                buildSizeAnalysis(objects)
        );
    }

    @Override
    public TransferObject save(TransferObject transferObject) {
        TransferObjectPO po = toPO(transferObject);
        if (po.getTransferId() == null) {
            transferObjectRepository.insert(po);
        } else {
            transferObjectRepository.updateById(po);
        }
        return toDomain(po);
    }

    private TransferObjectPO toPO(TransferObject transferObject) {
        return transferObjectMapper.toPO(transferObject, transferJsonMapper);
    }

    private TransferObject toDomain(TransferObjectPO po) {
        return transferObjectMapper.toDomain(po, transferJsonMapper);
    }

    private String buildTagFilterSql(String tagId, String tagCode, String tagValue) {
        StringBuilder sql = new StringBuilder("select transfer_id from t_transfer_object_tag where 1 = 1");
        boolean hasFilter = false;
        if (tagId != null && !tagId.isBlank()) {
            sql.append(" and tag_id = '").append(escapeSql(tagId.trim())).append("'");
            hasFilter = true;
        }
        if (tagCode != null && !tagCode.isBlank()) {
            sql.append(" and tag_code = '").append(escapeSql(tagCode.trim())).append("'");
            hasFilter = true;
        }
        if (tagValue != null && !tagValue.isBlank()) {
            sql.append(" and tag_value = '").append(escapeSql(tagValue.trim())).append("'");
            hasFilter = true;
        }
        return hasFilter ? sql.toString() : null;
    }

    private String escapeSql(String value) {
        return value.replace("'", "''");
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.valueOf(value);
    }

    private List<TransferObjectStatusCount> orderStatusCounts(Map<String, Long> statusCountMap) {
        return java.util.Arrays.stream(TransferStatus.values())
                .map(status -> {
                    String key = normalizeStatusKey(status.name());
                    Long count = statusCountMap.get(key);
                    if (count == null) {
                        return null;
                    }
                    return new TransferObjectStatusCount(status.name(), count);
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
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
                .toList();
    }

    private TransferObjectSizeAnalysis buildSizeAnalysis(List<TransferObject> objects) {
        long totalSizeBytes = 0L;
        Map<String, Long> extensionCountMap = new java.util.LinkedHashMap<>();
        for (TransferObject object : objects) {
            if (object != null && object.sizeBytes() != null) {
                totalSizeBytes += object.sizeBytes();
            }
            String extensionKey = normalizeExtensionKey(object == null ? null : object.extension());
            extensionCountMap.merge(extensionKey, 1L, Long::sum);
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
                .toList();
        return new TransferObjectSizeAnalysis((long) objects.size(), totalSizeBytes, extensionCounts);
    }

    private long countTaggedObjects(List<TransferObject> objects) {
        if (objects == null || objects.isEmpty()) {
            return 0L;
        }
        List<String> transferIds = objects.stream()
                .map(TransferObject::transferId)
                .filter(org.springframework.util.StringUtils::hasText)
                .toList();
        if (transferIds.isEmpty()) {
            return 0L;
        }
        return transferObjectTagGateway.listByTransferIds(transferIds).stream()
                .map(tag -> tag.transferId() == null ? null : tag.transferId().trim())
                .filter(org.springframework.util.StringUtils::hasText)
                .distinct()
                .count();
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

    private String normalizeSourceTypeKey(String value) {
        if (value == null) {
            return "-";
        }
        String text = value.trim();
        return text.isEmpty() ? "-" : text;
    }

    private String normalizeMailFolderKey(String value) {
        if (value == null) {
            return "-";
        }
        String text = value.trim();
        return text.isEmpty() ? "-" : text;
    }

    private String normalizeExtensionKey(String value) {
        if (value == null) {
            return "无后缀";
        }
        String text = value.trim();
        if (text.isEmpty()) {
            return "无后缀";
        }
        return text.startsWith(".") ? text.toLowerCase(Locale.ROOT) : "." + text.toLowerCase(Locale.ROOT);
    }
}
