package com.yss.valset.transfer.domain.model;

import java.util.List;

/**
 * 文件投递结果分页结果。
 */
public record TransferDeliveryRecordPage(
        List<TransferDeliveryRecord> records,
        long total,
        long pageIndex,
        long pageSize
) {
}
