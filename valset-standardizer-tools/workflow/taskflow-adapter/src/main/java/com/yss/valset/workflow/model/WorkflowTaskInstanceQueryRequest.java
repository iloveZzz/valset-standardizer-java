package com.yss.valset.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务实例查询请求。
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTaskInstanceQueryRequest {

    private String workflowCode;

    private Integer workflowVersionNo;

    private String taskName;

    private String workflowInstanceName;

    private String status;

    private String startTimeFrom;

    private String endTimeTo;

    private Integer pageIndex;

    private Integer pageSize;
}
