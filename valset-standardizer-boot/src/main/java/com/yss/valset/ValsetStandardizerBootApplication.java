package com.yss.valset;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 主题匹配服务的应用程序引导程序。
 */
@EnableAsync
@EnableDiscoveryClient
@EnableAspectJAutoProxy
@EnableScheduling
@SpringBootApplication(scanBasePackages = {"com.yss"})
public class ValsetStandardizerBootApplication {
    /**
     * 启动Spring应用程序。
     */
    public static void main(String[] args) {
        SpringApplication.run(ValsetStandardizerBootApplication.class, args);
    }
}
