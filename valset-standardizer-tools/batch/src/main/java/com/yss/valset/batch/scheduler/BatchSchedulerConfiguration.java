package com.yss.valset.batch.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 任务调度器配置。
 */
@Configuration
public class BatchSchedulerConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "db-scheduler", name = "enabled", havingValue = "false")
    public SchedulerService noopSchedulerService() {
        return new SchedulerService() {
            @Override
            public void triggerNow(Long taskId) {
            }
        };
    }
}
