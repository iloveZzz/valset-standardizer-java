package com.yss.valset.transfer.domain.model;

import java.util.List;

/**
 * 规则执行结果。
 */
public record RuleEvaluationResult(
        boolean matched,
        List<TransferRoute> routes,
        String message
) {
}
