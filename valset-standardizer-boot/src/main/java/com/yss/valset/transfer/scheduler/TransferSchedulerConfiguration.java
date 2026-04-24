package com.yss.valset.transfer.scheduler;

import com.yss.valset.transfer.application.port.TransferJobScheduler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * 文件分拣调度器配置。
 */
@Configuration
public class TransferSchedulerConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "db-scheduler", name = "enabled", havingValue = "false")
    public TransferJobScheduler noopTransferJobScheduler() {
        return new TransferJobScheduler() {
            @Override
            public String triggerIngest(String sourceId, String sourceType, String sourceCode, Map<String, Object> parameters, String ingestLockToken) {
                return null;
            }

            @Override
            public void scheduleIngestCron(String sourceId, String sourceType, String sourceCode, Map<String, Object> parameters, String cronExpression) {
            }

            @Override
            public void unscheduleIngest(String sourceId) {
            }

            @Override
            public void triggerRoute(String transferId) {
            }

            @Override
            public void triggerDeliver(String routeId, String transferId) {
            }

            @Override
            public void scheduleDeliverRetry(String routeId, String transferId, int retryCount, int delaySeconds) {
            }
        };
    }
}
