package com.yss.valset.workflow.model;

import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工作流回调请求。
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowCallbackRequest {

    @NotBlank
    private String stageCode;

    private String rawStatus;

    private String message;

    @Builder.Default
    private Map<String, Object> payload = new LinkedHashMap<>();
}
