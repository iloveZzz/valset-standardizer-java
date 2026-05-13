package com.yss.valset.transfer.domain.model;

/**
 * 文件分拣处理状态。
 */
public enum TransferStatus {
    PENDING,
    RECEIVED,
    IDENTIFIED,
    ROUTED,
    DELIVERING,
    DELIVERED,
    ARCHIVED,
    SKIPPED,
    QUARANTINED,
    FAILED
}
