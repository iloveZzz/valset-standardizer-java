package com.yss.subjectmatch.domain.model;

/**
 * 任务生命周期状态值。
 */
public enum TaskStatus {
    PENDING,
    SCHEDULED,
    RUNNING,
    SUCCESS,
    FAILED,
    RETRYING,
    CANCELED
}
