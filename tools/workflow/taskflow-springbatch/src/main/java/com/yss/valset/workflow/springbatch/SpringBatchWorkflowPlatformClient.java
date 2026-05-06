package com.yss.valset.workflow.springbatch;

import com.yss.valset.workflow.model.EtlPlatformType;
import com.yss.valset.workflow.model.WorkflowDefinitionDTO;
import com.yss.valset.workflow.model.WorkflowEngineBindingDTO;
import com.yss.valset.workflow.model.WorkflowInstanceDTO;
import com.yss.valset.workflow.model.WorkflowLogQueryRequest;
import com.yss.valset.workflow.model.WorkflowOperationType;
import com.yss.valset.workflow.model.WorkflowPlatformCommand;
import com.yss.valset.workflow.model.WorkflowPlatformExecutionResult;
import com.yss.valset.workflow.model.WorkflowRetryRequest;
import com.yss.valset.workflow.model.WorkflowStageDTO;
import com.yss.valset.workflow.model.WorkflowStageLogDTO;
import com.yss.valset.workflow.model.WorkflowStatus;
import com.yss.valset.workflow.model.WorkflowStopRequest;
import com.yss.valset.workflow.model.WorkflowTriggerRequest;
import com.yss.valset.workflow.service.AbstractWorkflowPlatformClient;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.ReferenceJobFactory;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.builder.SimpleJobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Spring Batch 工作流客户端。
 */
@Component
@RequiredArgsConstructor
public class SpringBatchWorkflowPlatformClient extends AbstractWorkflowPlatformClient {

    private final JobLauncher jobLauncher;
    private final JobRepository jobRepository;
    private final JobRegistry jobRegistry;
    private final SpringBatchWorkflowExecutionStore executionStore;
    private final SpringBatchStageProcessor stageProcessor;
    private final PlatformTransactionManager springBatchTransactionManager;

    @Override
    public EtlPlatformType platformType() {
        return EtlPlatformType.SPRING_BATCH;
    }

    @Override
    public void validate(WorkflowDefinitionDTO definition) {
        WorkflowEngineBindingDTO binding = definition == null ? null : definition.getEngineBinding();
        if (binding == null || binding.getPlatformType() != EtlPlatformType.SPRING_BATCH) {
            throw new IllegalArgumentException("Spring Batch 绑定信息不合法");
        }
        if (!StringUtils.hasText(binding.getExternalWorkflowId())) {
            throw new IllegalArgumentException("Spring Batch 需要配置作业名称");
        }
    }

    @Override
    public WorkflowPlatformExecutionResult trigger(WorkflowDefinitionDTO definition,
                                                   WorkflowInstanceDTO instance,
                                                   WorkflowTriggerRequest request) {
        WorkflowPlatformCommand command = buildCommand(WorkflowOperationType.TRIGGER, definition, instance, request);
        JobExecution execution = executeJob(definition, instance, request);
        return buildExecutionResult(definition, instance, execution, command, "Spring Batch 作业已提交");
    }

    @Override
    public WorkflowPlatformExecutionResult stop(WorkflowDefinitionDTO definition,
                                                WorkflowInstanceDTO instance,
                                                WorkflowStopRequest request) {
        WorkflowPlatformCommand command = buildCommand(WorkflowOperationType.STOP, definition, instance, request);
        JobExecution execution = locateExecution(instance).map(this::markStopped).orElse(null);
        return buildExecutionResult(definition, instance, execution, command,
                request == null || !StringUtils.hasText(request.getReason()) ? "Spring Batch 作业已停止" : request.getReason());
    }

    @Override
    public WorkflowPlatformExecutionResult retry(WorkflowDefinitionDTO definition,
                                                 WorkflowInstanceDTO instance,
                                                 WorkflowRetryRequest request) {
        WorkflowPlatformCommand command = buildCommand(WorkflowOperationType.RETRY, definition, instance, request);
        JobExecution execution = executeJob(definition, instance, request == null ? null : WorkflowTriggerRequest.builder()
                .workflowCode(definition == null ? null : definition.getWorkflowCode())
                .workflowVersionNo(definition == null ? null : definition.getWorkflowVersionNo())
                .businessKey(instance == null ? null : instance.getBusinessKey())
                .context(request.getContext())
                .build());
        return buildExecutionResult(definition, instance, execution, command, "Spring Batch 作业已重新提交");
    }

    @Override
    public WorkflowPlatformExecutionResult query(WorkflowDefinitionDTO definition,
                                                 WorkflowInstanceDTO instance) {
        WorkflowPlatformCommand command = buildCommand(WorkflowOperationType.QUERY, definition, instance, (WorkflowLogQueryRequest) null);
        JobExecution execution = locateExecution(instance).orElse(null);
        return buildExecutionResult(definition, instance, execution, command, "Spring Batch 作业查询");
    }

    @Override
    public List<WorkflowPlatformExecutionResult> queryLogs(WorkflowDefinitionDTO definition,
                                                          WorkflowInstanceDTO instance,
                                                          WorkflowLogQueryRequest request) {
        WorkflowPlatformCommand command = buildCommand(WorkflowOperationType.QUERY_LOGS, definition, instance, request);
        JobExecution execution = locateExecution(instance).orElse(null);
        if (execution == null) {
            return List.of();
        }
        List<WorkflowStageLogDTO> stageLogs = executionStore.listStageLogs(execution.getId(),
                request == null ? null : request.getStageCode());
        if (CollectionUtils.isEmpty(stageLogs)) {
            return List.of(buildExecutionResult(definition, instance, execution, command, "Spring Batch 作业日志"));
        }
        return stageLogs.stream()
                .map(stageLog -> WorkflowPlatformExecutionResult.builder()
                        .platformType(platformType())
                        .externalWorkflowId(resolveExternalWorkflowId(definition, instance))
                        .externalInstanceId(String.valueOf(execution.getId()))
                        .rawStatus(stageLog.getRawStatus())
                        .message(stageLog.getMessage())
                        .payload(buildLogPayload(definition, instance, execution, stageLog, command))
                        .stageLogs(List.of(stageLog))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    protected Map<String, Object> platformSpecificPayload(WorkflowDefinitionDTO definition,
                                                          WorkflowInstanceDTO instance,
                                                          WorkflowPlatformCommand command) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("jobName", resolveJobName(definition));
        payload.put("batchInfrastructure", "resourceless");
        payload.put("operationType", command == null || command.getOperationType() == null ? null : command.getOperationType().name());
        payload.put("canonicalStatus", WorkflowStatus.fromRawStatus(instance == null ? null : instance.getRawStatus()).name());
        return payload;
    }

    @Override
    protected String rawTriggerStatus() {
        return BatchStatus.STARTED.name();
    }

    @Override
    protected String rawStopStatus() {
        return BatchStatus.STOPPED.name();
    }

    @Override
    protected String rawRetryStatus() {
        return BatchStatus.STARTED.name();
    }

    private JobExecution executeJob(WorkflowDefinitionDTO definition,
                                    WorkflowInstanceDTO instance,
                                    WorkflowTriggerRequest request) {
        Job job = buildJob(definition, instance, request);
        registerJob(job);
        try {
            JobExecution execution = jobLauncher.run(job, buildJobParameters(definition, instance, request));
            executionStore.save(instance == null ? null : instance.getInstanceId(), execution);
            return execution;
        } catch (Exception e) {
            throw new IllegalStateException("Spring Batch 作业执行失败：" + e.getMessage(), e);
        }
    }

    private Job buildJob(WorkflowDefinitionDTO definition,
                         WorkflowInstanceDTO instance,
                         WorkflowTriggerRequest request) {
        String jobName = resolveJobName(definition);
        List<WorkflowStageDTO> stages = definition.getStages() == null ? List.of() : definition.getStages().stream()
                .sorted(Comparator.comparing(WorkflowStageDTO::getStageOrder, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());
        if (stages.isEmpty()) {
            stages = List.of(WorkflowStageDTO.builder()
                    .stageCode("DEFAULT")
                    .stageName("默认步骤")
                    .stageOrder(1)
                    .build());
        }
        JobBuilder jobBuilder = new JobBuilder(jobName, jobRepository);
        SimpleJobBuilder simpleJobBuilder = jobBuilder.start(buildStep(definition, instance, request, stages.get(0)));
        for (int index = 1; index < stages.size(); index++) {
            simpleJobBuilder = simpleJobBuilder.next(buildStep(definition, instance, request, stages.get(index)));
        }
        return simpleJobBuilder.build();
    }

    private Step buildStep(WorkflowDefinitionDTO definition,
                           WorkflowInstanceDTO instance,
                           WorkflowTriggerRequest request,
                           WorkflowStageDTO stage) {
        String stepName = stage == null || !StringUtils.hasText(stage.getStageCode())
                ? "default-step"
                : stage.getStageCode();
        Tasklet tasklet = (contribution, chunkContext) -> {
            ExecutionContext executionContext = chunkContext.getStepContext().getStepExecution().getExecutionContext();
            SpringBatchStageExecutionResult stageExecutionResult = stageProcessor.process(
                    definition,
                    instance,
                    stage,
                    buildCommand(WorkflowOperationType.TRIGGER, definition, instance, request));
            executionContext.putString("workflowCode", definition.getWorkflowCode());
            executionContext.putString("workflowVersionNo", String.valueOf(definition.getWorkflowVersionNo()));
            executionContext.putString("stageCode", stage == null ? null : stage.getStageCode());
            executionContext.putString("stageName", stage == null ? null : stage.getStageName());
            if (stageExecutionResult.getInput() != null) {
                stageExecutionResult.getInput().forEach((key, value) -> executionContext.put("input." + key, value));
            }
            if (stageExecutionResult.getOutput() != null) {
                stageExecutionResult.getOutput().forEach((key, value) -> executionContext.put("output." + key, value));
            }
            if (stageExecutionResult.getMetadata() != null) {
                stageExecutionResult.getMetadata().forEach((key, value) -> executionContext.put("meta." + key, value));
            }
            org.springframework.batch.core.StepExecution stepExecution = chunkContext.getStepContext().getStepExecution();
            Long executionId = stepExecution.getJobExecutionId();
            WorkflowStageLogDTO stageLog = WorkflowStageLogDTO.builder()
                    .instanceId(instance == null ? null : instance.getInstanceId())
                    .workflowCode(definition.getWorkflowCode())
                    .workflowVersionNo(definition.getWorkflowVersionNo())
                    .stageCode(stage == null ? stepExecution.getStepName() : stage.getStageCode())
                    .stageName(stage == null ? stepExecution.getStepName() : stage.getStageName())
                    .stageOrder(stage == null ? null : stage.getStageOrder())
                    .status(stageExecutionResult.getStatus())
                    .rawStatus(stageExecutionResult.getRawStatus())
                    .message(stageExecutionResult.getMessage())
                    .startTime(stageExecutionResult.getStartTime())
                    .endTime(stageExecutionResult.getEndTime())
                    .payload(buildStagePayload(stageExecutionResult, stepExecution))
                    .build();
            executionStore.saveStageLog(executionId, stageLog);
            return RepeatStatus.FINISHED;
        };
        return new StepBuilder(stepName, jobRepository)
                .tasklet(tasklet, springBatchTransactionManager)
                .build();
    }

    private JobParameters buildJobParameters(WorkflowDefinitionDTO definition,
                                             WorkflowInstanceDTO instance,
                                             WorkflowTriggerRequest request) {
        JobParametersBuilder builder = new JobParametersBuilder();
        builder.addString("workflowCode", definition.getWorkflowCode(), true);
        builder.addLong("workflowVersionNo", definition.getWorkflowVersionNo() == null ? null : definition.getWorkflowVersionNo().longValue(), true);
        builder.addString("workflowName", definition.getWorkflowName(), true);
        if (instance != null && StringUtils.hasText(instance.getBusinessKey())) {
            builder.addString("businessKey", instance.getBusinessKey(), false);
        }
        builder.addString("instanceId", instance == null ? null : instance.getInstanceId(), true);
        builder.addString("externalWorkflowId", resolveExternalWorkflowId(definition, instance), true);
        builder.addLocalDateTime("triggerTime", LocalDateTime.now(), true);
        if (request != null && request.getContext() != null) {
            request.getContext().forEach((key, value) -> {
                if (value != null) {
                    builder.addString("ctx." + key, String.valueOf(value), false);
                }
            });
        }
        return builder.toJobParameters();
    }

    private WorkflowPlatformExecutionResult buildExecutionResult(WorkflowDefinitionDTO definition,
                                                                 WorkflowInstanceDTO instance,
                                                                 JobExecution execution,
                                                                 WorkflowPlatformCommand command,
                                                                 String defaultMessage) {
        List<WorkflowStageLogDTO> stageLogs = execution == null
                ? List.of()
                : executionStore.listStageLogs(execution.getId(), null);
        return WorkflowPlatformExecutionResult.builder()
                .platformType(platformType())
                .externalWorkflowId(resolveExternalWorkflowId(definition, instance))
                .externalInstanceId(execution == null ? resolveExternalInstanceId(definition, instance) : String.valueOf(execution.getId()))
                .rawStatus(execution == null ? (instance == null ? null : instance.getRawStatus()) : execution.getStatus().name())
                .message(resolveMessage(execution, defaultMessage))
                .payload(buildPayload(definition, instance, command, execution))
                .stageLogs(stageLogs)
                .build();
    }

    private Map<String, Object> buildPayload(WorkflowDefinitionDTO definition,
                                             WorkflowInstanceDTO instance,
                                             WorkflowPlatformCommand command,
                                             JobExecution execution) {
        Map<String, Object> payload = new LinkedHashMap<>(platformSpecificPayload(definition, instance, command));
        payload.put("jobName", resolveJobName(definition));
        payload.put("jobExecutionId", execution == null || execution.getId() == null ? null : execution.getId());
        payload.put("batchStatus", execution == null ? null : execution.getStatus().name());
        payload.put("exitStatus", execution == null || execution.getExitStatus() == null ? null : execution.getExitStatus().getExitCode());
        payload.put("jobParameters", execution == null ? Map.of() : toParameterMap(execution.getJobParameters()));
        payload.put("stepCount", execution == null ? 0 : execution.getStepExecutions().size());
        return payload;
    }

    private Map<String, Object> buildLogPayload(WorkflowDefinitionDTO definition,
                                                WorkflowInstanceDTO instance,
                                                JobExecution execution,
                                                WorkflowStageLogDTO stageLog,
                                                WorkflowPlatformCommand command) {
        Map<String, Object> payload = new LinkedHashMap<>(buildPayload(definition, instance, command, execution));
        payload.put("stageCode", stageLog.getStageCode());
        payload.put("stageName", stageLog.getStageName());
        payload.put("stageOrder", stageLog.getStageOrder());
        payload.put("stageStatus", stageLog.getStatus() == null ? null : stageLog.getStatus().name());
        payload.put("stageExitStatus", stageLog.getRawStatus());
        return payload;
    }

    private Map<String, Object> buildStagePayload(SpringBatchStageExecutionResult stageExecutionResult,
                                                  org.springframework.batch.core.StepExecution stepExecution) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("readCount", stepExecution == null ? null : stepExecution.getReadCount());
        payload.put("writeCount", stepExecution == null ? null : stepExecution.getWriteCount());
        payload.put("commitCount", stepExecution == null ? null : stepExecution.getCommitCount());
        payload.put("rollbackCount", stepExecution == null ? null : stepExecution.getRollbackCount());
        payload.put("filterCount", stepExecution == null ? null : stepExecution.getFilterCount());
        payload.put("exitStatus", stepExecution == null || stepExecution.getExitStatus() == null ? null : stepExecution.getExitStatus().getExitCode());
        payload.put("summary", stepExecution == null ? null : stepExecution.getSummary());
        payload.put("input", stageExecutionResult == null ? Map.of() : stageExecutionResult.getInput());
        payload.put("output", stageExecutionResult == null ? Map.of() : stageExecutionResult.getOutput());
        payload.put("metadata", stageExecutionResult == null ? Map.of() : stageExecutionResult.getMetadata());
        payload.put("executionMessage", stageExecutionResult == null ? null : stageExecutionResult.getMessage());
        payload.put("executionRawStatus", stageExecutionResult == null ? null : stageExecutionResult.getRawStatus());
        payload.put("executionStatus", stageExecutionResult == null || stageExecutionResult.getStatus() == null ? null : stageExecutionResult.getStatus().name());
        return payload;
    }

    private String resolveMessage(JobExecution execution, String defaultMessage) {
        if (execution == null) {
            return defaultMessage;
        }
        if (execution.getExitStatus() != null && StringUtils.hasText(execution.getExitStatus().getExitDescription())) {
            return execution.getExitStatus().getExitDescription();
        }
        if (execution.getFailureExceptions() != null && !execution.getFailureExceptions().isEmpty()) {
            Throwable throwable = execution.getFailureExceptions().get(0);
            return throwable == null ? defaultMessage : throwable.getMessage();
        }
        return defaultMessage;
    }

    private Map<String, Object> toParameterMap(JobParameters jobParameters) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        if (jobParameters == null) {
            return parameters;
        }
        jobParameters.getParameters().forEach((key, value) -> parameters.put(key, value == null ? null : value.getValue()));
        return parameters;
    }

    private Optional<JobExecution> locateExecution(WorkflowInstanceDTO instance) {
        if (instance == null) {
            return Optional.empty();
        }
        if (StringUtils.hasText(instance.getExternalInstanceId())) {
            try {
                return executionStore.findByExecutionId(Long.parseLong(instance.getExternalInstanceId()));
            } catch (NumberFormatException ignored) {
                // 继续尝试使用内部实例 ID 索引。
            }
        }
        return executionStore.findByInstanceId(instance.getInstanceId());
    }

    private JobExecution markStopped(JobExecution execution) {
        if (execution == null) {
            return null;
        }
        execution.setStatus(BatchStatus.STOPPED);
        execution.setExitStatus(org.springframework.batch.core.ExitStatus.STOPPED);
        executionStore.save(null, execution);
        return execution;
    }

    private void registerJob(Job job) {
        try {
            if (!jobRegistry.getJobNames().contains(job.getName())) {
                jobRegistry.register(new ReferenceJobFactory(job));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Spring Batch 作业注册失败：" + e.getMessage(), e);
        }
    }

    private String resolveJobName(WorkflowDefinitionDTO definition) {
        if (definition == null) {
            return "spring-batch-job";
        }
        return definition.getWorkflowCode() + "-v" + definition.getWorkflowVersionNo();
    }
}
