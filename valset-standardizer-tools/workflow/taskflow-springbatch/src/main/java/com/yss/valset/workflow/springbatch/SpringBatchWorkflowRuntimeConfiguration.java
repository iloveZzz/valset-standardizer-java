package com.yss.valset.workflow.springbatch;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobOperator;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.item.database.support.DataFieldMaxValueIncrementerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.sql.DataSource;

/**
 * 批量任务 工作流运行时配置。
 *
 * <p>
 * 这里负责提供 批量任务 运行所需的最小基础设施，
 * 让估值表解析作业完全依赖数据库版 Batch 元数据表，而不是内存仓库。
 * </p>
 *
 * <p>
 * 这些 Bean 主要分成四类：
 * </p>
 * <ul>
 *     <li>作业仓库：基于 MySQL Batch 元数据表创建和组织作业对象。</li>
 *     <li>作业查询器：直接读取数据库中的执行记录，用于页面查询和重启判断。</li>
 *     <li>作业启动器和操作器：负责提交、停止、重启等运行态控制。</li>
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

    /**
     * 仅提供数据库版作业仓库，直接使用 Batch 元数据表。
     *
     * <p>
     * 这是整个重构的基础，后续 Job/Step/Execution 都由这里的仓库统一持久化。
     * </p>
     */
    @Bean(name = "springBatchJobRepository")
    @Primary
    public JobRepository springBatchJobRepository(DataSource dataSource,
                                                  @Qualifier("springBatchIncrementerFactory") DataFieldMaxValueIncrementerFactory incrementerFactory,
                                                  @Qualifier("springBatchTransactionManager") PlatformTransactionManager transactionManager) throws Exception {
        JobRepositoryFactoryBean factoryBean = new JobRepositoryFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setTransactionManager(transactionManager);
        factoryBean.setTablePrefix("BATCH_");
        factoryBean.setIncrementerFactory(incrementerFactory);
        // Oracle 在批量重试/并发触发时，默认的 ISOLATION_SERIALIZABLE 容易在 Batch 元数据写入阶段触发 ORA-08177。
        // 这里仅降低创建 JobExecution 的事务隔离级别，避免 BATCH_JOB_EXECUTION_PARAMS 插入互相冲突。
        factoryBean.setIsolationLevelForCreate("ISOLATION_READ_COMMITTED");
        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }

    /**
     * 批量任务 元数据主键统一走分布式号段，避免默认序列表回退到 0。
     */
    @Bean(name = "springBatchIncrementerFactory")
    @Primary
    public DataFieldMaxValueIncrementerFactory springBatchIncrementerFactory() {
        return new SpringBatchSegmentContextIncrementerFactory();
    }

    /**
     * 批作业查询器直接读取数据库中的执行元数据。
     *
     * <p>
     * 页面查询、任务详情和重启判断都依赖这个查询器回放执行状态。
     * </p>
     */
    @Bean(name = "springBatchJobExplorer")
    @Primary
    public JobExplorer springBatchJobExplorer(DataSource dataSource) throws Exception {
        JobExplorerFactoryBean factoryBean = new JobExplorerFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setTablePrefix("BATCH_");
        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }

    /**
     * 统一使用同步执行器，便于内部工作流在当前线程中直接推进。
     *
     * <p>
     * 这样可以让任务提交后立刻进入 Step 执行，减少额外线程带来的上下文同步成本。
     * </p>
     */
    @Bean
    public TaskExecutor springBatchTaskExecutor() {
        return new SyncTaskExecutor();
    }

    /**
     * 作业启动器负责把作业提交给上面的执行器。
     */
    @Bean(name = "springBatchJobLauncher")
    @Primary
    public JobLauncher springBatchJobLauncher(@Qualifier("springBatchJobRepository") JobRepository jobRepository,
                                              TaskExecutor springBatchTaskExecutor) throws Exception {
        SimpleJobLauncher launcher = new SimpleJobLauncher();
        launcher.setJobRepository(jobRepository);
        launcher.setTaskExecutor(springBatchTaskExecutor);
        launcher.afterPropertiesSet();
        return launcher;
    }

    /**
     * 作业操作器提供停止和重启等运行态控制能力。
     *
     * <p>
     * 迁移期保留这层控制器，方便页面上的停止、补跑和重试动作直接映射到 批量任务。
     * </p>
     */
    @Bean(name = "springBatchJobOperator")
    @Primary
    public JobOperator springBatchJobOperator(@Qualifier("springBatchJobLauncher") JobLauncher jobLauncher,
                                              @Qualifier("springBatchJobExplorer") JobExplorer jobExplorer,
                                              @Qualifier("springBatchJobRepository") JobRepository jobRepository,
                                              @Qualifier("springBatchJobRegistry") JobRegistry jobRegistry) throws Exception {
        SimpleJobOperator operator = new SimpleJobOperator();
        operator.setJobLauncher(jobLauncher);
        operator.setJobExplorer(jobExplorer);
        operator.setJobRepository(jobRepository);
        operator.setJobRegistry(jobRegistry);
        operator.afterPropertiesSet();
        return operator;
    }

    /**
     * 作业注册表用于按名称管理动态作业定义。
     */
    @Bean(name = "springBatchJobRegistry")
    @Primary
    public JobRegistry springBatchJobRegistry() {
        return new MapJobRegistry();
    }

    /**
     * 事务管理器直接绑定主数据源，保证 Step 写入可回滚。
     */
    @Bean
    @Primary
    public PlatformTransactionManager springBatchTransactionManager(DataSource dataSource) {
        return new org.springframework.jdbc.datasource.DataSourceTransactionManager(dataSource);
    }
}
