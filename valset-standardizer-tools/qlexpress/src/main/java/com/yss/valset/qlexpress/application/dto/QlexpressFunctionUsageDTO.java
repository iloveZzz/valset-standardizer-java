package com.yss.valset.qlexpress.application.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * QLExpress 函数使用关系视图。
 */
@Data
@Builder
public class QlexpressFunctionUsageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String functionId;

    private String functionName;

    private List<String> sourceModules;

    private List<QlexpressFunctionFlowUsageDTO> flowUsages;

    private List<QlexpressFunctionUsageReferenceDTO> directReferences;

    private List<QlexpressFunctionUsageReferenceDTO> dependencyReferences;

    private String usageStatus;

    private String usageStatusName;
}
