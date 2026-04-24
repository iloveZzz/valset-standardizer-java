package com.yss.valset.transfer.domain.model;

import java.util.List;

/**
 * 文件收发运行日志分页结果。
 */
public record TransferRunLogPage(
        List<TransferRunLog> records,
        long total,
        long pageIndex,
        long pageSize
) {
}
