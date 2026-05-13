package com.yss.valset.workflow.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工作流阶段定义。
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStageDTO implements java.io.Serializable {

    @NotBlank
    private String stageCode;

    @NotBlank
    private String stageName;

    @NotNull
    private Integer stageOrder;

    private String description;

    @Builder.Default
    private boolean retryable = true;

    private Integer timeoutSeconds;
}
