package com.yss.valset.workflow.springbatch;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.ResourcelessJobRepository;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring Batch 工作流运行时配置。
 */
@Configuration(proxyBeanMethods = false)
@ComponentScan(basePackageClasses = SpringBatchWorkflowRuntimeConfiguration.class)
public class SpringBatchWorkflowRuntimeConfiguration {

    @Bean
    public JobRepository jobRepository() {
        return new ResourcelessJobRepository();
    }

    @Bean
    public TaskExecutor springBatchTaskExecutor() {
        return new SyncTaskExecutor();
    }

    @Bean
    public JobLauncher jobLauncher(JobRepository jobRepository, TaskExecutor springBatchTaskExecutor) throws Exception {
        TaskExecutorJobLauncher launcher = new TaskExecutorJobLauncher();
        launcher.setJobRepository(jobRepository);
        launcher.setTaskExecutor(springBatchTaskExecutor);
        launcher.afterPropertiesSet();
        return launcher;
    }

    @Bean
    public JobRegistry jobRegistry() {
        return new MapJobRegistry();
    }

    @Bean
    public PlatformTransactionManager springBatchTransactionManager() {
        return new ResourcelessTransactionManager();
    }
}
