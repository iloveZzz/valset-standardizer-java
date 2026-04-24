package com.yss.valset.transfer.infrastructure.gateway;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yss.valset.transfer.domain.gateway.TransferRunLogGateway;
import com.yss.valset.transfer.domain.model.TransferRunLogAnalysis;
import com.yss.valset.transfer.domain.model.TransferRunLogStageAnalysis;
import com.yss.valset.transfer.domain.model.TransferRunLogStatusCount;
import com.yss.valset.transfer.domain.model.TransferRunLog;
import com.yss.valset.transfer.domain.model.TransferRunLogPage;
import com.yss.valset.transfer.domain.model.TransferRunStage;
import com.yss.valset.transfer.infrastructure.convertor.TransferRunLogMapper;
import com.yss.valset.transfer.infrastructure.entity.TransferRunLogPO;
import com.yss.valset.transfer.infrastructure.mapper.TransferRunLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MyBatis 支持的文件收发运行日志网关。
 */
@Primary
@Repository
@RequiredArgsConstructor
public class TransferRunLogGatewayImpl implements TransferRunLogGateway {

    private static final int DEFAULT_LIMIT = 100;

    private final TransferRunLogRepository transferRunLogRepository;
    private final TransferRunLogMapper transferRunLogMapper;

    @Override
    public TransferRunLog save(TransferRunLog transferRunLog) {
        TransferRunLogPO po = transferRunLogMapper.toPO(transferRunLog);
        if (po.getCreatedAt() == null) {
            po.setCreatedAt(LocalDateTime.now());
        }
        if (po.getRunLogId() == null) {
            transferRunLogRepository.insert(po);
        } else {
            transferRunLogRepository.updateById(po);
        }
        return transferRunLogMapper.toDomain(po);
    }

    @Override
    public Optional<TransferRunLog> findById(String runLogId) {
        Long id = parseLong(runLogId);
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(transferRunLogRepository.selectById(id))
                .map(transferRunLogMapper::toDomain);
    }

    @Override
    public long deleteFailedDeliverLogsByTransferId(String transferId) {
        Long transferIdValue = parseLong(transferId);
        if (transferIdValue == null) {
            return 0L;
        }
        return transferRunLogRepository.delete(
                Wrappers.lambdaQuery(TransferRunLogPO.class)
                        .eq(TransferRunLogPO::getTransferId, transferIdValue)
                        .eq(TransferRunLogPO::getRunStage, TransferRunStage.DELIVER.name())
                        .eq(TransferRunLogPO::getRunStatus, "FAILED")
        );
    }

    @Override
    public List<TransferRunLog> listLogs(String sourceId,
                                         String transferId,
                                         String routeId,
                                         String runStage,
                                         String runStatus,
                                         String triggerType,
                                         Integer limit) {
        int maxSize = limit == null || limit <= 0 ? DEFAULT_LIMIT : limit;
        List<TransferRunLog> logs;
        if (TransferRunStage.DELIVER.name().equalsIgnoreCase(trimToEmpty(runStage))
                && "FAILED".equalsIgnoreCase(trimToEmpty(runStatus))) {
            logs = collapseLatestTransferObjectLogs(loadLogs(sourceId, transferId, routeId, runStage, runStatus, triggerType, false, null, null));
        } else {
            logs = loadLogs(sourceId, transferId, routeId, runStage, runStatus, triggerType, true, null, limit);
        }
        return logs.size() <= maxSize ? logs : logs.subList(0, maxSize);
    }

    @Override
    public TransferRunLogPage pageLogs(String sourceId,
                                       String transferId,
                                       String routeId,
                                       String runStage,
                                       String runStatus,
        String triggerType,
        String keyword,
        Integer pageIndex,
        Integer pageSize) {
        int current = pageIndex == null || pageIndex < 0 ? 1 : pageIndex + 1;
        int size = pageSize == null || pageSize <= 0 ? DEFAULT_LIMIT : pageSize;
        if (TransferRunStage.DELIVER.name().equalsIgnoreCase(trimToEmpty(runStage))
                && "FAILED".equalsIgnoreCase(trimToEmpty(runStatus))) {
            List<TransferRunLog> logs = loadLogs(sourceId, transferId, routeId, runStage, runStatus, triggerType, false, keyword, null);
            List<TransferRunLog> collapsedLogs = collapseLatestTransferObjectLogs(logs);
            List<TransferRunLog> records = slice(collapsedLogs, current - 1, size);
            return new TransferRunLogPage(records, collapsedLogs.size(), current - 1, size);
        }
        Long sourceIdValue = parseLong(sourceId);
        Long transferIdValue = parseLong(transferId);
        Long routeIdValue = parseLong(routeId);
        Page<TransferRunLogPO> page = transferRunLogRepository.selectPage(
                new Page<>(current, size),
                Wrappers.lambdaQuery(TransferRunLogPO.class)
                        .eq(sourceIdValue != null, TransferRunLogPO::getSourceId, sourceIdValue)
                        .eq(transferIdValue != null, TransferRunLogPO::getTransferId, transferIdValue)
                        .eq(routeIdValue != null, TransferRunLogPO::getRouteId, routeIdValue)
                        .eq(runStage != null && !runStage.isBlank(), TransferRunLogPO::getRunStage, runStage)
                        .eq(runStatus != null && !runStatus.isBlank(), TransferRunLogPO::getRunStatus, runStatus)
                        .eq(triggerType != null && !triggerType.isBlank(), TransferRunLogPO::getTriggerType, triggerType)
                        .and(StringUtils.hasText(keyword), wrapper -> wrapper
                                .like(TransferRunLogPO::getSourceCode, keyword)
                                .or()
                                .like(TransferRunLogPO::getSourceName, keyword)
                                .or()
                                .like(TransferRunLogPO::getTransferId, keyword)
                                .or()
                                .like(TransferRunLogPO::getRouteId, keyword)
                                .or()
                                .like(TransferRunLogPO::getLogMessage, keyword)
                                .or()
                                .like(TransferRunLogPO::getErrorMessage, keyword))
                        .orderByDesc(TransferRunLogPO::getCreatedAt)
                        .orderByDesc(TransferRunLogPO::getRunLogId)
        );
        List<TransferRunLog> records = page.getRecords() == null ? Collections.emptyList() : page.getRecords().stream()
                .map(transferRunLogMapper::toDomain)
                .toList();
        return new TransferRunLogPage(records, page.getTotal(), page.getCurrent() - 1, page.getSize());
    }

    @Override
    public TransferRunLogAnalysis analyzeLogs(String sourceId,
                                              String transferId,
                                              String routeId,
                                              String runStage,
        String runStatus,
        String triggerType,
        String keyword) {
        List<TransferRunLog> logs = loadLogs(sourceId, transferId, routeId, runStage, runStatus, triggerType, false, keyword, null);
        Map<String, List<TransferRunLog>> logsByStage = logs.stream()
                .filter(log -> StringUtils.hasText(log.runStage()))
                .collect(Collectors.groupingBy(TransferRunLog::runStage, LinkedHashMap::new, Collectors.toList()));

        List<TransferRunLogStageAnalysis> stageAnalyses = EnumSet.allOf(com.yss.valset.transfer.domain.model.TransferRunStage.class)
                .stream()
                .map(stage -> {
                    List<TransferRunLog> stageLogs = logsByStage.getOrDefault(stage.name(), List.of());
                    if (TransferRunStage.DELIVER.name().equals(stage.name())) {
                        stageLogs = collapseLatestTransferObjectLogs(stageLogs);
                    }
                    Map<String, Long> statusCountMap = stageLogs.stream()
                            .filter(log -> StringUtils.hasText(log.runStatus()))
                            .collect(Collectors.groupingBy(TransferRunLog::runStatus, LinkedHashMap::new, Collectors.counting()));
                    List<TransferRunLogStatusCount> statusCounts = statusCountMap.entrySet().stream()
                            .map(entry -> new TransferRunLogStatusCount(entry.getKey(), entry.getValue()))
                            .toList();
                    return new TransferRunLogStageAnalysis(stage.name(), stageLogs.size(), statusCounts);
                })
                .toList();

        return new TransferRunLogAnalysis(logs.size(), stageAnalyses);
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.valueOf(value);
    }

    private List<TransferRunLog> loadLogs(String sourceId,
                                          String transferId,
                                          String routeId,
                                          String runStage,
                                          String runStatus,
                                          String triggerType,
                                          boolean useLimit,
                                          String keyword,
                                          Integer limit) {
        Long sourceIdValue = parseLong(sourceId);
        Long transferIdValue = parseLong(transferId);
        Long routeIdValue = parseLong(routeId);
        var query = Wrappers.lambdaQuery(TransferRunLogPO.class)
                .eq(sourceIdValue != null, TransferRunLogPO::getSourceId, sourceIdValue)
                .eq(transferIdValue != null, TransferRunLogPO::getTransferId, transferIdValue)
                .eq(routeIdValue != null, TransferRunLogPO::getRouteId, routeIdValue)
                .eq(runStage != null && !runStage.isBlank(), TransferRunLogPO::getRunStage, runStage)
                .eq(runStatus != null && !runStatus.isBlank(), TransferRunLogPO::getRunStatus, runStatus)
                .eq(triggerType != null && !triggerType.isBlank(), TransferRunLogPO::getTriggerType, triggerType)
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(TransferRunLogPO::getSourceCode, keyword)
                        .or()
                        .like(TransferRunLogPO::getSourceName, keyword)
                        .or()
                        .like(TransferRunLogPO::getTransferId, keyword)
                        .or()
                        .like(TransferRunLogPO::getRouteId, keyword)
                        .or()
                        .like(TransferRunLogPO::getLogMessage, keyword)
                        .or()
                        .like(TransferRunLogPO::getErrorMessage, keyword))
                .orderByDesc(TransferRunLogPO::getCreatedAt)
                .orderByDesc(TransferRunLogPO::getRunLogId);
        if (useLimit) {
            int maxSize = limit == null || limit <= 0 ? DEFAULT_LIMIT : limit;
            query.last("limit " + maxSize);
        }
        return transferRunLogRepository.selectList(query)
                .stream()
                .map(transferRunLogMapper::toDomain)
                .toList();
    }

    private List<TransferRunLog> collapseLatestTransferObjectLogs(List<TransferRunLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return List.of();
        }
        Map<String, TransferRunLog> latestByTransferId = new LinkedHashMap<>();
        for (TransferRunLog log : logs) {
            if (log == null) {
                continue;
            }
            String key = StringUtils.hasText(log.transferId())
                    ? log.transferId().trim()
                    : "__runLog__" + trimToEmpty(log.runLogId());
            latestByTransferId.putIfAbsent(key, log);
        }
        return new ArrayList<>(latestByTransferId.values());
    }

    private List<TransferRunLog> slice(List<TransferRunLog> logs, int pageIndex, int pageSize) {
        if (logs == null || logs.isEmpty()) {
            return List.of();
        }
        int fromIndex = Math.max(pageIndex, 0) * Math.max(pageSize, 1);
        if (fromIndex >= logs.size()) {
            return List.of();
        }
        int toIndex = Math.min(fromIndex + Math.max(pageSize, 1), logs.size());
        return logs.subList(fromIndex, toIndex);
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
