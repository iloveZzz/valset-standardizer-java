package com.yss.valset.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工作流实例。
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowInstanceDTO implements java.io.Serializable {

    private String instanceId;

    private String workflowCode;

    private Integer workflowVersionNo;

    private EtlPlatformType platformType;

    private String currentStageCode;

    private String businessKey;

    private String externalInstanceId;

    private String externalWorkflowId;

    private WorkflowStatus status;

    private String rawStatus;

    private LocalDateTime triggerTime;

    private LocalDateTime startTime;

    private String duration;

    private LocalDateTime endTime;

    private String message;

    @Builder.Default
    private Map<String, Object> context = new LinkedHashMap<>();

    @Builder.Default
    private List<WorkflowStageLogDTO> stageLogs = new ArrayList<>();
}
