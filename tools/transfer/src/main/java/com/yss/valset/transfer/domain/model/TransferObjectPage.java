package com.yss.valset.transfer.domain.model;

import java.util.List;

/**
 * 文件主对象分页结果。
 */
public record TransferObjectPage(
        List<TransferObject> records,
        long total,
        long pageIndex,
        long pageSize
) {
}
