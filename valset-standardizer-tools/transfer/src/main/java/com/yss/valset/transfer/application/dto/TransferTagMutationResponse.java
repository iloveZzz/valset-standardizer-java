package com.yss.valset.transfer.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 标签变更结果。
 */
@Data
@Builder
public class TransferTagMutationResponse {

    private String operation;

    private String message;

    private String formTemplateName;

    private TransferTagViewDTO tag;
}
