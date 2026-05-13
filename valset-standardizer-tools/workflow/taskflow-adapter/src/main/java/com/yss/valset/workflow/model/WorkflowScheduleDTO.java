package com.yss.valset.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工作流调度配置。
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowScheduleDTO implements java.io.Serializable {

    private Long scheduleId;

    private Long projectCode;

    private String workflowCode;

    private Integer workflowVersionNo;

    private Long workflowDefinitionCode;

    private String scheduleJson;

    private String warningType;

    private Integer warningGroupId;

    private String failureStrategy;

    private String workflowInstancePriority;

    private String workerGroup;

    private Long environmentCode;

    private String releaseState;

    private Boolean online;

    private String searchVal;

    private String message;

    @Builder.Default
    private Map<String, Object> attributes = new LinkedHashMap<>();
}
