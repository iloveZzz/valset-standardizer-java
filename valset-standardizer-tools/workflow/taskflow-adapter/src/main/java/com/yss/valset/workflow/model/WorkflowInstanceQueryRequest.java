package com.yss.valset.workflow.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工作流实例查询请求。
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowInstanceQueryRequest {

    @NotBlank
    private String workflowCode;
    @NotBlank
    private Integer workflowVersionNo;
    @NotBlank
    private EtlPlatformType platformType;

    private String workflowName;

    private String status;

    private String businessKey;

    private String instanceId;

    private String externalInstanceId;

    private String stageCode;

    private String triggerTimeFrom;

    private String triggerTimeTo;

    private Integer pageIndex;

    private Integer pageSize;
}
