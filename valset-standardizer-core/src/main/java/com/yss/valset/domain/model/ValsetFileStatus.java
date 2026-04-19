package com.yss.valset.domain.model;

/**
 * 文件生命周期状态。
 */
public enum ValsetFileStatus {
    RECEIVED,
    STORED,
    READY_FOR_EXTRACT,
    EXTRACTED,
    PARSED,
    MATCHED,
    FAILED,
    ARCHIVED
}
