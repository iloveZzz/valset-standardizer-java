package com.yss.valset;

import com.yss.cloud.EnableDistributedId;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 外部估值标准化服务的应用程序
 */
@EnableAsync
@EnableDiscoveryClient
@EnableAspectJAutoProxy
@EnableDistributedId
@EnableScheduling
@EnableFeignClients(basePackages = "com.yss.valset.workflow.dolphinscheduler")
@SpringBootApplication(scanBasePackages = {"com.yss.cloud","com.yss.datamiddle","com.yss.valset"})
public class ValsetStandardizerBootApplication {
    /**
     * 启动Spring应用程序。
     */
    public static void main(String[] args) {
        SpringApplication.run(ValsetStandardizerBootApplication.class, args);
    }
}
