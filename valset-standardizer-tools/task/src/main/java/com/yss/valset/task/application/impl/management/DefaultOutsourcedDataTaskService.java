package com.yss.valset.task.application.impl.management;

import com.yss.cloud.dto.result.PageResult;
import com.yss.valset.task.application.command.OutsourcedDataTaskActionCommand;
import com.yss.valset.task.application.command.OutsourcedDataTaskBatchCommand;
import com.yss.valset.task.application.command.OutsourcedDataTaskQueryCommand;
import com.yss.valset.task.application.service.workflow.WorkflowRuntimeCatalog;
import com.yss.valset.task.application.dto.OutsourcedDataTaskActionResultDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskBatchDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskBatchDetailDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskStageSummaryDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskStepDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskSummaryDTO;
import com.yss.valset.task.application.dto.OutsourcedDataTaskTraceDTO;
import com.yss.valset.task.application.port.OutsourcedDataTaskGateway;
import com.yss.valset.task.application.service.OutsourcedDataTaskService;
import com.yss.valset.task.domain.model.OutsourcedDataTaskStatus;
import com.yss.valset.parser.application.port.ParseExecutionUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 默认估值表解析任务应用服务。
 *
 * <p>
 * 当前仅提供网关代理和空兜底，不再维护样例批次数据。
 * </p>
 */
@Service
@Slf4j
public class DefaultOutsourcedDataTaskService implements OutsourcedDataTaskService {

    private static final Set<String> RETRYABLE_STATUSES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            OutsourcedDataTaskStatus.SUCCESS.name(),
            OutsourcedDataTaskStatus.FAILED.name(),
            OutsourcedDataTaskStatus.STOPPED.name())));

    private final OutsourcedDataTaskGateway outsourcedDataTaskGateway;
    private final ParseExecutionUseCase parseExecutionUseCase;
    private final JobExplorer springBatchJobExplorer;
    private final JobOperator springBatchJobOperator;
    private WorkflowRuntimeCatalog stageCatalog;

    public DefaultOutsourcedDataTaskService() {
        this(null, null, null, null);
    }

    @Autowired
    public DefaultOutsourcedDataTaskService(OutsourcedDataTaskGateway outsourcedDataTaskGateway,
            ParseExecutionUseCase parseExecutionUseCase,
            @org.springframework.beans.factory.annotation.Qualifier("springBatchJobExplorer") JobExplorer springBatchJobExplorer,
            @org.springframework.beans.factory.annotation.Qualifier("springBatchJobOperator") JobOperator springBatchJobOperator) {
        this.outsourcedDataTaskGateway = outsourcedDataTaskGateway;
        this.parseExecutionUseCase = parseExecutionUseCase;
        this.springBatchJobExplorer = springBatchJobExplorer;
        this.springBatchJobOperator = springBatchJobOperator;
    }

    @Autowired
    public void setStageCatalog(WorkflowRuntimeCatalog stageCatalog) {
        if (stageCatalog != null) {
            this.stageCatalog = stageCatalog;
        }
    }

    @Override
    public OutsourcedDataTaskSummaryDTO summary(OutsourcedDataTaskQueryCommand query) {
        if (outsourcedDataTaskGateway != null) {
            return enrichSummary(outsourcedDataTaskGateway.summary(query));
        }
        List<OutsourcedDataTaskBatchDTO> batches = loadBatches(query);
        OutsourcedDataTaskSummaryDTO summary = new OutsourcedDataTaskSummaryDTO();
        fillWorkflowMetadata(summary);
        summary.setTotalCount(batches.size());
        summary.setRunningCount(countByStatus(batches, OutsourcedDataTaskStatus.RUNNING));
        summary.setSuccessCount(countByStatus(batches, OutsourcedDataTaskStatus.SUCCESS));
        summary.setFailedCount(batches.stream()
                .filter(batch -> isAnyStatus(batch, OutsourcedDataTaskStatus.FAILED, OutsourcedDataTaskStatus.BLOCKED))
                .count());
        summary.setStepSummaries(buildStepSummaries(batches));
        summary.setStageCatalog(summary.getStepSummaries());
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
        return detail;
    }

    @Override
    public OutsourcedDataTaskTraceDTO getTrace(String batchId) {
        if (outsourcedDataTaskGateway != null) {
            return outsourcedDataTaskGateway.getTrace(batchId);
        }
        OutsourcedDataTaskTraceDTO trace = new OutsourcedDataTaskTraceDTO();
        trace.setBatch(requireBatch(batchId));
        trace.setTaskSteps(listSteps(batchId));
        return trace;
    }

    @Override
    public List<OutsourcedDataTaskStepDTO> listSteps(String batchId) {
        if (outsourcedDataTaskGateway != null) {
            return outsourcedDataTaskGateway.listSteps(batchId);
        }
        return Collections.emptyList();
    }

    @Override
    public OutsourcedDataTaskActionResultDTO execute(String batchId, OutsourcedDataTaskActionCommand command) {
        requireBatch(batchId);
        OutsourcedDataTaskStepDTO currentStep = requireCurrentStep(batchId);
        Long taskId = resolveTaskId(batchId, currentStep);
        triggerParseBatch(taskId);
        return accepted(batchId, currentStep.getStepId(), "EXECUTE", "已提交估值表解析任务执行请求，taskId=" + taskId);
    }

    @Override
    public OutsourcedDataTaskActionResultDTO retry(String batchId, OutsourcedDataTaskActionCommand command) {
        validateRetryableBatch(batchId);
        OutsourcedDataTaskStepDTO currentStep = requireCurrentStep(batchId);
        Long taskId = resolveTaskId(batchId, currentStep);
        triggerParseBatch(taskId);
        return accepted(batchId, currentStep.getStepId(), "RETRY", "已提交估值表解析任务重试请求，taskId=" + taskId);
    }

    @Override
    public OutsourcedDataTaskActionResultDTO stop(String batchId, OutsourcedDataTaskActionCommand command) {
        requireBatch(batchId);
        OutsourcedDataTaskStepDTO currentStep = requireCurrentStep(batchId);
        Long taskId = resolveTaskId(batchId, currentStep);
        Long executionId = stopRunningBatch(taskId);
        return accepted(batchId, currentStep.getStepId(), "STOP", "已提交估值表解析任务停止请求，executionId=" + executionId);
    }

    @Override
    public OutsourcedDataTaskActionResultDTO retryStep(String batchId, String stepId,
            OutsourcedDataTaskActionCommand command) {
        OutsourcedDataTaskStepDTO step = requireStep(batchId, stepId);
        Long taskId = resolveTaskId(batchId, step);
        triggerParseBatch(taskId);
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
                .collect(java.util.stream.Collectors.toList());
    }

    private List<OutsourcedDataTaskActionResultDTO> batchAction(OutsourcedDataTaskBatchCommand command,
            boolean manualExecute) {
        if (command == null || command.getBatchIds() == null) {
            return Collections.emptyList();
        }
        List<BatchActionContext> actionContexts = command.getBatchIds().stream()
                .map(batchId -> prepareBatchAction(batchId, manualExecute))
                .collect(Collectors.toList());
        actionContexts.forEach(context -> submitBatchAction(context, manualExecute, command));
        String action = manualExecute ? "BATCH_EXECUTE" : "BATCH_RETRY";
        String actionLabel = manualExecute ? "批量执行" : "批量重新解析";
        return actionContexts.stream()
                .map(context -> accepted(context.getBatchId(), context.getStepId(),
                        action,
                        actionLabel + "请求已异步提交，taskId=" + context.getTaskId()))
                .collect(Collectors.toList());
    }

    private BatchActionContext prepareBatchAction(String batchId, boolean manualExecute) {
        if (manualExecute) {
            requireBatch(batchId);
        } else {
            validateRetryableBatch(batchId);
        }
        OutsourcedDataTaskStepDTO currentStep = requireCurrentStep(batchId);
        Long taskId = resolveTaskId(batchId, currentStep);
        return new BatchActionContext(batchId, currentStep.getStepId(), taskId);
    }

    private void submitBatchAction(BatchActionContext context,
            boolean manualExecute,
            OutsourcedDataTaskBatchCommand command) {
        String action = manualExecute ? "execute" : "retry";
        log.info("已提交估值表解析任务{}请求，batchId={}，stepId={}，taskId={}，reason={}",
                action,
                context.getBatchId(),
                context.getStepId(),
                context.getTaskId(),
                command == null ? null : command.getReason());
        triggerParseBatch(context.getTaskId());
    }

    private OutsourcedDataTaskBatchDTO validateRetryableBatch(String batchId) {
        OutsourcedDataTaskBatchDTO batch = requireBatch(batchId);
        if (batch == null || !hasText(batch.getStatus()) || !RETRYABLE_STATUSES.contains(batch.getStatus())) {
            throw new IllegalArgumentException("当前批次不支持重新解析，仅允许已完成、失败或已停止的批次重试：" + batchId);
        }
        return batch;
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

    private Long resolveTaskId(String batchId, OutsourcedDataTaskStepDTO step) {
        Long taskId = parseLong(step == null ? null : step.getTaskId());
        if (taskId != null) {
            return taskId;
        }
        if (batchId != null && batchId.startsWith("TASK-")) {
            return parseLong(batchId.substring("TASK-".length()));
        }
        throw new IllegalArgumentException("估值表解析任务缺少可执行的 taskId：" + batchId);
    }

    private void triggerParseBatch(Long taskId) {
        if (taskId == null) {
            throw new IllegalArgumentException("估值表解析任务缺少可执行的 taskId");
        }
        if (parseExecutionUseCase == null) {
            throw new IllegalStateException("解析执行用例未启用，无法提交 批量任务 任务：" + taskId);
        }
        parseExecutionUseCase.execute(taskId);
    }

    private Long stopRunningBatch(Long taskId) {
        if (taskId == null) {
            throw new IllegalArgumentException("估值表解析任务缺少可停止的 taskId");
        }
        if (springBatchJobExplorer == null || springBatchJobOperator == null) {
            throw new IllegalStateException("批量任务 停止能力未启用，无法停止任务：" + taskId);
        }
        Set<JobExecution> runningExecutions = springBatchJobExplorer.findRunningJobExecutions("valuationParseJob");
        if (runningExecutions == null || runningExecutions.isEmpty()) {
            return null;
        }
        for (JobExecution execution : runningExecutions) {
            if (execution == null || execution.getJobParameters() == null) {
                continue;
            }
            Long executionTaskId = execution.getJobParameters().getLong("taskId");
            if (Objects.equals(taskId, executionTaskId)) {
                try {
                    springBatchJobOperator.stop(execution.getId());
                } catch (Exception exception) {
                    throw new IllegalStateException("停止 批量任务 任务失败，taskId=" + taskId, exception);
                }
                return execution.getId();
            }
        }
        return null;
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
        WorkflowRuntimeCatalog runtimeCatalog = stageCatalog();
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
        return runtimeCatalog.stageSequence().stream()
                .map(stage -> {
                    List<OutsourcedDataTaskBatchDTO> stageBatches = stageMap.getOrDefault(stage.name(),
                            Collections.emptyList());
                    OutsourcedDataTaskStageSummaryDTO summary = new OutsourcedDataTaskStageSummaryDTO();
                    summary.setStage(stage.name());
                    summary.setStep(stage.name());
                    summary.setStageName(runtimeCatalog.stageLabel(stage.name()));
                    summary.setStepName(runtimeCatalog.stageLabel(stage.name()));
                    summary.setStageDescription(runtimeCatalog.stageDescription(stage.name()));
                    summary.setStepDescription(runtimeCatalog.stageDescription(stage.name()));
                    summary.setTotalCount(stageBatches.size());
                    summary.setRunningCount(countByStatus(stageBatches, OutsourcedDataTaskStatus.RUNNING));
                    summary.setFailedCount(stageBatches.stream()
                            .filter(batch -> isAnyStatus(batch, OutsourcedDataTaskStatus.FAILED,
                                    OutsourcedDataTaskStatus.BLOCKED))
                            .count());
                    summary.setPendingCount(countByStatus(stageBatches, OutsourcedDataTaskStatus.PENDING));
                    return summary;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    private OutsourcedDataTaskSummaryDTO enrichSummary(OutsourcedDataTaskSummaryDTO summary) {
        if (summary == null) {
            return null;
        }
        fillWorkflowMetadata(summary);
        return summary;
    }

    private void fillWorkflowMetadata(OutsourcedDataTaskSummaryDTO summary) {
        if (summary == null) {
            return;
        }
        WorkflowRuntimeCatalog runtimeCatalog = stageCatalog();
        Optional<WorkflowRuntimeCatalog.ActiveWorkflowDefinition> definitionOpt = runtimeCatalog.activeWorkflowDefinition();
        if (definitionOpt.isPresent()) {
            WorkflowRuntimeCatalog.ActiveWorkflowDefinition definition = definitionOpt.get();
            summary.setWorkflowCode(definition.getWorkflowCode());
            summary.setWorkflowId(definition.getWorkflowId());
            summary.setVersionNo(definition.getVersionNo());
        } else {
            summary.setWorkflowCode(runtimeCatalog.activeWorkflowCode());
            summary.setWorkflowId(runtimeCatalog.activeWorkflowId());
            summary.setVersionNo(runtimeCatalog.activeWorkflowVersionNo());
        }
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

    private static boolean isFailedOrBlocked(String status) {
        return OutsourcedDataTaskStatus.FAILED.name().equals(status)
                || OutsourcedDataTaskStatus.BLOCKED.name().equals(status);
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

    private WorkflowRuntimeCatalog stageCatalog() {
        return Objects.requireNonNull(stageCatalog, "WorkflowRuntimeCatalog 未注入");
    }

    private static final class BatchActionContext {

        private final String batchId;
        private final String stepId;
        private final Long taskId;

        private BatchActionContext(String batchId, String stepId, Long taskId) {
            this.batchId = batchId;
            this.stepId = stepId;
            this.taskId = taskId;
        }

        private String getBatchId() {
            return batchId;
        }

        private String getStepId() {
            return stepId;
        }

        private Long getTaskId() {
            return taskId;
        }
    }
}
