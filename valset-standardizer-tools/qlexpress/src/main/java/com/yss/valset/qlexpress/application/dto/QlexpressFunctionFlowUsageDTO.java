package com.yss.valset.qlexpress.application.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * QLExpress 函数在业务流程中的可用关系。
 */
@Data
@Builder
public class QlexpressFunctionFlowUsageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String flowCode;

    private String flowName;

    private String runnerScope;

    private Boolean matched;
}
