package com.yss.valset.transfer.application.impl.management;

import com.yss.valset.transfer.application.command.TransferRunLogRedeliverCommand;
import com.yss.valset.transfer.application.dto.TransferRunLogCleanupResponse;
import com.yss.valset.transfer.application.dto.TransferRunLogRedeliverItemViewDTO;
import com.yss.valset.transfer.application.dto.TransferRunLogRedeliverResponse;
import com.yss.valset.transfer.application.service.TransferRunLogManagementAppService;
import com.yss.valset.transfer.domain.gateway.TransferRunLogGateway;
import com.yss.valset.transfer.domain.model.TransferRunLog;
import com.yss.valset.transfer.domain.model.TransferRunStage;
import com.yss.valset.transfer.domain.model.TransferRunStatus;
import com.yss.valset.transfer.application.port.TransferProcessUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 默认文件收发运行日志管理服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultTransferRunLogManagementAppService implements TransferRunLogManagementAppService {

    private static final ZoneId CLEANUP_ZONE_ID = ZoneId.of("Asia/Shanghai");

    private final TransferRunLogGateway transferRunLogGateway;
    private final TransferProcessUseCase transferProcessUseCase;

    @Override
    public TransferRunLogRedeliverResponse redeliver(TransferRunLogRedeliverCommand command) {
        List<String> runLogIds = normalizeIds(command == null ? null : command.getRunLogIds());
        if (runLogIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择需要重新投递的运行日志");
        }
        log.info("开始运行日志重投递，requestedCount={}", runLogIds.size());

        List<TransferRunLogRedeliverItemViewDTO> items = new ArrayList<>();
        Set<String> processedKeys = new LinkedHashSet<>();
        int successCount = 0;
        int failureCount = 0;
        int skippedCount = 0;

        for (String runLogId : runLogIds) {
            TransferRunLog runLog = transferRunLogGateway.findById(runLogId).orElse(null);
            if (runLog == null) {
                failureCount++;
                items.add(buildItem(runLogId, null, null, null, null, false, "未找到运行日志，runLogId=" + runLogId));
                continue;
            }

            if (!isFailedDeliverLog(runLog)) {
                skippedCount++;
                items.add(buildItem(runLog.runLogId(), runLog.transferId(), runLog.routeId(), runLog.runStage(), runLog.runStatus(), false,
                        "仅支持对目标投递失败日志执行重投递"));
                continue;
            }

            String dedupeKey = buildDeduplicateKey(runLog.routeId(), runLog.transferId());
            if (!processedKeys.add(dedupeKey)) {
                skippedCount++;
                items.add(buildItem(runLog.runLogId(), runLog.transferId(), runLog.routeId(), runLog.runStage(), runLog.runStatus(), false,
                        "已跳过重复的路由与分拣对象组合"));
                continue;
            }

            try {
                transferProcessUseCase.deliver(runLog.routeId(), runLog.transferId());
                successCount++;
                items.add(buildItem(runLog.runLogId(), runLog.transferId(), runLog.routeId(), runLog.runStage(), runLog.runStatus(), true,
                        "重新投递成功，routeId=" + runLog.routeId() + "，transferId=" + runLog.transferId()));
            } catch (RuntimeException exception) {
                failureCount++;
                items.add(buildItem(runLog.runLogId(), runLog.transferId(), runLog.routeId(), runLog.runStage(), runLog.runStatus(), false,
                        buildFailureMessage(exception)));
            }
        }
        log.info("运行日志重投递完成，requestedCount={}，successCount={}，failureCount={}，skippedCount={}",
                runLogIds.size(),
                successCount,
                failureCount,
                skippedCount);

        return TransferRunLogRedeliverResponse.builder()
                .requestedCount(runLogIds.size())
                .successCount(successCount)
                .failureCount(failureCount)
                .skippedCount(skippedCount)
                .items(items)
                .build();
    }

    @Override
    public TransferRunLogCleanupResponse cleanupYesterdayLogs() {
        LocalDate today = LocalDate.now(CLEANUP_ZONE_ID);
        LocalDate yesterday = today.minusDays(1);
        LocalDateTime startInclusive = yesterday.atStartOfDay();
        LocalDateTime endExclusive = today.atStartOfDay();
        long deletedCount = transferRunLogGateway.deleteLogsCreatedBetween(startInclusive, endExclusive);
        log.info("文件收发运行日志手动清理完成，cleanupDate={}，startInclusive={}，endExclusive={}，deletedCount={}",
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

    private boolean isFailedDeliverLog(TransferRunLog runLog) {
        return runLog != null
                && TransferRunStage.DELIVER.name().equalsIgnoreCase(trimToEmpty(runLog.runStage()))
                && TransferRunStatus.FAILED.name().equalsIgnoreCase(trimToEmpty(runLog.runStatus()))
                && StringUtils.hasText(runLog.routeId())
                && StringUtils.hasText(runLog.transferId());
    }

    private List<String> normalizeIds(List<String> runLogIds) {
        if (runLogIds == null || runLogIds.isEmpty()) {
            return List.of();
        }
        return runLogIds.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private TransferRunLogRedeliverItemViewDTO buildItem(String runLogId,
                                                         String transferId,
                                                         String routeId,
                                                         String runStage,
                                                         String runStatus,
                                                         boolean success,
                                                         String message) {
        return TransferRunLogRedeliverItemViewDTO.builder()
                .runLogId(runLogId)
                .transferId(transferId)
                .routeId(routeId)
                .runStage(runStage)
                .runStatus(runStatus)
                .success(success)
                .message(message)
                .build();
    }

    private String buildDeduplicateKey(String routeId, String transferId) {
        return trimToEmpty(routeId) + "::" + trimToEmpty(transferId);
    }

    private String buildFailureMessage(RuntimeException exception) {
        if (exception == null) {
            return "重新投递失败";
        }
        String message = exception.getMessage();
        if (!StringUtils.hasText(message)) {
            return "重新投递失败，原因未知";
        }
        return "重新投递失败：" + message;
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
