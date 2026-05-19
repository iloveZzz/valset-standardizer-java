package com.yss.valset.qlexpress.application.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Set;

/**
 * QLExpress 自定义函数调试结果。
 */
@Data
@Builder
public class QlexpressFunctionDebugResultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Boolean success;

    private Object result;

    private String errorMessage;

    private Long costMs;

    private Set<String> outFunctions;

    private Set<String> outVarNames;
}
