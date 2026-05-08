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
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Spring Batch 工作流运行时配置。
 *
 * <p>
 * 这里负责提供 Spring Batch 运行所需的最小基础设施：
 * </p>
 * <ul>
 *     <li>作业仓库：用于创建和组织作业对象，但不依赖外部数据库。</li>
 *     <li>作业启动器：负责提交作业执行。</li>
 *     <li>作业注册表：用于按名称注册和复用动态构建的作业。</li>
 *     <li>事务管理器：用于步骤执行时的事务边界控制。</li>
 * </ul>
 *
 * <p>
 * 所有 bean 都使用带前缀的名称，避免与 Spring Boot Batch 自动配置的默认 bean 重名。
 * </p>
 */
@Configuration(proxyBeanMethods = false)
@ComponentScan(basePackageClasses = SpringBatchWorkflowRuntimeConfiguration.class)
public class SpringBatchWorkflowRuntimeConfiguration {

    /** 仅提供最小化作业仓库，不依赖数据库。 */
    @Bean(name = "springBatchJobRepository")
    @Primary
    public JobRepository springBatchJobRepository() {
        return new ResourcelessJobRepository();
    }

    /** 统一使用同步执行器，便于内部工作流在当前线程中直接推进。 */
    @Bean
    public TaskExecutor springBatchTaskExecutor() {
        return new SyncTaskExecutor();
    }

    /** 作业启动器负责把作业提交给上面的执行器。 */
    @Bean(name = "springBatchJobLauncher")
    @Primary
    public JobLauncher springBatchJobLauncher(@Qualifier("springBatchJobRepository") JobRepository jobRepository,
                                              TaskExecutor springBatchTaskExecutor) throws Exception {
        TaskExecutorJobLauncher launcher = new TaskExecutorJobLauncher();
        launcher.setJobRepository(jobRepository);
        launcher.setTaskExecutor(springBatchTaskExecutor);
        launcher.afterPropertiesSet();
        return launcher;
    }

    /** 作业注册表用于按名称管理动态作业定义。 */
    @Bean(name = "springBatchJobRegistry")
    @Primary
    public JobRegistry springBatchJobRegistry() {
        return new MapJobRegistry();
    }

    /** 事务管理器只负责步骤内的事务边界，不依赖外部资源。 */
    @Bean
    public PlatformTransactionManager springBatchTransactionManager() {
        return new ResourcelessTransactionManager();
    }
}
