package com.yss.valset.transfer.domain.model;

import java.time.LocalDateTime;

/**
 * 文件投递结果。
 */
public record TransferDeliveryRecord(
        String deliveryId,
        String routeId,
        String transferId,
        String targetType,
        String targetCode,
        String executeStatus,
        Integer retryCount,
        String requestSnapshotJson,
        String responseSnapshotJson,
        String errorMessage,
        LocalDateTime deliveredAt
) {
}
