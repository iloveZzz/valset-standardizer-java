package com.yss.valset.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台执行结果。
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowPlatformExecutionResult {

    private EtlPlatformType platformType;

    private String externalWorkflowId;

    private String externalInstanceId;

    private String rawStatus;

    private String message;

    @Builder.Default
    private Map<String, Object> payload = new LinkedHashMap<>();

    @Builder.Default
    private List<WorkflowStageLogDTO> stageLogs = new ArrayList<>();
}
