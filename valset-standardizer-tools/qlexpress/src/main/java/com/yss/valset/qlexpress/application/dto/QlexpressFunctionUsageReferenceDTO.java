package com.yss.valset.qlexpress.application.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * QLExpress 函数引用来源。
 */
@Data
@Builder
public class QlexpressFunctionUsageReferenceDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String flowCode;

    private String flowName;

    private String runnerScope;

    private String expression;

    private String sourceType;

    private String sourceTypeName;

    private String sourceId;

    private String sourceName;

    private Boolean enabled;

    private List<String> referencedFunctions;
}
