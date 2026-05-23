package com.yss.valset.task.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 解析任务异步执行器配置。
 */
@Configuration(proxyBeanMethods = false)
public class TaskAsyncConfiguration {

    @Bean(name = "parseTaskExecutor")
    public Executor parseTaskExecutor(
            @Value("${subject.match.parse.concurrent.concurrency:5}") int concurrency,
            @Value("${subject.match.parse.concurrent.queue-capacity:100}") int queueCapacity,
            @Value("${subject.match.parse.concurrent.keep-alive-seconds:120}") int keepAliveSeconds) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int effectiveConcurrency = Math.max(1, concurrency);
        executor.setCorePoolSize(effectiveConcurrency);
        executor.setMaxPoolSize(effectiveConcurrency);
        executor.setQueueCapacity(Math.max(0, queueCapacity));
        executor.setKeepAliveSeconds(Math.max(1, keepAliveSeconds));
        executor.setThreadNamePrefix("parse-task-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
