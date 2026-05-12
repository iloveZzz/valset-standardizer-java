package com.yss.valset.transfer.application.impl.query;

import com.yss.valset.transfer.application.dto.TransferDeliveryRecordSummaryViewDTO;
import com.yss.valset.transfer.application.service.TransferDeliveryRecordQueryService;
import com.yss.valset.transfer.domain.gateway.TransferDeliveryGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 默认文件投递结果查询服务。
 */
@Service
@RequiredArgsConstructor
public class DefaultTransferDeliveryRecordQueryService implements TransferDeliveryRecordQueryService {

    private static final ZoneId SHANGHAI_ZONE_ID = ZoneId.of("Asia/Shanghai");

    private final TransferDeliveryGateway transferDeliveryGateway;

    @Override
    public TransferDeliveryRecordSummaryViewDTO summarizeToday() {
        LocalDate today = LocalDate.now(SHANGHAI_ZONE_ID);
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
        long todayDeliveryCount = transferDeliveryGateway.countByDeliveredAtBetween(startOfDay, endOfDay);
        long todaySuccessCount = transferDeliveryGateway.countByDeliveredAtBetweenAndExecuteStatus(startOfDay, endOfDay, "SUCCESS");
        long todayFailedCount = transferDeliveryGateway.countByDeliveredAtBetweenAndExecuteStatus(startOfDay, endOfDay, "FAILED");
        double successRate = todayDeliveryCount == 0L
                ? 0D
                : Math.round(todaySuccessCount * 1000D / todayDeliveryCount) / 10D;
        return TransferDeliveryRecordSummaryViewDTO.builder()
                .todayDeliveryCount(todayDeliveryCount)
                .todaySuccessCount(todaySuccessCount)
                .todayFailedCount(todayFailedCount)
                .successRate(successRate)
                .build();
    }
}
