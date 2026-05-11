package com.yss.valset.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 任务实例分页结果。
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTaskInstancePageDTO {

    @Builder.Default
    private List<WorkflowTaskInstanceDTO> taskList = new ArrayList<>();

    private String workflowInstanceState;

    private Long totalCount;

    private Integer pageIndex;

    private Integer pageSize;
}
