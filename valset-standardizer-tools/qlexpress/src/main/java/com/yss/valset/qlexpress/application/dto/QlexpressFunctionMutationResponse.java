package com.yss.valset.qlexpress.application.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * QLExpress 自定义函数变更响应。
 */
@Data
@Builder
public class QlexpressFunctionMutationResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String operation;

    private String message;

    private QlexpressFunctionViewDTO function;
}
