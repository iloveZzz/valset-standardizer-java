package com.yss.valset.transfer.application.impl.query;

import com.yss.cloud.dto.response.PageResult;
import com.yss.valset.transfer.application.dto.TransferRunLogViewDTO;
import com.yss.valset.transfer.application.dto.TransferRunLogAnalysisViewDTO;
import com.yss.valset.transfer.application.dto.TransferRunLogStageAnalysisViewDTO;
import com.yss.valset.transfer.application.dto.TransferRunLogStatusCountViewDTO;
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

/**
 * 默认文件收发运行日志查询服务。
 */
@Service
@RequiredArgsConstructor
public class DefaultTransferRunLogQueryService implements TransferRunLogQueryService {

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
                                                Integer limit) {
        String normalizedStage = normalizeEnum(runStage, TransferRunStage.class, "运行阶段");
        String normalizedStatus = normalizeEnum(runStatus, TransferRunStatus.class, "运行状态");
        String normalizedTriggerType = normalizeEnum(triggerType, TransferTriggerType.class, "触发类型");
        return transferRunLogGateway.listLogs(sourceId, transferId, routeId, normalizedStage, normalizedStatus, normalizedTriggerType, limit)
                .stream()
                .map(this::toView)
                .toList();
    }

    @Override
    public PageResult<TransferRunLogViewDTO> pageLogs(String sourceId,
                                                      String transferId,
                                                      String routeId,
                                                      String runStage,
                                                      String runStatus,
                                                      String triggerType,
                                                      String keyword,
                                                      Integer pageIndex,
                                                      Integer pageSize) {
        String normalizedStage = normalizeEnum(runStage, TransferRunStage.class, "运行阶段");
        String normalizedStatus = normalizeEnum(runStatus, TransferRunStatus.class, "运行状态");
        String normalizedTriggerType = normalizeEnum(triggerType, TransferTriggerType.class, "触发类型");
        TransferRunLogPage page = transferRunLogGateway.pageLogs(
                sourceId,
                transferId,
                routeId,
                normalizedStage,
                normalizedStatus,
                normalizedTriggerType,
                keyword,
                pageIndex,
                pageSize
        );
        return PageResult.of(
                page.records().stream().map(this::toView).toList(),
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
                                                     String keyword) {
        String normalizedStage = normalizeEnum(runStage, TransferRunStage.class, "运行阶段");
        String normalizedStatus = normalizeEnum(runStatus, TransferRunStatus.class, "运行状态");
        String normalizedTriggerType = normalizeEnum(triggerType, TransferTriggerType.class, "触发类型");
        TransferRunLogAnalysis analysis = transferRunLogGateway.analyzeLogs(
                sourceId,
                transferId,
                routeId,
                normalizedStage,
                normalizedStatus,
                normalizedTriggerType,
                keyword
        );
        return TransferRunLogAnalysisViewDTO.builder()
                .totalCount(analysis.totalCount())
                .sourceCount(stageTotal(analysis, "INGEST"))
                .routeCount(stageTotal(analysis, "ROUTE"))
                .targetCount(stageTotal(analysis, "DELIVER"))
                .stageAnalyses(analysis.stageAnalyses() == null ? List.of() : analysis.stageAnalyses().stream().map(this::toStageView).toList())
                .build();
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
                .statusCounts(stageAnalysis.statusCounts().stream().map(this::toStatusView).toList())
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
        return switch (runStage.trim().toUpperCase(Locale.ROOT)) {
            case "INGEST" -> "来源";
            case "ROUTE" -> "路由";
            case "DELIVER" -> "目标";
            default -> runStage;
        };
    }

    private String resolveStatusLabel(String runStatus) {
        if (!StringUtils.hasText(runStatus)) {
            return "-";
        }
        return switch (runStatus.trim().toUpperCase(Locale.ROOT)) {
            case "SUCCESS" -> "成功";
            case "FAILED" -> "失败";
            default -> runStatus;
        };
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
