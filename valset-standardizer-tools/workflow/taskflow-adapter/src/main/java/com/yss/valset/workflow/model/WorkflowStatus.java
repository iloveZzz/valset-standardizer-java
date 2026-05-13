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
        switch (normalized) {
            case "DRAFT":
                return DRAFT;
            case "READY":
            case "ONLINE":
            case "ENABLED":
                return READY;
            case "SUBMITTED":
            case "SUBMITTED_SUCCESS":
            case "DISPATCHED":
                return SUBMITTED;
            case "RUNNING":
            case "EXECUTING":
            case "STARTED":
            case "RUNNING_EXECUTION":
                return RUNNING;
            case "SUCCEEDED":
            case "SUCCESS":
            case "COMPLETED":
            case "FINISHED":
                return SUCCEEDED;
            case "FAILED":
            case "FAILURE":
            case "ERROR":
                return FAILED;
            case "STOPPED":
            case "STOP":
            case "PAUSE":
            case "READY_PAUSE":
            case "READY_STOP":
            case "CANCELED":
            case "CANCELLED":
            case "TERMINATED":
                return STOPPED;
            case "RETRYING":
            case "RETRY":
                return RETRYING;
            default:
                return UNKNOWN;
        }
    }
}
