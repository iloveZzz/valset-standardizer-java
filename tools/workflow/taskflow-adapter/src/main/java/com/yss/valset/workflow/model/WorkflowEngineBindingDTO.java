package com.yss.valset.workflow.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工作流引擎绑定信息。
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowEngineBindingDTO {

    @NotNull
    private EtlPlatformType platformType;

    private String externalWorkflowId;

    private String externalProjectCode;

    private String externalNamespace;

    private String externalJobGroup;

    private String externalJobHandler;

    private String configJson;

    private Boolean externalOnline;

    private String externalReleaseState;

    private WorkflowSyncStatus syncStatus;

    private LocalDateTime firstSyncedAt;

    private LocalDateTime lastSyncedAt;

    private String syncFailureReason;

    private Integer remoteWorkflowVersionNo;

    @Builder.Default
    private Map<String, Object> attributes = new LinkedHashMap<>();
}
