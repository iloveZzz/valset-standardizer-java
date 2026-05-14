package com.yss.valset.parser.application.impl.execution;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yss.valset.application.command.ParseTaskCommand;
import com.yss.valset.parser.application.port.ParseExecutionUseCase;
import com.yss.valset.domain.gateway.WorkflowTaskGateway;
import com.yss.valset.domain.model.WorkflowTask;
import com.yss.valset.common.support.TaskFailureClassifier;
import com.yss.valset.domain.model.TaskStage;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 解析工作流程实现。
 *
 * <p>
 * 这个应用服务只负责 Spring Batch 的启动编排：
 * 先补齐作业参数，再提交作业，最后根据执行结果把旧任务状态做兼容回写。
 * 真正的解析、标准化和落库逻辑都在具体 Step 中完成。
 * </p>
 */
@Slf4j
@Service
public class ParseExecutionAppServiceImpl implements ParseExecutionUseCase {

    private final WorkflowTaskGateway taskGateway;
    private final ObjectMapper objectMapper;
    private final JobLauncher springBatchJobLauncher;
    private final Job valuationParseJob;

    public ParseExecutionAppServiceImpl(
            WorkflowTaskGateway taskGateway,
            ObjectMapper objectMapper,
            @Qualifier("springBatchJobLauncher") JobLauncher springBatchJobLauncher,
            @Qualifier("valuationParseJob") Job valuationParseJob
    ) {
        this.taskGateway = taskGateway;
        this.objectMapper = objectMapper;
        this.springBatchJobLauncher = springBatchJobLauncher;
        this.valuationParseJob = valuationParseJob;
    }

    /**
     * 启动估值表解析作业。
     *
     * <p>
     * 这里不直接执行业务逻辑，而是把任务信息封装成 JobParameters 交给 Spring Batch，
     * 由 Job 内部的三个 Step 依次完成。
     * </p>
     */
    @Override
    public void execute(Long taskId) {
        WorkflowTask workflowTask = taskGateway.findById(taskId);
        if (workflowTask == null) {
            throw new IllegalStateException("未找到解析任务，taskId=" + taskId);
        }
        log.info("开始执行 Spring Batch 估值解析任务，taskId={}", taskId);
        taskGateway.markRunning(taskId, TaskStage.PARSE.name(), java.time.LocalDateTime.now());
        String inputPayload = workflowTask.getInputPayload();
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("taskId", taskId, true)
                .addString("taskType", workflowTask.getTaskType() == null ? null : workflowTask.getTaskType().name(), true)
                .addString("taskStage", workflowTask.getTaskStage() == null ? null : workflowTask.getTaskStage().name(), true)
                .addString("businessKey", workflowTask.getBusinessKey(), false)
                .addLong("fileId", workflowTask.getFileId(), false)
                .addString("inputPayload", StringUtils.hasText(inputPayload) ? inputPayload : null, false)
                .addDate("triggerTime", new java.util.Date(), false)
                .toJobParameters();
        try {
            JobExecution jobExecution = springBatchJobLauncher.run(valuationParseJob, jobParameters);
            updateLegacyTaskResult(taskId, jobExecution);
            if (jobExecution == null || jobExecution.getStatus() == null || jobExecution.getStatus().isUnsuccessful()) {
                String failureMessage = resolveFailureMessage(jobExecution, taskId);
                taskGateway.markFailed(taskId, failureMessage);
                throw new IllegalStateException(failureMessage);
            }
            log.info("Spring Batch 估值解析任务执行完成，taskId={}, jobExecutionId={}, status={}",
                    taskId,
                    jobExecution.getId(),
                    jobExecution.getStatus());
        } catch (Exception exception) {
            String failureMessage = TaskFailureClassifier.resolveReadableMessage(exception);
            taskGateway.markFailed(taskId, failureMessage);
            log.error("Spring Batch 估值解析任务执行失败，taskId={}", taskId, exception);
            throw new IllegalStateException("Failed to execute parse task " + taskId, exception);
        }
    }

    /**
     * 将 Spring Batch 的执行结果同步回旧任务模型，保证迁移期页面和接口还能读到结果。
     */
    private void updateLegacyTaskResult(Long taskId, JobExecution jobExecution) {
        if (jobExecution == null) {
            return;
        }
        long fileParseMs = readExecutionContextLong(jobExecution, com.yss.valset.parser.application.support.ParseBatchStepSupport.JOB_CONTEXT_FILE_PARSE_MS);
        long standardizeMs = readExecutionContextLong(jobExecution, com.yss.valset.parser.application.support.ParseBatchStepSupport.JOB_CONTEXT_STANDARDIZE_MS);
        if (fileParseMs > 0 || standardizeMs > 0) {
            taskGateway.updateTaskTimings(taskId, fileParseMs, standardizeMs, null);
        }
        if (BatchStatus.COMPLETED.equals(jobExecution.getStatus())) {
            String resultPayload = jobExecution.getExecutionContext().getString(
                    com.yss.valset.parser.application.support.ParseBatchStepSupport.JOB_CONTEXT_RESULT_PAYLOAD,
                    null);
            if (resultPayload != null) {
                taskGateway.markSuccess(taskId, resultPayload);
            }
        }
    }

    /**
     * 从作业上下文读取耗时字段，避免步骤间重复计算。
     */
    private long readExecutionContextLong(JobExecution jobExecution, String key) {
        if (jobExecution == null || jobExecution.getExecutionContext() == null) {
            return 0L;
        }
        return jobExecution.getExecutionContext().getLong(key, 0L);
    }

    /**
     * 归一化作业失败消息，优先返回真正的异常信息。
     */
    private String resolveFailureMessage(JobExecution jobExecution, Long taskId) {
        if (jobExecution == null) {
            return "Spring Batch 解析任务失败，taskId=" + taskId;
        }
        if (jobExecution.getAllFailureExceptions() != null && !jobExecution.getAllFailureExceptions().isEmpty()) {
            Throwable throwable = jobExecution.getAllFailureExceptions().get(0);
            return TaskFailureClassifier.resolveReadableMessage(throwable);
        }
        return "Spring Batch 解析任务失败，taskId=" + taskId + ", status=" + jobExecution.getStatus();
    }
}
