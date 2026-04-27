package com.yss.valset.transfer.application.impl.management;

import com.yss.valset.transfer.application.dto.TransferRunLogCleanupResponse;
import com.yss.valset.transfer.application.port.TransferRunLogMaintenanceUseCase;
import com.yss.valset.transfer.domain.gateway.TransferRunLogGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 默认文件收发运行日志维护服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultTransferRunLogMaintenanceAppService implements TransferRunLogMaintenanceUseCase {

    private static final ZoneId CLEANUP_ZONE_ID = ZoneId.of("Asia/Shanghai");

    private final TransferRunLogGateway transferRunLogGateway;

    @Override
    public TransferRunLogCleanupResponse cleanupYesterdayLogs() {
        LocalDate today = LocalDate.now(CLEANUP_ZONE_ID);
        LocalDate yesterday = today.minusDays(1);
        LocalDateTime startInclusive = yesterday.atStartOfDay();
        LocalDateTime endExclusive = today.atStartOfDay();
        long deletedCount = transferRunLogGateway.deleteLogsCreatedBetween(startInclusive, endExclusive);
        log.info("文件收发运行日志清理完成，cleanupDate={}，startInclusive={}，endExclusive={}，deletedCount={}",
                yesterday,
                startInclusive,
                endExclusive,
                deletedCount);
        return TransferRunLogCleanupResponse.builder()
                .cleanupDate(yesterday)
                .startInclusive(startInclusive)
                .endExclusive(endExclusive)
                .deletedCount(deletedCount)
                .build();
    }
}
