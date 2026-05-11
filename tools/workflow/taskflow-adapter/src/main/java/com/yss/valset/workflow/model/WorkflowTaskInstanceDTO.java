package com.yss.valset.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 工作流任务实例。
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTaskInstanceDTO {

    private Long id;

    private String name;

    private String taskType;

    private String workflowInstanceId;

    private String workflowInstanceName;

    private Long projectCode;

    private Long taskCode;

    private Integer taskDefinitionVersion;

    private String processDefinitionName;

    private Integer taskGroupPriority;

    private String state;

    private String firstSubmitTime;

    private String submitTime;

    private String startTime;

    private String endTime;

    private String host;

    private String executePath;

    private String logPath;

    private Integer retryTimes;

    private String alertFlag;

    private Map<String, Object> workflowInstance;

    private Map<String, Object> workflowDefinition;

    private Map<String, Object> taskDefine;

    private Long pid;

    private String appLink;

    private String flag;

    private Long duration;

    private Integer maxRetryTimes;

    private Integer retryInterval;

    private String taskInstancePriority;

    private String workflowInstancePriority;

    private String workerGroup;

    private Long environmentCode;

    private Map<String, Object> environmentConfig;

    private Long executorId;

    private Map<String, Object> varPool;

    private String executorName;

    private Integer delayTime;

    private String taskParams;

    private Integer dryRun;

    private Long taskGroupId;

    private Integer cpuQuota;

    private Integer memoryMax;

    private String taskExecuteType;

    private Map<String, Object> taskInstanceDependentResults;
}
