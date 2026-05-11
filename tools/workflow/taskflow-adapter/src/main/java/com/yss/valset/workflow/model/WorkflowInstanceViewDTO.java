package com.yss.valset.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 工作流实例视图。
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowInstanceViewDTO {

    private String instanceId;

    private String workflowCode;

    private String workflowName;

    private Integer workflowVersionNo;

    private EtlPlatformType platformType;

    private String businessKey;

    private String externalInstanceId;

    private String externalWorkflowId;

    private WorkflowStatus status;

    private String rawStatus;

    private String currentStageCode;

    private String currentStageName;

    private LocalDateTime triggerTime;

    private LocalDateTime startTime;

    private String duration;

    private LocalDateTime endTime;

    private String message;

    private Integer stageCount;
}
