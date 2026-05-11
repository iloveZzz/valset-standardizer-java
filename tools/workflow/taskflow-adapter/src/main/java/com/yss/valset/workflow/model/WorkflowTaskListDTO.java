package com.yss.valset.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 工作流任务实例列表。
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTaskListDTO {

    @Builder.Default
    private List<WorkflowTaskInstanceDTO> taskList = new ArrayList<>();

    private String workflowInstanceState;
}
