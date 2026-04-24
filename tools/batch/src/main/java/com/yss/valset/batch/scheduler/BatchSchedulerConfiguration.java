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

            @Override
            public void scheduleCron(Long scheduleId, String scheduleKey, String cronExpression) {
            }

            @Override
            public void pause(String scheduleKey) {
            }

            @Override
            public void resume(String scheduleKey) {
            }

            @Override
            public void delete(String scheduleKey) {
            }
        };
    }
}
