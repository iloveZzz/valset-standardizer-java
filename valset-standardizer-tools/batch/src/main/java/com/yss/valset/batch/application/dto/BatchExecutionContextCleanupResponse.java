package com.yss.valset.batch.application.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 批处理执行上下文清理结果。
 */
@Data
@Builder
public class BatchExecutionContextCleanupResponse implements java.io.Serializable {

    /**
     * 清理日期。
     */
    private LocalDate cleanupDate;

    /**
     * 清理阈值时间，早于该时间的执行上下文会被删除。
     */
    private LocalDateTime cleanupBefore;

    /**
     * 清理的 JobExecutionContext 数量。
     */
    private Long jobExecutionContextDeletedCount;

    /**
     * 清理的 StepExecutionContext 数量。
     */
    private Long stepExecutionContextDeletedCount;

    /**
     * 清理总数量。
     */
    private Long deletedCount;
}
