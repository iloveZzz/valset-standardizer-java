package com.yss.valset.transfer.application.impl.query;

import com.yss.cloud.dto.result.PageResult;
import com.yss.valset.transfer.application.dto.TransferRunLogViewDTO;
import com.yss.valset.transfer.application.dto.TransferRunLogAnalysisViewDTO;
import com.yss.valset.transfer.application.dto.TransferRunLogStageAnalysisViewDTO;
import com.yss.valset.transfer.application.dto.TransferRunLogStatusCountViewDTO;
import com.yss.valset.transfer.application.dto.TransferRunLogTrendViewDTO;
import com.yss.valset.transfer.application.service.TransferRunLogQueryService;
import com.yss.valset.transfer.domain.gateway.TransferRunLogGateway;
import com.yss.valset.transfer.domain.gateway.TransferRouteGateway;
import com.yss.valset.transfer.domain.gateway.TransferSourceGateway;
import com.yss.valset.transfer.domain.gateway.TransferTargetGateway;
import com.yss.valset.transfer.domain.gateway.TransferObjectGateway;
import com.yss.valset.transfer.domain.model.TransferRunLogAnalysis;
import com.yss.valset.transfer.domain.model.TransferRunLogStageAnalysis;
import com.yss.valset.transfer.domain.model.TransferRunLogStatusCount;
import com.yss.valset.transfer.domain.model.TransferRunStage;
import com.yss.valset.transfer.domain.model.TransferRunStatus;
import com.yss.valset.transfer.domain.model.TransferTriggerType;
import com.yss.valset.transfer.domain.model.TransferRunLogPage;
import com.yss.valset.transfer.domain.model.TransferRunLog;
import com.yss.valset.transfer.domain.model.TransferRoute;
import com.yss.valset.transfer.domain.model.TransferSource;
import com.yss.valset.transfer.domain.model.TransferTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

/**
 * 默认文件收发运行日志查询服务。
 */
@Service
@RequiredArgsConstructor
public class DefaultTransferRunLogQueryService implements TransferRunLogQueryService {

    private static final ZoneId SHANGHAI_ZONE_ID = ZoneId.of("Asia/Shanghai");

    private final TransferRunLogGateway transferRunLogGateway;
    private final TransferRouteGateway transferRouteGateway;
    private final TransferSourceGateway transferSourceGateway;
    private final TransferTargetGateway transferTargetGateway;
    private final TransferObjectGateway transferObjectGateway;

    @Override
    public List<TransferRunLogViewDTO> listLogs(String sourceId,
                                                String transferId,
                                                String routeId,
                                                String runStage,
                                                String runStatus,
                                                String triggerType,
                                                String taskDate,
                                                Integer limit) {
        String normalizedStage = normalizeEnum(runStage, TransferRunStage.class, "运行阶段");
        String normalizedStatus = normalizeEnum(runStatus, TransferRunStatus.class, "运行状态");
        String normalizedTriggerType = normalizeEnum(triggerType, TransferTriggerType.class, "触发类型");
        String normalizedTaskDate = normalizeTaskDate(taskDate);
        LocalDateTime taskStart = resolveTaskStart(normalizedTaskDate);
        LocalDateTime taskEnd = resolveTaskEnd(normalizedTaskDate);
        return transferRunLogGateway.listLogs(sourceId, transferId, routeId, normalizedStage, normalizedStatus, normalizedTriggerType, taskStart, taskEnd, limit)
                .stream()
                .map(this::toView)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public PageResult<TransferRunLogViewDTO> pageLogs(String sourceId,
                                                      String transferId,
                                                      String routeId,
                                                      String runStage,
                                                      String runStatus,
                                                      String triggerType,
                                                      String keyword,
                                                      String taskDate,
                                                      Integer pageIndex,
                                                      Integer pageSize) {
        String normalizedStage = normalizeEnum(runStage, TransferRunStage.class, "运行阶段");
        String normalizedStatus = normalizeEnum(runStatus, TransferRunStatus.class, "运行状态");
        String normalizedTriggerType = normalizeEnum(triggerType, TransferTriggerType.class, "触发类型");
        String normalizedTaskDate = normalizeTaskDate(taskDate);
        LocalDateTime taskStart = resolveTaskStart(normalizedTaskDate);
        LocalDateTime taskEnd = resolveTaskEnd(normalizedTaskDate);
        TransferRunLogPage page = transferRunLogGateway.pageLogs(
                sourceId,
                transferId,
                routeId,
                normalizedStage,
                normalizedStatus,
                normalizedTriggerType,
                keyword,
                taskStart,
                taskEnd,
                pageIndex,
                pageSize
        );
        return PageResult.of(
                page.records().stream().map(this::toView).collect(java.util.stream.Collectors.toList()),
                page.total(),
                page.pageSize(),
                pageIndex
        );
    }

    @Override
    public TransferRunLogAnalysisViewDTO analyzeLogs(String sourceId,
                                                     String transferId,
                                                     String routeId,
                                                     String runStage,
                                                     String runStatus,
                                                     String triggerType,
                                                     String keyword,
                                                     String taskDate) {
        String normalizedStage = normalizeEnum(runStage, TransferRunStage.class, "运行阶段");
        String normalizedStatus = normalizeEnum(runStatus, TransferRunStatus.class, "运行状态");
        String normalizedTriggerType = normalizeEnum(triggerType, TransferTriggerType.class, "触发类型");
        String normalizedTaskDate = normalizeTaskDate(taskDate);
        LocalDateTime taskStart = resolveTaskStart(normalizedTaskDate);
        LocalDateTime taskEnd = resolveTaskEnd(normalizedTaskDate);
        TransferRunLogAnalysis analysis = transferRunLogGateway.analyzeLogs(
                sourceId,
                transferId,
                routeId,
                normalizedStage,
                normalizedStatus,
                normalizedTriggerType,
                keyword,
                taskStart,
                taskEnd
        );
        return TransferRunLogAnalysisViewDTO.builder()
                .totalCount(analysis.totalCount())
                .sourceCount(stageTotal(analysis, "INGEST"))
                .routeCount(stageTotal(analysis, "ROUTE"))
                .targetCount(stageTotal(analysis, "DELIVER"))
                .stageAnalyses(analysis.stageAnalyses() == null ? java.util.Arrays.asList() : analysis.stageAnalyses().stream().map(this::toStageView).collect(java.util.stream.Collectors.toList()))
                .build();
    }

    @Override
    public List<TransferRunLogTrendViewDTO> trendLogs(Integer days, String taskDate) {
        int window = days == null || days <= 0 ? 30 : days;
        LocalDate endDate = resolveTaskDate(taskDate);
        LocalDate startDate = endDate.minusDays(Math.max(window, 1) - 1L);
        LocalDateTime startInclusive = startDate.atStartOfDay();
        LocalDateTime endExclusive = endDate.plusDays(1L).atStartOfDay();
        return transferRunLogGateway.trendLogs(startInclusive, endExclusive)
                .stream()
                .map(trend -> TransferRunLogTrendViewDTO.builder()
                        .trendDate(trend.getTrendDate())
                        .count(trend.getCount())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    private LocalDateTime resolveTaskStart(String taskDate) {
        LocalDate taskDay = resolveTaskDate(taskDate);
        return taskDay == null ? null : taskDay.atStartOfDay();
    }

    private LocalDateTime resolveTaskEnd(String taskDate) {
        LocalDate taskDay = resolveTaskDate(taskDate);
        return taskDay == null ? null : taskDay.plusDays(1L).atStartOfDay();
    }

    private LocalDate resolveTaskDate(String taskDate) {
        if (!StringUtils.hasText(taskDate)) {
            return LocalDate.now(SHANGHAI_ZONE_ID);
        }
        try {
            return LocalDate.parse(taskDate.trim());
        } catch (DateTimeParseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的任务日期: " + taskDate, exception);
        }
    }

    private String normalizeTaskDate(String taskDate) {
        if (!StringUtils.hasText(taskDate)) {
            return null;
        }
        try {
            LocalDate.parse(taskDate.trim());
            return taskDate.trim();
        } catch (DateTimeParseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的任务日期: " + taskDate, exception);
        }
    }

    private String normalizeEnum(String value, Class<? extends Enum<?>> enumType, String fieldName) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        try {
            Enum.valueOf((Class) enumType, normalized);
            return normalized;
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的" + fieldName + ": " + value, exception);
        }
    }

    private TransferRunLogViewDTO toView(TransferRunLog runLog) {
        Map<String, String> names = resolveDisplayNames(runLog);
        return TransferRunLogViewDTO.builder()
                .runLogId(runLog.runLogId() == null ? null : String.valueOf(runLog.runLogId()))
                .sourceId(runLog.sourceId() == null ? null : String.valueOf(runLog.sourceId()))
                .sourceType(runLog.sourceType())
                .sourceCode(runLog.sourceCode())
                .sourceName(names.get("sourceName"))
                .originalName(resolveOriginalName(runLog))
                .routeName(names.get("routeName"))
                .targetName(names.get("targetName"))
                .transferId(runLog.transferId() == null ? null : String.valueOf(runLog.transferId()))
                .routeId(runLog.routeId() == null ? null : String.valueOf(runLog.routeId()))
                .triggerType(runLog.triggerType())
                .runStage(runLog.runStage())
                .runStatus(runLog.runStatus())
                .runStatusLabel(resolveStatusLabel(runLog.runStatus()))
                .logMessage(runLog.logMessage())
                .errorMessage(runLog.errorMessage())
                .createdAt(runLog.createdAt())
                .build();
    }

    private Map<String, String> resolveDisplayNames(TransferRunLog runLog) {
        String sourceName = resolveSourceName(runLog);
        TransferRoute route = StringUtils.hasText(runLog.routeId())
                ? transferRouteGateway.findById(runLog.routeId()).orElse(null)
                : null;
        String routeTargetName = route == null ? null : resolveTargetNameByCode(route.targetCode());
        String targetDisplayName = route == null
                ? null
                : fallbackName(routeTargetName, route.targetCode());
        String routeName = route == null
                ? fallbackName(sourceName, runLog.routeId())
                : joinDisplayName(resolveSourceNameById(route.sourceId()), targetDisplayName, route.routeId());
        String targetName = targetDisplayName;
        Map<String, String> names = new java.util.LinkedHashMap<>();
        names.put("sourceName", sourceName);
        names.put("routeName", routeName);
        names.put("targetName", targetName);
        return names;
    }

    private String resolveOriginalName(TransferRunLog runLog) {
        if (!StringUtils.hasText(runLog.transferId())) {
            return null;
        }
        return transferObjectGateway.findById(runLog.transferId())
                .map(transferObject -> trimToNull(transferObject.originalName()))
                .orElse(null);
    }

    private String resolveSourceName(TransferRunLog runLog) {
        String sourceName = trimToNull(runLog.sourceName());
        if (sourceName != null) {
            return sourceName;
        }
        if (!StringUtils.hasText(runLog.sourceId())) {
            return fallbackName(runLog.sourceCode(), runLog.sourceId());
        }
        return transferSourceGateway.findById(runLog.sourceId())
                .map(TransferSource::sourceName)
                .map(this::trimToNull)
                .filter(StringUtils::hasText)
                .orElse(fallbackName(runLog.sourceCode(), runLog.sourceId()));
    }

    private String resolveSourceNameById(String sourceId) {
        if (!StringUtils.hasText(sourceId)) {
            return null;
        }
        return transferSourceGateway.findById(sourceId)
                .map(TransferSource::sourceName)
                .map(this::trimToNull)
                .filter(StringUtils::hasText)
                .orElse(null);
    }

    private String resolveTargetNameByCode(String targetCode) {
        if (!StringUtils.hasText(targetCode)) {
            return null;
        }
        return transferTargetGateway.findByTargetCode(targetCode)
                .map(TransferTarget::targetName)
                .map(this::trimToNull)
                .filter(StringUtils::hasText)
                .orElse(null);
    }

    private String joinDisplayName(String sourceName, String targetName, String fallback) {
        if (StringUtils.hasText(sourceName) && StringUtils.hasText(targetName)) {
            return sourceName + " → " + targetName;
        }
        if (StringUtils.hasText(sourceName)) {
            return sourceName;
        }
        if (StringUtils.hasText(targetName)) {
            return targetName;
        }
        return fallback;
    }

    private String fallbackName(String preferred, String fallback) {
        String value = trimToNull(preferred);
        if (value != null) {
            return value;
        }
        return trimToNull(fallback);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    private TransferRunLogStageAnalysisViewDTO toStageView(TransferRunLogStageAnalysis stageAnalysis) {
        return TransferRunLogStageAnalysisViewDTO.builder()
                .runStage(stageAnalysis.runStage())
                .stageLabel(resolveStageLabel(stageAnalysis.runStage()))
                .totalCount(stageAnalysis.totalCount())
                .statusCounts(stageAnalysis.statusCounts().stream().map(this::toStatusView).collect(java.util.stream.Collectors.toList()))
                .build();
    }

    private TransferRunLogStatusCountViewDTO toStatusView(TransferRunLogStatusCount statusCount) {
        return TransferRunLogStatusCountViewDTO.builder()
                .runStatus(statusCount.runStatus())
                .statusLabel(resolveStatusLabel(statusCount.runStatus()))
                .count(statusCount.count())
                .build();
    }

    private String resolveStageLabel(String runStage) {
        if (!StringUtils.hasText(runStage)) {
            return "-";
        }
        String normalized = runStage.trim().toUpperCase(Locale.ROOT);
        if ("INGEST".equals(normalized)) {
            return "来源";
        }
        if ("ROUTE".equals(normalized)) {
            return "路由";
        }
        if ("DELIVER".equals(normalized)) {
            return "目标";
        }
        return runStage;
    }

    private String resolveStatusLabel(String runStatus) {
        if (!StringUtils.hasText(runStatus)) {
            return "-";
        }
        String normalized = runStatus.trim().toUpperCase(Locale.ROOT);
        if ("SUCCESS".equals(normalized)) {
            return "成功";
        }
        if ("FAILED".equals(normalized)) {
            return "失败";
        }
        return runStatus;
    }

    private Long stageTotal(TransferRunLogAnalysis analysis,
                            String stage) {
        if (analysis == null || analysis.stageAnalyses() == null) {
            return 0L;
        }
        return analysis.stageAnalyses().stream()
                .filter(item -> stage.equalsIgnoreCase(item.runStage()))
                .findFirst()
                .map(TransferRunLogStageAnalysis::totalCount)
                .orElse(0L);
    }
}
