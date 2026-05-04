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
import com.yss.valset.task.application.service.OutsourcedDataTaskService;
import com.yss.valset.task.domain.model.OutsourcedDataTaskStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.batch.scheduler.SchedulerService;
import com.yss.valset.domain.gateway.WorkflowTaskGateway;
import com.yss.valset.domain.model.TaskStage;
import com.yss.valset.domain.model.TaskStatus;
import com.yss.valset.domain.model.TaskType;
import com.yss.valset.domain.model.WorkflowTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 默认估值表解析任务应用服务。
 *
 * <p>当前仅提供网关代理和空兜底，不再维护样例批次数据。</p>
 */
@Service
public class DefaultOutsourcedDataTaskService implements OutsourcedDataTaskService {

    private static final OutsourcedDataTaskStageCatalog DEFAULT_STAGE_CATALOG = new OutsourcedDataTaskStageCatalog();

    private final OutsourcedDataTaskGateway outsourcedDataTaskGateway;
    private final WorkflowTaskGateway workflowTaskGateway;
    private final SchedulerService schedulerService;
    private final ObjectMapper objectMapper;
    private OutsourcedDataTaskStageCatalog stageCatalog = new OutsourcedDataTaskStageCatalog();

    public DefaultOutsourcedDataTaskService() {
        this(null, null, null, null);
    }

    @Autowired
    public DefaultOutsourcedDataTaskService(OutsourcedDataTaskGateway outsourcedDataTaskGateway,
                                           WorkflowTaskGateway workflowTaskGateway,
                                           SchedulerService schedulerService,
                                           ObjectMapper objectMapper) {
        this.outsourcedDataTaskGateway = outsourcedDataTaskGateway;
        this.workflowTaskGateway = workflowTaskGateway;
        this.schedulerService = schedulerService;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
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
        return Collections.emptyList();
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
        OutsourcedDataTaskStepDTO currentStep = requireCurrentStep(batchId);
        WorkflowTask sourceTask = requireWorkflowTask(currentStep);
        boolean resumeFromFailure = isManualExecuteResume(batchId);
        triggerCurrentTask(batchId, sourceTask, resumeFromFailure);
        String message = resumeFromFailure
                ? "已提交估值表解析任务继续执行请求"
                : "已提交估值表解析任务手动执行请求";
        return accepted(batchId, currentStep.getStepId(), "EXECUTE", message);
    }

    @Override
    public OutsourcedDataTaskActionResultDTO retry(String batchId, OutsourcedDataTaskActionCommand command) {
        requireBatch(batchId);
        OutsourcedDataTaskStepDTO currentStep = requireCurrentStep(batchId);
        WorkflowTask sourceTask = requireWorkflowTask(currentStep);
        Long taskId = cloneAndTrigger(sourceTask);
        return accepted(batchId, currentStep.getStepId(), "RETRY", "已提交估值表解析任务全流程重跑请求，taskId=" + taskId);
    }

    @Override
    public OutsourcedDataTaskActionResultDTO stop(String batchId, OutsourcedDataTaskActionCommand command) {
        requireBatch(batchId);
        return accepted(batchId, null, "STOP", "已提交估值表解析任务停止请求");
    }

    @Override
    public OutsourcedDataTaskActionResultDTO retryStep(String batchId, String stepId, OutsourcedDataTaskActionCommand command) {
        OutsourcedDataTaskStepDTO step = requireStep(batchId, stepId);
        WorkflowTask sourceTask = requireWorkflowTask(step);
        Long taskId = cloneAndTrigger(sourceTask);
        return accepted(batchId, stepId, "RETRY_STEP", "已提交估值表解析任务阶段重跑请求，taskId=" + taskId);
    }

    @Override
    public List<OutsourcedDataTaskActionResultDTO> batchExecute(OutsourcedDataTaskBatchCommand command) {
        return batchAction(command, true);
    }

    @Override
    public List<OutsourcedDataTaskActionResultDTO> batchRetry(OutsourcedDataTaskBatchCommand command) {
        return batchAction(command, false);
    }

    @Override
    public List<OutsourcedDataTaskActionResultDTO> batchStop(OutsourcedDataTaskBatchCommand command) {
        if (command == null || command.getBatchIds() == null) {
            return Collections.emptyList();
        }
        return command.getBatchIds().stream()
                .map(batchId -> {
                    requireBatch(batchId);
                    return accepted(batchId, null, "STOP", "已提交批量停止请求");
                })
                .collect(Collectors.toList());
    }

    private List<OutsourcedDataTaskActionResultDTO> batchAction(OutsourcedDataTaskBatchCommand command, boolean manualExecute) {
        if (command == null || command.getBatchIds() == null) {
            return Collections.emptyList();
        }
        return command.getBatchIds().stream()
                .map(batchId -> {
                    return manualExecute ? execute(batchId, null) : retry(batchId, null);
                })
                .collect(Collectors.toList());
    }

    private void triggerCurrentTask(String batchId, WorkflowTask sourceTask, boolean resumeFromFailure) {
        if (sourceTask == null || sourceTask.getTaskId() == null) {
            throw new IllegalArgumentException("估值表解析任务缺少可调度的工作流任务：" + batchId);
        }
        if (sourceTask.getTaskStatus() == TaskStatus.SUCCESS || sourceTask.getTaskStatus() == TaskStatus.RUNNING) {
            throw new IllegalStateException("当前任务状态不允许重新调度：" + batchId + "，taskStatus=" + sourceTask.getTaskStatus());
        }
        if (resumeFromFailure) {
            if (workflowTaskGateway == null || !workflowTaskGateway.markRetrying(sourceTask.getTaskId())) {
                throw new IllegalStateException("估值表解析任务无法切换到重试状态：" + batchId);
            }
        }
        triggerNow(sourceTask.getTaskId(), batchId);
    }

    private Long cloneAndTrigger(WorkflowTask sourceTask) {
        if (sourceTask == null || sourceTask.getTaskType() == null) {
            throw new IllegalArgumentException("工作流任务不能为空");
        }
        if (workflowTaskGateway == null) {
            throw new IllegalStateException("工作流任务网关或调度器未启用，无法重跑任务");
        }
        WorkflowTask clone = WorkflowTask.builder()
                .taskType(sourceTask.getTaskType())
                .taskStatus(TaskStatus.PENDING)
                .taskStage(sourceTask.getTaskStage() == null
                        ? inferTaskStage(sourceTask.getTaskType())
                        : sourceTask.getTaskStage())
                .businessKey(sourceTask.getBusinessKey())
                .fileId(sourceTask.getFileId())
                .inputPayload(normalizeJson(sourceTask.getInputPayload()))
                .build();
        Long taskId = workflowTaskGateway.save(clone);
        triggerNow(taskId, null);
        return taskId;
    }

    private OutsourcedDataTaskStepDTO requireCurrentStep(String batchId) {
        List<OutsourcedDataTaskStepDTO> steps = listSteps(batchId);
        return steps.stream()
                .filter(step -> step != null && hasText(step.getTaskId()))
                .filter(step -> Boolean.TRUE.equals(step.getCurrentFlag())
                        || isFailedOrBlocked(step.getStatus())
                        || OutsourcedDataTaskStatus.RUNNING.name().equals(step.getStatus()))
                .findFirst()
                .orElseGet(() -> steps.stream()
                        .filter(step -> step != null && hasText(step.getTaskId()))
                        .reduce((left, right) -> right)
                        .orElseThrow(() -> new IllegalArgumentException("估值表解析任务阶段不存在：" + batchId)));
    }

    private WorkflowTask requireWorkflowTask(OutsourcedDataTaskStepDTO step) {
        Long taskId = parseLong(step == null ? null : step.getTaskId());
        if (taskId == null || workflowTaskGateway == null) {
            throw new IllegalArgumentException("估值表解析任务缺少对应的工作流任务：" + (step == null ? null : step.getStepId()));
        }
        return workflowTaskGateway.findById(taskId);
    }

    private boolean isManualExecuteResume(String batchId) {
        OutsourcedDataTaskBatchDTO batch = requireBatch(batchId);
        return batch != null && isAnyStatus(batch, OutsourcedDataTaskStatus.FAILED, OutsourcedDataTaskStatus.BLOCKED, OutsourcedDataTaskStatus.STOPPED);
    }

    private static boolean isFailedOrBlocked(String status) {
        return OutsourcedDataTaskStatus.FAILED.name().equals(status)
                || OutsourcedDataTaskStatus.BLOCKED.name().equals(status);
    }

    private static TaskStage inferTaskStage(TaskType taskType) {
        if (taskType == null) {
            return TaskStage.OTHER;
        }
        return switch (taskType) {
            case EXTRACT_DATA -> TaskStage.EXTRACT;
            case PARSE_WORKBOOK -> TaskStage.PARSE;
            case MATCH_SUBJECT -> TaskStage.MATCH;
            default -> TaskStage.OTHER;
        };
    }

    private String normalizeJson(String payload) {
        if (payload == null || payload.isBlank() || objectMapper == null) {
            return payload;
        }
        try {
            Object json = objectMapper.readValue(payload, Object.class);
            return objectMapper.writeValueAsString(json);
        } catch (Exception ignored) {
            return payload;
        }
    }

    private void triggerNow(Long taskId, String batchId) {
        if (schedulerService == null) {
            throw new IllegalStateException("调度器未启用，无法触发估值表解析任务：" + (batchId == null ? taskId : batchId));
        }
        try {
            schedulerService.triggerNow(taskId);
        } catch (Exception exception) {
            throw new IllegalStateException("触发调度失败：" + (batchId == null ? taskId : batchId), exception);
        }
    }

    private static Long parseLong(String value) {
        if (!hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private OutsourcedDataTaskBatchDTO requireBatch(String batchId) {
        if (outsourcedDataTaskGateway != null) {
            Optional<OutsourcedDataTaskBatchDTO> persisted = outsourcedDataTaskGateway.findTask(batchId);
            if (persisted.isPresent()) {
                return persisted.get();
            }
        }
        throw new IllegalArgumentException("估值表解析任务批次不存在：" + batchId);
    }

    private OutsourcedDataTaskStepDTO requireStep(String batchId, String stepId) {
        if (outsourcedDataTaskGateway != null) {
            return outsourcedDataTaskGateway.listSteps(batchId).stream()
                    .filter(step -> Objects.equals(step.getStepId(), stepId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("估值表解析任务阶段不存在：" + stepId));
        }
        throw new IllegalArgumentException("估值表解析任务阶段不存在：" + stepId);
    }

    private List<OutsourcedDataTaskBatchDTO> filterBatches(OutsourcedDataTaskQueryCommand query) {
        if (query == null) {
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }

    private List<OutsourcedDataTaskBatchDTO> loadBatches(OutsourcedDataTaskQueryCommand query) {
        if (outsourcedDataTaskGateway != null) {
            return outsourcedDataTaskGateway.listTasks(query);
        }
        return filterBatches(query);
    }

    private List<OutsourcedDataTaskStageSummaryDTO> buildStepSummaries(List<OutsourcedDataTaskBatchDTO> batches) {
        Map<String, List<OutsourcedDataTaskBatchDTO>> stageMap = new java.util.HashMap<>();
        batches.forEach(batch -> {
            if (batch == null || !hasText(batch.getBatchId())) {
                return;
            }
            listSteps(batch.getBatchId()).stream()
                    .map(OutsourcedDataTaskStepDTO::getStage)
                    .filter(DefaultOutsourcedDataTaskService::hasText)
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
}
