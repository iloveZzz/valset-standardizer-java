package com.yss.valset.transfer.domain.model;

import java.util.Map;

/**
 * 投递执行上下文。
 */
public record TransferContext(
        TransferObject transferObject,
        TransferRoute transferRoute,
        TransferTarget transferTarget,
        Map<String, Object> attributes
) {
}
