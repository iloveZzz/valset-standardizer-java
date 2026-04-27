package com.yss.valset.transfer.infrastructure.gateway;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yss.valset.transfer.domain.gateway.TransferDeliveryGateway;
import com.yss.valset.transfer.domain.gateway.TransferRouteGateway;
import com.yss.valset.transfer.domain.model.TransferDeliveryRecord;
import com.yss.valset.transfer.domain.model.TransferDeliveryRecordPage;
import com.yss.valset.transfer.domain.model.TransferResult;
import com.yss.valset.transfer.domain.model.TransferRoute;
import com.yss.valset.transfer.infrastructure.convertor.TransferDeliveryRecordMapper;
import com.yss.valset.transfer.infrastructure.convertor.TransferJsonMapper;
import com.yss.valset.transfer.infrastructure.entity.TransferDeliveryRecordPO;
import com.yss.valset.transfer.infrastructure.mapper.TransferDeliveryRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * MyBatis 支持的文件投递结果网关。
 */
@Primary
@Repository
@RequiredArgsConstructor
public class TransferDeliveryGatewayImpl implements TransferDeliveryGateway {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final TransferDeliveryRecordRepository transferDeliveryRecordRepository;
    private final TransferRouteGateway transferRouteGateway;
    private final TransferJsonMapper transferJsonMapper;
    private final TransferDeliveryRecordMapper transferDeliveryRecordMapper;

    @Override
    public void recordResult(String routeId, String transferId, TransferResult transferResult) {
        recordResult(routeId, transferId, transferResult, 0);
    }

    @Override
    public void recordResult(String routeId, String transferId, TransferResult transferResult, Integer retryCount) {
        TransferRoute route = transferRouteGateway.findById(routeId).orElse(null);
        TransferDeliveryRecordPO po = new TransferDeliveryRecordPO();
        po.setRouteId(routeId);
        po.setTransferId(transferId);
        po.setTargetType(route == null || route.targetType() == null ? null : route.targetType().name());
        po.setTargetCode(route == null ? null : route.targetCode());
        po.setExecuteStatus(transferResult.success() ? "SUCCESS" : "FAILED");
        po.setRetryCount(retryCount == null ? 0 : retryCount);
        po.setRequestSnapshotJson(transferJsonMapper.toJson(buildRequestSnapshot(route, retryCount)));
        po.setResponseSnapshotJson(transferJsonMapper.toJson(buildResponseSnapshot(transferResult)));
        po.setDeliveredAt(LocalDateTime.now());
        transferDeliveryRecordRepository.insert(po);
    }

    @Override
    public long countByRouteId(String routeId) {
        Long id = parseLong(routeId);
        if (id == null) {
            return 0L;
        }
        return transferDeliveryRecordRepository.selectCount(
                Wrappers.lambdaQuery(TransferDeliveryRecordPO.class)
                        .eq(TransferDeliveryRecordPO::getRouteId, id)
        );
    }

    @Override
    public Optional<TransferDeliveryRecord> findById(String deliveryId) {
        return Optional.ofNullable(transferDeliveryRecordRepository.selectById(parseLong(deliveryId))).map(this::toDomain);
    }

    @Override
    public List<TransferDeliveryRecord> listRecords(String routeId, String transferId, String targetCode, String executeStatus, Integer limit) {
        Long routeIdValue = parseLong(routeId);
        Long transferIdValue = parseLong(transferId);
        var query = Wrappers.lambdaQuery(TransferDeliveryRecordPO.class)
                .eq(routeIdValue != null, TransferDeliveryRecordPO::getRouteId, routeIdValue)
                .eq(transferIdValue != null, TransferDeliveryRecordPO::getTransferId, transferIdValue)
                .like(targetCode != null && !targetCode.isBlank(), TransferDeliveryRecordPO::getTargetCode, targetCode)
                .eq(executeStatus != null && !executeStatus.isBlank(), TransferDeliveryRecordPO::getExecuteStatus, executeStatus)
                .orderByDesc(TransferDeliveryRecordPO::getDeliveredAt)
                .orderByDesc(TransferDeliveryRecordPO::getDeliveryId);
        if (limit != null && limit > 0) {
            query.last("limit " + limit);
        }
        return transferDeliveryRecordRepository.selectList(query)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<TransferDeliveryRecord> listRecordsByTransferIds(List<String> transferIds, String executeStatus) {
        if (transferIds == null || transferIds.isEmpty()) {
            return List.of();
        }
        List<Long> normalizedTransferIds = transferIds.stream()
                .map(this::parseLong)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (normalizedTransferIds.isEmpty()) {
            return List.of();
        }
        return transferDeliveryRecordRepository.selectList(
                        Wrappers.lambdaQuery(TransferDeliveryRecordPO.class)
                                .in(TransferDeliveryRecordPO::getTransferId, normalizedTransferIds)
                                .eq(executeStatus != null && !executeStatus.isBlank(), TransferDeliveryRecordPO::getExecuteStatus, executeStatus)
                                .orderByDesc(TransferDeliveryRecordPO::getDeliveredAt)
                                .orderByDesc(TransferDeliveryRecordPO::getDeliveryId)
                )
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public TransferDeliveryRecordPage pageRecords(String routeId,
                                                  String transferId,
                                                  String targetCode,
                                                  String executeStatus,
                                                  Integer pageIndex,
                                                  Integer pageSize) {
        int current = pageIndex == null || pageIndex < 0 ? 1 : pageIndex + 1;
        int size = pageSize == null || pageSize <= 0 ? DEFAULT_PAGE_SIZE : pageSize;
        Long routeIdValue = parseLong(routeId);
        Long transferIdValue = parseLong(transferId);
        Page<TransferDeliveryRecordPO> page = transferDeliveryRecordRepository.selectPage(
                new Page<>(current, size),
                Wrappers.lambdaQuery(TransferDeliveryRecordPO.class)
                        .eq(routeIdValue != null, TransferDeliveryRecordPO::getRouteId, routeIdValue)
                        .eq(transferIdValue != null, TransferDeliveryRecordPO::getTransferId, transferIdValue)
                        .like(targetCode != null && !targetCode.isBlank(), TransferDeliveryRecordPO::getTargetCode, targetCode)
                        .eq(executeStatus != null && !executeStatus.isBlank(), TransferDeliveryRecordPO::getExecuteStatus, executeStatus)
                        .orderByDesc(TransferDeliveryRecordPO::getDeliveredAt)
                        .orderByDesc(TransferDeliveryRecordPO::getDeliveryId)
        );
        List<TransferDeliveryRecord> records = page.getRecords() == null ? List.of() : page.getRecords().stream()
                .map(this::toDomain)
                .toList();
        return new TransferDeliveryRecordPage(records, page.getTotal(), page.getCurrent() - 1, page.getSize());
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.valueOf(value);
    }

    private TransferDeliveryRecord toDomain(TransferDeliveryRecordPO po) {
        return transferDeliveryRecordMapper.toDomain(po);
    }

    private java.util.Map<String, Object> buildRequestSnapshot(TransferRoute route, Integer retryCount) {
        java.util.Map<String, Object> snapshot = new java.util.LinkedHashMap<>();
        snapshot.put("routeId", route == null ? null : route.routeId());
        snapshot.put("ruleId", route == null ? null : route.ruleId());
        snapshot.put("targetType", route == null || route.targetType() == null ? null : route.targetType().name());
        snapshot.put("targetCode", route == null ? null : route.targetCode());
        snapshot.put("targetPath", route == null ? null : route.targetPath());
        snapshot.put("renamePattern", route == null ? null : route.renamePattern());
        snapshot.put("routeStatus", route == null || route.routeStatus() == null ? null : route.routeStatus().name());
        snapshot.put("retryCount", retryCount == null ? 0 : retryCount);
        snapshot.put("triggerType", route == null || route.routeMeta() == null ? null : route.routeMeta().get("triggerType"));
        snapshot.put("maxRetryCount", route == null || route.routeMeta() == null ? null : route.routeMeta().get("maxRetryCount"));
        snapshot.put("retryDelaySeconds", route == null || route.routeMeta() == null ? null : route.routeMeta().get("retryDelaySeconds"));
        return snapshot;
    }

    private java.util.Map<String, Object> buildResponseSnapshot(TransferResult transferResult) {
        java.util.Map<String, Object> snapshot = new java.util.LinkedHashMap<>();
        java.util.List<String> messages = transferResult == null || transferResult.messages() == null ? java.util.List.of() : transferResult.messages();
        int previewSize = Math.min(messages.size(), 3);
        snapshot.put("success", transferResult != null && transferResult.success());
        snapshot.put("fileId", transferResult == null ? null : transferResult.fileId());
        snapshot.put("messageCount", messages.size());
        snapshot.put("messages", messages.subList(0, previewSize));
        snapshot.put("truncated", messages.size() > previewSize);
        return snapshot;
    }
}
