package com.yss.valset.task.application.impl.management;

import com.yss.cloud.dto.response.PageResult;
import com.yss.valset.task.application.command.OutsourcedDataTaskActionCommand;
import com.yss.valset.task.application.command.OutsourcedDataTaskBatchCommand;
import com.yss.valset.task.application.command.OutsourcedDataTaskQueryCommand;
import com.yss.valset.task.application.config.OutsourcedDataTaskStageCatalog;
import com.yss.valset.task.application.dto.OutsourcedDataTaskActionResultDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskBatchDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskBatchDetailDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskLogDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskStageSummaryDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskStepDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskSummaryDTO;
import com.yss.valset.task.application.port.OutsourcedDataTaskGateway;
import com.yss.valset.task.application.service.OutsourcedDataTaskManagementAppService;
import com.yss.valset.task.domain.model.OutsourcedDataTaskStage;
import com.yss.valset.task.domain.model.OutsourcedDataTaskStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 默认估值表解析任务管理应用服务。
 *
 * <p>当前先提供稳定的页面契约和静态聚合数据，后续接入持久化网关后替换数据来源。</p>
 */
@Service
public class DefaultOutsourcedDataTaskManagementAppService implements OutsourcedDataTaskManagementAppService {

    private static final OutsourcedDataTaskStageCatalog DEFAULT_STAGE_CATALOG = new OutsourcedDataTaskStageCatalog();
    private static final List<OutsourcedDataTaskBatchDTO> SAMPLE_BATCHES = buildSampleBatches(DEFAULT_STAGE_CATALOG);

    private static final List<OutsourcedDataTaskStepDTO> SAMPLE_STEPS = buildSampleSteps(DEFAULT_STAGE_CATALOG);

    private final OutsourcedDataTaskGateway outsourcedDataTaskGateway;
    private OutsourcedDataTaskStageCatalog stageCatalog = new OutsourcedDataTaskStageCatalog();

    public DefaultOutsourcedDataTaskManagementAppService() {
        this.outsourcedDataTaskGateway = null;
    }

    @Autowired
    public DefaultOutsourcedDataTaskManagementAppService(OutsourcedDataTaskGateway outsourcedDataTaskGateway) {
        this.outsourcedDataTaskGateway = outsourcedDataTaskGateway;
    }

    @Autowired
    public void setStageCatalog(OutsourcedDataTaskStageCatalog stageCatalog) {
        if (stageCatalog != null) {
            this.stageCatalog = stageCatalog;
        }
    }

    @Override
    public OutsourcedDataTaskSummaryDTO summary(OutsourcedDataTaskQueryCommand query) {
        List<OutsourcedDataTaskBatchDTO> batches = loadBatches(query);
        OutsourcedDataTaskSummaryDTO summary = new OutsourcedDataTaskSummaryDTO();
        summary.setTotalCount(batches.size());
        summary.setRunningCount(countByStatus(batches, OutsourcedDataTaskStatus.RUNNING));
        summary.setSuccessCount(countByStatus(batches, OutsourcedDataTaskStatus.SUCCESS));
        summary.setFailedCount(batches.stream()
                .filter(batch -> isAnyStatus(batch, OutsourcedDataTaskStatus.FAILED, OutsourcedDataTaskStatus.BLOCKED))
                .count());
        summary.setStepSummaries(buildStepSummaries(batches));
        return summary;
    }

    @Override
    public PageResult<OutsourcedDataTaskBatchDTO> pageTasks(OutsourcedDataTaskQueryCommand query) {
        if (outsourcedDataTaskGateway != null) {
            return outsourcedDataTaskGateway.pageTasks(query);
        }
        List<OutsourcedDataTaskBatchDTO> filtered = filterBatches(query);
        int pageIndex = normalizePageIndex(query == null ? null : query.getPageIndex());
        int pageSize = normalizePageSize(query == null ? null : query.getPageSize());
        int fromIndex = Math.min((pageIndex - 1) * pageSize, filtered.size());
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
        return PageResult.of(filtered.subList(fromIndex, toIndex), filtered.size(), pageSize, pageIndex);
    }

    @Override
    public OutsourcedDataTaskBatchDetailDTO getTask(String batchId) {
        OutsourcedDataTaskBatchDTO batch = requireBatch(batchId);
        OutsourcedDataTaskBatchDetailDTO detail = new OutsourcedDataTaskBatchDetailDTO();
        detail.setBatch(batch);
        detail.setSteps(listSteps(batchId));
        detail.setCurrentBlockPoint(batch.getLastErrorMessage());
        detail.setFileResultUrl("/files/" + batch.getFileId());
        detail.setRawDataUrl("/valuation-workflows/" + batch.getFileId() + "/raw-data");
        detail.setStgDataUrl("/valuation-workflows/" + batch.getFileId() + "/stg-data");
        detail.setDwdDataUrl("/valuation-workflows/" + batch.getFileId() + "/dwd-data");
        return detail;
    }

    @Override
    public List<OutsourcedDataTaskStepDTO> listSteps(String batchId) {
        if (outsourcedDataTaskGateway != null) {
            return outsourcedDataTaskGateway.listSteps(batchId);
        }
        requireBatch(batchId);
        return SAMPLE_STEPS.stream()
                .filter(step -> Objects.equals(step.getBatchId(), batchId))
                .sorted(Comparator.comparingInt(step -> stageOrder(step.getStage())))
                .collect(Collectors.toList());
    }

    @Override
    public PageResult<OutsourcedDataTaskLogDTO> pageLogs(String batchId, String stage, Integer pageIndex, Integer pageSize) {
        if (outsourcedDataTaskGateway != null) {
            return outsourcedDataTaskGateway.pageLogs(batchId, stage, pageIndex, pageSize);
        }
        requireBatch(batchId);
        List<OutsourcedDataTaskLogDTO> logs = listSteps(batchId).stream()
                .filter(step -> !hasText(stage) || Objects.equals(step.getStage(), stage))
                .map(this::toLog)
                .collect(Collectors.toList());
        int normalizedPageIndex = normalizePageIndex(pageIndex);
        int normalizedPageSize = normalizePageSize(pageSize);
        int fromIndex = Math.min((normalizedPageIndex - 1) * normalizedPageSize, logs.size());
        int toIndex = Math.min(fromIndex + normalizedPageSize, logs.size());
        return PageResult.of(logs.subList(fromIndex, toIndex), logs.size(), normalizedPageSize, normalizedPageIndex);
    }

    @Override
    public OutsourcedDataTaskActionResultDTO execute(String batchId, OutsourcedDataTaskActionCommand command) {
        requireBatch(batchId);
        return accepted(batchId, null, "EXECUTE", "已提交估值表解析任务执行请求");
    }

    @Override
    public OutsourcedDataTaskActionResultDTO retry(String batchId, OutsourcedDataTaskActionCommand command) {
        requireBatch(batchId);
        return accepted(batchId, null, "RETRY", "已提交估值表解析任务重跑请求");
    }

    @Override
    public OutsourcedDataTaskActionResultDTO stop(String batchId, OutsourcedDataTaskActionCommand command) {
        requireBatch(batchId);
        return accepted(batchId, null, "STOP", "已提交估值表解析任务停止请求");
    }

    @Override
    public OutsourcedDataTaskActionResultDTO retryStep(String batchId, String stepId, OutsourcedDataTaskActionCommand command) {
        requireStep(batchId, stepId);
        return accepted(batchId, stepId, "RETRY_STEP", "已提交估值表解析任务阶段重跑请求");
    }

    @Override
    public List<OutsourcedDataTaskActionResultDTO> batchExecute(OutsourcedDataTaskBatchCommand command) {
        return batchAction(command, "EXECUTE", "已提交批量执行请求");
    }

    @Override
    public List<OutsourcedDataTaskActionResultDTO> batchRetry(OutsourcedDataTaskBatchCommand command) {
        return batchAction(command, "RETRY", "已提交批量重跑请求");
    }

    @Override
    public List<OutsourcedDataTaskActionResultDTO> batchStop(OutsourcedDataTaskBatchCommand command) {
        return batchAction(command, "STOP", "已提交批量停止请求");
    }

    private List<OutsourcedDataTaskActionResultDTO> batchAction(OutsourcedDataTaskBatchCommand command, String action, String message) {
        if (command == null || command.getBatchIds() == null) {
            return Collections.emptyList();
        }
        return command.getBatchIds().stream()
                .map(batchId -> {
                    requireBatch(batchId);
                    return accepted(batchId, null, action, message);
                })
                .collect(Collectors.toList());
    }

    private OutsourcedDataTaskBatchDTO requireBatch(String batchId) {
        if (outsourcedDataTaskGateway != null) {
            Optional<OutsourcedDataTaskBatchDTO> persisted = outsourcedDataTaskGateway.findTask(batchId);
            if (persisted.isPresent()) {
                return persisted.get();
            }
        }
        return SAMPLE_BATCHES.stream()
                .filter(batch -> Objects.equals(batch.getBatchId(), batchId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("估值表解析任务批次不存在：" + batchId));
    }

    private OutsourcedDataTaskStepDTO requireStep(String batchId, String stepId) {
        return SAMPLE_STEPS.stream()
                .filter(step -> Objects.equals(step.getBatchId(), batchId))
                .filter(step -> Objects.equals(step.getStepId(), stepId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("估值表解析任务阶段不存在：" + stepId));
    }

    private List<OutsourcedDataTaskBatchDTO> filterBatches(OutsourcedDataTaskQueryCommand query) {
        if (query == null) {
            return SAMPLE_BATCHES.stream()
                    .filter(this::isCurrentBatch)
                    .collect(Collectors.toList());
        }
        Predicate<OutsourcedDataTaskBatchDTO> predicate = batch -> matches(query.getBusinessDate(), batch.getBusinessDate())
                && contains(batch.getManagerName(), query.getManagerName())
                && (contains(batch.getProductName(), query.getProductKeyword()) || contains(batch.getProductCode(), query.getProductKeyword()))
                && matches(query.getStage(), batch.getCurrentStage())
                && matches(query.getStatus(), batch.getStatus())
                && matches(query.getSourceType(), batch.getSourceType())
                && (!hasText(query.getErrorType()) || contains(batch.getLastErrorCode(), query.getErrorType()) || contains(batch.getLastErrorMessage(), query.getErrorType()));
        return SAMPLE_BATCHES.stream()
                .filter(predicate)
                .filter(batch -> isVisibleBatch(batch, query))
                .collect(Collectors.toList());
    }

    private List<OutsourcedDataTaskBatchDTO> loadBatches(OutsourcedDataTaskQueryCommand query) {
        if (outsourcedDataTaskGateway != null) {
            return outsourcedDataTaskGateway.listTasks(query);
        }
        return filterBatches(query);
    }

    private boolean isVisibleBatch(OutsourcedDataTaskBatchDTO batch, OutsourcedDataTaskQueryCommand query) {
        if (query != null && Boolean.TRUE.equals(query.getIncludeHistory())) {
            return true;
        }
        return isCurrentBatch(batch);
    }

    private boolean isCurrentBatch(OutsourcedDataTaskBatchDTO batch) {
        if (batch == null) {
            return false;
        }
        return hasText(batch.getBatchId())
                && (batch.getBatchId().startsWith("FILE-") || hasText(batch.getFileId()));
    }

    private List<OutsourcedDataTaskStageSummaryDTO> buildStepSummaries(List<OutsourcedDataTaskBatchDTO> batches) {
        Map<String, List<OutsourcedDataTaskBatchDTO>> stageMap = new java.util.HashMap<>();
        batches.forEach(batch -> {
            if (batch == null || !hasText(batch.getBatchId())) {
                return;
            }
            listSteps(batch.getBatchId()).stream()
                    .map(OutsourcedDataTaskStepDTO::getStage)
                    .filter(DefaultOutsourcedDataTaskManagementAppService::hasText)
                    .distinct()
                    .forEach(stage -> stageMap.computeIfAbsent(stage, key -> new ArrayList<>()).add(batch));
        });
        return stageCatalog.stageSequence().stream()
                .map(stage -> {
                    List<OutsourcedDataTaskBatchDTO> stageBatches = stageMap.getOrDefault(stage.name(), Collections.emptyList());
                    OutsourcedDataTaskStageSummaryDTO summary = new OutsourcedDataTaskStageSummaryDTO();
                    summary.setStage(stage.name());
                    summary.setStep(stage.name());
                    summary.setStageName(stageCatalog.stageLabel(stage.name()));
                    summary.setStepName(stageCatalog.stageLabel(stage.name()));
                    summary.setStageDescription(stageCatalog.stageDescription(stage.name()));
                    summary.setStepDescription(stageCatalog.stageDescription(stage.name()));
                    summary.setTotalCount(stageBatches.size());
                    summary.setRunningCount(countByStatus(stageBatches, OutsourcedDataTaskStatus.RUNNING));
                    summary.setFailedCount(stageBatches.stream()
                            .filter(batch -> isAnyStatus(batch, OutsourcedDataTaskStatus.FAILED, OutsourcedDataTaskStatus.BLOCKED))
                            .count());
                    summary.setPendingCount(countByStatus(stageBatches, OutsourcedDataTaskStatus.PENDING));
                    return summary;
                })
                .collect(Collectors.toList());
    }

    private OutsourcedDataTaskLogDTO toLog(OutsourcedDataTaskStepDTO step) {
        OutsourcedDataTaskLogDTO log = new OutsourcedDataTaskLogDTO();
        log.setLogId("LOG-" + step.getStepId());
        log.setBatchId(step.getBatchId());
        log.setStepId(step.getStepId());
        log.setStage(step.getStage());
        log.setLogLevel(OutsourcedDataTaskStatus.FAILED.name().equals(step.getStatus()) ? "ERROR" : "INFO");
        log.setMessage(hasText(step.getErrorMessage()) ? step.getErrorMessage() : step.getStageName() + "执行完成");
        log.setOccurredAt(step.getEndedAt() == null ? step.getStartedAt() : step.getEndedAt());
        return log;
    }

    private OutsourcedDataTaskActionResultDTO accepted(String batchId, String stepId, String action, String message) {
        OutsourcedDataTaskActionResultDTO result = new OutsourcedDataTaskActionResultDTO();
        result.setBatchId(batchId);
        result.setStepId(stepId);
        result.setAccepted(true);
        result.setAction(action);
        result.setMessage(message);
        return result;
    }

    private static long countByStatus(List<OutsourcedDataTaskBatchDTO> batches, OutsourcedDataTaskStatus status) {
        return batches.stream()
                .filter(batch -> Objects.equals(batch.getStatus(), status.name()))
                .count();
    }

    private static boolean isAnyStatus(OutsourcedDataTaskBatchDTO batch, OutsourcedDataTaskStatus... statuses) {
        return Arrays.stream(statuses)
                .anyMatch(status -> Objects.equals(batch.getStatus(), status.name()));
    }

    private static boolean matches(String expected, String actual) {
        return !hasText(expected) || Objects.equals(expected, actual);
    }

    private static boolean contains(String actual, String keyword) {
        if (!hasText(keyword)) {
            return true;
        }
        return actual != null && actual.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static int normalizePageIndex(Integer pageIndex) {
        return pageIndex == null || pageIndex < 1 ? 1 : pageIndex;
    }

    private static int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 200);
    }

    private int stageOrder(String stage) {
        return stageCatalog.stageOrder(stage);
    }

    private static List<OutsourcedDataTaskBatchDTO> buildSampleBatches(OutsourcedDataTaskStageCatalog catalog) {
        return Arrays.asList(
                batch("BATCH-20250227-001", "估值产品5 2025-02-27 估值数据处理", "2025-02-27", "2025-02-27", "W213412", "估值产品5", "临时机构", "FILE-001", "FS-001", "估值产品5估值表.xlsx", "MANUAL_UPLOAD", OutsourcedDataTaskStage.SUBJECT_RECOGNIZE, OutsourcedDataTaskStatus.RUNNING, 66, null, null, catalog),
                batch("BATCH-20250227-002", "估值产品6 2025-02-27 估值数据处理", "2025-02-27", "2025-02-27", "W213413", "估值产品6", "临时机构", "FILE-002", "FS-002", "估值产品6估值表.xlsx", "FILESYS", OutsourcedDataTaskStage.STANDARD_LANDING, OutsourcedDataTaskStatus.FAILED, 70, "LANDING_FAILED", "标准表落地失败：DWD 持仓写入冲突", catalog),
                batch("BATCH-20250227-003", "估值产品7 2025-02-27 估值数据处理", "2025-02-27", "2025-02-27", "W213414", "估值产品7", "临时机构", "FILE-003", "FS-003", "估值产品7估值表.xlsx", "EMAIL", OutsourcedDataTaskStage.VERIFY_ARCHIVE, OutsourcedDataTaskStatus.SUCCESS, 100, null, null, catalog)
        );
    }

    private static OutsourcedDataTaskBatchDTO batch(String batchId,
                                                   String batchName,
                                                   String businessDate,
                                                   String valuationDate,
                                                   String productCode,
                                                   String productName,
                                                   String managerName,
                                                   String fileId,
                                                   String filesysFileId,
                                                   String originalFileName,
                                                   String sourceType,
                                                   OutsourcedDataTaskStage currentStage,
                                                   OutsourcedDataTaskStatus status,
                                                   Integer progress,
                                                   String errorCode,
                                                   String errorMessage,
                                                   OutsourcedDataTaskStageCatalog catalog) {
        OutsourcedDataTaskBatchDTO batch = new OutsourcedDataTaskBatchDTO();
        batch.setBatchId(batchId);
        batch.setBatchName(batchName);
        batch.setBusinessDate(businessDate);
        batch.setValuationDate(valuationDate);
        batch.setProductCode(productCode);
        batch.setProductName(productName);
        batch.setManagerName(managerName);
        batch.setFileId(fileId);
        batch.setFilesysFileId(filesysFileId);
        batch.setOriginalFileName(originalFileName);
        batch.setSourceType(sourceType);
        batch.setCurrentStage(currentStage.name());
        batch.setCurrentStep(currentStage.name());
        batch.setCurrentStageName(catalog.stageLabel(currentStage.name()));
        batch.setCurrentStepName(catalog.stageLabel(currentStage.name()));
        batch.setStatus(status.name());
        batch.setStatusName(catalog.statusLabel(status.name()));
        batch.setProgress(progress);
        batch.setStartedAt(businessDate + " 09:30:00");
        batch.setEndedAt(status == OutsourcedDataTaskStatus.RUNNING ? null : businessDate + " 09:42:00");
        batch.setDurationMs(status == OutsourcedDataTaskStatus.RUNNING ? null : 720000L);
        batch.setDurationText(status == OutsourcedDataTaskStatus.RUNNING ? "运行中" : "12m");
        batch.setLastErrorCode(errorCode);
        batch.setLastErrorMessage(errorMessage);
        return batch;
    }

    private static List<OutsourcedDataTaskStepDTO> buildSampleSteps(OutsourcedDataTaskStageCatalog catalog) {
        List<OutsourcedDataTaskStepDTO> steps = new ArrayList<>();
        SAMPLE_BATCHES.forEach(batch -> catalog.stageSequence().stream()
                .forEach(stage -> steps.add(step(batch, stage, catalog))));
        return steps;
    }

    private static OutsourcedDataTaskStepDTO step(OutsourcedDataTaskBatchDTO batch,
                                                  OutsourcedDataTaskStage stage,
                                                  OutsourcedDataTaskStageCatalog catalog) {
        OutsourcedDataTaskStepDTO step = new OutsourcedDataTaskStepDTO();
        step.setStepId(batch.getBatchId() + "-" + stage.name());
        step.setBatchId(batch.getBatchId());
        step.setStage(stage.name());
        step.setStep(stage.name());
        step.setStageName(stage.getLabel());
        step.setStepName(stage.getLabel());
        step.setTaskId("TASK-" + step.getStepId());
        step.setTaskType(stage.name());
        step.setRunNo(1);
        boolean scheduledStage = stage == OutsourcedDataTaskStage.FILE_PARSE;
        step.setTriggerMode(scheduledStage ? "SCHEDULE" : "DEPENDENCY");
        step.setTriggerModeName(scheduledStage ? "调度执行" : "依赖触发");
        boolean beforeCurrentStage = catalog.stageOrder(stage.name()) < catalog.stageOrder(batch.getCurrentStage());
        boolean currentStage = Objects.equals(stage.name(), batch.getCurrentStage());
        OutsourcedDataTaskStatus status = beforeCurrentStage ? OutsourcedDataTaskStatus.SUCCESS : (currentStage ? OutsourcedDataTaskStatus.valueOf(batch.getStatus()) : OutsourcedDataTaskStatus.PENDING);
        step.setStatus(status.name());
        step.setStatusName(catalog.statusLabel(status.name()));
        step.setProgress(status == OutsourcedDataTaskStatus.SUCCESS ? 100 : (currentStage ? batch.getProgress() : 0));
        step.setStartedAt(batch.getBusinessDate() + " 09:30:00");
        step.setEndedAt(status == OutsourcedDataTaskStatus.RUNNING || status == OutsourcedDataTaskStatus.PENDING ? null : batch.getBusinessDate() + " 09:32:00");
        step.setDurationMs(status == OutsourcedDataTaskStatus.PENDING ? null : 120000L);
        step.setDurationText(status == OutsourcedDataTaskStatus.PENDING ? "-" : "2m");
        step.setInputSummary(stage.getDescription());
        step.setOutputSummary(status == OutsourcedDataTaskStatus.SUCCESS ? catalog.stageLabel(stage.name()) + "已完成" : null);
        step.setErrorCode(currentStage ? batch.getLastErrorCode() : null);
        step.setErrorMessage(currentStage ? batch.getLastErrorMessage() : null);
        step.setLogRef("task:" + step.getTaskId());
        return step;
    }

}
