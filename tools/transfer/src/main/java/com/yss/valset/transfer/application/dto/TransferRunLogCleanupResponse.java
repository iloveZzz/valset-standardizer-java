package com.yss.valset.transfer.application.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 文件收发运行日志清理返回结果。
 */
@Data
@Builder
public class TransferRunLogCleanupResponse {

    /**
     * 清理日期。
     */
    private LocalDate cleanupDate;

    /**
     * 清理起始时间。
     */
    private LocalDateTime startInclusive;

    /**
     * 清理结束时间。
     */
    private LocalDateTime endExclusive;

    /**
     * 清理数量。
     */
    private Long deletedCount;
}
