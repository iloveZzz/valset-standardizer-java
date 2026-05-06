package com.yss.valset.workflow.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工作流触发请求。
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTriggerRequest {

    @NotBlank
    private String workflowCode;

    @NotNull
    private Integer workflowVersionNo;

    private String businessKey;

    private String stageCode;

    private boolean force;

    @Builder.Default
    private Map<String, Object> context = new LinkedHashMap<>();
}
