package com.yss.valset.workflow.model;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 工作流定义。
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDefinitionDTO implements java.io.Serializable {

    @NotBlank
    private String workflowCode;

    @NotBlank
    private String workflowName;

    @NotNull
    private Integer workflowVersionNo;

    @NotNull
    private EtlPlatformType platformType;

    private String description;

    @Builder.Default
    private boolean enabled = true;

    @Valid
    @Builder.Default
    private List<WorkflowStageDTO> stages = new ArrayList<>();

    @Valid
    private WorkflowEngineBindingDTO engineBinding;
}
