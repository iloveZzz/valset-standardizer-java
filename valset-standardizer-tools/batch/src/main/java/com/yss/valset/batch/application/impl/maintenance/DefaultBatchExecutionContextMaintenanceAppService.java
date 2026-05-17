package com.yss.valset.batch.application.impl.maintenance;

import com.yss.valset.batch.application.dto.BatchExecutionContextCleanupResponse;
import com.yss.valset.batch.application.port.BatchExecutionContextMaintenanceUseCase;
import com.yss.valset.batch.infrastructure.mapper.BatchExecutionContextMaintenanceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 默认批处理执行上下文维护服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultBatchExecutionContextMaintenanceAppService implements BatchExecutionContextMaintenanceUseCase {

    private static final ZoneId CLEANUP_ZONE_ID = ZoneId.of("Asia/Shanghai");

    private final BatchExecutionContextMaintenanceMapper maintenanceMapper;

    @Value("${subject.match.batch.execution-context-cleanup.retention-days:1}")
    private int retentionDays;

    @Override
    public BatchExecutionContextCleanupResponse cleanupHistoricalExecutionContexts() {
        LocalDate cleanupDate = LocalDate.now(CLEANUP_ZONE_ID);
        int effectiveRetentionDays = Math.max(1, retentionDays);
        LocalDateTime cleanupBefore = cleanupDate.minusDays(effectiveRetentionDays).atStartOfDay();
        long stepDeletedCount = deleteStepExecutionContextsBefore(cleanupBefore);
        long jobDeletedCount = deleteJobExecutionContextsBefore(cleanupBefore);
        long deletedCount = stepDeletedCount + jobDeletedCount;
        log.info("批处理执行上下文清理完成，cleanupDate={}，retentionDays={}，cleanupBefore={}，jobDeletedCount={}，stepDeletedCount={}，deletedCount={}",
                cleanupDate,
                effectiveRetentionDays,
                cleanupBefore,
                jobDeletedCount,
                stepDeletedCount,
                deletedCount);
        return BatchExecutionContextCleanupResponse.builder()
                .cleanupDate(cleanupDate)
                .cleanupBefore(cleanupBefore)
                .jobExecutionContextDeletedCount(jobDeletedCount)
                .stepExecutionContextDeletedCount(stepDeletedCount)
                .deletedCount(deletedCount)
                .build();
    }

    private long deleteJobExecutionContextsBefore(LocalDateTime cleanupBefore) {
        return maintenanceMapper.deleteJobExecutionContextsBefore(cleanupBefore);
    }

    private long deleteStepExecutionContextsBefore(LocalDateTime cleanupBefore) {
        return maintenanceMapper.deleteStepExecutionContextsBefore(cleanupBefore);
    }
}
