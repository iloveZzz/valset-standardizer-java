package com.yss.valset.transfer.domain.model;

import java.util.List;

/**
 * 标签分页结果。
 */
public record TransferTagPage(
        List<TransferTagDefinition> records,
        long total,
        long pageIndex,
        long pageSize
) {
}
