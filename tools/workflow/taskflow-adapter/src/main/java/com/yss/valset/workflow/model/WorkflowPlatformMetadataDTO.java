package com.yss.valset.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 工作流平台元数据。
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowPlatformMetadataDTO {

    private EtlPlatformType platformType;

    private String platformName;

    private String description;

    @Builder.Default
    private List<String> requiredBindingFields = new ArrayList<>();

    @Builder.Default
    private List<String> supportedOperations = new ArrayList<>();
}
