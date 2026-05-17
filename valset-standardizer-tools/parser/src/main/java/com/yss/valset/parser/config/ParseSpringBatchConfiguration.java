package com.yss.valset.parser.config;

import com.yss.valset.parser.application.support.ParseBatchStepSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 估值表解析 批量任务 配置。
 *
 * <p>
 * 这里把估值表解析拆成固定的三段式作业：
 * 文件解析 -> 结构标准化 -> 标准表落地。
 * Job 只负责调度顺序，真正的业务处理都下沉到 {@link ParseBatchStepSupport}。
 * </p>
 */
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class ParseSpringBatchConfiguration {

    private static final String JOB_NAME = "valuationParseJob";

    private final ParseBatchStepSupport parseBatchStepSupport;

    /**
     * 解析作业入口。
     *
     * <p>
     * 按照固定顺序串联三个 Step，保证每次执行都先解析文件，再做结构标准化，
     * 最后统一落到标准表和结果表。
     * </p>
     */
    @Bean(name = "valuationParseJob")
    public Job valuationParseJob(@Qualifier("springBatchJobRepository") JobRepository jobRepository,
                                 @Qualifier("valuationFileParseStep") Step valuationFileParseStep,
                                 @Qualifier("valuationStructureStandardizeStep") Step valuationStructureStandardizeStep,
                                 @Qualifier("valuationStandardLandingStep") Step valuationStandardLandingStep) {
        return new JobBuilder(JOB_NAME)
                .repository(jobRepository)
                .start(valuationFileParseStep)
                .next(valuationStructureStandardizeStep)
                .next(valuationStandardLandingStep)
                .build();
    }

    /**
     * 第一步：读取原始估值文件，解析成内存中的标准结构，并把中间结果写入作业上下文。
     */
    @Bean(name = "valuationFileParseStep")
    public Step valuationFileParseStep(@Qualifier("springBatchJobRepository") JobRepository jobRepository,
                                       @Qualifier("springBatchTransactionManager") PlatformTransactionManager transactionManager) {
        return new StepBuilder("FILE_PARSE")
                .repository(jobRepository)
                .transactionManager(transactionManager)
                .tasklet(buildTasklet((taskId, jobExecutionContext) -> parseBatchStepSupport.executeFileParse(taskId, jobExecutionContext)))
                .build();
    }

    /**
     * 第二步：对第一步的解析结果做字段、标题、指标等结构标准化。
     */
    @Bean(name = "valuationStructureStandardizeStep")
    public Step valuationStructureStandardizeStep(@Qualifier("springBatchJobRepository") JobRepository jobRepository,
                                                  @Qualifier("springBatchTransactionManager") PlatformTransactionManager transactionManager) {
        return new StepBuilder("STRUCTURE_STANDARDIZE")
                .repository(jobRepository)
                .transactionManager(transactionManager)
                .tasklet(buildTasklet((taskId, jobExecutionContext) -> parseBatchStepSupport.executeStructureStandardize(taskId, jobExecutionContext)))
                .build();
    }

    /**
     * 第三步：把标准化后的结果统一写入标准表、宽表和结果回写接口。
     */
    @Bean(name = "valuationStandardLandingStep")
    public Step valuationStandardLandingStep(@Qualifier("springBatchJobRepository") JobRepository jobRepository,
                                             @Qualifier("springBatchTransactionManager") PlatformTransactionManager transactionManager) {
        return new StepBuilder("STANDARD_LANDING")
                .repository(jobRepository)
                .transactionManager(transactionManager)
                .tasklet(buildTasklet((taskId, jobExecutionContext) -> parseBatchStepSupport.executeStandardLanding(taskId, jobExecutionContext)))
                .build();
    }

    /**
     * 统一的 Step 包装器。
     *
     * <p>
     * 这里只做两件事：
     * 1. 从 JobParameters 中拿到任务 ID；
     * 2. 把 JobExecutionContext 传给具体的业务步骤。
     * </p>
     */
    private Tasklet buildTasklet(ParseStepAction action) {
        return (contribution, chunkContext) -> {
            JobExecution jobExecution = chunkContext.getStepContext().getStepExecution().getJobExecution();
            JobParameters jobParameters = jobExecution.getJobParameters();
            Long taskId = jobParameters == null ? null : jobParameters.getLong("taskId");
            if (taskId == null) {
                throw new IllegalStateException("批量任务 解析作业缺少 taskId");
            }
            ExecutionContext jobExecutionContext = jobExecution.getExecutionContext();
            action.execute(taskId, jobExecutionContext);
            return RepeatStatus.FINISHED;
        };
    }

    @FunctionalInterface
    private interface ParseStepAction {
        void execute(Long taskId, ExecutionContext jobExecutionContext);
    }
}
