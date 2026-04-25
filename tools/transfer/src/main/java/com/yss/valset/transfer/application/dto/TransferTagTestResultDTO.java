package com.yss.valset.transfer.application.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 标签试跑结果。
 */
@Data
@Builder
public class TransferTagTestResultDTO {

    private String tagId;

    private Boolean matched;

    private String matchStrategy;

    private String matchReason;

    private Boolean matchedByScript;

    private Boolean matchedByRegex;

    private String matchedField;

    private String matchedValue;

    private Map<String, Object> contextSnapshot;

    private String errorMessage;
}
