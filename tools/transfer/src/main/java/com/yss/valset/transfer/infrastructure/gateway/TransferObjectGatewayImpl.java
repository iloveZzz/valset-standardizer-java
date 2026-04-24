package com.yss.valset.transfer.infrastructure.gateway;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yss.valset.transfer.domain.gateway.TransferObjectGateway;
import com.yss.valset.transfer.domain.model.TransferObject;
import com.yss.valset.transfer.domain.model.TransferObjectAnalysis;
import com.yss.valset.transfer.domain.model.TransferObjectPage;
import com.yss.valset.transfer.domain.model.TransferObjectSourceAnalysis;
import com.yss.valset.transfer.domain.model.TransferObjectStatusCount;
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
    public TransferObjectPage pageObjects(String sourceId, String sourceType, String sourceCode, String status, String mailId, String fingerprint, String routeId, Integer pageIndex, Integer pageSize) {
        int current = pageIndex == null || pageIndex < 0 ? 1 : pageIndex + 1;
        int size = pageSize == null || pageSize <= 0 ? DEFAULT_PAGE_SIZE : pageSize;
        Long sourceIdValue = parseLong(sourceId);
        Long routeIdValue = parseLong(routeId);
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
                                                 String routeId) {
        Long sourceIdValue = parseLong(sourceId);
        Long routeIdValue = parseLong(routeId);
        List<TransferObject> objects = transferObjectRepository.selectList(
                        Wrappers.lambdaQuery(TransferObjectPO.class)
                                .eq(sourceIdValue != null, TransferObjectPO::getSourceId, sourceIdValue)
                                .eq(sourceType != null && !sourceType.isBlank(), TransferObjectPO::getSourceType, sourceType)
                                .eq(sourceCode != null && !sourceCode.isBlank(), TransferObjectPO::getSourceCode, sourceCode)
                                .eq(status != null && !status.isBlank(), TransferObjectPO::getStatus, status)
                                .eq(mailId != null && !mailId.isBlank(), TransferObjectPO::getMailId, mailId)
                                .eq(fingerprint != null && !fingerprint.isBlank(), TransferObjectPO::getFingerprint, fingerprint)
                                .eq(routeIdValue != null, TransferObjectPO::getRouteId, routeIdValue)
                                .orderByDesc(TransferObjectPO::getReceivedAt)
                                .orderByDesc(TransferObjectPO::getTransferId)
                )
                .stream()
                .map(this::toDomain)
                .toList();
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
                    List<TransferObjectStatusCount> statusCounts = orderStatusCounts(statusCountMap);
                    return new TransferObjectSourceAnalysis(
                            entry.getKey(),
                            (long) sourceObjects.size(),
                            statusCounts
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
        return new TransferObjectAnalysis((long) objects.size(), sourceAnalyses);
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
}
