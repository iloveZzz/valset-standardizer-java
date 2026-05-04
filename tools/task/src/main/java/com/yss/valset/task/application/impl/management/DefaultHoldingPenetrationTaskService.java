package com.yss.valset.task.application.impl.management;

import com.yss.cloud.dto.response.PageResult;
import com.yss.valset.task.application.command.OutsourcedDataTaskActionCommand;
import com.yss.valset.task.application.command.OutsourcedDataTaskBatchCommand;
import com.yss.valset.task.application.command.OutsourcedDataTaskQueryCommand;
import com.yss.valset.task.application.dto.OutsourcedDataTaskActionResultDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskBatchDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskBatchDetailDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskLogDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskStageSummaryDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskStepDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskSummaryDTO;
import com.yss.valset.task.application.service.HoldingPenetrationTaskService;
import com.yss.valset.task.application.service.OutsourcedDataTaskService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 默认持仓穿透任务应用服务。
 *
 * <p>当前复用既有估值解析任务服务，并将阶段与文案翻译为持仓穿透语义。</p>
 */
@Service
public class DefaultHoldingPenetrationTaskService implements HoldingPenetrationTaskService {

    private static final List<String> HOLDING_STAGE_SEQUENCE = List.of(
            "NET_VALUE_STANDARDIZE",
            "POSITION_STANDARDIZE",
            "TAG_VERIFY",
            "POSITION_VERIFY",
            "SECURITY_STANDARDIZE"
    );

    private static final Map<String, String> LEGACY_TO_HOLDING_STAGE = Map.of(
            "RAW_DATA_EXTRACT", "NET_VALUE_STANDARDIZE",
            "FILE_PARSE", "NET_VALUE_STANDARDIZE",
            "STRUCTURE_STANDARDIZE", "POSITION_STANDARDIZE",
            "SUBJECT_RECOGNIZE", "TAG_VERIFY",
            "STANDARD_LANDING", "POSITION_VERIFY",
            "DATA_PROCESSING", "POSITION_VERIFY",
            "VERIFY_ARCHIVE", "SECURITY_STANDARDIZE"
    );

    private static final Map<String, String> HOLDING_TO_LEGACY_STAGE = Map.of(
            "NET_VALUE_STANDARDIZE", "FILE_PARSE",
            "POSITION_STANDARDIZE", "STRUCTURE_STANDARDIZE",
            "TAG_VERIFY", "SUBJECT_RECOGNIZE",
            "POSITION_VERIFY", "STANDARD_LANDING",
            "SECURITY_STANDARDIZE", "VERIFY_ARCHIVE"
    );

    private final OutsourcedDataTaskService outsourcedDataTaskService;

    public DefaultHoldingPenetrationTaskService(OutsourcedDataTaskService outsourcedDataTaskService) {
        this.outsourcedDataTaskService = outsourcedDataTaskService;
    }

    @Override
    public OutsourcedDataTaskSummaryDTO summary(OutsourcedDataTaskQueryCommand query) {
        OutsourcedDataTaskSummaryDTO summary = translateSummary(outsourcedDataTaskService.summary(translateQuery(query)));
        return summary;
    }

    @Override
    public PageResult<OutsourcedDataTaskBatchDTO> pageTasks(OutsourcedDataTaskQueryCommand query) {
        PageResult<OutsourcedDataTaskBatchDTO> result = outsourcedDataTaskService.pageTasks(translateQuery(query));
        return PageResult.of(
                translateBatchList(result == null ? null : result.getData()),
                result == null ? 0 : result.getTotalCount(),
                result == null ? 10 : result.getPageSize(),
                result == null ? 1 : result.getPageIndex()
        );
    }

    @Override
    public OutsourcedDataTaskBatchDetailDTO getTask(String batchId) {
        return translateDetail(outsourcedDataTaskService.getTask(batchId));
    }

    @Override
    public List<OutsourcedDataTaskStepDTO> listSteps(String batchId) {
        return translateStepList(outsourcedDataTaskService.listSteps(batchId));
    }

    @Override
    public PageResult<OutsourcedDataTaskLogDTO> pageLogs(String batchId, String stage, Integer pageIndex, Integer pageSize) {
        PageResult<OutsourcedDataTaskLogDTO> result = outsourcedDataTaskService.pageLogs(
                batchId,
                translateHoldingStageToLegacy(stage),
                pageIndex,
                pageSize
        );
        return PageResult.of(
                translateLogList(result == null ? null : result.getData()),
                result == null ? 0 : result.getTotalCount(),
                result == null ? 10 : result.getPageSize(),
                result == null ? 1 : result.getPageIndex()
        );
    }

    @Override
    public OutsourcedDataTaskActionResultDTO execute(String batchId, OutsourcedDataTaskActionCommand command) {
        return translateActionResult(outsourcedDataTaskService.execute(batchId, command));
    }

    @Override
    public OutsourcedDataTaskActionResultDTO retry(String batchId, OutsourcedDataTaskActionCommand command) {
        return translateActionResult(outsourcedDataTaskService.retry(batchId, command));
    }

    @Override
    public OutsourcedDataTaskActionResultDTO stop(String batchId, OutsourcedDataTaskActionCommand command) {
        return translateActionResult(outsourcedDataTaskService.stop(batchId, command));
    }

    @Override
    public OutsourcedDataTaskActionResultDTO retryStep(String batchId, String stepId, OutsourcedDataTaskActionCommand command) {
        return translateActionResult(outsourcedDataTaskService.retryStep(batchId, stepId, command));
    }

    @Override
    public List<OutsourcedDataTaskActionResultDTO> batchExecute(OutsourcedDataTaskBatchCommand command) {
        return translateActionList(outsourcedDataTaskService.batchExecute(command));
    }

    @Override
    public List<OutsourcedDataTaskActionResultDTO> batchRetry(OutsourcedDataTaskBatchCommand command) {
        return translateActionList(outsourcedDataTaskService.batchRetry(command));
    }

    @Override
    public List<OutsourcedDataTaskActionResultDTO> batchStop(OutsourcedDataTaskBatchCommand command) {
        return translateActionList(outsourcedDataTaskService.batchStop(command));
    }

    private OutsourcedDataTaskQueryCommand translateQuery(OutsourcedDataTaskQueryCommand query) {
        if (query == null) {
            return null;
        }
        OutsourcedDataTaskQueryCommand translated = new OutsourcedDataTaskQueryCommand();
        translated.setBatchId(query.getBatchId());
        translated.setTaskDate(query.getTaskDate());
        translated.setBusinessDate(query.getBusinessDate());
        translated.setManagerName(query.getManagerName());
        translated.setProductKeyword(query.getProductKeyword());
        translated.setStage(translateHoldingStageToLegacy(query.getStage()));
        translated.setStatus(query.getStatus());
        translated.setSourceType(query.getSourceType());
        translated.setErrorType(query.getErrorType());
        translated.setIncludeHistory(query.getIncludeHistory());
        translated.setPageIndex(query.getPageIndex());
        translated.setPageSize(query.getPageSize());
        return translated;
    }

    private OutsourcedDataTaskSummaryDTO translateSummary(OutsourcedDataTaskSummaryDTO summary) {
        if (summary == null) {
            return null;
        }
        summary.setStepSummaries(mergeStageSummaries(summary.getStepSummaries()));
        return summary;
    }

    private List<OutsourcedDataTaskStageSummaryDTO> mergeStageSummaries(List<OutsourcedDataTaskStageSummaryDTO> summaries) {
        if (summaries == null || summaries.isEmpty()) {
            return summaries;
        }
        Map<String, OutsourcedDataTaskStageSummaryDTO> merged = new LinkedHashMap<>();
        for (OutsourcedDataTaskStageSummaryDTO item : summaries) {
            String stage = translateHoldingStage(item == null ? null : firstText(item.getStage(), item.getStep()));
            if (!StringUtils.hasText(stage)) {
                continue;
            }
            OutsourcedDataTaskStageSummaryDTO target = merged.computeIfAbsent(stage, key -> {
                OutsourcedDataTaskStageSummaryDTO dto = new OutsourcedDataTaskStageSummaryDTO();
                dto.setStage(key);
                dto.setStep(key);
                dto.setStageName(stageLabel(key));
                dto.setStepName(stageLabel(key));
                dto.setStageDescription(stageDescription(key));
                dto.setStepDescription(stageDescription(key));
                return dto;
            });
            target.setTotalCount(target.getTotalCount() + item.getTotalCount());
            target.setRunningCount(target.getRunningCount() + item.getRunningCount());
            target.setFailedCount(target.getFailedCount() + item.getFailedCount());
            target.setPendingCount(target.getPendingCount() + item.getPendingCount());
        }
        return merged.values().stream()
                .sorted(Comparator.comparingInt(item -> holdingStageOrder(item.getStage())))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<OutsourcedDataTaskBatchDTO> translateBatchList(List<OutsourcedDataTaskBatchDTO> batches) {
        if (batches == null || batches.isEmpty()) {
            return batches;
        }
        return batches.stream().map(this::translateBatch).toList();
    }

    private OutsourcedDataTaskBatchDTO translateBatch(OutsourcedDataTaskBatchDTO batch) {
        if (batch == null) {
            return null;
        }
        OutsourcedDataTaskBatchDTO translated = copyBatch(batch);
        translated.setCurrentStage(translateHoldingStage(firstText(batch.getCurrentStage(), batch.getCurrentStep())));
        translated.setCurrentStep(translated.getCurrentStage());
        translated.setCurrentStageName(stageLabel(translated.getCurrentStage()));
        translated.setCurrentStepName(stageLabel(translated.getCurrentStep()));
        return translated;
    }

    private OutsourcedDataTaskBatchDetailDTO translateDetail(OutsourcedDataTaskBatchDetailDTO detail) {
        if (detail == null) {
            return null;
        }
        OutsourcedDataTaskBatchDetailDTO translated = new OutsourcedDataTaskBatchDetailDTO();
        translated.setBatch(translateBatch(detail.getBatch()));
        translated.setSteps(translateStepList(detail.getSteps()));
        translated.setCurrentBlockPoint(translateText(detail.getCurrentBlockPoint()));
        translated.setFileResultUrl(detail.getFileResultUrl());
        translated.setRawDataUrl(detail.getRawDataUrl());
        translated.setStgDataUrl(detail.getStgDataUrl());
        translated.setDwdDataUrl(detail.getDwdDataUrl());
        return translated;
    }

    private List<OutsourcedDataTaskStepDTO> translateStepList(List<OutsourcedDataTaskStepDTO> steps) {
        if (steps == null || steps.isEmpty()) {
            return steps;
        }
        Map<String, OutsourcedDataTaskStepDTO> merged = new LinkedHashMap<>();
        for (OutsourcedDataTaskStepDTO step : steps) {
            OutsourcedDataTaskStepDTO translated = translateStep(step);
            if (translated == null) {
                continue;
            }
            String stage = translated.getStage();
            merged.merge(stage, translated, this::preferStep);
        }
        return merged.values().stream()
                .sorted(Comparator.comparingInt(item -> holdingStageOrder(item.getStage())))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private OutsourcedDataTaskStepDTO translateStep(OutsourcedDataTaskStepDTO step) {
        if (step == null) {
            return null;
        }
        String stage = translateHoldingStage(firstText(step.getStage(), step.getStep()));
        if (!StringUtils.hasText(stage)) {
            return null;
        }
        OutsourcedDataTaskStepDTO translated = copyStep(step);
        translated.setStage(stage);
        translated.setStep(stage);
        translated.setStageName(stageLabel(stage));
        translated.setStepName(stageLabel(stage));
        translated.setInputSummary(translateText(step.getInputSummary()));
        translated.setOutputSummary(translateText(step.getOutputSummary()));
        translated.setErrorMessage(translateText(step.getErrorMessage()));
        translated.setLogRef(step.getLogRef());
        return translated;
    }

    private List<OutsourcedDataTaskLogDTO> translateLogList(List<OutsourcedDataTaskLogDTO> logs) {
        if (logs == null || logs.isEmpty()) {
            return logs;
        }
        return logs.stream().map(log -> {
            if (log == null) {
                return null;
            }
            OutsourcedDataTaskLogDTO translated = copyLog(log);
            translated.setStage(translateHoldingStage(log.getStage()));
            translated.setMessage(translateText(log.getMessage()));
            return translated;
        }).filter(Objects::nonNull).toList();
    }

    private OutsourcedDataTaskActionResultDTO translateActionResult(OutsourcedDataTaskActionResultDTO result) {
        if (result == null) {
            return null;
        }
        OutsourcedDataTaskActionResultDTO translated = copyActionResult(result);
        translated.setMessage(translateText(result.getMessage()));
        translated.setAction(firstText(result.getAction(), result.getAction()));
        return translated;
    }

    private List<OutsourcedDataTaskActionResultDTO> translateActionList(List<OutsourcedDataTaskActionResultDTO> results) {
        if (results == null || results.isEmpty()) {
            return results;
        }
        return results.stream()
                .map(this::translateActionResult)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private OutsourcedDataTaskStepDTO preferStep(OutsourcedDataTaskStepDTO left, OutsourcedDataTaskStepDTO right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        if (Boolean.TRUE.equals(right.getCurrentFlag()) && !Boolean.TRUE.equals(left.getCurrentFlag())) {
            return right;
        }
        if (Boolean.TRUE.equals(left.getCurrentFlag()) && !Boolean.TRUE.equals(right.getCurrentFlag())) {
            return left;
        }
        int timeDiff = String.valueOf(firstText(right.getStartedAt(), ""))
                .compareTo(String.valueOf(firstText(left.getStartedAt(), "")));
        if (timeDiff != 0) {
            return timeDiff > 0 ? right : left;
        }
        int runDiff = Integer.compare(
                right.getRunNo() == null ? 0 : right.getRunNo(),
                left.getRunNo() == null ? 0 : left.getRunNo()
        );
        return runDiff >= 0 ? right : left;
    }

    private OutsourcedDataTaskBatchDTO copyBatch(OutsourcedDataTaskBatchDTO source) {
        OutsourcedDataTaskBatchDTO target = new OutsourcedDataTaskBatchDTO();
        target.setBatchId(source.getBatchId());
        target.setBatchName(source.getBatchName());
        target.setBusinessDate(source.getBusinessDate());
        target.setProductCode(source.getProductCode());
        target.setProductName(source.getProductName());
        target.setManagerName(source.getManagerName());
        target.setFileId(source.getFileId());
        target.setFilesysFileId(source.getFilesysFileId());
        target.setOriginalFileName(source.getOriginalFileName());
        target.setSourceType(source.getSourceType());
        target.setCurrentStage(source.getCurrentStage());
        target.setCurrentStep(source.getCurrentStep());
        target.setCurrentStageName(source.getCurrentStageName());
        target.setCurrentStepName(source.getCurrentStepName());
        target.setStatus(source.getStatus());
        target.setStatusName(source.getStatusName());
        target.setProgress(source.getProgress());
        target.setStartedAt(source.getStartedAt());
        target.setEndedAt(source.getEndedAt());
        target.setDurationMs(source.getDurationMs());
        target.setDurationText(source.getDurationText());
        target.setLastErrorCode(source.getLastErrorCode());
        target.setLastErrorMessage(translateText(source.getLastErrorMessage()));
        return target;
    }

    private OutsourcedDataTaskStepDTO copyStep(OutsourcedDataTaskStepDTO source) {
        OutsourcedDataTaskStepDTO target = new OutsourcedDataTaskStepDTO();
        target.setStepId(source.getStepId());
        target.setBatchId(source.getBatchId());
        target.setStage(source.getStage());
        target.setStep(source.getStep());
        target.setStageName(source.getStageName());
        target.setStepName(source.getStepName());
        target.setTaskId(source.getTaskId());
        target.setTaskType(source.getTaskType());
        target.setRunNo(source.getRunNo());
        target.setCurrentFlag(source.getCurrentFlag());
        target.setTriggerMode(source.getTriggerMode());
        target.setTriggerModeName(source.getTriggerModeName());
        target.setStatus(source.getStatus());
        target.setStatusName(source.getStatusName());
        target.setProgress(source.getProgress());
        target.setStartedAt(source.getStartedAt());
        target.setEndedAt(source.getEndedAt());
        target.setDurationMs(source.getDurationMs());
        target.setDurationText(source.getDurationText());
        target.setInputSummary(source.getInputSummary());
        target.setOutputSummary(source.getOutputSummary());
        target.setErrorCode(source.getErrorCode());
        target.setErrorMessage(source.getErrorMessage());
        target.setLogRef(source.getLogRef());
        return target;
    }

    private OutsourcedDataTaskLogDTO copyLog(OutsourcedDataTaskLogDTO source) {
        OutsourcedDataTaskLogDTO target = new OutsourcedDataTaskLogDTO();
        target.setLogId(source.getLogId());
        target.setBatchId(source.getBatchId());
        target.setStepId(source.getStepId());
        target.setStage(source.getStage());
        target.setLogLevel(source.getLogLevel());
        target.setMessage(source.getMessage());
        target.setOccurredAt(source.getOccurredAt());
        return target;
    }

    private OutsourcedDataTaskActionResultDTO copyActionResult(OutsourcedDataTaskActionResultDTO source) {
        OutsourcedDataTaskActionResultDTO target = new OutsourcedDataTaskActionResultDTO();
        target.setBatchId(source.getBatchId());
        target.setStepId(source.getStepId());
        target.setAccepted(source.isAccepted());
        target.setAction(source.getAction());
        target.setMessage(source.getMessage());
        return target;
    }

    private String translateHoldingStage(String stage) {
        if (!StringUtils.hasText(stage)) {
            return stage;
        }
        String normalized = stage.trim();
        return LEGACY_TO_HOLDING_STAGE.getOrDefault(normalized, normalized);
    }

    private String translateHoldingStageToLegacy(String stage) {
        if (!StringUtils.hasText(stage)) {
            return stage;
        }
        String normalized = stage.trim();
        return HOLDING_TO_LEGACY_STAGE.getOrDefault(normalized, normalized);
    }

    private int holdingStageOrder(String stage) {
        if (!StringUtils.hasText(stage)) {
            return HOLDING_STAGE_SEQUENCE.size();
        }
        int index = HOLDING_STAGE_SEQUENCE.indexOf(stage.trim());
        return index >= 0 ? index : HOLDING_STAGE_SEQUENCE.size();
    }

    private String stageLabel(String stage) {
        if (!StringUtils.hasText(stage)) {
            return stage;
        }
        return switch (stage.trim()) {
            case "NET_VALUE_STANDARDIZE" -> "净值指标标准化";
            case "POSITION_STANDARDIZE" -> "持仓标准化";
            case "TAG_VERIFY" -> "标签校验";
            case "POSITION_VERIFY" -> "持仓校验";
            case "SECURITY_STANDARDIZE" -> "证券标准化";
            default -> stage;
        };
    }

    private String stageDescription(String stage) {
        if (!StringUtils.hasText(stage)) {
            return "";
        }
        return switch (stage.trim()) {
            case "NET_VALUE_STANDARDIZE" -> "净值指标校验、标准化转换、指标口径统一";
            case "POSITION_STANDARDIZE" -> "持仓字段映射、数据清洗、标准持仓转换";
            case "TAG_VERIFY" -> "标签匹配、标签完整性与一致性校验";
            case "POSITION_VERIFY" -> "持仓数据一致性校验、异常定位与确认";
            case "SECURITY_STANDARDIZE" -> "证券标识映射、标准证券结果输出";
            default -> "";
        };
    }

    private String translateText(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return value
                .replace("估值表解析任务", "持仓穿透任务")
                .replace("估值表解析", "持仓穿透")
                .replace("DWD", "结果层")
                .replace("STG", "中间层")
                .replace("文件解析", "净值指标标准化");
    }

    private String firstText(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
