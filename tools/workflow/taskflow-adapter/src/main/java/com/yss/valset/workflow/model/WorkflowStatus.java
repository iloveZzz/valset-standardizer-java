package com.yss.valset.workflow.model;

/**
 * 通用工作流状态。
 */
public enum WorkflowStatus {
    DRAFT,
    READY,
    SUBMITTED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    STOPPED,
    RETRYING,
    UNKNOWN;

    public static WorkflowStatus fromRawStatus(String rawStatus) {
        if (rawStatus == null) {
            return UNKNOWN;
        }
        String normalized = rawStatus.trim().toUpperCase();
        return switch (normalized) {
            case "DRAFT" -> DRAFT;
            case "READY", "ONLINE", "ENABLED" -> READY;
            case "SUBMITTED", "DISPATCHED" -> SUBMITTED;
            case "RUNNING", "EXECUTING", "STARTED" -> RUNNING;
            case "SUCCEEDED", "SUCCESS", "COMPLETED", "FINISHED" -> SUCCEEDED;
            case "FAILED", "ERROR" -> FAILED;
            case "STOPPED", "CANCELED", "CANCELLED" -> STOPPED;
            case "RETRYING", "RETRY" -> RETRYING;
            default -> UNKNOWN;
        };
    }
}
