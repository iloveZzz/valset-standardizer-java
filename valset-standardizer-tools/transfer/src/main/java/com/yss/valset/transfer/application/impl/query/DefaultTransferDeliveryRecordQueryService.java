package com.yss.valset.transfer.application.impl.query;

import com.yss.valset.transfer.application.dto.TransferDeliveryRecordSummaryViewDTO;
import com.yss.valset.transfer.application.service.TransferDeliveryRecordQueryService;
import com.yss.valset.transfer.domain.gateway.TransferDeliveryGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
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
    public TransferDeliveryRecordSummaryViewDTO summarizeToday(String taskDate) {
        LocalDate day = resolveTaskDate(taskDate);
        LocalDateTime startOfDay = day.atStartOfDay();
        LocalDateTime endOfDay = day.plusDays(1).atStartOfDay();
        long todayDeliveryCount = transferDeliveryGateway.countDistinctTransferIdsByDeliveredAtBetween(startOfDay, endOfDay);
        long todaySuccessCount = transferDeliveryGateway.countDistinctTransferIdsByDeliveredAtBetweenAndExecuteStatus(startOfDay, endOfDay, "SUCCESS");
        long todayFailedCount = transferDeliveryGateway.countDistinctTransferIdsByDeliveredAtBetweenAndExecuteStatus(startOfDay, endOfDay, "FAILED");
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

    private LocalDate resolveTaskDate(String taskDate) {
        if (taskDate == null || taskDate.trim().isEmpty()) {
            return LocalDate.now(SHANGHAI_ZONE_ID);
        }
        try {
            return LocalDate.parse(taskDate.trim());
        } catch (DateTimeParseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的任务日期: " + taskDate, exception);
        }
    }
}
