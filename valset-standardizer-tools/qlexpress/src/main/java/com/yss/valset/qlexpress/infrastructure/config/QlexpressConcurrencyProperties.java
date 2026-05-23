package com.yss.valset.qlexpress.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 解析任务并发配置。
 */
@Component
@ConfigurationProperties(prefix = "subject.match.parse.concurrent")
public class QlexpressConcurrencyProperties {

    /**
     * 允许同时处理的解析任务数量。
     */
    private int concurrency = 5;

    /**
     * 解析任务线程池等待队列长度。
     */
    private int queueCapacity = 100;

    /**
     * 线程回收等待时间。
     */
    private int keepAliveSeconds = 120;

    public int getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(int concurrency) {
        this.concurrency = concurrency;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public int getKeepAliveSeconds() {
        return keepAliveSeconds;
    }

    public void setKeepAliveSeconds(int keepAliveSeconds) {
        this.keepAliveSeconds = keepAliveSeconds;
    }
}
