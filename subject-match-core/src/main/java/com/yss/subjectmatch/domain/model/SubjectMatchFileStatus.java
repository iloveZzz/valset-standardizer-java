package com.yss.subjectmatch.domain.model;

/**
 * 文件生命周期状态。
 */
public enum SubjectMatchFileStatus {
    RECEIVED,
    STORED,
    READY_FOR_EXTRACT,
    EXTRACTED,
    PARSED,
    MATCHED,
    FAILED,
    ARCHIVED
}
