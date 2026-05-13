package com.yss.valset.workflow.model;

/**
 * 工作流触发模式。
 */
public enum WorkflowTriggerMode {
    START_PROCESS,
    START_FAILURE_TASK_PROCESS,
    START_SUSPEND_TASK_PROCESS;

    public static WorkflowTriggerMode fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return START_PROCESS;
        }
        try {
            return WorkflowTriggerMode.valueOf(value.trim().toUpperCase());
        } catch (Exception ignored) {
            return START_PROCESS;
        }
    }
}
