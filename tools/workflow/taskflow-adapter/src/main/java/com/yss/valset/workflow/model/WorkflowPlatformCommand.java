package com.yss.valset.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 平台执行命令。
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowPlatformCommand {

    private WorkflowOperationType operationType;

    private EtlPlatformType platformType;

    private String workflowCode;

    private Integer workflowVersionNo;

    private String workflowName;

    private String instanceId;

    private String externalWorkflowId;

    private String externalInstanceId;

    private String businessKey;

    private String stageCode;

    private String stageName;

    private Integer stageOrder;

    private String reason;

    @Builder.Default
    private boolean force = false;

    @Builder.Default
    private Map<String, Object> context = new LinkedHashMap<>();

    @Builder.Default
    private Map<String, Object> parameters = new LinkedHashMap<>();
}
