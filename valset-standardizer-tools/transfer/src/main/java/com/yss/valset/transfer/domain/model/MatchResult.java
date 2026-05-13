package com.yss.valset.transfer.domain.model;

import java.util.List;

/**
 * 规则匹配结果。
 */
public record MatchResult(
        boolean matched,
        List<TransferRoute> routes,
        String reason
) {
}
