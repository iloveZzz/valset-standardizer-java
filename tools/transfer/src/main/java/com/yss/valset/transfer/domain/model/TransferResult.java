package com.yss.valset.transfer.domain.model;

import java.util.List;

/**
 * 投递执行结果。
 */
public record TransferResult(
        boolean success,
        List<String> messages
) {
}
