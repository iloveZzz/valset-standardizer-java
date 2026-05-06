package com.yss.valset.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工作流阶段日志。
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStageLogDTO {

    private String instanceId;

    private String workflowCode;

    private Integer workflowVersionNo;

    private String stageCode;

    private String stageName;

    private Integer stageOrder;

    private WorkflowStatus status;

    private String rawStatus;

    private String message;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Builder.Default
    private Map<String, Object> payload = new LinkedHashMap<>();
}
