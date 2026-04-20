package com.yss.valset.application.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 投递目标变更返回结果。
 */
@Data
@Builder
public class TransferTargetMutationResponse {

    private String operation;
    private String message;
    private TransferTargetViewDTO target;
}
